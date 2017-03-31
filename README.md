## 使用教程

### 一、初始化
    在Applcation里面初始化ApiManager:
    ```ApiManager.getInstance().init(this);```<br><br>
    ApiManager是连接easyretrofit与app的中间类，建议直接拷贝代码至项目中类
  
### 二、使用
    NetWorkRequest.getInstance().asyncNetWork(tag, requestCode, ApiManager.getInstance().getApiService().login(), 
                   new NetworkResponse<ResponseLoginEntity>() {
            @Override
            public void onDataReady(ResponseLoginEntity response) {
                
            }

            @Override
            public void onDataError(int requestCode, int responseCode, String message) {

            }
    });
   <br>参数解析:
   <br>* tag String类型用于区分不用页面的请求
   <br>* requestCode int类型 用于区分相同页面的不通请求
   <br>* Call<T> Retrofit动态代理 泛型为返回实体 必须继承BaseResponseEntity 
   <br>* NetworkResponse<T> 接口回调 泛型为返回实体 必须继承BaseResponseEntity（如有特殊需要可修改源码）
   
   NetWorkRequest.getInstance().cancelCall(String TAG, Integer code)
   取消某一TAG的某一请求<br>
   NetWorkRequest.getInstance().cancelTagCall(String TAG)}
   取消某一TAG所有请求
