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
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;

import java.io.InputStream;

/**
 * SpeechRecognizerDemo class
 *
 * 一句话识别Demo
 * @author siwei
 * @date 2018/5/29
 */
public class SpeechRecognizerDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;

    /**
     * @param appKey
     * @param token
     */
    public SpeechRecognizerDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        // Step0 创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
        client = new NlsClient(accessToken);
    }

    private static SpeechRecognizerListener getRecognizerListener() {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            // 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                // 事件名称 RecognitionResultChanged
                System.out.println("name: " + response.getName() +
                        // 状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        // 一句话识别的中间结果
                        ", result: " + response.getRecognizedText());
            }

            // 识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                // 事件名称 RecognitionCompleted
                System.out.println("name: " + response.getName() +
                        // 状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        // 一句话识别的完整结果
                        ", result: " + response.getRecognizedText());
            }
        };
        return listener;
    }

    public void process(InputStream ins) {
        SpeechRecognizer recognizer = null;
        try {
            // Step1 创建实例,建立连接
            recognizer = new SpeechRecognizer(client, getRecognizerListener());
            recognizer.setAppKey(appKey);
            // 设置音频编码格式
            recognizer.setFormat(InputFormatEnum.PCM);
            // 设置音频采样率
            recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            // 设置是否返回中间识别结果
            recognizer.setEnableIntermediateResult(true);

            // Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
            recognizer.start();
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
        }
    }

    public void shutdown() {
        client.shutdown();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("SpeechRecognizerDemo need params: <app-key> <token>");
            System.exit(-1);
        }

        String appKey = args[0];
        String token = args[1];

        SpeechRecognizerDemo demo = new SpeechRecognizerDemo(appKey, token);
        InputStream ins = SpeechRecognizerDemo.class.getResourceAsStream("/nls-sample-16k.wav");
        if (null == ins) {
            System.err.println("open the audio file failed!");
            System.exit(-1);
        }
        demo.process(ins);
        demo.shutdown();
    }

}
