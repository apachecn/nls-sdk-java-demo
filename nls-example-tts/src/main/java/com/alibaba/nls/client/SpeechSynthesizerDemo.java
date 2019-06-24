package com.alibaba.nls.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;

/**
 * @author zhishen.ml
 * @date 2018-06-12
 */
public class SpeechSynthesizerDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;

    public SpeechSynthesizerDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
        client = new NlsClient(accessToken);
    }

    public SpeechSynthesizerDemo(String appKey, String token, String url) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
        client = new NlsClient(url, accessToken);
    }

    private static SpeechSynthesizerListener getSynthesizerListener() {

        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {

                File f=new File("tts_test.wav");
                FileOutputStream fout = new FileOutputStream(f);
                //语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
                    System.out.println("name: " + response.getName() +
                        ", status: " + response.getStatus()+
                        ", output file :"+f.getAbsolutePath()
                    );
                }

                //语音合成的语音二进制数据
                @Override
                public void onMessage(ByteBuffer message) {
                    try {
                        byte[] bytesArray = new byte[message.remaining()];
                        message.get(bytesArray, 0, bytesArray.length);
                        System.out.println("write arrya:" + bytesArray.length);
                        fout.write(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFail(SpeechSynthesizerResponse response){
                    System.out.println(
                        "task_id: " + response.getTaskId() +
                            //状态码 20000000 表示识别成功
                            ", status: " + response.getStatus() +
                            //错误信息
                            ", status_text: " + response.getStatusText());
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listener;
    }

    public void process() {
        SpeechSynthesizer synthesizer = null;
        try {
            //创建实例,建立连接
            synthesizer = new SpeechSynthesizer(client, getSynthesizerListener());
            synthesizer.setAppKey(appKey);
            //设置返回音频的编码格式
            synthesizer.setFormat(OutputFormatEnum.WAV);
            //设置返回音频的采样率
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            //发音人
            synthesizer.setVoice("xiaoyun");
            //语调，范围是-500~500，可选，默认是0
            synthesizer.setPitchRate(100);
            //语速，范围是-500~500，默认是0
            synthesizer.setSpeechRate(100);
            //设置用于语音合成的文本
            synthesizer.setText("北京明天天气怎么样啊");

            //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            synthesizer.start();
            //等待语音合成结束
            synthesizer.waitForComplete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭连接
            if (null != synthesizer) {
                synthesizer.close();
            }
        }
    }

    public void shutdown() {
        client.shutdown();
    }

    public static void main(String[] args) throws Exception {
        String appKey = null;
        String token = null;
        String url = null;
        SpeechSynthesizerDemo demo =null;
        if (args.length == 2) {
            appKey = args[0];
            token = args[1];
            //default url is wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1
            demo = new SpeechSynthesizerDemo(appKey, token);
        }else if(args.length == 3){
            appKey = args[0];
            token = args[1];
            url = args[2];
            demo = new SpeechSynthesizerDemo(appKey, token, url);
        }else{
            System.err.println("SpeechSynthesizerDemo need params(url is optional): " +
                "<app-key> <token> [<url>]");
            System.exit(-1);
        }
        demo.process();
        demo.shutdown();
    }
}
