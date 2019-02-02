# 关键接口
NlsClient：语音处理client，相当于所有语音相关处理类的factory，全局创建一个实例即可。线程安全。
SpeechTranscriber：实时语音识别类，设置请求参数，发送请求及声音数据。非线程安全。
SpeechTranscriberListener：实时语音识别结果监听类，监听识别结果。非线程安全。
更多介绍参见API文档链接: [Java API接口说明](http://g.alicdn.com/idst-fe/nls-sdk-doc-api/2.0.6/GateWay_Java_2.0.2/index.html?spm=a2c4g.11186623.2.18.51515397OQA7ZZ)

# SDK 调用注意事项
NlsClient对象创建一次可以重复使用，每次创建消耗性能。NlsClient使用了netty的框架，创建时比较消耗时间和资源，但创建之后可以重复利用。建议调用程序将NlsClient的创建和关闭与程序本身的生命周期结合。
SpeechTranscriber对象不能重复使用，一个识别任务对应一个SpeechTranscriber对象。例如有N个音频文件，则要进行N次识别任务，创建N个SpeechTranscriber对象。
实现的SpeechTranscriberListener对象和SpeechTranscriber对象是一一对应的，不能将一个SpeechTranscriberListener对象设置到多个SpeechTranscriber对象中，否则不能区分是哪个识别任务。
