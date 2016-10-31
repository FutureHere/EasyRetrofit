package com.hly.easyretrofit.download;


import android.util.Log;

import com.hly.easyretrofit.download.db.DownLoadEntity;
import com.hly.easyretrofit.retrofit.KKNetWorkRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by hly on 16/4/11.
 * email hugh_hly@sina.cn
 */
public class DownLoadTask implements Runnable {

    private final String TAG = DownLoadTask.class.getSimpleName();

    private int mTaskId;

    private String mSaveFileName;

    private DownLoadTaskListener mDownLoadTaskListener;

    private DownLoadService mApiService = KKNetWorkRequest.getInstance().getDownLoadService();

    private Call<ResponseBody> mResponseCall;

    private long mFileSizeDownloaded;

    private DownLoadEntity mDownLoadEntity;

    private long mNeedDownSize;

    private final long CALL_BACK_LENGTH = 1024 * 1024;

    DownLoadTask(int taskId, DownLoadEntity downLoadEntity, DownLoadTaskListener downLoadTaskListener) {
        this.mDownLoadEntity = downLoadEntity;
        this.mDownLoadTaskListener = downLoadTaskListener;
        this.mSaveFileName = downLoadEntity.saveName;
        this.mTaskId = taskId;
        this.mNeedDownSize = downLoadEntity.end - (downLoadEntity.start + downLoadEntity.downed);
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        if (mDownLoadEntity.downed != 0) {
            mResponseCall = mApiService.downloadFile(mDownLoadEntity.url, "bytes=" + (mDownLoadEntity.downed + mDownLoadEntity.start) + "-" + mDownLoadEntity.end);
        } else {
            mResponseCall = mApiService.downloadFile(mDownLoadEntity.url, "bytes=" + mDownLoadEntity.start + "-" + mDownLoadEntity.end);
        }
        ResponseBody result = null;
        try {
            Response response = mResponseCall.execute();
            //onStart();
            result = (ResponseBody) response.body();
            if (response.isSuccessful()) {
                if (writeToFile(result, mDownLoadEntity.start, mDownLoadEntity.downed)) {
                    onCompleted();
                }
            } else {
                onError(new Throwable(response.message()));
            }
        } catch (IOException e) {
            onError(new Throwable(e.getMessage()));
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    private boolean writeToFile(ResponseBody body, long startSet, long mDownedSet) {
        try {
            File futureStudioIconFile = new File(mSaveFileName);

            if (!futureStudioIconFile.exists()) {
                futureStudioIconFile.createNewFile();
            }

            RandomAccessFile oSavedFile = new RandomAccessFile(futureStudioIconFile, "rw");

            oSavedFile.seek(startSet + mDownedSet);

            InputStream inputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                mFileSizeDownloaded = 0;

                inputStream = body.byteStream();

                while (mFileSizeDownloaded < mNeedDownSize) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    oSavedFile.write(fileReader, 0, read);

                    mFileSizeDownloaded += read;

                    mDownLoadEntity.downed += read;

                    if (mFileSizeDownloaded >= CALL_BACK_LENGTH) {
                        onDownLoading(mFileSizeDownloaded);
                        mNeedDownSize -= mFileSizeDownloaded;
                        mFileSizeDownloaded = 0;
                    } else {
                        if (mNeedDownSize < CALL_BACK_LENGTH) {
                            if (mFileSizeDownloaded - 1 == mNeedDownSize) {
                                onDownLoading(mFileSizeDownloaded);
                                break;
                            } else if (mFileSizeDownloaded - 1 > mNeedDownSize) {
                                onDownLoading(mNeedDownSize);
                                mDownLoadEntity.downed = (mDownLoadEntity.end - mDownLoadEntity.start) + 1;
                                break;
                            }
                        }
                    }
                }
                return true;
            } finally {
                Log.d("abc", Thread.currentThread().getState().toString());
                oSavedFile.close();

                if (inputStream != null) {
                    inputStream.close();
                }

                if (body != null) {
                    body.close();
                }
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                if (mFileSizeDownloaded > 0) {
                    onDownLoading(mFileSizeDownloaded);
                    mFileSizeDownloaded = 0;
                }
                onError(e);
            } else if (e instanceof InterruptedIOException) {
                onCancel();
            } else {
                if (mFileSizeDownloaded > 0) {
                    onDownLoading(mFileSizeDownloaded);
                    mFileSizeDownloaded = 0;
                }
                onError(e);
            }
            return false;
        }
    }

//    private void onStart() {
//        mDownLoadTaskListener.onStart();
//    }

    private void onCancel() {
        mResponseCall.cancel();
        mResponseCall = null;
        mDownLoadTaskListener.onCancel(mDownLoadEntity);
    }

    private void onCompleted() {
        mResponseCall = null;
        mDownLoadTaskListener.onCompleted(mDownLoadEntity);
    }

    private void onDownLoading(long downSize) {
        mDownLoadTaskListener.onDownLoading(downSize);
    }

    private void onError(Throwable throwable) {
        mDownLoadTaskListener.onError(mDownLoadEntity, throwable);
    }

    public static final class Builder {
        private DownLoadEntity mDownModel;

        private int mTaskId;

        private DownLoadTaskListener mDownLoadTaskListener;

        public Builder downLoadModel(DownLoadEntity downLoadEntity) {
            mDownModel = downLoadEntity;
            return this;
        }

        public Builder downLoadTaskListener(DownLoadTaskListener downLoadTaskListener) {
            mDownLoadTaskListener = downLoadTaskListener;
            return this;
        }

        public Builder taskId(int id) {
            this.mTaskId = id;
            return this;
        }

        public DownLoadTask build() {
            if (mDownModel.url.isEmpty()) {
                throw new IllegalStateException("DownLoad URL required.");
            }

            if (mDownLoadTaskListener == null) {
                throw new IllegalStateException("DownLoadTaskListener required.");
            }

            if (mDownModel.end == 0) {
                throw new IllegalStateException("SetCount required.");
            }

            if (mTaskId == 0) {
                throw new IllegalStateException("TaskId required.");
            }
            return new DownLoadTask(mTaskId, mDownModel, mDownLoadTaskListener);
        }
    }
}
