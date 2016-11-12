package com.hly.easyretrofit.download;


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

    private String mSaveFileName;

    private DownLoadTaskListener mDownLoadTaskListener;

    private Call<ResponseBody> mResponseCall;

    private long mFileSizeDownloaded;

    private DownLoadEntity mDownLoadEntity;

    private long mNeedDownSize;

    private final long CALL_BACK_LENGTH = 1024 * 1024;

    DownLoadTask(DownLoadEntity downLoadEntity, DownLoadTaskListener downLoadTaskListener) {
        this.mDownLoadEntity = downLoadEntity;
        this.mDownLoadTaskListener = downLoadTaskListener;
        this.mSaveFileName = downLoadEntity.saveName;
        this.mNeedDownSize = downLoadEntity.end - (downLoadEntity.start + downLoadEntity.downed);
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        if (mDownLoadEntity.downed != 0) {
            mResponseCall = KKNetWorkRequest.getInstance().getDownLoadService().downloadFile(mDownLoadEntity.url,
                    "bytes=" + (mDownLoadEntity.downed + mDownLoadEntity.start) + "-" + mDownLoadEntity.end);
        } else {
            mResponseCall = KKNetWorkRequest.getInstance().getDownLoadService().downloadFile(mDownLoadEntity.url,
                    "bytes=" + mDownLoadEntity.start + "-" + mDownLoadEntity.end);
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

                inputStream = body.byteStream();

                while (mFileSizeDownloaded < mNeedDownSize) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    oSavedFile.write(fileReader, 0, read);

                    mFileSizeDownloaded += read;

                    if (mFileSizeDownloaded >= CALL_BACK_LENGTH) {
                        onDownLoading(mFileSizeDownloaded);
                        mNeedDownSize -= mFileSizeDownloaded;
                        mFileSizeDownloaded = 0;
                    } else {
                        if (mNeedDownSize < CALL_BACK_LENGTH) {
                            if (mFileSizeDownloaded - 1 == mNeedDownSize) {
                                onDownLoading(mFileSizeDownloaded);
                                break;
                            }
                        }
                    }
                }
                return true;
            } finally {
                oSavedFile.close();

                if (inputStream != null) {
                    inputStream.close();
                }

                if (body != null) {
                    body.close();
                }
            }
        } catch (IOException e) {
            if (e instanceof InterruptedIOException && !(e instanceof SocketTimeoutException)) {
                onCancel();
            } else {
                onError(e);
            }
            return false;
        }
    }

    private void onStart() {
        mDownLoadTaskListener.onStart();
    }

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
        mDownLoadEntity.downed += downSize;
    }

    private void onError(Throwable throwable) {
        mDownLoadTaskListener.onError(mDownLoadEntity, throwable);
    }

    public static final class Builder {
        private DownLoadEntity mDownModel;

        private DownLoadTaskListener mDownLoadTaskListener;

        public Builder downLoadModel(DownLoadEntity downLoadEntity) {
            mDownModel = downLoadEntity;
            return this;
        }

        public Builder downLoadTaskListener(DownLoadTaskListener downLoadTaskListener) {
            mDownLoadTaskListener = downLoadTaskListener;
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
                throw new IllegalStateException("End required.");
            }

            return new DownLoadTask(mDownModel, mDownLoadTaskListener);
        }
    }
}
