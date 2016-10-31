package com.hly.easyretrofit.download;


import com.hly.easyretrofit.download.db.DownLoadDatabase;
import com.hly.easyretrofit.download.db.DownLoadEntity;
import com.hly.easyretrofit.retrofit.KKNetWorkRequest;
import com.hly.easyretrofit.util.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Created by hly on 16/4/11.
 * email hugh_hly@sina.cn
 */
public class DownLoadManager {

    private final String TAG = DownLoadManager.class.getSimpleName();

    // 获取文件大小的线程池
    private static ExecutorService mGetFileCountService;

    //所有下载Task
    private Map<String, DownLoadRequest> mTaskMap = new ConcurrentHashMap<>();

    private DownLoadDatabase mDownLoadDatabase = new DownLoadDatabase(KKNetWorkRequest.getInstance().mContext);

    private DownLoadManager() {
    }

    private static DownLoadManager sInstance = new DownLoadManager();

    private MainThreadImpl mMainThread = MainThreadImpl.getMainThread();

    public static DownLoadManager getInstance() {
        init();
        return sInstance;
    }

    private static synchronized void init() {
        if (mGetFileCountService == null || mGetFileCountService.isShutdown()) {
            mGetFileCountService = Executors.newFixedThreadPool(CommonUtils.getNumCores() + 1);
        }
    }

    //取消所有任务
    public void cancelAll() {
        if (!mTaskMap.isEmpty()) {
            Iterator iterator = mTaskMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, DownLoadRequest> entry = (Map.Entry<String, DownLoadRequest>) iterator.next();
                cancelTag(entry.getKey());
            }
        }
        releaseDatabase();
    }

    //取消当前tag所有任务
    public void cancelTag(String tag) {
        if (!mTaskMap.isEmpty()) {
            if (mTaskMap.containsKey(tag)) {
                mTaskMap.get(tag).stop();
                mTaskMap.remove(tag);
            }
        }
    }

    //释放数据库
    private synchronized void releaseDatabase() {
        if (mDownLoadDatabase != null) {
            mDownLoadDatabase.destroy();
            mDownLoadDatabase = null;
        }
    }

    /**
     * @param list
     * @param downLoadTaskListener 页面的回调
     */
    public synchronized void addDownLoadTask(final List<DownLoadEntity> list, final String tag, final DownLoadBackListener downLoadTaskListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DownLoadEntity> mList = queryDownLoadData(list);
                Iterator iterator = mList.iterator();
                DownCallBackListener urlListener = new DownCallBackListener(downLoadTaskListener);
                long totalFileSize = 0;
                long hasDownSize = 0;
                while (iterator.hasNext()) {
                    DownLoadEntity downLoadEntity = (DownLoadEntity) iterator.next();
                    hasDownSize += downLoadEntity.downed;
                    if (downLoadEntity.total == 0) {
                        mMainThread.post(new Runnable() {
                            @Override
                            public void run() {
                                downLoadTaskListener.onError(new Throwable("文件读取失败"));
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
                            downLoadTaskListener.onCompleted();
                        }
                    });
                    return;
                }
                urlListener.setTotalSize(totalFileSize);
                urlListener.setHasDownSize(hasDownSize);
                urlListener.onStart();
                DownLoadRequest downLoadRequest = new DownLoadRequest(mDownLoadDatabase, urlListener, mList);
                mTaskMap.put(tag, downLoadRequest);
                downLoadRequest.start();
            }
        }).start();

    }

    public synchronized void addDownLoadTask(DownLoadEntity downLoadEntity, String tag, DownLoadBackListener downLoadTaskListener) {
        List<DownLoadEntity> list = new ArrayList<>();
        list.add(downLoadEntity);
        addDownLoadTask(list, tag, downLoadTaskListener);
    }

    //汇总所有下载信息
    private synchronized List<DownLoadEntity> queryDownLoadData(List<DownLoadEntity> list) {
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            final DownLoadEntity downLoadEntity = (DownLoadEntity) iterator.next();
            List<DownLoadEntity> dataList = mDownLoadDatabase.query(downLoadEntity.url);
            if (dataList.size() > 0) {
                File file = new File(downLoadEntity.saveName);
                if (file.exists()) {
                    //文件存在 下载剩余
                    downLoadEntity.multiList = dataList;
                    Iterator dataIterator = dataList.iterator();
                    while (dataIterator.hasNext()) {
                        DownLoadEntity dataEntity = (DownLoadEntity) dataIterator.next();
                        downLoadEntity.downed += dataEntity.downed;
                        downLoadEntity.total = dataEntity.total;
                    }
                } else {
                    //文件不存在 删除数据库 重新下载
                    mDownLoadDatabase.deleteAllByUrl(downLoadEntity.url);
                    downLoadEntity.total = dataList.get(0).total;
                }
            } else {
                final Call<ResponseBody> mResponseCall = KKNetWorkRequest.getInstance().getDownLoadService().downloadFile(downLoadEntity.url, "bytes=" + 0 + "-" + 0);
                GetFileCountListener mGetFileCountListener = new GetFileCountListener() {
                    int reCount = 3;

                    @Override
                    public void success(Long fileSize) {
                        downLoadEntity.total = fileSize;
                    }

                    @Override
                    public void failed() {
                        if (reCount <= 0) {

                        } else {
                            executeGetFileWork(mResponseCall, this);
                            reCount--;
                        }
                    }
                };
                executeGetFileWork(mResponseCall, mGetFileCountListener);
            }
            if (!iterator.hasNext()) {
                mGetFileCountService.shutdown();
            }
        }
        while (!mGetFileCountService.isShutdown() || !mGetFileCountService.isTerminated()) {
        }
        return list;
    }

    private void executeGetFileWork(Call<ResponseBody> call, GetFileCountListener listener) {
        GetFileCountTask getFileCountTask = new GetFileCountTask(call, listener);
        mGetFileCountService.submit(getFileCountTask);
    }

    /**
     * 总回调代理
     */
    class DownCallBackListener implements DownLoadTaskListener {

        private DownLoadBackListener mBackListener;
        long mTotalSize;
        long mHasDownSize;

        private boolean isReturnStart = false;
        private boolean isReturnErr = false;
        private boolean isReturnCancel = false;

        public void setTotalSize(long totalSize) {
            this.mTotalSize = totalSize;
        }

        public void setHasDownSize(long hasDownSize) {
            this.mHasDownSize = hasDownSize;
        }

        public DownCallBackListener(DownLoadBackListener downLoadBackListener) {
            this.mBackListener = downLoadBackListener;
        }

        @Override
        public synchronized void onStart() {
            if (!isReturnStart) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mBackListener.onStart((double) mHasDownSize / mTotalSize);
                    }
                });
            }
            isReturnStart = true;
        }

        @Override
        public synchronized void onCancel(DownLoadEntity downLoadEntity) {
            if (!isReturnCancel) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mBackListener.onCancel();
                    }
                });
            }
            isReturnCancel = true;
        }

        @Override
        public synchronized void onDownLoading(long downSize) {
            mHasDownSize += downSize;
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mBackListener.onDownLoading((double) mHasDownSize / mTotalSize);
                }
            });
        }

        @Override
        public synchronized void onCompleted(DownLoadEntity downLoadEntity) {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mBackListener.onCompleted();
                }
            });
        }

        @Override
        public synchronized void onError(DownLoadEntity downLoadEntity, final Throwable throwable) {
            if (!isReturnErr) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mBackListener.onError(throwable);
                    }
                });
            }
            isReturnErr = true;
        }
    }
}
