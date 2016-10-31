package com.hly.easyretrofit.download;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by hly on 16/6/15.
 * email hugh_hly@sina.cn
 */
public class GetFileCountTask implements Runnable {
    private Call<ResponseBody> mResponseCall;

    private GetFileCountListener mGetFileCountListener;

    public GetFileCountTask(Call<ResponseBody> responseCall, GetFileCountListener getFileCountListener) {
        mResponseCall = responseCall;
        mGetFileCountListener = getFileCountListener;
    }

    @Override
    public void run() {
        try {
            Response response = mResponseCall.execute();
            if (response.isSuccessful()) {
                if (mGetFileCountListener != null) {
                    mGetFileCountListener.success(Long.parseLong(response.headers().get("Content-Range").split("/")[1]));
                }
            } else {
                if (mGetFileCountListener != null) {
                    mGetFileCountListener.failed();
                }
            }
            if (response.body() != null) {
                ((ResponseBody) response.body()).close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
