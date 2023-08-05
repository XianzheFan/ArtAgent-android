# ArtAgent-android
An Art Agent based on GPT4, Stable Diffusion, and finetuned VisualGLM-6B

# TODO
edit image一系列操作

从相册中选择图片发到聊天框里时，MainActivity的界面老是弹出来

>1.初始界面是聊天框 

>2.在chat页面的左上角加一个页面跳转至工具栏的按钮，在工具栏左上角加一个页面跳转至聊天框的按钮，左下角的按钮只用来收起页面（单击为缩成一横条，双击为缩成一个小圆）

如何把听歌识曲和情绪识别与ivsend.click绑定

无障碍识别的问题：公众号文章有时无法识别

现在先不取消各个功能显示的界面，怕出错，可以在点击文本框发送的时候再加个 log

有消息的时候自动滚动到底部

[ChatGPT](https://chat.openai.com/share/b48620a3-3fa7-4a16-88ba-1d5c30b70625)

服务器正在处理请求的时候加上旋转的 loading…

加一个 new topic 按钮（在不清除聊天记录的情况下消除 Agent 记忆）+ clear 按钮（清除聊天记录）

推荐主题（类似于gpt最新的界面）

返回图片会有网络问题：java.net.SocketException: Connection reset 可能是我流量数据的问题

无法在其他应用中使用键盘（WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY的性质，暂时没法改）
