package com.hly.easyretrofit.retrofit;


/**
 * Created by hly on 16/3/30.
 * email hugh_hly@sina.cn
 */
public interface NetworkResponse<T extends BaseResponseEntity> {
    /**
     * 服务器返回成功回调
     *
     * @param response 网络请求返信息
     */
    void onDataReady(T response);

    /**
     * 调用失败回调
     */
    void onDataError(int requestCode, int responseCode, String message);
}
