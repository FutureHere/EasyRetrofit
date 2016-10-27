# EasyRetrofit
an encapsulation for Retrofit2.1

Its a module needed to be dependence from your main app  

write it to your app gradle like this:
dependencies {
    compile project(':network')
}


##useage
InitIialize the KKNetWorkRequest:

KKNetWorkRequest.getInstance().init(Context context, String baseURL);

and invoke KKNetWorkRequest.getInstance().create() initIialize your apiService;

Suggestion:
do the two step in a single instance;


KKNetWorkRequest.getInstance().asyncNetWork(String TAG, int requestCode, Call<T> requestCall, KKNetworkResponse<T> responseListener)

TAG:To part each Activity or Fragment

requestCode:part each request

Call<T>:retrofit call

KKNetworkResponse: callback


