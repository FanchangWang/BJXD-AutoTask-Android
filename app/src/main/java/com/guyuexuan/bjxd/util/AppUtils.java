package com.guyuexuan.bjxd.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.guyuexuan.bjxd.R;


public class AppUtils {
    public static String getAppNameWithVersion(Context context) {
        String versionName = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // 忽略解析异常
        }
        return context.getString(R.string.app_name) + " v" + versionName;
    }
}
