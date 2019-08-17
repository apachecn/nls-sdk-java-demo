package com.alibaba.nls.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import com.alibaba.nls.client.util.OpuCodec;
import com.alibaba.nls.client.util.OpuCodec.EncodeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 此示例演示了
 *      ASR一句话识别API调用
 *      多线程并发调用
 *      用户自定义参数设置
 *      动态获取token
 *      通过本地模拟实时流发送
 *      识别耗时计算
 * (仅作演示，需用户根据实际情况实现)
 */

class MyRecognizerListener extends SpeechRecognizerListener {
    // TODO 用户自定义参数
    private int myIndex;
    private String filePath;

    MyRecognizerListener(int index, String filePath) {
        this.myIndex = index;
        this.filePath = filePath;
    }

    //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
    @Override
    public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
        System.out.println("task_id: " + response.getTaskId() +
            //事件名称 RecognitionResultChanged
            ", name: " + response.getName() +
            ", index: " + myIndex +
            ", filePath: " + filePath +
            //状态码 20000000 表示识别成功
            ", status: " + response.getStatus() +
            //语音识别文本
            ", result: " + response.getRecognizedText());
    }

    @Override
    public void onRecognitionCompleted(SpeechRecognizerResponse response) {
        System.out.println("task_id: " + response.getTaskId() +
            //事件名称 RecognitionCompleted
            ", name: " + response.getName() +
            ", index: " + myIndex +
            ", filePath: " + filePath +
            //状态码 20000000 表示识别成功
            ", status: " + response.getStatus() +
            //语音识别文本
            ", result: " + response.getRecognizedText());
    }

    @Override
    public void onStarted(SpeechRecognizerResponse response) {
        System.out.println(
            "task_id: " + response.getTaskId()+
                ", name: " + response.getName() +
                //状态码 20000000 表示识别成功
                ", status: " + response.getStatus());
    }

    @Override
    public void onFail(SpeechRecognizerResponse response) {
        // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
        System.out.println("onFail : " +
            " index: " + myIndex +
            ", filePath: " + filePath +
            ", task_id: " + response.getTaskId() +
            //状态码 20000000 表示识别成功
            ", status: " + response.getStatus() +
            //语音识别文本
            ", status_text: " + response.getStatusText());
    }
}

public class SpeechRecognizerMultiThreadDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognizerMultiThreadDemo.class);
    private static SampleRateEnum sampleRateEnum=SampleRateEnum.SAMPLE_RATE_16K;
    final static OpuCodec codec = new OpuCodec();

    static class Task implements Runnable {
        private String appKey;
        private NlsClient client;
        private CountDownLatch latch;
        private int index;
        private String audioFile;
        private boolean isOpuCompress;

        public Task(String appKey, NlsClient client, CountDownLatch latch, int index, String audioFile, boolean isOpuCompress) {
            this.appKey = appKey;
            this.client = client;
            this.latch = latch;
            this.index = index;
            this.audioFile = audioFile;
            this.isOpuCompress=isOpuCompress;
        }

        @Override
        public void run() {
            try {
                // TODO 重要提示： 这里用一个本地文件来模拟发送实时流数据，实际使用时，用户可以从某处实时采集或接收语音流并发送到ASR服务端
                File file = new File(audioFile);
                MyRecognizerListener listener = new MyRecognizerListener(index, audioFile);
                //创建实例,建立连接
                final SpeechRecognizer recognizer = new SpeechRecognizer(client, listener);
                recognizer.setAppKey(appKey);
                //设置音频编码格式
                recognizer.setFormat(InputFormatEnum.PCM);
                //设置音频采样率
                recognizer.setSampleRate(sampleRateEnum);
                //设置是否返回中间识别结果
                recognizer.setEnableIntermediateResult(true);
                InputStream ins = new FileInputStream(file);

                if(isOpuCompress){
                    // TODO 如果启用Opu压缩，注意以下设置
                    recognizer.setFormat(InputFormatEnum.OPU);
                    recognizer.start();
                    codec.encode(16000, ins, new EncodeListener() {
                        @Override
                        public void onEncodedData(byte[] data) {
                            System.out.println("send audio " + data.length);
                            recognizer.send(data);
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }else {
                    //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                    recognizer.start();

                    // TODO 重要提示： 这里用一个本地文件来模拟发送实时流数据，实际使用时，用户可以从某处实时采集或接收语音流并发送到ASR服务端
                    FileInputStream fis = new FileInputStream(file);
                    byte[] b = new byte[3200];
                    int len;
                    while ((len = fis.read(b)) > 0) {
                        logger.info("send data pack length: " + len);
                        recognizer.send(b);

                        // TODO  重要提示：这里是用读取本地文件的形式模拟实时获取语音流并发送的，因为read很快，所以这里需要sleep
                        // TODO  如果是真正的实时获取语音，则无需sleep, 如果是8k采样率语音，第二个参数改为8000
                        // 8000采样率情况下，3200byte字节建议 sleep 200ms，16000采样率情况下，3200byte字节建议 sleep 100ms
                        int deltaSleep = getSleepDelta(len, sampleRateEnum.value);
                        Thread.sleep(deltaSleep);
                    }
                }

                //通知服务端语音数据发送完毕,等待服务端处理完成
                recognizer.stop();
                recognizer.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                latch.countDown();
            }
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
    }

    public static void main(String[] args) throws Exception{
        if (args.length < 4) {
            System.err.println("SpeechRecognizerMultiThreadDemo need params: <app-key> <AccessKeyId> <AccessKeySecret> <thread-num>");
            System.exit(-1);
        }
        String appKey          = args[0];
        String accessKeyId     = args[1];
        String accessKeySecret = args[2];
        int threadNum          = Integer.parseInt(args[3]);

        String audioFile       = "nls-sample-16k.wav";

        // 设置音频采样率
        sampleRateEnum = SampleRateEnum.SAMPLE_RATE_16K;

        // 是否启用opu压缩(一定程度减少带宽， 非必须)
        boolean isOpuCompress = false;

        NlsClient client = null;

        //TODO 重要提示 创建NlsClient实例,应用全局创建一个即可,生命周期可和整个应用保持一致,默认服务地址为阿里云线上服务地址
        //TODO 这里简单演示了获取token 的代码，该token会过期，实际使用时注意在accessToken.getExpireTime()过期前再次获取token
        AccessToken accessToken = new AccessToken(accessKeyId, accessKeySecret);
        try {
            accessToken.apply();
            System.out.println("get token: " + accessToken.getToken() + ", expire time: " + accessToken.getExpireTime());
            // TODO 创建NlsClient实例,应用全局创建一个即可
            client = new NlsClient(accessToken.getToken());
        } catch (IOException e) {
            e.printStackTrace();
        }

        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                Task task = new Task(appKey, client, latch, i, audioFile, isOpuCompress);
                new Thread(task).start();
            }
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
