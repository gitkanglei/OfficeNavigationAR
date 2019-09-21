package com.google.ar.sceneform.samples.solarsystem

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.iflytek.cloud.SpeechUtility

/**
 * @author: crease.xu
 * @version: v1.0
 * @since: 2019-09-20
 * Description:
 *
 * Modification History:
 * -----------------------------------------------------------------------------------
 * Why & What is modified:
 */
class MyNewApplication : Application(){
  override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)
    MultiDex.install(base)
  }

  override fun onCreate() {
    super.onCreate()
    // 应用程序入口处调用，避免手机内存过小，杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
    // 如在Application中调用初始化，需要在Mainifest中注册该Applicaiton
    // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
    // 参数间使用半角“,”分隔。
    // 设置你申请的应用appid,请勿在'='与appid之间添加空格及空转义符
    // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误
    SpeechUtility.createUtility(this, "appid=5d858168" )
  }
}