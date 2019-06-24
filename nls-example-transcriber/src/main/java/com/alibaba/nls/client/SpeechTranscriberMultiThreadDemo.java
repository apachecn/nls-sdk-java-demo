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

/**
 * Created by siwei on 2018/06/20
 */
public class SpeechTranscriberMultiThreadDemo {
    private static SampleRateEnum sampleRateEnum = SampleRateEnum.SAMPLE_RATE_16K;
    final static OpuCodec codec = new OpuCodec();

    private static SpeechTranscriberListener getTranscriberListener(final String inputName) {
        SpeechTranscriberListener listener = new SpeechTranscriberListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
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

    /**
     * jvm optional param -DisOpuEnable=true -DsampleRate=8000
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("SpeechTranscriberMultiThreadDemo need params: " +
                "<app-key> <token> <url> <audio-file>  <thread-num>");
            System.exit(-1);
        }

        String appKey = args[0];
        String token = args[1];
        String url = args[2];
        String audioFile = args[3];

        String isOpuEnable = System.getProperty("isOpuEnable", "false");
        String sampleRate = System.getProperty("sampleRate", "16000");
        if ("8000".equals(sampleRate)) {
            sampleRateEnum = SampleRateEnum.SAMPLE_RATE_8K;
        }

        int threadNum = Integer.parseInt(args[4]);

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

                    //语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
                    transcriber.send(ins, 3200, 100);
                }

                // 通知服务端语音数据发送完毕,等待服务端处理完成
                transcriber.stop();
                transcriber.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }
}
