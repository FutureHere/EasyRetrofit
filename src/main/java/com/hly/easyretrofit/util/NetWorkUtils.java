package com.hly.easyretrofit.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


/**
 * Created on 16-5-3.
 *
 * @author guoning
 */
public class NetWorkUtils {

    /**
     * 获取当前网络类型
     *
     * @return 0：未连接   1：WIFI网络   2：Mobile网络    3：ETHERNET网络
     */
    public static final int NETTYPE_WIFI = 1;
    public static final int NETTYPE_MOBILE = 2;


    public static boolean isNetConnect(Context context) {
        int net_status = getNetworkType(context);
        if (net_status == 0) {
            return false;
        }
        return true;
    }


    public static int getNetworkType(Context context) {
        int netType = 0;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            netType = NETTYPE_MOBILE;
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = NETTYPE_WIFI;
        }
        return netType;
    }
}
