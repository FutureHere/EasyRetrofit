package com.hly.easyretrofit.download;

import com.hly.easyretrofit.download.db.DownLoadDatabase;
import com.hly.easyretrofit.download.db.DownLoadEntity;
import com.hly.easyretrofit.util.CommonUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Created by hly on 2016/10/29.
 * email hly910206@gmail.com
 */

class DownLoadRequest {

    private MainThreadImpl mMainThread = MainThreadImpl.getMainThread();

    // 下载线程池
    private ExecutorService mDownLoadService;

    //多线程下载文件最低大小10mb
    private long mMultiLine = 10 * 1024 * 1024;

    private final long NEW_DOWN_BEGIN = 0;

    private DownLoadBackListener mCallBackListener;

    private DownLoadDatabase mDownLoadDatabase;

    //URL下载Task
    private Map<String, Map<Integer, Future>> mUrlTaskMap = new ConcurrentHashMap<>();

    //下载的任务
    private List<DownLoadEntity> mList;

    //总回调
    private DownCallBackListener mDownCallBackListener;

    private DownLoadHandle mDownLoadHandle;

    DownLoadRequest(DownLoadDatabase downLoadDatabase, DownLoadBackListener callBackListener, List<DownLoadEntity> list, long multiLine) {
        mDownLoadDatabase = downLoadDatabase;
        mDownLoadHandle = new DownLoadHandle(downLoadDatabase);
        mCallBackListener = callBackListener;
        mList = list;
        mMultiLine = multiLine;
        mDownLoadService = Executors.newFixedThreadPool(CommonUtils.getNumCores() + 1);
    }

