package com.hly.easyretrofit.config;

import com.hly.easyretrofit.retrofit.KKNetWorkRequest;
import com.hly.easyretrofit.util.NetWorkUtils;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

/**
 * Created by hly on 16/3/31.
 * email hugh_hly@sina.cn
 */
public class CommonInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder newBuilder = originalRequest.newBuilder();
        Request compressedRequest;
        if (!NetWorkUtils.isNetConnect(KKNetWorkRequest.getInstance().mContext)) {
            newBuilder.cacheControl(CacheControl.FORCE_CACHE);//从缓存中读取
        } else {
            newBuilder.cacheControl(CacheControl.FORCE_NETWORK);
        }

         newBuilder.header("User-Agent", "KKWeight_Android");
//
//        if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
//            newBuilder
//                    .header("User-Agent", "KKTablet/Android");
////                    .header("Content-Type", "application/x-www-form-urlencoded");
//        } else {
//            newBuilder.header("User-Agent", "KKTablet/Android")
////                    .header("Content-Type", "application/octet-stream")
//                    .header("Content-Encoding", "gzip")
//                    .method(originalRequest.method(), gzip(originalRequest.body()));
//        }

        compressedRequest = newBuilder.build();

        Response response = chain.proceed(compressedRequest);

        if (NetWorkUtils.isNetConnect(KKNetWorkRequest.getInstance().mContext)) {
            int maxAge = 60 * 60; // 有网络时 设置缓存超时时间一小时
            response = response.newBuilder()
                    .removeHeader("Pragma")
                    //清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                    .header("Cache-Control", "public, max-age=" + maxAge)//设置缓存超时时间
                    .build();
        } else {
            int maxStale = 60 * 5; // 无网络时，设置超时为5分钟
            response = response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                    //设置缓存策略，及超时策略
                    .build();
        }

//        String status = response.header("coach_status");
//
//        if (status != null) {
//            if (status.equals("2")) {//删除状态
//                CoachPadApp.getApp().mHandler.sendEmptyMessage(CoachPadApp.ERROR_COACH_DELETE);
//                return null;
//            } else if (status.equals("3")) {//锁定状态
//                CoachPadApp.getApp().mHandler.sendEmptyMessage(CoachPadApp.ERROR_COACH_LOCKED);
//                return null;
//            }
//        }


        return response;
    }

    private RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return body.contentType();
            }

            @Override
            public long contentLength() {
                return -1; // 无法知道压缩后的数据大小
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}
