package com.hly.easyretrofit.download;

import com.hly.easyretrofit.download.db.DownLoadDatabase;
import com.hly.easyretrofit.download.db.DownLoadEntity;
import com.hly.easyretrofit.retrofit.NetWorkRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hly on 2016/10/29.
 * email hly910206@gmail.com
 */

public class DownLoadManager {

    private DownLoadDatabase mDownLoadDatabase = new DownLoadDatabase(NetWorkRequest.getInstance().mContext);

    private ExecutorService mExecutorService = Executors.newCachedThreadPool();

    //多线程下载文件最低大小10mb
    private final long MULTI_LINE = 10 * 1024 * 1024;

    //所有下载Task
    private Map<String, DownLoadRequest> mDownLoadRequestMap = new ConcurrentHashMap<>();

    private DownLoadManager() {
    }

    private static class DownLoadManagerHolder {
        private static final DownLoadManager INSTANCE = new DownLoadManager();
    }

    public static final DownLoadManager getInstance() {
        return DownLoadManagerHolder.INSTANCE;
    }

    //默认支持多线程下载
    public void downLoad(final List<DownLoadEntity> list, final String tag, final DownLoadBackListener downLoadTaskListener) {
        downLoad(list, tag, downLoadTaskListener, MULTI_LINE);
    }


    public void downLoad(final List<DownLoadEntity> list, final String tag, final DownLoadBackListener downLoadTaskListener, final long multiLine) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                DownLoadRequest downLoadRequest = new DownLoadRequest(mDownLoadDatabase, downLoadTaskListener, list, multiLine);
                downLoadRequest.start();
                mDownLoadRequestMap.put(tag, downLoadRequest);
            }
        });
    }

    //默认支持多线程下载
    public void downLoad(final DownLoadEntity downLoadEntity, final String tag, final DownLoadBackListener downLoadTaskListener) {
        List<DownLoadEntity> list = new ArrayList<>();
        list.add(downLoadEntity);
        downLoad(list, tag, downLoadTaskListener, MULTI_LINE);
    }

    public void downLoad(final DownLoadEntity downLoadEntity, final String tag, final DownLoadBackListener downLoadTaskListener, final long multiLine) {
        List<DownLoadEntity> list = new ArrayList<>();
        list.add(downLoadEntity);
        downLoad(list, tag, downLoadTaskListener, multiLine);
    }

    //取消所有任务
    public void cancel() {
        if (!mDownLoadRequestMap.isEmpty()) {
            Iterator iterator = mDownLoadRequestMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                cancel(key);
            }
        }
    }

    //取消当前tag所有任务
    public void cancel(String tag) {
        if (!mDownLoadRequestMap.isEmpty()) {
            if (mDownLoadRequestMap.containsKey(tag)) {
                mDownLoadRequestMap.get(tag).stop();
                mDownLoadRequestMap.remove(tag);
            }
        }
    }
}
