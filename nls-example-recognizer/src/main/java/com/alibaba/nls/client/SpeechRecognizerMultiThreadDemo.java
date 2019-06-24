package com.alibaba.nls.client;

import java.io.File;
import java.io.FileInputStream;
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

/**
 * Created by siwei on 2018/06/20
 */
public class SpeechRecognizerMultiThreadDemo {
    private static SampleRateEnum sampleRateEnum=SampleRateEnum.SAMPLE_RATE_16K;
    final static OpuCodec codec = new OpuCodec();

    private static SpeechRecognizerListener getRecognizerListener(final String inputName) {
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            //识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                //事件名称 RecognitionResultChanged
                ", input stream: " + inputName +
                        ", name: " + response.getName() +
                        //状态码 20000000 表示识别成功
                        ", status: " + response.getStatus() +
                        //语音识别文本
                        ", result: " + response.getRecognizedText());
            }

            //识别完毕
            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                System.out.println("task_id: " + response.getTaskId() +
                //事件名称 RecognitionCompleted
                ", input stream: " + inputName +
                        ", name: " + response.getName() +
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
                System.out.println("input stream: " + inputName +
                    ", task_id: " + response.getTaskId() +
                    //状态码 20000000 表示识别成功
                    ", status: " + response.getStatus() +
                    //语音识别文本
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

        public Task(String appKey, NlsClient client, CountDownLatch latch, String audioFile,boolean isOpuEncode) {
            this.appKey = appKey;
            this.client = client;
            this.latch = latch;
            this.audioFile = audioFile;
            this.isOpuEncode=isOpuEncode;
        }

        @Override
        public void run() {
            try {
                File file = new File(audioFile);
                String audioFileName = file.getName();
                //创建实例,建立连接
                final SpeechRecognizer recognizer = new SpeechRecognizer(client, getRecognizerListener(audioFileName));
                recognizer.setAppKey(appKey);
                //设置音频编码格式
                recognizer.setFormat(InputFormatEnum.PCM);
                //设置音频采样率
                recognizer.setSampleRate(sampleRateEnum);
                //设置是否返回中间识别结果
                recognizer.setEnableIntermediateResult(true);
                InputStream ins = new FileInputStream(file);
                if(isOpuEncode){
                    recognizer.setFormat(InputFormatEnum.OPU);
                    recognizer.start();
                    codec.encode(16000, ins, new EncodeListener() {
                        @Override
                        public void onEncodedData(byte[] data) {
                            recognizer.send(data);
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }else{
                    //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                    recognizer.start();

                    //语音数据来自声音文件用此方法,控制发送速率;若语音来自实时录音,不需控制发送速率直接调用 recognizer.sent(ins)即可
                    recognizer.send(ins, 3200, 100);
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
    }

    public static void main(String[] args) throws Exception{
        if (args.length < 5) {
            System.err.println("SpeechRecognizerMultiThreadDemo need params: " +
                    "<app-key> <token> <url> <audio-file> <thread-num>");
            System.exit(-1);
        }

        String appKey = args[0];
        String token = args[1];
        String url = args[2];
        String audioFile = args[3];
        int threadNum = Integer.parseInt(args[4]);

        String isOpuEnable=System.getProperty("isOpuEnable","false");
        String sampleRate=System.getProperty("sampleRate","16000");
        if("8000".equals(sampleRate)){
            sampleRateEnum=SampleRateEnum.SAMPLE_RATE_8K;
        }

        final NlsClient client = new NlsClient(url, token);
        CountDownLatch latch = new CountDownLatch(threadNum);

        try {
            for (int i = 0; i < threadNum; i++) {
                Task task = new Task(appKey, client, latch, audioFile,Boolean.parseBoolean(isOpuEnable));
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
