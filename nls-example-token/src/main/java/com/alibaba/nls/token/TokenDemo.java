package com.alibaba.nls.token;

import com.alibaba.nls.client.AccessToken;

public class TokenDemo {

    public static void requestTokenTest(String akId, String akSecret) {
        try {
            System.out.println("akid = " + akId + ", secret = " + akSecret);
            AccessToken accessToken = new AccessToken(akId, akSecret);

            // 请求token
            accessToken.apply();
            System.out.println("Created token: " + accessToken.getToken() +
                // 有效时间，单位为秒
                ", expire time(s): " + accessToken.getExpireTime());
            if(!accessToken.getToken().isEmpty()) {
                long expireTime = accessToken.getExpireTime();
                System.err.println("token 过期时间点 ： " + expireTime);
                long now = System.currentTimeMillis() / 1000;
                long expirePeriodSecond = expireTime - now;
                System.err.println("token 剩余有效时间: " + expirePeriodSecond);

                // TODO 重要提示： 请务必在有效时间(expirePeriodSecond) 过期之前重新获取token，否则token失效，进而无法调用各类语音服务接口
                // 比如：
                //if(expirePeriodSecond <= 0) {
                //    /// 重新获取
                //}
            } else {

            }

            Thread.sleep(3000);

            // 第二次请求token
            accessToken.apply();
            System.out.println("Created token: " + accessToken.getToken() +
                // 有效时间，单位为秒
                ", expire time(s): " + accessToken.getExpireTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String akId = ""; //"输入你的阿里云AccessKey ID";
        String akSecret = ""; // "输入你的阿里云AccessKey Secret";
        if(akId.isEmpty() || akSecret.isEmpty()){
            System.err.println("akId or akSecret must not be empty!");
            System.exit(-1);
        }

        TokenDemo.requestTokenTest(akId, akSecret);
        System.out.println("----\n\n");

        SpeechTokenGeneratorDemo generator = new SpeechTokenGeneratorDemo(akId, akSecret);
        generator.start();

        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //requestTokenTest(akId, akSecret);

        System.out.println("### Game Over ###");
    }

}
