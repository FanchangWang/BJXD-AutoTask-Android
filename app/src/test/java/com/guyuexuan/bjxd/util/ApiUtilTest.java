
package com.guyuexuan.bjxd.util;

import com.google.gson.JsonObject;
import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class ApiUtilTest {

    private final String token = "北京现代账号 token";

    @Test
    public void getUserInfo_success() throws IOException {
        User user = ApiUtil.getUserInfo(token);

        System.out.println(user);

        assertNotNull(user);
    }

    @Test
    public void getScore_success() throws IOException {
        JsonObject data = ApiUtil.getScore(token);

        System.out.println(data);

        assertNotNull(data);
    }

    @Test
    public void getTaskStatus_success() throws IOException {
        TaskStatus data = ApiUtil.getTaskStatus(token);

        System.out.println(data);

        assertNotNull(data);
    }

    @Test
    public void getQuestionInfo_success() throws IOException {
        JsonObject data = ApiUtil.getQuestionInfo(token);

        System.out.println(data);

        assertNotNull(data);
    }

    @Test
    public void askAI_success() throws IOException {
        String aiApiKey = "智谱 AI API Key";
        String aiRequestUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        String aiModel = "glm-4.5-flash";
        String aiRequestParams = "{\"do_sample\": false}";
        String question = "Q：冬季常有雨雪天气，沐飒的哪种驾驶模式可以通过控制变速箱，降低扭矩的输出，让车辆在冰雪路面上起步更平稳，减少轮胎打滑现象\nA. 经济模式\nB. 运动模式\nC. 雪地模式\nD. 舒适模式";

        String data = ApiUtil.askAI(aiApiKey,aiRequestUrl,aiModel,aiRequestParams,question);

        System.out.println(data);
    }

}
