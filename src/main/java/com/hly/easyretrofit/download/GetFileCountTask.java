package com.hly.easyretrofit.download;

import android.text.TextUtils;

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

    GetFileCountTask(Call<ResponseBody> responseCall, GetFileCountListener getFileCountListener) {
        mResponseCall = responseCall;
        mGetFileCountListener = getFileCountListener;
    }

    @Override
    public void run() {
        Response response = null;
        try {
            response = mResponseCall.execute();
            if (response.isSuccessful()) {
                if (mGetFileCountListener != null) {
                    mGetFileCountListener.success((!TextUtils.isEmpty(response.headers().get("Content-Range")) && !TextUtils.isEmpty(response.headers().get("Content-Length"))), response.code() != 206, response.headers().get("Last-Modified"), Long.parseLong(response.headers().get("Content-Range").split("/")[1]));
                }
            } else {
                if (mGetFileCountListener != null) {
                    mGetFileCountListener.failed();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if (mGetFileCountListener != null) {
                mGetFileCountListener.failed();
            }
        } finally {
            if (response.body() != null) {
                ((ResponseBody) response.body()).close();
            }
        }
    }
}
