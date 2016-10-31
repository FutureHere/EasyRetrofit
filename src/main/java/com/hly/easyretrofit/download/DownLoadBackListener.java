package com.hly.easyretrofit.download;

/**
 * Created by hly on 16/4/11.
 * email hugh_hly@sina.cn
 */
public interface DownLoadBackListener {
    void onStart(double percent);

    void onCancel();

    void onDownLoading(double percent);

    void onCompleted();

    void onError(Throwable throwable);
}
