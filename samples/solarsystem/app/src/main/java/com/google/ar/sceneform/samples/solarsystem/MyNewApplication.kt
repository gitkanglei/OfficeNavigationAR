package com.google.ar.sceneform.samples.solarsystem

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex

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
}