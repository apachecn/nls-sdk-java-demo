[![Build Status](https://travis-ci.com/lihangqi/nls-sdk-java-demo.svg?branch=master)](https://travis-ci.com/lihangqi/nls-sdk-java-demo)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f63ee8a318984e0c8223afdcfa3d53fe)](https://www.codacy.com/app/weslie0803/nls-sdk-java-demo?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=lihangqi/nls-sdk-java-demo&amp;utm_campaign=Badge_Grade)

# nls-sdk-java-demo
阿里云智能语音交互DEMO
demo 解压后，在pom 目录运行mvn package ，会在target目录生成可执行jar nls-example-transcriber-2.0.0-jar-with-dependencies.jar 将此jar拷贝到目标服务器，可用于快速验证及压测服务。

## 服务验证
 
```java -cp nls-example-transcriber-2.0.0-jar-with-dependencies.jar com.alibaba.nls.client.SpeechTranscriberDemo```
并按提示提供相应参数，运行后在jar包同目录生成logs/nls.log

## 服务压测
```java -jar nls-example-transcriber-2.0.0-jar-with-dependencies.jar```
并按提示提供相应参数，其中阿里云服务url参数为： wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1 ，语音文件请提供16k采样率 pcm 格式文件，并发数根据用户已购买并发谨慎选择。

**温馨提示：自行压测超过10并发会产生费用。**

## 关键接口
- NlsClient：语音处理client，相当于所有语音相关处理类的factory，全局创建一个实例即可。线程安全。
- SpeechTranscriber：实时语音识别类，设置请求参数，发送请求及声音数据。非线程安全。
- SpeechTranscriberListener：实时语音识别结果监听类，监听识别结果。非线程安全。

## SDK 调用注意事项
NlsClient对象创建一次可以重复使用，每次创建消耗性能。NlsClient使用了netty的框架，创建时比较消耗时间和资源，但创建之后可以重复利用。建议调用程序将NlsClient的创建和关闭与程序本身的生命周期结合。
SpeechTranscriber对象不能重复使用，一个识别任务对应一个SpeechTranscriber对象。例如有N个音频文件，则要进行N次识别任务，创建N个SpeechTranscriber对象。
实现的SpeechTranscriberListener对象和SpeechTranscriber对象是一一对应的，不能将一个SpeechTranscriberListener对象设置到多个SpeechTranscriber对象中，否则不能区分是哪个识别任务。
Java SDK依赖了Netty网络库，版本需设置为4.1.17.Final及以上。如果您的应用中依赖了Netty，请确保版本符合要求。

Demo中使用了SDK内置的默认实时语音识别服务的外网访问URL，如果您使用阿里云上海ECS并想使用内网访问URL，则在创建NlsClient对象时，设置内网访问的URL：```client = new NlsClient("ws://nls-gateway.cn-shanghai-internal.aliyuncs.com/ws/v1", accessToken);```

**示例**
```java
import java.io.InputStream;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
/**
 * 实时语音识别Demo
 */
public class SpeechTranscriberDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;
    public SpeechTranscriberDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
        client = new NlsClient(token, accessToken);
    }
    public SpeechTranscriberDemo(String appKey, String token, String url) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
        client = new NlsClient(url, accessToken);
    }
    private static SpeechTranscriberListener getTranscriberListener() {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                    //状态码 20000000 表示正常识别
                    ", status: " + response.getStatus() +
                    //句子编号，从1开始递增
                    ", index: " + response.getTransSentenceIndex() +
                    //当前的识别结果
                    ", result: " + response.getTransSentenceText() +
                    //当前已处理的音频时长，单位是毫秒
                    ", time: " + response.getTransSentenceTime());
            }
            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                    ", status: " + response.getStatus());
            }
            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                    ", status: " + response.getStatus());
            }
            //识别出一句话.服务端会智能断句,当识别到一句话结束时会返回此消息
            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                    //状态码 20000000 表示正常识别
                    ", status: " + response.getStatus() +
                    //句子编号，从1开始递增
                    ", index: " + response.getTransSentenceIndex() +
                    //当前的识别结果
                    ", result: " + response.getTransSentenceText() +
                    //置信度
                    ", confidence: " + response.getConfidence() +
                    //开始时间
                    ", begin_time: " + response.getSentenceBeginTime() +
                    //当前已处理的音频时长，单位是毫秒
                    ", time: " + response.getTransSentenceTime());
            }
            //识别完毕
            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                    ", status: " + response.getStatus());
            }
            @Override
            public void onFail(SpeechTranscriberResponse response) {
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
        SpeechTranscriber transcriber = null;
        try {
            //创建实例,建立连接
            transcriber = new SpeechTranscriber(client, getTranscriberListener());
            transcriber.setAppKey(appKey);
            //输入音频编码方式
            transcriber.setFormat(InputFormatEnum.PCM);
            //输入音频采样率
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            //是否返回中间识别结果
            transcriber.setEnableIntermediateResult(false);
            //是否生成并返回标点符号
            transcriber.setEnablePunctuation(true);
            //是否将返回结果规整化,比如将一百返回为100
            transcriber.setEnableITN(false);
            //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            transcriber.start();
            //语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 transcriber.sent(ins)即可
            transcriber.send(ins, 3200, 100);
            //通知服务端语音数据发送完毕,等待服务端处理完成
            transcriber.stop();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            if (null != transcriber) {
                transcriber.close();
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
        SpeechTranscriberDemo demo =null;
        if (args.length == 2) {
            appKey = args[0];
            token = args[1];
            //default url is wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1
            demo = new SpeechTranscriberDemo(appKey, token);
        }else if(args.length == 3){
            appKey = args[0];
            token = args[1];
            url = args[2];
            demo = new SpeechTranscriberDemo(appKey, token, url);
        }else{
            System.err.println("SpeechTranscriberDemo need params(url is optional): " +
                "<app-key> <token> [<url>]");
            System.exit(-1);
        }
        InputStream ins = SpeechTranscriberDemo.class.getResourceAsStream("/nls-sample-16k.wav");
        if (null == ins) {
            System.err.println("open the audio file failed!");
            return;
        }
        demo.process(ins);
        demo.shutdown();
    }
}
```
