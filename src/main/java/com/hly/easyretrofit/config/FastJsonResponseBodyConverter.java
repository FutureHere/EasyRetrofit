package com.hly.easyretrofit.config;

import com.alibaba.fastjson.JSON;
import com.kk.tool.util.KLog;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Converter;

/**
 * Created by My Zhihua on 2016/4/19.
 * Description : 转换服务器返回数据的类
 */
public class FastJsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Type type;

    public FastJsonResponseBodyConverter(Type type) {
        this.type = type;
    }

    /*
    * 转换方法
    */
    @Override
    public T convert(ResponseBody value) throws IOException {
        BufferedSource bufferedSource = Okio.buffer(value.source());
        String tempStr = bufferedSource.readUtf8();
        KLog.e("------------json:" + tempStr);
        bufferedSource.close();
        if (tempStr.trim().startsWith("[")) {   //个别接口传的是jsonarray，服务器历史问题
            tempStr = "{\"array\":" + tempStr + "}";
        }
        try {
            return JSON.parseObject(tempStr, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
