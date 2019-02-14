[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f63ee8a318984e0c8223afdcfa3d53fe)](https://www.codacy.com/app/weslie0803/nls-sdk-java-demo?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=lihangqi/nls-sdk-java-demo&amp;utm_campaign=Badge_Grade)

# 关键接口
NlsClient：语音处理client，相当于所有语音相关处理类的factory，全局创建一个实例即可。线程安全。
SpeechTranscriber：实时语音识别类，设置请求参数，发送请求及声音数据。非线程安全。
SpeechTranscriberListener：实时语音识别结果监听类，监听识别结果。非线程安全。
更多介绍参见API文档链接: [Java API接口说明](http://g.alicdn.com/idst-fe/nls-sdk-doc-api/2.0.6/GateWay_Java_2.0.2/index.html?spm=a2c4g.11186623.2.18.51515397OQA7ZZ)

# SDK 调用注意事项
NlsClient对象创建一次可以重复使用，每次创建消耗性能。NlsClient使用了netty的框架，创建时比较消耗时间和资源，但创建之后可以重复利用。建议调用程序将NlsClient的创建和关闭与程序本身的生命周期结合。
SpeechTranscriber对象不能重复使用，一个识别任务对应一个SpeechTranscriber对象。例如有N个音频文件，则要进行N次识别任务，创建N个SpeechTranscriber对象。
实现的SpeechTranscriberListener对象和SpeechTranscriber对象是一一对应的，不能将一个SpeechTranscriberListener对象设置到多个SpeechTranscriber对象中，否则不能区分是哪个识别任务。

# 代码示例
**说明1**：Demo中使用的音频文件为16000Hz采样率，请在管控台中将appKey对应项目的模型设置为通用模型，以获取正确的识别结果；如果使用其他音频，请设置为支持该音频场景的模型，模型设置请阅读[管理项目](https://help.aliyun.com/document_detail/72214.html?spm=a2c4g.11186623.2.19.6fe753972xVCYN)一节。
**说明2**：Demo中使用了SDK内置的默认实时语音识别服务的外网访问URL，如果您使用阿里云上海ECS并想使用内网访问URL，则在创建NlsClient对象时，设置内网访问的URL：

```client = new NlsClient("ws://nls-gateway.cn-shanghai-internal.aliyuncs.com/ws/v1", accessToken);```

**示例:**
```
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import java.io.InputStream;
/**
 * SpeechTranscriberDemo class
 *
 * 实时音频流识别Demo
 */
public class SpeechTranscriberDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;
    public SpeechTranscriberDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        // Step0 创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
        client = new NlsClient(accessToken);
    }
    private static SpeechTranscriberListener getTranscriberListener() {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            // 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                System.out.println("name: " + response.getName() +
                        // 状态码 20000000 表示正常识别
                        ", status: " + response.getStatus() +
                        // 句子编号，从1开始递增
                        ", index: " + response.getTransSentenceIndex() +
                        // 当前句子的中间识别结果
                        ", result: " + response.getTransSentenceText() +
                        // 当前已处理的音频时长，单位是毫秒
                        ", time: " + response.getTransSentenceTime());
            }
            // 识别出一句话.服务端会智能断句,当识别到一句话结束时会返回此消息
            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                System.out.println("name: " + response.getName() +
                        // 状态码 20000000 表示正常识别
                        ", status: " + response.getStatus() +
                        // 句子编号，从1开始递增
                        ", index: " + response.getTransSentenceIndex() +
                        // 当前句子的完整识别结果
                        ", result: " + response.getTransSentenceText() +
                        // 当前已处理的音频时长，单位是毫秒
                        ", time: " + response.getTransSentenceTime() +
                        // SentenceBegin事件的时间，单位是毫秒
                        ", begin time: " + response.getSentenceBeginTime() +
                        // 识别结果置信度，取值范围[0.0, 1.0]，值越大表示置信度越高
                        ", confidence: " + response.getConfidence());
            }
            // 识别完毕
            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                System.out.println("name: " + response.getName() +
                        ", status: " + response.getStatus());
            }
        };
        return listener;
    }
    public void process(InputStream ins) {
        SpeechTranscriber transcriber = null;
        try {
            // Step1 创建实例,建立连接
            transcriber = new SpeechTranscriber(client, getTranscriberListener());
            transcriber.setAppKey(appKey);
            // 输入音频编码方式
            transcriber.setFormat(InputFormatEnum.PCM);
            // 输入音频采样率
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            // 是否返回中间识别结果
            transcriber.setEnableIntermediateResult(false);
            // 是否生成并返回标点符号
            transcriber.setEnablePunctuation(true);
            // 是否将返回结果规整化,比如将一百返回为100
            transcriber.setEnableITN(true);
            // Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            transcriber.start();
            // Step3 语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
            transcriber.send(ins, 3200, 100);
            // Step4 通知服务端语音数据发送完毕,等待服务端处理完成
            transcriber.stop();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            // Step5 关闭连接
            if (null != transcriber) {
                transcriber.close();
            }
        }
    }
    public void shutdown() {
        client.shutdown();
    }
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("SpeechTranscriberDemo need params: <app-key> <token>");
            System.exit(-1);
        }
        String appKey = args[0];
        String token = args[1];
        SpeechTranscriberDemo demo = new SpeechTranscriberDemo(appKey, token);
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
