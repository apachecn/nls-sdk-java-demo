package com.alibaba.nls.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;

/**
 * Created by siwei on 2018/06/20
 */
public class SpeechSynthesizerMultiThreadDemo {
    private static int sampleRate;

    private static SpeechSynthesizerListener getSynthesizerListener(final String audioFileName) {
        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {
                //如果需要播放音频,且对实时性要求较高,建议使用流式播放
                File f = new File(audioFileName);
                FileOutputStream fout = new FileOutputStream(f);
                // 语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
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
                        byte[] bytesArray = new byte[message.remaining()];
                        message.get(bytesArray, 0, bytesArray.length);
                        System.out.println("thread id: " + Thread.currentThread().getId() +
                            ", write arrya:" + bytesArray.length);
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
                synthesizer.setSampleRate(sampleRate);
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

        String appKey = args[0];
        String token = args[1];
        String url = args[2];
        String text = args[3];
        String audioFileName = args[4];
        int threadNum = Integer.parseInt(args[5]);

        String sampleRateStr=System.getProperty("sampleRate","16000");
        sampleRate=Integer.parseInt(sampleRateStr);

        final NlsClient client = new NlsClient(url, token);
        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                String temp = null;
                int index = audioFileName.lastIndexOf(".");
                if (index <= 0) {
                    temp = audioFileName + "_" + (i + 1);
                } else {
                    temp = audioFileName.substring(0, index) +
                            "_" + (i + 1) + audioFileName.substring(index, audioFileName.length());
                }
                Task task = new Task(appKey, client, latch, text, temp);
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
