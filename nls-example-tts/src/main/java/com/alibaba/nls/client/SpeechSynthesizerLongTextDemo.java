package com.alibaba.nls.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;

/**
 * tts 支持最多300个字符,此demo展示超过300字符的调用方式
 *
 * @author zhishen.ml
 * @date 2018-06-12
 */
public class SpeechSynthesizerLongTextDemo {
    private String appKey;
    private String accessToken;
    NlsClient client;

    public SpeechSynthesizerLongTextDemo(String appKey, String token) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
        client = new NlsClient(accessToken);
    }

    public SpeechSynthesizerLongTextDemo(String appKey, String token, String url) {
        this.appKey = appKey;
        this.accessToken = token;
        //创建NlsClient实例,应用全局创建一个即可,用户指定服务地址
        client = new NlsClient(url, accessToken);
    }

    private static SpeechSynthesizerListener getSynthesizerListener(final FileOutputStream fout) {

        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {

                //语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
                    System.out.println("task_id: " + response.getTaskId() +
                    ", name: " + response.getName() +
                        ", status: " + response.getStatus()
                    );
                }

                //语音合成的语音二进制数据
                @Override
                public void onMessage(ByteBuffer message) {
                    try {
                        byte[] bytesArray = new byte[message.remaining()];
                        message.get(bytesArray, 0, bytesArray.length);
                        System.out.println("write arrya:" + bytesArray.length);
                        fout.write(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFail(SpeechSynthesizerResponse response) {
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

    public void process(final String longText, final FileOutputStream fout) {
        List<String> textArr = splitLongText(longText, 100);
        SpeechSynthesizer synthesizer = null;
        try {
            //创建实例,建立连接
            synthesizer = new SpeechSynthesizer(client, getSynthesizerListener(fout));
            synthesizer.setAppKey(appKey);
            //此处一定要设置为pcm格式,才能将多次结果拼接起来
            synthesizer.setFormat(OutputFormatEnum.PCM);
            //设置返回音频的采样率
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);

            for (int i = 0; i < textArr.size(); i++) {
                //设置用于语音合成的文本
                synthesizer.setText(textArr.get(i));
                //此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
                synthesizer.start();
                //等待语音合成结束
                synthesizer.waitForComplete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭连接
            if (null != synthesizer) {
                synthesizer.close();
            }
        }
    }

    /**
     * 将长文本切分为每句字数不大于size数目的短句
     *
     * @param text
     * @param size
     * @return
     */
    public static List<String> splitLongText(String text, int size) {
        //先按标点符号切分
        String[] texts = text.split("[、，。；？！,!\\?]");
        StringBuilder textPart = new StringBuilder();
        List<String> result = new ArrayList<String>();
        int len = 0;
        //再按size merge,避免标点符号切分出来的太短
        for (int i = 0; i < texts.length; i++) {
            if (textPart.length() + texts[i].length() + 1 > size) {
                result.add(textPart.toString());
                textPart.delete(0, textPart.length());

            }
            textPart.append(texts[i]);
            len += texts[i].length();
            if(len<text.length()){
                textPart.append(text.charAt(len));
                len += 1;
            }

        }
        if (textPart.length() > 0) {
            result.add(textPart.toString());
        }

        return result;

    }

    public void shutdown() {
        client.shutdown();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("SpeechSynthesizerDemo need params: <app-key> <token> <url>");
            System.exit(-1);
        }
        String ttsTextLong = "百草堂与三味书屋 鲁迅 \n" +
            "我家的后面有一个很大的园，相传叫作百草园。现在是早已并屋子一起卖给朱文公的子孙了，连那最末次的相见也已经隔了七八年，其中似乎确凿只有一些野草；但那时却是我的乐园。\n" +
            "不必说碧绿的菜畦，光滑的石井栏，高大的皂荚树，紫红的桑葚；也不必说鸣蝉在树叶里长吟，肥胖的黄蜂伏在菜花上，轻捷的叫天子(云雀)忽然从草间直窜向云霄里去了。\n" +
            "单是周围的短短的泥墙根一带，就有无限趣味。油蛉在这里低唱，蟋蟀们在这里弹琴。翻开断砖来，有时会遇见蜈蚣；还有斑蝥，倘若用手指按住它的脊梁，便会啪的一声，\n" +
            "从后窍喷出一阵烟雾。何首乌藤和木莲藤缠络着，木莲有莲房一般的果实，何首乌有臃肿的根。有人说，何首乌根是有像人形的，吃了便可以成仙，我于是常常拔它起来，牵连不断地拔起来，\n" +
            "也曾因此弄坏了泥墙，却从来没有见过有一块根像人样。如果不怕刺，还可以摘到覆盆子，像小珊瑚珠攒成的小球，又酸又甜，色味都比桑葚要好得远......";

        String appKey = args[0];
        String token = args[1];
        String url = args[2];

        SpeechSynthesizerLongTextDemo demo = new SpeechSynthesizerLongTextDemo(appKey, token, url);
        File out = new File("longText4TTS.pcm");
        demo.process(ttsTextLong, new FileOutputStream(out));
        demo.shutdown();
    }
}
