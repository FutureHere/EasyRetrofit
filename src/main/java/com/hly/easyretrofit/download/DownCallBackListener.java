package com.hly.easyretrofit.download;

import com.hly.easyretrofit.download.db.DownLoadEntity;

/**
 * Created by hly on 2016/10/31.
 * email hly910206@gmail.com
 */

public class DownCallBackListener implements DownLoadTaskListener {
    private MainThreadImpl mMainThread = MainThreadImpl.getMainThread();


    private DownLoadBackListener mBackListener;
    long mTotalSize;
    long mHasDownSize;

    private boolean isReturnStart;
    private boolean isReturnErr;
    private boolean isReturnCancel;

    public DownCallBackListener(DownLoadBackListener backListener, long totalSize, long hasDownSize) {
        mBackListener = backListener;
        mTotalSize = totalSize;
        mHasDownSize = hasDownSize;
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
    public synchronized void onError(final DownLoadEntity downLoadEntity, final Throwable throwable) {
        if (!isReturnErr) {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mBackListener.onError(downLoadEntity,throwable);
                }
            });
        }
        isReturnErr = true;
    }
}
