# dtlive

完全开源可用的直播app demo. [English.](https://github.com/peterfuture/dtlive_android/blob/master/README_EN.md)

## 功能介
* 直播功能: H264+AAC编码通过rtmp协议推送
* 点播功能: 一个简单的使用videoview实现的demo
* 设置: 设置直播服务器地址等
 
## 测试方法
* [搭建red5服务器](https://github.com/red5-cn/red5-tutorial/wiki/1-%E5%A6%82%E4%BD%95%E6%BA%90%E7%A0%81%E7%BC%96%E8%AF%91%E5%B9%B6%E9%83%A8%E7%BD%B2red5)
* 手机或者终端通过设置页面设置推送地址，如rtmp://192.168.1.101:1935/live/test
* 点击直播按钮开始推送
* 通过pc播放：ffplay -probesize 1024 rtmp://192.168.1.101:1935/live/test


## 编译方法
* git clone https://github.com/peterfuture/dtlive_android
* 安装android studio以及sdk
* import源码并编译

[下载]()编译好的apk.

## 依赖仓库

* [dtcodec](https://github.com/peterfuture/dtcodec)－音视频编码开源库
* [dtrtmp](https://github.com/peterfuture/dtrtmp)－提供rtmp推送以及flv mux功能


## 参考项目
* [BottomBar](https://github.com/roughike/BottomBar)
* [Android UI素材库](https://github.com/google/material-design-icons/)
* [AnimeTaste](https://github.com/daimajia/AnimeTaste)



## 联系作者

* Email: peter_future@outlook.com
* QQ: 2575999523
