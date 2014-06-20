VoiceManager
============
1.实现了录音，播放；\n
2.暂停录音开始录音，合并为同一个；\n
3.显示录音时间，录了多少秒，时分秒；
4.代码总布局已经写好，可以自己修改；
这个包可以直接依赖。

用法：VoiceManage mVoiceManage = new VoiceManage(mActivity, v, path);

第一个参数为： Activity上下文，
第二个参数为： view 这个view具体是干嘛的？ 是干录完返回的，具体的返回方法自己定义，返回录音文件路径，
               具体在可以在VoiceManager 的// TODO 中看到
第三个参数为：保存录音文件的路径，一般都是包名+自定义文件名
