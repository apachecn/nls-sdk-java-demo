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
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * SpeechTranscriberMultiThreadDemo class
 *
 * 多线程实时音频流识别Demo
 * @author siwei
 * @date 2018/6/25
 */
public class SpeechTranscriberMultiThreadDemo {
    private static SpeechTranscriberListener getTranscriberListener(final String inputName) {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            // 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                System.out.println("input stream: " + inputName +
                        ", name: " + response.getName() +
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
                System.out.println("input stream: " + inputName +
                        ", name: " + response.getName() +
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
                System.out.println("input stream: " + inputName +
                        ", name: " + response.getName() +
                        ", status: " + response.getStatus());
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
            SpeechTranscriber transcriber = null;
            try {
                File file = new File(audioFile);
                String audioFileName = file.getName();
                SpeechTranscriberListener listener = getTranscriberListener(audioFileName);
                // Step1 创建实例,建立连接
                transcriber = new SpeechTranscriber(client, listener);
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
                transcriber.setEnableITN(false);

                // Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                transcriber.start();
                // Step3 语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
                InputStream ins = new FileInputStream(file);
                transcriber.send(ins, 6400, 200);
                // Step4 通知服务端语音数据发送完毕,等待服务端处理完成
                transcriber.stop();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                // Step5 关闭连接
                if (null != transcriber) {
                    transcriber.close();
                }
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("SpeechTranscriberMultiThreadDemo need params: " +
                    "<app-key> <token> <audio-file>  <thread-num>");
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
