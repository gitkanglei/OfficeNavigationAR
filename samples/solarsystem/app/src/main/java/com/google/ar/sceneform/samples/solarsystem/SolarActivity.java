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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
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
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
  private boolean hasPlacedSolarSystem = false;
  private List<Anchor> anchors = new ArrayList<>();
  private PathFinder pathFinder;

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

    EditText et_name = findViewById(R.id.et_name);
    et_name.setOnKeyListener(new View.OnKeyListener() {
      @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_ENTER == keyCode && KeyEvent.ACTION_DOWN == event.getAction()) {
          start(et_name.getText().toString());
          et_name.setText("");
          InputMethodManager imm =
              (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.showSoftInput(et_name, InputMethodManager.SHOW_FORCED);

          imm.hideSoftInputFromWindow(et_name.getWindowToken(), 0);
          return true;
        }
        return false;
      }
    });
  }

  private void loadModels() {
    String[] arrays = {
        "Sol.sfb", "Mercury.sfb", "Venus.sfb",
        "Earth.sfb", "Luna.sfb", "Mars.sfb",
        "Jupiter.sfb", "Saturn.sfb", "Uranus.sfb",
        "Neptune.sfb"
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

            resume(point, pathFinder.findPoint(goalName));
            arSceneView.getScene().removeOnUpdateListener(this);
          }
        }
    );
  }

  private void resume(DogPoint startPoint, DogPoint endPoint) {
    DrawLineHelper drawLineHelper = new DrawLineHelper(this, arSceneView);
    List<DogPoint> dogPointList = pathFinder.findRoutes(startPoint, endPoint);
    if (dogPointList.isEmpty()) {
      ToastUtils.showShort("两点之间不可达");
      return;
    }
    for (DogPoint dogPoint : dogPointList) {
      Anchor anchor1 =
          arSceneView.getSession().createAnchor(new Pose(dogPoint.position, dogPoint.rotation));
      createAnchor(anchor1);
      anchors.add(anchor1);
    }
    for (int i = 0; i < anchors.size(); i++) {
      int nextpostion = i + 1;
      if (nextpostion == anchors.size()) {
        break;
      }
      Anchor dogPoint = anchors.get(i);
      Anchor nextPoint = anchors.get(nextpostion);
      drawLineHelper.drawLine(dogPoint, nextPoint);
    }
  }

  private void createAnchor(Anchor anchor) {
    AnchorNode anchorNode = new AnchorNode(anchor);
    anchorNode.setParent(arSceneView.getScene());
    Node no = new Node();
    no.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
    no.setRenderable(earthRenderable);
    no.setParent(anchorNode);
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
            faceToCameraNode.setLocalPosition(new Vector3(0f, 0.1f, 0f));
            faceToCameraNode.setRenderable(viewRenderable);
          }
        });
  }

  private void hideLoadingMessage() {
    if (loadingMessageSnackbar == null) {
      return;
    }

    loadingMessageSnackbar.dismiss();
    loadingMessageSnackbar = null;
  }
}
