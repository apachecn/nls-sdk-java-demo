package com.alibaba.nls.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.alibaba.nls.client.util.OpuCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeechTranscriberMultiThreadDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpeechTranscriberMultiThreadDemo.class);

    private static SampleRateEnum sampleRateEnum = SampleRateEnum.SAMPLE_RATE_16K;
    final static OpuCodec codec = new OpuCodec();

    /// 根据二进制数据大小计算对应的同等语音长度
    /// sampleRate 仅支持8000或16000
    public static int getSleepDelta(int dataSize, int sampleRate) {
        // 仅支持16位采样
        int sampleBytes = 16;
        // 仅支持单通道
        int soundChannel = 1;
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }

    private static SpeechTranscriberListener getTranscriberListener(final String inputName) {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
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
                    ",name: " + response.getName() +
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
                    ", input stream: " + inputName +
                    ", name: " + response.getName() +
                    ", status: " + response.getStatus());
            }

            @Override
            public void onFail(SpeechTranscriberResponse response) {
                // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
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

    static class Task implements Runnable {
        private String appKey;
        private NlsClient client;
        private CountDownLatch latch;
        private String audioFile;
        private boolean isOpuEncode;

        public Task(String appKey, NlsClient client, CountDownLatch latch, String audioFile, boolean isOpuEncode) {
            this.appKey = appKey;
            this.client = client;
            this.latch = latch;
            this.audioFile = audioFile;
            this.isOpuEncode = isOpuEncode;
        }

        @Override
        public void run() {
            try {
                // TODO 重要提示： 这里用一个本地文件来模拟发送实时流数据，实际使用时，用户可以从某处实时采集或接收语音流并发送到ASR服务端
                File file = new File(audioFile);
                String audioFileName = file.getName();
                SpeechTranscriberListener listener = getTranscriberListener(audioFileName);
                final SpeechTranscriber transcriber = new SpeechTranscriber(client, listener);
                transcriber.setAppKey(appKey);
                //输入音频编码方式
                transcriber.setFormat(InputFormatEnum.PCM);
                //输入音频采样率
                transcriber.setSampleRate(sampleRateEnum);
                //是否返回中间识别结果
                transcriber.setEnableIntermediateResult(false);
                //是否生成并返回标点符号
                transcriber.setEnablePunctuation(true);
                //是否将返回结果规整化,比如将一百返回为100
                transcriber.setEnableITN(true);

                InputStream ins = new FileInputStream(file);
                if (isOpuEncode) {
                    transcriber.setFormat(InputFormatEnum.OPU);
                    transcriber.start();
                    codec.encode(16000, ins, new OpuCodec.EncodeListener() {
                        @Override
                        public void onEncodedData(byte[] data) {
                            transcriber.send(data);
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                    transcriber.start();

                    FileInputStream fis = new FileInputStream(file);
                    byte[] b = new byte[3200];
                    int len;
                    while ((len = fis.read(b)) > 0) {
                        logger.info("send data length: " + len);
                        transcriber.send(b);
                        // TODO  重要提示：这里是用读取本地文件的形式模拟实时获取语音流并发送的，因为read很快，所以这里需要sleep
                        // TODO  如果是真正的实时获取语音，则无需sleep, 如果是8k采样率语音，第二个参数改为8000
                        // 8000采样率情况下，3200byte字节建议 sleep 200ms，16000采样率情况下，3200byte字节建议 sleep 100ms
                        int deltaSleep = getSleepDelta(len, sampleRateEnum.value);
                        Thread.sleep(deltaSleep);
                    }
                }

                // 通知服务端语音数据发送完毕,等待服务端处理完成
                long now = System.currentTimeMillis();
                logger.info("ASR wait for complete");
                transcriber.stop();
                logger.info("ASR latency : " + (System.currentTimeMillis() - now) + " ms");
                transcriber.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }

    /**
     * jvm optional param -DisOpuEnable=true -DsampleRate=8000
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("SpeechTranscriberMultiThreadDemo need params: <app-key> <token> <url> <audio-file>  <thread-num>");
            System.exit(-1);
        }

        String appKey    = args[0];
        String token     = args[1];
        String url       = args[2];   // 默认 wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1
        String audioFile = args[3];
        int threadNum    = Integer.parseInt(args[4]);

        System.out.println("appkey " + appKey);
        System.out.println("token " + token);
        System.out.println("url " + url);
        System.out.println("audioFile " + audioFile);

        String isOpuEnable = System.getProperty("isOpuEnable", "false");
        String sampleRate = System.getProperty("sampleRate", "16000");
        if ("8000".equals(sampleRate)) {
            sampleRateEnum = SampleRateEnum.SAMPLE_RATE_8K;
        }


        final NlsClient client = new NlsClient(url, token);
        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                Task task = new Task(appKey, client, latch, audioFile, Boolean.parseBoolean(isOpuEnable));
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