    void start() {
        //下面是一个耗时操作
        List<DownLoadEntity> queryList = mDownLoadHandle.queryDownLoadData(mList);
        Iterator iterator = queryList.iterator();
        long totalFileSize = 0;
        long hasDownSize = 0;
        while (iterator.hasNext()) {
            final DownLoadEntity downLoadEntity = (DownLoadEntity) iterator.next();
            hasDownSize += downLoadEntity.downed;
            if (downLoadEntity.total == 0) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallBackListener.onError(downLoadEntity, new Throwable("文件读取失败"));
                    }
                });
                return;
            } else {
                totalFileSize += downLoadEntity.total;
            }
        }
        if (hasDownSize >= totalFileSize) {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mCallBackListener.onCompleted();
                }
            });
            return;
        }
        mDownCallBackListener = new DownCallBackListener(mCallBackListener, totalFileSize, hasDownSize);
        mDownCallBackListener.onStart();
        for (DownLoadEntity entity : mList) {
            if (entity.downed == entity.total) {
                continue;
            }
            addDownLoadTask(entity);
        }
    }

    private void addDownLoadTask(DownLoadEntity downLoadEntity) {
        Map<Integer, Future> downLoadTaskMap = new ConcurrentHashMap<>();
        MultiDownLoaderListener multiDownLoaderListener = new MultiDownLoaderListener(mDownCallBackListener);
        if (downLoadEntity.multiList != null && downLoadEntity.multiList.size() != 0) {
            for (int i = 0; i < downLoadEntity.multiList.size(); i++) {
                DownLoadEntity entity = downLoadEntity.multiList.get(i);
                //当前分支是否下载完成
                if (entity.downed + entity.start > entity.end) {
                    continue;
                }
                DownLoadTask downLoadTask = new DownLoadTask.Builder().downLoadModel(entity).downLoadTaskListener(multiDownLoaderListener).build();
                executeNetWork(entity, downLoadTask, downLoadTaskMap);
            }
        } else {
            //文件不存在 直接下载
            createDownLoadTask(downLoadEntity, NEW_DOWN_BEGIN, downLoadTaskMap, multiDownLoaderListener);
        }
    }

    /**
     * 创建下载任务
     *
     * @param beginSize            开始位置
     * @param downLoadTaskMap      当前url本地缓存
     * @param downLoadTaskListener
     */
    private void createDownLoadTask(DownLoadEntity downLoadEntity, long beginSize, final Map<Integer, Future> downLoadTaskMap, DownLoadTaskListener downLoadTaskListener) {
        long startSize, endSize;

        DownLoadTask downLoadTask;
        if (mMultiLine != 0 && downLoadEntity.total > mMultiLine) {
            //多线程下载
            int threadNum = (int) ((downLoadEntity.total % mMultiLine == 0) ? downLoadEntity.total / mMultiLine : downLoadEntity.total / mMultiLine + 1);

            for (int i = 0; i < threadNum; i++) {
                startSize = beginSize + i * mMultiLine;
                endSize = startSize + mMultiLine - 1;
                if (i == threadNum - 1) {
                    if (endSize > downLoadEntity.total) {
                        endSize = downLoadEntity.total - 1;
                    }
                }
                DownLoadEntity entity = mDownLoadDatabase.insert(downLoadEntity.url, (int) startSize, (int) endSize, (int) downLoadEntity.total, downLoadEntity.saveName);
                downLoadTask = new DownLoadTask.Builder().downLoadModel(entity).downLoadTaskListener(downLoadTaskListener).build();
                executeNetWork(entity, downLoadTask, downLoadTaskMap);
            }
        } else {
            //单线程下载
            DownLoadEntity entity = mDownLoadDatabase.insert(downLoadEntity.url, 0, (int) downLoadEntity.total - 1, (int) downLoadEntity.total, downLoadEntity.saveName);
            downLoadTask = new DownLoadTask.Builder().downLoadModel(entity).downLoadTaskListener(downLoadTaskListener).build();
            executeNetWork(entity, downLoadTask, downLoadTaskMap);
        }
    }

    //加入下载线程池
    private void executeNetWork(DownLoadEntity entity, DownLoadTask downLoadTask, Map<Integer, Future> map) {
        map.put(entity.dataId, mDownLoadService.submit(downLoadTask));
        mUrlTaskMap.put(entity.url, map);
    }

    /**
     * 子任务回调
     */
    private class MultiDownLoaderListener implements DownLoadTaskListener {

        private DownLoadTaskListener downLoadTaskListener;
        //重复次数
        private int repeatCount = 10;

        MultiDownLoaderListener(DownLoadTaskListener downLoadTaskListener) {
            this.downLoadTaskListener = downLoadTaskListener;
        }

        @Override
        public synchronized void onDownLoading(long downSize) {
            downLoadTaskListener.onDownLoading(downSize);
        }

        @Override
        public synchronized void onStart() {
            downLoadTaskListener.onStart();
        }

        @Override
        public synchronized void onCancel(DownLoadEntity downLoadEntity) {
            mDownLoadDatabase.update(downLoadEntity);
        }

        @Override
        public synchronized void onCompleted(DownLoadEntity downLoadEntity) {
            mDownLoadDatabase.update(downLoadEntity);
            if (!isRepeatExecute(downLoadEntity, repeatCount, this)) {
                if (removeTask(downLoadEntity)) {
                    downLoadTaskListener.onCompleted(downLoadEntity);
                }
            } else {
                repeatCount--;
            }
        }

        @Override
        public synchronized void onError(DownLoadEntity downLoadEntity, Throwable throwable) {
            mDownLoadDatabase.update(downLoadEntity);
            if (!isRepeatExecute(downLoadEntity, repeatCount, this)) {
                if (repeatCount <= 0) {
                    downLoadTaskListener.onError(downLoadEntity, throwable);
                }
            } else {
                repeatCount--;
            }
        }
    }

    //移除map记录 只有在完成后移除
    private boolean removeTask(DownLoadEntity downLoadEntity) {
        Map<Integer, Future> map = mUrlTaskMap.get(downLoadEntity.url);
        if (map.isEmpty()) {
            return true;
        }
        if (map.containsKey(downLoadEntity.dataId)) {
            map.remove(downLoadEntity.dataId);
        }
        if (map.size() == 0) {
            mUrlTaskMap.remove(downLoadEntity.url);
        }
        return mUrlTaskMap.size() == 0;
    }


    //取消当前url所有任务
    private void cancel(String url) {
        Map<Integer, Future> downLoadMap = mUrlTaskMap.get(url);
        if (downLoadMap != null && downLoadMap.size() > 0) {
            Iterator iterator = downLoadMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Future> map = (Map.Entry<Integer, Future>) iterator.next();
                Future future = map.getValue();
                future.cancel(true);
                iterator.remove();
            }
            mUrlTaskMap.remove(url);
        }
    }

    //取消所有任务
    public void stop() {
        if (mUrlTaskMap.size() != 0) {
            Iterator iterator = mUrlTaskMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                cancel(key);
                iterator.remove();
            }
        }
    }

    //判断是否下载完整
    private boolean isRepeatExecute(DownLoadEntity downLoadEntity, int repeatCount, DownLoadTaskListener loadTaskListener) {
        if ((downLoadEntity.downed + downLoadEntity.start <= downLoadEntity.end) && repeatCount > 0) {
            //没下载完
            DownLoadTask downLoadTask = new DownLoadTask.Builder().downLoadModel(downLoadEntity).downLoadTaskListener(loadTaskListener).build();
            executeNetWork(downLoadEntity, downLoadTask, mUrlTaskMap.get(downLoadEntity.url));
            return true;
        }
        return false;
    }
}
