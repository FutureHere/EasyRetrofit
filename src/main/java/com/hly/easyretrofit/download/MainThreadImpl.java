package com.hly.easyretrofit.download;


import android.os.Handler;
import android.os.Looper;

/**
 * Created by hly on 16/9/26.
 * email hly910206@gmail.com
 */

public class MainThreadImpl implements IMainThread {

    private MainThreadImpl() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    private Handler mHandler;
    private static MainThreadImpl sMMainThreadImpl = new MainThreadImpl();

    public static MainThreadImpl getMainThread() {
        return sMMainThreadImpl;
    }

    @Override
    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}
