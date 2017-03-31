## 使用教程

### 一、初始化
    在Applcation里面初始化ApiManager:
    ```ApiManager.getInstance().init(this);```<br><br>
    ApiManager是连接easyretrofit与app的中间类，建议直接拷贝代码至项目中类
  
### 二、使用

  ```NetWorkRequest.getInstance().asyncNetWork(tag, 1, ApiManager.getInstance().getApiService().login(), new NetworkResponse<ResponseLoginEntity>() {
            @Override
            public void onDataReady(ResponseLoginEntity response) {
                
            }

            @Override
            public void onDataError(int requestCode, int responseCode, String message) {

            }
        });```
        
