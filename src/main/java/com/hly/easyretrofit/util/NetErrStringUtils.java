package com.hly.easyretrofit.util;

import android.content.Context;

import com.hly.easyretrofit.R;


/**
 * Created by hly on 16/8/26.
 * email hly910206@gmail.com
 */
public class NetErrStringUtils {

    public static final int ERR_404 = 404;

    public static final int ERR_500 = 500;

    public static final int ERR_502 = 502;

    public static String getErrString(Context context, int code) {
        String result;
        switch (code) {
            case ERR_404:
                result = context.getString(R.string.err404);
                break;
            case ERR_500:
                result = context.getString(R.string.err500);
                break;
            case ERR_502:
                result = context.getString(R.string.err502);
                break;
            default:
                result = context.getString(R.string.errDefault);
                break;
        }
        return result;
    }

    public static String getErrString(Context context, Throwable t) {
        String result;
        if (t instanceof java.net.SocketTimeoutException) {
            result = context.getString(R.string.errTimeout);
        } else if (t != null && t.getMessage() != null && t.getMessage().equalsIgnoreCase("canceled")) {
            result = "";
        } else {
            result = context.getString(R.string.errDefault);
        }
        return result;
    }
}
