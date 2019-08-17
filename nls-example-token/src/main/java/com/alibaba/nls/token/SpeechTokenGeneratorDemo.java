package com.alibaba.nls.token;

import com.alibaba.nls.client.AccessToken;

public class SpeechTokenGeneratorDemo {

    private String accessKeyId = null;

    private String accessKeySecret = null;

    private String token;

    public SpeechTokenGeneratorDemo(String id, String secret) {
        setAccessKeyParam(id, secret);
    }

    public void setAccessKeyParam(String id, String secret) {
        this.accessKeyId = id;
        this.accessKeySecret = secret;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            while(true){
                try {
                    if(accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
                        throw new Exception("accessKeyId or accessKeySecret is invalid!");
                    }

                    AccessToken accessToken = new AccessToken(accessKeyId, accessKeySecret);
                    accessToken.apply();
                    System.out.println("Created token: " + accessToken.getToken() +
                        // 有效时间，单位为秒
                        ", expire time(s): " + accessToken.getExpireTime());

                    if(!accessToken.getToken().isEmpty()) {
                        long expireTime = accessToken.getExpireTime();
                        long now = System.currentTimeMillis() / 1000;
                        long expirePeriodSecond = expireTime - now;

                        if (expirePeriodSecond > 3600 * 2) {
                            /// 比如可以比server端建议的时间提前2个小时
                            expirePeriodSecond = expirePeriodSecond - 3600 * 2;
                        }
                        Thread.sleep(expirePeriodSecond * 1000);
                        //Thread.sleep(10 * 1000);
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    public static void main(String[] args) throws InterruptedException {
        String akId = ""; //"输入你的阿里云AccessKey ID";
        String akSecret = ""; // "输入你的阿里云AccessKey Secret";
        if(akId.isEmpty() || akSecret.isEmpty()){
            System.err.println("akId or akSecret must not be empty!");
            System.exit(-1);
        }

        SpeechTokenGeneratorDemo demo = new SpeechTokenGeneratorDemo(akId, akSecret);
        demo.start();
        Thread.sleep(50000 * 1000);

    }
}
