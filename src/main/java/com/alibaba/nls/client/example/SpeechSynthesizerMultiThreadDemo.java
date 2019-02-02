/*
 * Copyright 2015 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nls.client.example;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * SpeechSynthesizerDemo class
 *
 * 多线程语音合成（TTS）Demo
 * @author siwei
 * @date 2018/6/25
 */
public class SpeechSynthesizerMultiThreadDemo {
    private static SpeechSynthesizerListener getSynthesizerListener(final String audioFileName) {
        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {
                File f = new File(audioFileName);
                FileOutputStream fout = new FileOutputStream(f);
                // 语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
                    System.out.println("thread id: " + Thread.currentThread().getId() +
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
                                ", write array:" + bytesArray.length);
                        fout.write(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                // Step1 创建实例,建立连接
                synthesizer = new SpeechSynthesizer(client, getSynthesizerListener(audioFileName));
                synthesizer.setAppKey(appKey);
                // 设置返回音频的编码格式
                synthesizer.setFormat(OutputFormatEnum.WAV);
                // 设置返回音频的采样率
                synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                // 设置用于语音合成的文本
                synthesizer.setText(text);

                // Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                synthesizer.start();
                // Step3 等待语音合成结束
                synthesizer.waitForComplete();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                // Step4 关闭连接
                if (null != synthesizer) {
                    synthesizer.close();
                }
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("SpeechTranscriberMultiThreadDemo need params: " +
                    "<app-key> <token> <text> <save-audio-name> <thread-num>");
            System.exit(-1);
        }

        String appKey = args[0];
        String token = args[1];
        String text = args[2];
        String audioFileName = args[3];
        int threadNum = Integer.parseInt(args[4]);

        final NlsClient client = new NlsClient(token);
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
