[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f63ee8a318984e0c8223afdcfa3d53fe)](https://www.codacy.com/app/weslie0803/nls-sdk-java-demo?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=lihangqi/nls-sdk-java-demo&amp;utm_campaign=Badge_Grade)

# nls-sdk-java-demo
阿里云智能语音交互DEMO
demo 解压后，在pom 目录运行mvn package ，会在target目录生成可执行jar nls-example-transcriber-2.0.0-jar-with-dependencies.jar 将此jar拷贝到目标服务器，可用于快速验证及压测服务。

## 介绍
本示例代码是阿里云智能语音交互服务相关的java 语言示例。 包括了一句话识别、实时识别、录音文件识别、语音合成等多个功能的演示。

**需要说明的是：以下代码均为demo示例，当需要集成到自己的系统中时，注意根据实际情况进行相应修改，比如逻辑调整、参数设置、异常处理等等。**

### 一句话识别(nls-example-recognizer)
- SpeechRecognizerDemo ：单线程调用演示一句话识别接口
- SpeechRecognizerMultiThreadDemo ：多线程调用演示一句话识别接口

### 实时识别(nls-example-transcriber)
- SpeechTranscriberDemo ：单线程调用演示实时语音识别接口
- SpeechTranscriberMultiThreadDemo ：多线程调用演示实时语音识别接口
- SpeechTranscriberWithMicrophoneDemo ：演示了从麦克风采集语音并实时识别的过程

### 语音合成(nls-example-tts)
- SpeechSynthesizerDemo ：单线程调用演示语音合成接口
- SpeechSynthesizerMultiThreadDemo ：多线程调用演示语音合成接口
- SpeechSynthesizerLongTextDemo ：演示长文本语音合成调用时，如何拆分文本的功能

### token获取(nls-example-token)
- TokenDemo ： 演示token的获取方式
- SpeechTokenGeneratorDemo ： 演示token定时获取的方式


2019年07月19日

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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 此示例演示了
 *      ASR一句话识别API调用
 *      动态获取token
 *      通过本地文件模拟实时流发送
 *      识别耗时计算
 * (仅作演示，需用户根据实际情况实现)
 */
public class SpeechRecognizerDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognizerDemo.class);
    private String appKey;
    NlsClient client;
    public SpeechRecognizerDemo(String appKey, String id, String secret, String url) {
        this.appKey = appKey;
        //TODO 重要提示 创建NlsClient实例,应用全局创建一个即可,生命周期可和整个应用保持一致,默认服务地址为阿里云线上服务地址
        //TODO 这里简单演示了获取token 的代码，该token会过期，实际使用时注意在accessToken.getExpireTime()过期前再次获取token
        AccessToken accessToken = new AccessToken(id, secret);
        try {
            accessToken.apply();
            System.out.println("get token: " + accessToken.getToken() + ", expire time: " + accessToken.getExpireTime());
            // TODO 创建NlsClient实例,应用全局创建一个即可
            if(url.isEmpty()) {
                client = new NlsClient(accessToken.getToken());
            }else {
                client = new NlsClient(url, accessToken.getToken());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static SpeechRecognizerListener getRecognizerListener(int myOrder, String userParam) {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                //事件名称 RecognitionResultChanged、 状态码(20000000 表示识别成功)、语音识别文本
                System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
            }
            //识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                //事件名称 RecognitionCompleted, 状态码 20000000 表示识别成功, getRecognizedText是识别结果文本
                System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
            }
            @Override
            public void onStarted(SpeechRecognizerResponse response) {
                System.out.println("myOrder: " + myOrder + "; myParam: " + userParam + "; task_id: " + response.getTaskId());
            }
            @Override
            public void onFail(SpeechRecognizerResponse response) {
                // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
                System.out.println("task_id: " + response.getTaskId() + ", status: " + response.getStatus() + ", status_text: " + response.getStatusText());
            }
        };
        return listener;
    }
    /// 根据二进制数据大小计算对应的同等语音长度
    /// sampleRate 仅支持8000或16000
    public static int getSleepDelta(int dataSize, int sampleRate) {
        // 仅支持16位采样
        int sampleBytes = 16;
        // 仅支持单通道
        int soundChannel = 1;
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }
    public void process(String filepath, int sampleRate) {
        SpeechRecognizer recognizer = null;
        try {
            // 传递用户自定义参数
            String myParam = "user-param";
            int myOrder = 1234;
            SpeechRecognizerListener listener = getRecognizerListener(myOrder, myParam);
            recognizer = new SpeechRecognizer(client, listener);
            recognizer.setAppKey(appKey);
            //设置音频编码格式 TODO 如果是opus文件，请设置为 InputFormatEnum.OPUS
            recognizer.setFormat(InputFormatEnum.PCM);
            //设置音频采样率
            if(sampleRate == 16000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            } else if(sampleRate == 8000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            }
            //设置是否返回中间识别结果
            recognizer.setEnableIntermediateResult(true);
            //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            long now = System.currentTimeMillis();
            recognizer.start();
            logger.info("ASR start latency : " + (System.currentTimeMillis() - now) + " ms");
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            byte[] b = new byte[3200];
            int len;
            while ((len = fis.read(b)) > 0) {
                logger.info("send data pack length: " + len);
                recognizer.send(b);
                // TODO  重要提示：这里是用读取本地文件的形式模拟实时获取语音流并发送的，因为read很快，所以这里需要sleep
                // TODO  如果是真正的实时获取语音，则无需sleep, 如果是8k采样率语音，第二个参数改为8000
                int deltaSleep = getSleepDelta(len, sampleRate);
                Thread.sleep(deltaSleep);
            }
            //通知服务端语音数据发送完毕,等待服务端处理完成
            now = System.currentTimeMillis();
            // TODO 计算实际延迟: stop返回之后一般即是识别结果返回时间
            logger.info("ASR wait for complete");
            recognizer.stop();
            logger.info("ASR stop latency : " + (System.currentTimeMillis() - now) + " ms");
            fis.close();
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
        String appKey = null; // "填写你的appkey";
        String id = null; // "填写你在阿里云网站上的AccessKeyId";
        String secret = null; // "填写你在阿里云网站上的AccessKeySecret";
        String url = ""; // 默认即可，默认值：wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1
        if (args.length == 3) {
            appKey   = args[0];
            id       = args[1];
            secret   = args[2];
        } else if (args.length == 4) {
            appKey   = args[0];
            id       = args[1];
            secret   = args[2];
            url      = args[3];
        } else {
            System.err.println("run error, need params(url is optional): " + "<app-key> <AccessKeyId> <AccessKeySecret> [url]");
            System.exit(-1);
        }
        SpeechRecognizerDemo demo = new SpeechRecognizerDemo(appKey, id, secret, url);
        // TODO 重要提示： 这里用一个本地文件来模拟发送实时流数据，实际使用时，用户可以从某处实时采集或接收语音流并发送到ASR服务端
        demo.process("./nls-sample-16k.wav", 16000);
        //demo.process("./nls-sample.opus", 16000);
        demo.shutdown();
    }
}
```
