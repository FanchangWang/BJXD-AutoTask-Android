package com.guyuexuan.bjxd.sync

import com.guyuexuan.bjxd.model.User
import com.guyuexuan.bjxd.util.ApiUtil
import com.guyuexuan.bjxd.util.StorageUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * 青龙账号双向同步。
 *
 * 顺序：
 * 1. 调用接口验证青龙账号，清理失效账号；
 * 2. 青龙有本地没有 → 导入本地；两边都有 → 按更新时间取较新的 Token；
 * 3. **先把合并结果写入本地**（本地数据优先保证正确）；
 * 4. 最后尽力写入青龙：每个账号的云端操作最多尝试 3 次，间隔 5 秒，失败则跳过继续下一个。
 */
class QinglongSync(
    private val storageUtil: StorageUtil,
    private val logger: (String) -> Unit,
) {
    /** failed 记录写入云端失败的账号名（BJXD 编号），本地数据不受影响 */
    data class SyncResult(val failed: List<String>)


    companion object {
        /** 5 分钟内的差异视为同一时间，避免 59 秒 / 下一分钟 0 秒这种边界误判 */
        private const val TIME_EPSILON_MS = 5 * 60 * 1000L
        private const val SEND_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 5_000L
    }

    private data class CloudAccount(
        val token: String,
        val phone: String,
        val nickname: String,
        val hid: String,
        val expireAt: Long,
    )

    fun run(): SyncResult {
        val failed = mutableListOf<String>()
        // 校验时遇到网络/解析异常的账号：保留云端原变量，绝不当作失效删除
        val protectedEnvIds = mutableSetOf<Long>()
        val users = storageUtil.userList
        val baseUrl = storageUtil.qinglongRequestUrl
        val clientId = storageUtil.qinglongClientId
        val clientSecret = storageUtil.qinglongClientSecret

        val qlToken = ApiUtil.getQinglongToken(baseUrl, clientId, clientSecret)
        val pattern = Pattern.compile("^BJXD(\\d+)$")
        val envs = ApiUtil.getQinglongEnvList(baseUrl, qlToken, "BJXD")
            .filter { it.name()?.let { name -> pattern.matcher(name).matches() } == true }

        logger("云端 BJXD 账号 ${envs.size} 个，本地账号 ${users.size} 个")

        // 1. 验证青龙账号，收集有效账号
        val cloud = mutableListOf<CloudAccount>()
        for (env in envs) {
            val name = env.name()
            val token = env.value()
            // 先从 JWT 解析过期时间和 user_hid，省掉大部分接口调用
            val expireAt = User.parseJwtLong(token, "exp") * 1000L
            val hid = User.parseJwtHid(token)
            // exp 解析成功且确实过期才判失效；解析失败不能当作过期，改走接口实测
            if (expireAt > 0L && System.currentTimeMillis() > expireAt) {
                logger("✗ $name 已过期，将从云端清除")
                continue
            }
            val local = users.firstOrNull { it.hid == hid }
            if (local != null) {
                cloud.add(
                    CloudAccount(
                        token = token,
                        phone = local.phone,
                        nickname = local.nickname,
                        hid = local.hid,
                        expireAt = expireAt,
                    )
                )
                logger("✓ $name ${mask(local.phone)} 有效（本地匹配，未调用接口）")
            } else {
                // 本地没有该账号，才调接口获取昵称和手机号以便导入
                try {
                    val info = ApiUtil.getUserInfo(token)
                    cloud.add(
                        CloudAccount(
                            token = token,
                            phone = info.phone,
                            nickname = info.nickname,
                            hid = info.hid,
                            expireAt = expireAt,
                        )
                    )
                    logger("✓ $name ${mask(info.phone)} 有效")
                } catch (e: Exception) {
                    val message = e.message.orEmpty()
                    if (message.contains("403") || message.contains("过期") || message.contains("失效")) {
                        logger("✗ $name 已失效，将从云端清除：$message")
                    } else {
                        // 网络抖动 / 服务端错误 / 解析异常：保留云端账号，不删除
                        protectedEnvIds.add(env.id())
                        failed.add(name.orEmpty())
                        logger("! $name 校验失败，保留云端账号：$message")
                    }
                }
            }
        }

        val cloudByPhone = cloud.associateBy { it.phone }
        val localByPhone = users.associateBy { it.phone }

        // 2. 合并：青龙独有账号排前面，本地账号保持原顺序
        val finalUsers = mutableListOf<User>()
        for (c in cloud.filter { it.phone !in localByPhone }) {
            val imported = User(c.token, c.nickname, c.phone, c.hid)
            finalUsers.add(imported)
            logger("↓ 青龙账号 ${mask(c.phone)} 导入本地")
        }

        for (user in users) {
            val cloudAccount = cloudByPhone[user.phone]
            if (cloudAccount == null) {
                finalUsers.add(user)
                continue
            }
            // 用 token 的 exp 判断哪个有效期更长；解析失败的一方视为 0，不覆盖对方
            val diff = cloudAccount.expireAt - user.tokenExpireAt
            when {
                diff > TIME_EPSILON_MS -> {
                    finalUsers.add(
                        User(
                            cloudAccount.token,
                            cloudAccount.nickname,
                            cloudAccount.phone,
                            cloudAccount.hid
                        )
                    )
                    logger("↺ ${mask(user.phone)} 青龙 Token 有效期更长，已覆盖本地")
                }

                diff < -TIME_EPSILON_MS -> {
                    finalUsers.add(user)
                    logger("↑ ${mask(user.phone)} 本地 Token 有效期更长，将覆盖青龙")
                }

                else -> {
                    finalUsers.add(user)
                    logger("= ${mask(user.phone)} 双方 Token 有效期相同，保持现有 Token")
                }
            }
        }

        // 3. 先写本地：统一把更新时间设为当前，随后写入的青龙 updatedAt 也是当前，
        //    下次同步时两者差值在 5 分钟内，不会重复覆盖。
        val now = formatLocalTime(System.currentTimeMillis())
        finalUsers.forEach { it.setAddedTime(now) }
        storageUtil.saveUserList(finalUsers)
        logger("本地账号已更新为 ${finalUsers.size} 个，开始写入青龙")

        // 4. 最后尽力写云端：每个账号最多 3 次尝试，失败则跳过继续下一个
        for (env in envs) {
            if (env.id() in protectedEnvIds) continue
            val name = env.name().orEmpty()
            val ok = retrySend("删除云端 $name") {
                ApiUtil.deleteQinglongEnv(baseUrl, qlToken, env.id())
            }
            if (!ok) {
                failed.add(name)
                logger("跳过删除 $name，继续下一个账号")
            }
        }

        finalUsers.forEachIndexed { index, user ->
            val name = "BJXD${index + 1}"
            val ok = retrySend("写入 $name") {
                ApiUtil.createQinglongEnv(
                    baseUrl,
                    qlToken,
                    name,
                    user.token,
                    "BJXD ${user.maskedPhone}"
                ) != null
            }
            if (ok) {
                logger("$name ${user.maskedPhone} 已写入青龙")
            } else {
                failed.add(name)
                logger("$name 写入失败（已尝试 $SEND_ATTEMPTS 次），跳过继续下一个账号")
            }
        }

        return SyncResult(failed = failed.distinct())
    }

    /** 云端操作最多尝试 3 次，每次间隔 5 秒；action 返回 false 也视为失败并重试 */
    private fun retrySend(actionName: String, action: () -> Boolean): Boolean {
        for (attempt in 1..SEND_ATTEMPTS) {
            var failed = false
            try {
                if (action()) return true
                failed = true
                logger("$actionName 第 $attempt 次返回失败")
            } catch (e: Exception) {
                failed = true
                logger("$actionName 第 $attempt 次失败：${e.message ?: "未知错误"}")
            }
            if (failed && attempt < SEND_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
        }
        return false
    }

    private fun formatLocalTime(epoch: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epoch))
    }

    private fun mask(phone: String): String {
        return if (phone.length == 11) {
            phone.substring(0, 3) + "******" + phone.substring(9)
        } else {
            phone
        }
    }
}
