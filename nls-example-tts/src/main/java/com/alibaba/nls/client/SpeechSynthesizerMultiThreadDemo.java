package com.alibaba.nls.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 此示例演示了
 *      语音合成SDK API调用
 *      多线程并发调用
 *      用户自定义参数设置
 *      流式合成TTS
 *      首包延迟计算
 * (仅作演示，需用户根据实际情况实现)
 */
public class SpeechSynthesizerMultiThreadDemo {
    private static final Logger logger = LoggerFactory.getLogger(SpeechSynthesizerMultiThreadDemo.class);

    private static SpeechSynthesizerListener getSynthesizerListener(final String audioFileName) {
        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {
                // TODO 如果需要播放音频,且对实时性要求较高,建议使用流式播放
                File f = new File(audioFileName);
                FileOutputStream fout = new FileOutputStream(f);
                private boolean firstRecvBinary = true;
                private long firstRecvBinaryTimeStamp = System.currentTimeMillis();

                // 语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
                    // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
                    System.out.println("task_id: " + response.getTaskId() +
                    ", thread id: " + Thread.currentThread().getId() +
                        ", name: " + response.getName() +
                        ", status: " + response.getStatus()+
                        ", output file :"+f.getAbsolutePath()
                    );
                }

                // 语音合成的语音二进制数据
                @Override
                public void onMessage(ByteBuffer message) {
                    try {
                        if(firstRecvBinary) {
                            // TODO 此处是计算首包语音流的延迟，收到第一包语音流时，即可以进行语音播放，以提升响应速度(特别是实时交互场景下)
                            firstRecvBinary = false;
                            long now = System.currentTimeMillis();
                            logger.info("first latency : " + (now - firstRecvBinaryTimeStamp) + " ms");
                        }
                        byte[] bytesArray = new byte[message.remaining()];
                        message.get(bytesArray, 0, bytesArray.length);
                        System.out.println("output: " + audioFileName + ", thread id: "
                                    + Thread.currentThread().getId() + ", write arrya:" + bytesArray.length);
                        fout.write(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFail(SpeechSynthesizerResponse response){
                    // TODO 重要提示： task_id很重要，是调用方和服务端通信的唯一ID标识，当遇到问题时，需要提供此task_id以便排查
                    System.out.println(
                        "task_id: " + response.getTaskId() + ", status: " + response.getStatus() + ", status_text: " + response.getStatusText());
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listener;
    }

    static class Task implements Runnable {
        private CountDownLatch latch;
        private NlsClient client;
        private String appKey;
        private String text;
        private String audioFileName;

        public Task(String appKey, NlsClient client, CountDownLatch latch, String text, String audioFileName) {
            this.appKey = appKey;
            this.client = client;
            this.latch = latch;
            this.text = text;
            this.audioFileName = audioFileName;
        }

        @Override
        public void run() {
            SpeechSynthesizer synthesizer = null;
            try {
                // 创建实例,建立连接
                synthesizer = new SpeechSynthesizer(client, getSynthesizerListener(audioFileName));
                synthesizer.setAppKey(appKey);
                // 设置返回音频的编码格式
                synthesizer.setFormat(OutputFormatEnum.WAV);
                // 设置返回音频的采样率
                synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                // 设置用于语音合成的文本
                synthesizer.setText(text);

                // 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                synthesizer.start();
                // 等待语音合成结束
                synthesizer.waitForComplete();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != synthesizer) {
                    synthesizer.close();
                }
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("SpeechSynthesizerMultiThreadDemo need params: " +
                "<app-key> <token> <url> <text> <save-audio-name> <thread-num>");
            System.exit(-1);
        }

        String appKey = args[0];     // appkey
        String token = args[1];      // token
        String url = args[2];        // url
        String text = args[3];       // 合成文本
        String audioFileName = args[4];    // 待保存文件名
        int threadNum = Integer.parseInt(args[5]);  // 并发数

        final NlsClient client = new NlsClient(url, token);
        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                // 拼接一个文件名
                String path = null;
                int index = audioFileName.lastIndexOf(".");
                if (index <= 0) {
                    path = audioFileName + "_" + (i + 1) + ".wav";
                } else {
                    path = audioFileName.substring(0, index) +
                            "_" + (i + 1) + audioFileName.substring(index, audioFileName.length());
                }

                // 创建一个请求任务线程
                Task task = new Task(appKey, client, latch, text, path);
                new Thread(task).start();
            }

            // 等待所有请求任务结束
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
