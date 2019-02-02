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

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * SpeechRecognizerMultiThreadDemo class
 *
 * 多线程一句话识别Demo
 * @author siwei
 * @date 2018/6/25
 */
public class SpeechRecognizerMultiThreadDemo {
    private static SpeechRecognizerListener getRecognizerListener(final String inputName) {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            // 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                // 事件名称 RecognitionResultChanged
                System.out.println("input stream: " + inputName +
                        ", name: " + response.getName() +
                        // 状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        // 一句话识别的中间结果
                        ", result: " + response.getRecognizedText());
            }

            //识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                // 事件名称 RecognitionCompleted
                System.out.println("input stream: " + inputName +
                        ", name: " + response.getName() +
                        // 状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        // 一句话识别的完整结果
                        ", result: " + response.getRecognizedText());
            }
        };
        return listener;
    }

    static class Task implements Runnable {
        private String appKey;
        private NlsClient client;
        private CountDownLatch latch;
        private String audioFile;

        public Task(String appKey, NlsClient client, CountDownLatch latch, String audioFile) {
            this.appKey = appKey;
            this.client = client;
            this.latch = latch;
            this.audioFile = audioFile;
        }

        @Override
        public void run() {
            SpeechRecognizer recognizer = null;
            try {
                File file = new File(audioFile);
                String audioFileName = file.getName();
                // Step1 创建实例,建立连接
                recognizer = new SpeechRecognizer(client, getRecognizerListener(audioFileName));
                recognizer.setAppKey(appKey);
                // 设置音频编码格式
                recognizer.setFormat(InputFormatEnum.PCM);
                // 设置音频采样率
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                // 设置是否返回中间识别结果
                recognizer.setEnableIntermediateResult(false);

                // Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                recognizer.start();
                InputStream ins = new FileInputStream(file);
                // Step3 语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
                recognizer.send(ins, 6400, 200);
                // Step4 通知服务端语音数据发送完毕,等待服务端处理完成
                recognizer.stop();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                // Step5 关闭连接
                if (null != recognizer) {
                    recognizer.close();
                }
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("SpeechRecognizerMultiThreadDemo need params: " +
                    "<app-key> <token> <audio-file> <thread-num>");
            System.exit(-1);
        }

        String appKey = args[0];
        String token = args[1];
        String audioFile = args[2];
        int threadNum = Integer.parseInt(args[3]);

        final NlsClient client = new NlsClient(token);
        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                Task task = new Task(appKey, client, latch, audioFile);
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
