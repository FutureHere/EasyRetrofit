package com.hly.easyretrofit.download;

import android.text.TextUtils;

import com.hly.easyretrofit.download.db.DownLoadDatabase;
import com.hly.easyretrofit.download.db.DownLoadEntity;
import com.hly.easyretrofit.retrofit.NetWorkRequest;
import com.hly.easyretrofit.util.CommonUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Created by hly on 2016/10/31.
 * email hly910206@gmail.com
 */

class DownLoadHandle {
    private DownLoadDatabase mDownLoadDatabase;

    private ExecutorService mGetFileService = Executors.newFixedThreadPool(CommonUtils.getNumCores() + 1);

    DownLoadHandle(DownLoadDatabase downLoadDatabase) {
        mDownLoadDatabase = downLoadDatabase;
    }

    private int mDownLoadCount;

    //汇总所有下载信息
    List<DownLoadEntity> queryDownLoadData(List<DownLoadEntity> list) {
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            DownLoadEntity downLoadEntity = (DownLoadEntity) iterator.next();
            downLoadEntity.downed = 0;
            Call<ResponseBody> mResponseCall = null;
            List<DownLoadEntity> dataList = mDownLoadDatabase.query(downLoadEntity.url);
            if (dataList.size() > 0) {
                if (!TextUtils.isEmpty(dataList.get(0).lastModify)) {
                    mResponseCall = NetWorkRequest.getInstance().getDownLoadService().getHttpHeaderWithIfRange(downLoadEntity.url, dataList.get(0).lastModify, "bytes=" + 0 + "-" + 0);
                }
            } else {
                mResponseCall = NetWorkRequest.getInstance().getDownLoadService().getHttpHeader(downLoadEntity.url, "bytes=" + 0 + "-" + 0);
            }
            executeGetFileWork(mResponseCall, new GetFileCount(downLoadEntity, mResponseCall));
        }
        while (!mGetFileService.isShutdown() && getCount() != list.size()) {

        }
        return list;
    }


    private void executeGetFileWork(Call<ResponseBody> call, GetFileCountListener listener) {
        GetFileCountTask getFileCountTask = new GetFileCountTask(call, listener);
        mGetFileService.submit(getFileCountTask);
    }

    private synchronized void setCount() {
        mDownLoadCount++;
    }

    private synchronized int getCount() {
        return mDownLoadCount;
    }

    private class GetFileCount implements GetFileCountListener {

        private DownLoadEntity mDownLoadEntity;

        private Call<ResponseBody> mResponseCall;


        public GetFileCount(DownLoadEntity downLoadEntity, Call<ResponseBody> responseCall) {
            mDownLoadEntity = downLoadEntity;
            mResponseCall = responseCall;
        }

        int reCount = 3;

        @Override
        public void success(boolean isSupportMulti, boolean isNew, String modified, Long fileSize) {
            mDownLoadEntity.total = fileSize;
            mDownLoadEntity.lastModify = modified;
            mDownLoadEntity.isSupportMulti = isSupportMulti;
            if (!isNew) {
                //未更换资源
                List<DownLoadEntity> dataList = mDownLoadDatabase.query(mDownLoadEntity.url);
                if (dataList.size() > 0) {//说明下载过
                    File file = new File(mDownLoadEntity.saveName);
                    if (file.exists()) {
                        //文件存在 下载剩余
                        mDownLoadEntity.multiList = dataList;
                        Iterator dataIterator = dataList.iterator();
                        while (dataIterator.hasNext()) {
                            DownLoadEntity dataEntity = (DownLoadEntity) dataIterator.next();
                            mDownLoadEntity.downed += dataEntity.downed;
                        }
                    } else {
                        //文件不存在 删除数据库 重新下载
                        mDownLoadDatabase.deleteAllByUrl(mDownLoadEntity.url);
                    }
                }
            } else {
                //更换资源，重新下载
                mDownLoadDatabase.deleteAllByUrl(mDownLoadEntity.url);
            }
            setCount();
        }

        @Override
        public void failed() {
            if (reCount <= 0) {
                setCount();
                if (!mGetFileService.isShutdown()) {
                    mGetFileService.shutdownNow();
                }
            } else {
                reCount--;
                executeGetFileWork(mResponseCall, this);
            }
        }
    }
}
