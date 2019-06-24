package com.alibaba.nls.client;

import java.io.InputStream;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;

/**
 * @author zhishen.ml
 * @date 2018-06-12
 */
public class SpeechRecognizerDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;

    public SpeechRecognizerDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
        client = new NlsClient(accessToken);
    }

    public SpeechRecognizerDemo(String appKey, String token, String url) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
        client = new NlsClient(url, accessToken);
    }

    private static SpeechRecognizerListener getRecognizerListener() {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                //事件名称 RecognitionResultChanged
                System.out.println("name: " + response.getName() +
                    //状态码 20000000 表示识别成功
                    ", status: " + response.getStatus() +
                    //语音识别文本
                    ", result: " + response.getRecognizedText());
            }

            //识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                //事件名称 RecognitionCompleted
                System.out.println("name: " + response.getName() +
                    //状态码 20000000 表示识别成功
                    ", status: " + response.getStatus() +
                    //语音识别文本
                    ", result: " + response.getRecognizedText());
            }

            @Override
            public void onStarted(SpeechRecognizerResponse response) {
                System.out.println(
                    "task_id: " + response.getTaskId());
            }

            @Override
            public void onFail(SpeechRecognizerResponse response) {
                System.out.println(
                    "task_id: " + response.getTaskId() +
                        //状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        //错误信息
                        ", status_text: " + response.getStatusText());

            }
        };
        return listener;
    }

    public void process(InputStream ins) {
        SpeechRecognizer recognizer = null;
        try {
            //创建实例,建立连接
            recognizer = new SpeechRecognizer(client, getRecognizerListener());
            recognizer.setAppKey(appKey);
            //设置音频编码格式
            recognizer.setFormat(InputFormatEnum.PCM);
            //设置音频采样率
            recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            //设置是否返回中间识别结果
            recognizer.setEnableIntermediateResult(true);

            //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            recognizer.start();
            //语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
            recognizer.send(ins, 3200, 100);
            //通知服务端语音数据发送完毕,等待服务端处理完成
            recognizer.stop();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            //关闭连接
            if (null != recognizer) {
                recognizer.close();
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
        SpeechRecognizerDemo demo = null;
        if (args.length == 2) {
            appKey = args[0];
            token = args[1];
            //default url is wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1
            demo = new SpeechRecognizerDemo(appKey, token);
        } else if (args.length == 3) {
            appKey = args[0];
            token = args[1];
            url = args[2];
            demo = new SpeechRecognizerDemo(appKey, token, url);
        } else {
            System.err.println("SpeechRecognizerDemo need params(url is optional): " +
                "<app-key> <token> [<url>]");
            System.exit(-1);
        }
        InputStream ins = SpeechRecognizerDemo.class.getResourceAsStream("/nls-sample-16k.wav");
        if (null == ins) {
            System.err.println("open the audio file failed!");
            System.exit(-1);
        }
        demo.process(ins);
        demo.shutdown();
    }

}
