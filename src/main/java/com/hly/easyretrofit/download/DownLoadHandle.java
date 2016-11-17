package com.hly.easyretrofit.download;

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
            final DownLoadEntity downLoadEntity = (DownLoadEntity) iterator.next();
            downLoadEntity.downed = 0;
            List<DownLoadEntity> dataList = mDownLoadDatabase.query(downLoadEntity.url);
            if (dataList.size() > 0) {//说明下载过
                File file = new File(downLoadEntity.saveName);
                if (file.exists()) {
                    //文件存在 下载剩余
                    downLoadEntity.multiList = dataList;
                    Iterator dataIterator = dataList.iterator();
                    while (dataIterator.hasNext()) {
                        DownLoadEntity dataEntity = (DownLoadEntity) dataIterator.next();
                        downLoadEntity.total = dataEntity.total;
                        downLoadEntity.downed += dataEntity.downed;
                    }
                } else {
                    //文件不存在 删除数据库 重新下载
                    mDownLoadDatabase.deleteAllByUrl(downLoadEntity.url);
                    downLoadEntity.total = dataList.get(0).total;
                }
                setCount();
            } else {
                //数据库中没记录 说明是新任务 获取文件长度
               final Call<ResponseBody> mResponseCall = NetWorkRequest.getInstance().getDownLoadService().downloadFile(downLoadEntity.url, "bytes=" + 0 + "-" + 0);
                GetFileCountListener mGetFileCountListener = new GetFileCountListener() {
                    int reCount = 3;

                    @Override
                    public void success(Long fileSize) {
                        setCount();
                        downLoadEntity.total = fileSize;
                    }

                    @Override
                    public void failed() {
                        if (reCount <= 0) {
                            if (!mGetFileService.isShutdown()) {
                                mGetFileService.shutdownNow();
                            }
                        } else {
                            reCount--;
                            executeGetFileWork(mResponseCall, this);
                        }
                    }
                };
                executeGetFileWork(mResponseCall, mGetFileCountListener);
            }
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
}
