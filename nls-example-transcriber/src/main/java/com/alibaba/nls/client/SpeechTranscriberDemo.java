package com.alibaba.nls.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 此示例演示了
 *      ASR实时识别API调用
 *      动态获取token
 *      通过本地模拟实时流发送
 *      识别耗时计算
 * (仅作演示，需用户根据实际情况实现)
 */
public class SpeechTranscriberDemo {
    private String appKey;
    private NlsClient client;

    private static final Logger logger = LoggerFactory.getLogger(SpeechTranscriberDemo.class);

    public SpeechTranscriberDemo(String appKey, String id, String secret, String url) {
        this.appKey = appKey;
        //TODO 重要提示 创建NlsClient实例,应用全局创建一个即可,生命周期可和整个应用保持一致,默认服务地址为阿里云线上服务地址
        //TODO 这里简单演示了获取token 的代码，该token会过期，实际使用时注意在accessToken.getExpireTime()过期前再次获取token
        AccessToken accessToken = new AccessToken(id, secret);
        try {
            accessToken.apply();
            System.out.println("get token: " + ", expire time: " + accessToken.getExpireTime());
            // TODO 创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
            if(url.isEmpty()) {
                client = new NlsClient(accessToken.getToken());
            }else {
                client = new NlsClient(url, accessToken.getToken());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SpeechTranscriberListener getTranscriberListener() {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            //TODO 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
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
                // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
                System.out.println("task_id: " + response.getTaskId() + ", name: " + response.getName() + ", status: " + response.getStatus());
            }

            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                System.out.println("task_id: " + response.getTaskId() + ", name: " + response.getName() + ", status: " + response.getStatus());

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
                System.out.println("task_id: " + response.getTaskId() + ", name: " + response.getName() + ", status: " + response.getStatus());
            }

            @Override
            public void onFail(SpeechTranscriberResponse response) {
                // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
                System.out.println("task_id: " + response.getTaskId() +  ", status: " + response.getStatus() + ", status_text: " + response.getStatusText());
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

    public void process(String filepath) {
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

            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            byte[] b = new byte[3200];
            int len;
            while ((len = fis.read(b)) > 0) {
                logger.info("send data pack length: " + len);
                transcriber.send(b);
                // TODO  重要提示：这里是用读取本地文件的形式模拟实时获取语音流并发送的，因为read很快，所以这里需要sleep
                // TODO  如果是真正的实时获取语音，则无需sleep, 如果是8k采样率语音，第二个参数改为8000
                // 8000采样率情况下，3200byte字节建议 sleep 200ms，16000采样率情况下，3200byte字节建议 sleep 100ms
                int deltaSleep = getSleepDelta(len, 16000);
                Thread.sleep(deltaSleep);
            }

            //通知服务端语音数据发送完毕,等待服务端处理完成
            long now = System.currentTimeMillis();
            logger.info("ASR wait for complete");
            transcriber.stop();
            logger.info("ASR latency : " + (System.currentTimeMillis() - now) + " ms");
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

        String appKey = "填写你的appkey";
        String id = "填写你在阿里云网站上的AccessKeyId";
        String secret = "填写你在阿里云网站上的AccessKeySecret";
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

        // TODO 重要提示： 这里用一个本地文件来模拟发送实时流数据，实际使用时，用户可以从某处实时采集或接收语音流并发送到ASR服务端
        String filepath = "nls-sample-16k.wav";
        SpeechTranscriberDemo demo = new SpeechTranscriberDemo(appKey, id, secret, url);

        demo.process(filepath);
        demo.shutdown();
    }
}