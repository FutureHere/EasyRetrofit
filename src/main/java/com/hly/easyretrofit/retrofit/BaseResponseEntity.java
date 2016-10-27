package com.hly.easyretrofit.retrofit;

/**
 * Created by guoning on 16/5/19.
 */
public class BaseResponseEntity {
    /**
     * 接口请求requestCode,用于区分多个请求同时发起的情况
     */
    public int requestCode;

    /**
     * 接口请求返回的状态码
     */
    public int responseCode;

    /**
     * 接口请求返回的提示信息
     */
    public String serverTip;
}
