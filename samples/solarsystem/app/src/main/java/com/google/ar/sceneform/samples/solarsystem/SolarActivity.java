/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.solarsystem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ardog.model.DogPoint;
import com.ardog.models.ModelLoaderManager;
import com.ardog.utils.DrawLineHelper;
import com.ardog.utils.FileUtils;
import com.ardog.utils.PathFinder;
import com.ardog.utils.PointUtil;
import com.blankj.utilcode.util.ToastUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.setting.IatSettings;
import com.iflytek.speech.util.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore and Sceneform APIs.
 */
public class SolarActivity extends AppCompatActivity {
  private static final int RC_PERMISSIONS = 0x123;
  // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
  private boolean installRequested;
  private Snackbar loadingMessageSnackbar = null;
  private ArSceneView arSceneView;
  private ModelRenderable earthRenderable;
  private ModelRenderable manRenderable;
  private ModelRenderable senceRenderable;
  private boolean hasPlacedSolarSystem = false;
  private List<Anchor> anchors = new ArrayList<>();
  private PathFinder pathFinder;

    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    private String resultType = "json";
    private boolean mTranslateEnable = false;
    private boolean cyclic = false;//音频流识别是否循环调用
    private StringBuffer buffer = new StringBuffer();
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private SharedPreferences mSharedPreferences;
    /**
     * 初始化监听。
     */
    private InitListener mInitTipsListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                ToastUtils.showShort("初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                ToastUtils.showShort("初始化失败,错误码：" + code);
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onSpeakPaused() {
        }

        @Override
        public void onSpeakResumed() {
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        @Override
        public void onCompleted(SpeechError error) {
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}

            //当设置SpeechConstant.TTS_DATA_NOTIFY为1时，抛出buf数据
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
						byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
						Log.e("MscSpeechLog", "buf is =" + buf);
					}*/

        }
    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            if( mTranslateEnable ){
            //翻译过来的
            }else{
                printResult(results);
            }
        }

        /**
         * 识别回调错误.
         */
        @Override
        public void onError(SpeechError error) {
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                ToastUtils.showShort( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                ToastUtils.showShort(error.getPlainDescription(true));
            }
        }

    };

  @Override
  @SuppressWarnings({ "AndroidApiChecker", "FutureReturnValueIgnored" })
  // CompletableFuture requires api level 24
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!DemoUtils.checkIsSupportedDeviceOrFinish(this)) {
      // Not a supported device.
      return;
    }

    setContentView(R.layout.activity_solar);
    arSceneView = findViewById(R.id.ar_scene_view);
    loadModels();
    // Set an update listener on the Scene that will hide the loading message once a Plane is
    // detected.
    arSceneView
        .getScene()
        .addOnUpdateListener(
            frameTime -> {
              if (loadingMessageSnackbar == null) {
                return;
              }

              Frame frame = arSceneView.getArFrame();
              if (frame == null) {
                return;
              }

              if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
              }

              for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                  hideLoadingMessage();
                }
              }
            });

    //arSceneView.getScene().addOnUpdateListener(
    //    //    new Scene.OnUpdateListener() {
    //    //      @Override public void onUpdate(FrameTime frameTime) {
    //    //        Frame frame = arSceneView.getArFrame();
    //    //        if (null == frame) {
    //    //          return;
    //    //        }
    //    //        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
    //    //          return;
    //    //        }
    //    //        if (isResumed) {
    //    //          resume();
    //    //          isResumed = false;
    //    //        }
    //    //      }
    //    //    }
    //    //);

    pathFinder = new PathFinder();
    pathFinder.init(PointUtil.json2List(PointUtil.readFromFile()));

    // LasøØly request CAMERA permission which is required by ARCore.
    DemoUtils.requestCameraPermission(this, RC_PERMISSIONS);

    findViewById(R.id.bt_name).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            buffer.setLength(0);
            mIatResults.clear();
            setListenParam();
            boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
            if (isShowDialog) {
                // 显示听写对话框
                mIatDialog.setListener(mRecognizerDialogListener);
                mIatDialog.show();
            }
        }
    });
      // 初始化合成对象
      mTts = SpeechSynthesizer.createSynthesizer(this, mInitTipsListener);
      // 初始化识别无UI识别对象
      // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
      mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
      // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
      // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
      mIatDialog = new RecognizerDialog(this, mInitListener);
      mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
  }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        start(resultBuffer.toString());
    }

  private void loadModels() {
    String[] arrays = {
        "Sol.sfb", "Mercury.sfb", "Venus.sfb",
        "Earth.sfb", "Luna.sfb", "Mars.sfb",
        "Jupiter.sfb", "Saturn.sfb", "Uranus.sfb",
        "Neptune.sfb","man.sfb","scene.sfb"
    };
    // Build a renderable from a 2D View.
    CompletableFuture<ViewRenderable> solarControlsStage =
        ViewRenderable.builder().setView(this, R.layout.solar_controls).build();
    CompletableFuture<ViewRenderable> personFuture =
        ViewRenderable.builder().setView(this, R.layout.view_person).build();
    Map<String, CompletableFuture<ViewRenderable>> extraRenderable = new LinkedHashMap<>();
    extraRenderable.put("solar", solarControlsStage);
    extraRenderable.put("person", personFuture);

    ModelLoaderManager modelLoaderManager = new ModelLoaderManager(this);
    modelLoaderManager.loadModelRenderablesFromDirectory(FileUtils.getARPath(), arrays,
        extraRenderable, maps -> {
          earthRenderable = (ModelRenderable) maps.get("Earth.sfb");
                manRenderable = (ModelRenderable) maps.get("man.sfb");
                senceRenderable = (ModelRenderable) maps.get("scene.sfb");
            });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (arSceneView == null) {
      return;
    }

    if (arSceneView.getSession() == null) {
      // If the session wasn't created yet, don't resume rendering.
      // This can happen if ARCore needs to be updated or permissions are not granted yet.
      try {
        Session session = DemoUtils.createArSession(this, installRequested);
        if (session == null) {
          installRequested = DemoUtils.hasCameraPermission(this);
          return;
        } else {
          arSceneView.setupSession(session);
        }
      } catch (UnavailableException e) {
        DemoUtils.handleSessionException(this, e);
      }
    }

    try {
      arSceneView.resume();
    } catch (CameraNotAvailableException ex) {
      DemoUtils.displayError(this, "Unable to get camera", ex);
      finish();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (arSceneView != null) {
      arSceneView.pause();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (arSceneView != null) {
      arSceneView.destroy();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
    if (!DemoUtils.hasCameraPermission(this)) {
      if (!DemoUtils.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        DemoUtils.launchPermissionSettings(this);
      } else {
        Toast.makeText(
            this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
            .show();
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      // Standard Android full-screen functionality.
      getWindow()
          .getDecorView()
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  private void start(String goalName) {
    arSceneView.getScene().addOnUpdateListener(
        new Scene.OnUpdateListener() {
          @Override public void onUpdate(FrameTime frameTime) {
            Frame frame = arSceneView.getArFrame();
            if (null == frame) {
              return;
            }
            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
              return;
            }

                        if (null == arSceneView.getSession()) {
                            return;
                        }
                        Pose currentPos = frame.getAndroidSensorPose();
                        DogPoint point = pathFinder.findNearestPoint(currentPos);
                        if (null == point) {
                            return;
                        }
                        String name = goalName.replace("。", "");
                        resume(point, pathFinder.findPoint(name));
                        arSceneView.getScene().removeOnUpdateListener(this);
                    }
                }
        );
    }

  private void resume(DogPoint startPoint, DogPoint endPoint) {
      if (endPoint==null){
          ToastUtils.showShort("没找到");
          return;
      }
    DrawLineHelper drawLineHelper = new DrawLineHelper(this, arSceneView);
    List<DogPoint> dogPointList = pathFinder.findRoutes(startPoint, endPoint);
    if (dogPointList.isEmpty()) {
      ToastUtils.showShort("两点之间不可达");
      return;
    }
    for (int i =0 ; i<dogPointList.size() ; i++) {
        DogPoint dogPoint = dogPointList.get(i);
      Anchor anchor1 =
          arSceneView.getSession().createAnchor(new Pose(dogPoint.position, dogPoint.rotation));
      boolean isDestination = i == dogPointList.size()-1;
      createAnchor(anchor1,dogPoint.name,isDestination);
      anchors.add(anchor1);
    }
      Log.e("TAG","start line");

    for (int i = 0; i < anchors.size(); i++) {
      int nextpostion = i + 1;
      if (nextpostion == anchors.size()) {
        break;
      }
        Log.e("TAG","draw line"+i);
      Anchor dogPoint = anchors.get(i);
      Anchor nextPoint = anchors.get(nextpostion);
      drawLineHelper.drawLine(dogPoint, nextPoint);
    }
      Log.e("TAG","end line");
  }

  private void createAnchor(Anchor anchor,String name,boolean isDestation) {
    AnchorNode anchorNode = new AnchorNode(anchor);
    anchorNode.setParent(arSceneView.getScene());
    Node no = new Node();
    no.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
    no.setRenderable(isDestation?senceRenderable:earthRenderable);
    no.setParent(anchorNode);
    if(no.getParent()!=null){
        no.getParent().removeChild(no);
    }
    if(isDestation){
        ViewRenderable.builder()
                .setView(this, R.layout.view_person)
                .build()
                .thenAccept(new Consumer<ViewRenderable>() {
                    @Override
                    public void accept(ViewRenderable viewRenderable) {
                        viewRenderable.setShadowCaster(false);
                        FaceToCameraNode faceToCameraNode = new FaceToCameraNode();
                        faceToCameraNode.setParent(anchorNode);
                        //faceToCameraNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));
                        faceToCameraNode.setLocalPosition(new Vector3(0f, 0.2f, 0f));
                        faceToCameraNode.setRenderable(viewRenderable);
                        View view = viewRenderable.getView();
                        TextView tvName = view.findViewById(R.id.tv_name);
                        if (!TextUtils.isEmpty(tvName.getText().toString())){
                            tvName.setText("今天请假不在公司");
                        }
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (TextUtils.isEmpty(tvName.getText().toString())){
                                    return;
                                }
                                sayHello("姓名张红川部分技术部");
                            }
                        });
                    }
                });
    }
  }
    /**
     * 文字转语音
     */
    private void sayHello(String texts) {
        // 设置参数
        setSayParam();
        int code = mTts.startSpeaking(texts, mTtsListener);
//			/**
//			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
//			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
//			*/
			/*String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
			int code = mTts.synthesizeToUri(text, path, mTtsListener);*/

        if (code != ErrorCode.SUCCESS) {
            ToastUtils.showShort("语音合成失败,错误码: " + code);
        }
    }

    /**
     * 参数设置
     *
     * @return
     */
    private void setSayParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //支持实时音频返回，仅在synthesizeToUri条件下支持
            //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaorong");
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "50");
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");

        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.pcm");
    }

    /**
     * 参数设置
     *
     * @return
     */
    public void setListenParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
        if( mTranslateEnable ){
            mIat.setParameter( SpeechConstant.ASR_SCH, "1" );
            mIat.setParameter( SpeechConstant.ADD_CAP, "translate" );
            mIat.setParameter( SpeechConstant.TRS_SRC, "its" );
        }

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "en" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "cn" );
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "cn" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "en" );
            }
        }
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

  private void hideLoadingMessage() {
    if (loadingMessageSnackbar == null) {
      return;
    }

    loadingMessageSnackbar.dismiss();
    loadingMessageSnackbar = null;
  }
}
