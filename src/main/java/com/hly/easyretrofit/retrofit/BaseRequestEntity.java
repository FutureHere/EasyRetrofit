package com.hly.easyretrofit.retrofit;

/**
 * Created by hly on 16/7/27.
 * email hugh_hly@sina.cn
 */
public class BaseRequestEntity {
    private String tag;
    private int requestCode;
    private KKNetworkResponse kKNetworkResponse;

    public BaseRequestEntity(String tag, int requestCode, KKNetworkResponse kKNetworkResponse) {
        this.tag = tag;
        this.requestCode = requestCode;
        this.kKNetworkResponse = kKNetworkResponse;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public KKNetworkResponse getkKNetworkResponse() {
        return kKNetworkResponse;
    }

}
