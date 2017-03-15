##前言
首先这篇文章是面向对Retrofit有了解的朋友，如果您对Retrofit并不了解，请自行查阅其用法，本文不会讲解Retrofit的基础用法。
写这篇文章的目的很简单：
1.为了让自己回忆一下（代码半年前就完成了），看是否有改进的地方。
2.如果能帮到有同样需求的朋友，那是再好不过的。
3.如果大家对文章有不同意见之处，本人表示200%的欢迎提议。

这次封装实现的功能：
1.抽离网络层为module，实现即插即用。
2.统一网络层入口，统一实现方法。
3.支持网络请求缓存，自动添加删除缓存，也可以手动cancel请求。

######本文是基于Retrofit2.1版本,高于此版本在使用思想方法上应该不会有任何问题。

##正题
![最终的版本应该是这样的，请忽略download包，这是实现了断点续传和多线程下载的功能](http://upload-images.jianshu.io/upload_images/3376157-34d472b1a1793d16.jpeg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

###一、初始化
网络层的总入口，在这里进行Retrofit的初始化，配置。包括网络请求的调用方法，当然，他一定是单列的。
很简单，根据你app的需求，自行配置相关方法，相信用过Retrofit的朋友这部分不会陌生，所以这里不做解释。
```
public NetWorkRequest init(Context context, String baseURL) {    
this.mContext = context;    
synchronized (NetWorkRequest.this) {        
mOkHttpClient = new OkHttpClient.Builder()
.cache(new Cache(new File(context.getExternalCacheDir(), "http_cache"), 1024 * 1024 * 100))                
.readTimeout(15, TimeUnit.SECONDS) 
.writeTimeout(10, TimeUnit.SECONDS) 
.connectTimeout(10, TimeUnit.SECONDS)
.addInterceptor(new CommonInterceptor())
.addInterceptor(new LoggingInterceptor())
.cookieJar(new CookieManager())
.authenticator(new AuthenticatorManager())
.build();       
 mRetrofit = new Retrofit.Builder()
.addConverterFactory(FastJsonConverterFactory.create())
.baseUrl(baseURL)//主机地址                
.client(mOkHttpClient)                
.build();   
 }   
 return this;
}
```
第三行的synchronized关键字是为了保证初始化线程安全的。Retrofit是利用接口和注释生成网络请求的，这句话你一定不陌生：
`ApiService mApiService = mRetrofit.create(ApiService.class);`
因为我们现在将网络层抽离为module，是由我们的app项目引用的。所以我们需要在代码中提供create方法：
`public <T> T create(Class<T> tClass) { 
     return mRetrofit.create(tClass);
}`
这样我们的主项目就可以拿到我们的APIService来创建我们的网络请求Call<T>了。思路很清晰，在主项目中执行网络层的初始化方法，并利用create方法获取Api实例，创建接口对应的Call对象。我们在主项目中，创建一个单例类，ApiManager，部分代码如下:
```
public void init(Context context) {  
  NetWorkRequest.getInstance().init(context, AppConfig.APP_ROOT_URL);    
  mApiService = NetWorkRequest.getInstance().create(ApiService.class);
}
public ApiService getApiService() {   
  return mApiService;
}
```
在主项目的Application的onCreate方法中初始化ApiManager：
`ApiManager.getInstance().init(this);`
初始化部分我们就搞定了，是不是很简单。顺便说一下：因为我们的NetWorkRequest是单列的，所以这里的Context一定要是Application的Context,以防内存回收造成的泄漏问题。

###二、调用方法封装
先上代码:
```
public <T extends BaseResponseEntity> void asyncNetWork(final String TAG, final int requestCode, final Call<T> requestCall, final KKNetworkResponse<T> responseListener) { 
1.   Call<T> call;  
      if (requestCall.isExecuted()) {  
       call = requestCall.clone();  
      } else {     
       call = requestCall;  
     }    
2.  addCall(TAG, requestCode, call);   
    call.enqueue(new Callback<T>() {   
     @Override       
    public void onResponse(Call<T> call, Response<T> response) {            
3.    cancelCall(TAG, requestCode);           
      if (response.isSuccessful()) {    
         T result = response.body();  
4.         if (result == null) {
             responseListener.onDataError(requestCode, ""); 
             return;                
         }        
5.       result.requestCode = requestCode;     
         result.serverTip = response.message(); 
         result.responseCode = response.code();
         responseListener.onDataReady(result);  
       } else {  
       responseListener.onDataError(requestCode, NetErrCodeConfig.getErrString(mContext, response.code())); 
       }       
  }     
     @Override     
     public void onFailure(Call<T> call, Throwable t) { 
        cancelCall(TAG, requestCode);
        responseListener.onDataError(requestCode, NetErrCodeConfig.getErrString(mContext, t));
        }  
     });
}
```
async顾名思义是异步的意思，方法中四个参数逐一解释一下：
TAG：开始说到设计思路要缓存网络请求，你可以把TAG当做不同Activity的网络请求区分字段。

requestCode:上面把TAG比作一个Activity,那requestCode就是同一个页面的不同请求，为了区分并发回调判断是哪个接口。那怎么做缓存呢，思路很简单了，用Map！
`private Map<String, Map<Integer, Call>> mRequestMap = new ConcurrentHashMap<>();`
我们使用ConcurrentHashMap实现，为了保证数据的安全性。key即是TAG，value是另一个map集合，requestCode即是key，相信看到这里已经很清楚了。结合代码看第2、3标注部分看分别是加入、清除缓存，至于怎么实现，就不贴代码了，都是很基础的东西，对map进行增和删。

requestCall：即是retrofit为我们创建的call对象，对应ApiManager.getInstance().getApiService().xxxx();

KKNetworkResponse<T>:
![KKNetworkResponse](http://upload-images.jianshu.io/upload_images/3376157-a6f74b4348b57f7a.jpeg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)可以看到这里使用了泛型，范围是继承BaseResponseEntity的类，解释一下为什么这么做：
我们可能会遇到这样的情况，一个页面同时会并发多个网络请求，每个接口我们都调用asyncNetWork()方法，如果不使用内部类传递KKNetworkResponse对象，我们可能会这么做:
`public class WaitWeightIPresenter implements WaitWeightContract.IPresenter, NetworkResponse<PadUserWeighingGetUserDataResponseArgs> {`
使用类实现KKNetworkResponse接口，然后重写KKNetworkResponse中的方法，在asyncNetWork()中传入this对象像这样：
```@Override
public void onDataReady(BaseResponseEntity response) {
    ViewUtils.closeLoadingDialog();
    switch (response.requestCode) {
        case HttpConstants.HTTP_GET_MY_DIET_DETAIL:
            if (mIMyDietView != null){
               mIMyDietView.onGetedMyDietData((ResponseMyDietEntity) response);
            } 
            break;
        case HttpConstants.HTTP_POST_EVERYDAY_DIET_DETAIL:
            if (mIMyDietView != null) {
              mIMyDietView.onSubmitedMealData((ResponseUpLoadDietPlanEntity) response);
            }
            break;
        case HttpConstants.HTTP_GET_EVERYMONTH_DIET_STATUS:  
            if (mIMyDietView != null) {
              mIMyDietView.onGetedDietMonthData((DietCalendarEntity) response);
            }
            break;
    }
}
```
我们知道Retrofit的response对象是在Api接口中定义好的，接口成功返回后会自动反序列化得到response bean。我们如何在多个response中区别是哪个接口返回的数据呢，你不可能要求后台大哥哥们单独为你在每个返回对象中加个接口参数吧，所以这就需要我们自己去做。代码中的
`switch (response.requestCode) {`
requestCode是不是很眼熟？这不就是asyncNetWork()中的参数吗？看asyncNetWork中的代码，第5部分，我们自己赋值给response这样回调中就可以通过requestCode来判断是哪个接口了，回调实体BaseResponseEntity直接向下强转T类型，注意这里的向下转型是安全的，response此时已经是子类的引用。
至于你都可以在BaseResponse中存放什么字段？基于你们项目的规范，放一些共有的属性，比如responseCode，serverTip等等。（建议这里跟后台好好商量，合理的公有属性设定可以减少客户端很多的工作）
整体的逻辑大概就是这些了，最后我们来回顾一下到底做了些什么：
1.初始化Retrofit库
2.在主项目中获取retrofit对象，create ApiService
3.调用NetWorkRequest.getInstance().asyncNetWork()方法
4.对回调数据进行处理
网络缓存的缓存和清除都在asyncNetWork()中自动进行处理，当然你也可以在每个Activity的Destory方法中，手动清除当前TAG的所有请求，或者是哪个TAG的哪个request请求，都随你啦(当然这里建议统一在基类里处理)。
