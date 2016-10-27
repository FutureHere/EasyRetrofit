package com.hly.easyretrofit.config;



import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by hly on 16/4/1.
 * email hugh_hly@sina.cn
 *
 * Modify by guoning, delete local tag.
 */
public class AuthenticatorManager implements Authenticator {
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        System.out.println("Authenticating for response: " + response);
        System.out.println("Challenges: " + response.challenges());
        if (responseCount(response) >= 3) {
            return null; // If we've failed 3 times, give up.
        }

//        String userName = PreferencesUtils.get(PreferencesUtils.LOGIN_NAME,"").toString();
//        String passWord = PreferencesUtils.get(PreferencesUtils.LOGIN_PSW,"").toString();
//
//        String www_Authenticate = response.headers().get("WWW-Authenticate");
//
//        String toMd5 = (www_Authenticate + userName + AuthUtils
//                .base64Encode(passWord.getBytes())).replace("\n", "");
//
//        String authString = AuthUtils.base64Encode(AuthUtils
//                .md5Digest32(toMd5).getBytes());
//        authString = authString.replace("\n", "");
//        String authenticate = "user=\"" + userName + "\",response=\"" + authString + "\"";

        return response.request().newBuilder()
//                .header("Authorization", authenticate)
                .build();
    }

    /**
     * 重复请求次数限制
     *
     * @param response
     * @return
     */
    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
