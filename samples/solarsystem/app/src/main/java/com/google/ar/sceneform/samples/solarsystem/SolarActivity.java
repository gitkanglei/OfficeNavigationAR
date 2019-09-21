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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;
import com.ardog.model.DogPoint;
import com.ardog.models.ModelLoaderManager;
import com.ardog.utils.DrawLineHelper;
import com.ardog.utils.FileUtils;
import com.ardog.utils.PointUtil;
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

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore and Sceneform APIs.
 */
public class SolarActivity extends AppCompatActivity {
  private static final int RC_PERMISSIONS = 0x123;
  // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
  private static final float AU_TO_METERS = 0.5f;
  private final SolarSettings solarSettings = new SolarSettings();
  private boolean installRequested;
  private GestureDetector gestureDetector;
  private Snackbar loadingMessageSnackbar = null;
  private ArSceneView arSceneView;
  private ModelRenderable sunRenderable;
  private ModelRenderable mercuryRenderable;
  private ModelRenderable venusRenderable;
  private ModelRenderable earthRenderable;
  private ModelRenderable lunaRenderable;
  private ModelRenderable marsRenderable;
  private ModelRenderable jupiterRenderable;
  private ModelRenderable saturnRenderable;
  private ModelRenderable uranusRenderable;
  private ModelRenderable neptuneRenderable;
  private ViewRenderable solarControlsRenderable;
  private ViewRenderable personRender;
  // True once scene is loaded
  private boolean hasFinishedLoading = false;
  // True once the scene has been placed.
  private boolean hasPlacedSolarSystem = false;
  private ModelLoaderManager modelLoaderManager;
  private boolean isResumed = false;
  private List<Anchor> anchors = new ArrayList<>();

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
    // Set up a tap gesture detector.
    gestureDetector =
        new GestureDetector(
            this,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
              }

              @Override
              public boolean onDown(MotionEvent e) {
                return true;
              }
            });

    // Set a touch listener on the Scene to listen for taps.
    arSceneView
        .getScene()
        .setOnTouchListener(
            (HitTestResult hitTestResult, MotionEvent event) -> {
              // If the solar system hasn't been placed yet, detect a tap and then check to see if
              // the tap occurred on an ARCore plane to place the solar system.
              if (!hasPlacedSolarSystem) {
                return gestureDetector.onTouchEvent(event);
              }

              // Otherwise return false so that the touch event can propagate to the scene.
              return false;
            });

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
            if (isResumed) {
              resume();
              isResumed = false;
            }
          }
        }
    );

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
            Anchor anchor = arSceneView.getSession().createAnchor(frame.getAndroidSensorPose());
            onLocationChanged(anchor);
          }
        }
    );

    // LasøØly request CAMERA permission which is required by ARCore.
    DemoUtils.requestCameraPermission(this, RC_PERMISSIONS);
  }

  private void onLocationChanged(Anchor currentAnchor) {
    
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

    modelLoaderManager = new ModelLoaderManager(this);
    modelLoaderManager.loadModelRenderablesFromDirectory(FileUtils.getARPath(), arrays,
        extraRenderable, maps -> {
          sunRenderable = (ModelRenderable) maps.get("Sol.sfb");
          mercuryRenderable = (ModelRenderable) maps.get("Mercury.sfb");
          venusRenderable = (ModelRenderable) maps.get("Venus.sfb");
          earthRenderable = (ModelRenderable) maps.get("Earth.sfb");
          lunaRenderable = (ModelRenderable) maps.get("Luna.sfb");
          marsRenderable = (ModelRenderable) maps.get("Mars.sfb");
          jupiterRenderable = (ModelRenderable) maps.get("Jupiter.sfb");
          saturnRenderable = (ModelRenderable) maps.get("Saturn.sfb");
          uranusRenderable = (ModelRenderable) maps.get("Uranus.sfb");
          neptuneRenderable = (ModelRenderable) maps.get("Neptune.sfb");
          solarControlsRenderable = solarControlsStage.get();

          // Everything finished loading successfully.
          hasFinishedLoading = true;
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
      return;
    }

    if (arSceneView.getSession() != null) {
      showLoadingMessage();
      isResumed = true;
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

  private void onSingleTap(MotionEvent tap) {
    if (!hasFinishedLoading) {
      // We can't do anything yet.
      return;
    }

//    Frame frame = arSceneView.getArFrame();
////    if (frame != null) {
////      if (!hasPlacedSolarSystem && tryPlaceSolarSystem(tap, frame)) {
////        hasPlacedSolarSystem = true;
////      }
////    }
  }

  private void resume() {
    DrawLineHelper drawLineHelper = new DrawLineHelper(this, arSceneView);
    List<DogPoint> dogPointList = PointUtil.json2List(PointUtil.readFromFile());
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
  }

  private boolean tryPlaceSolarSystem(MotionEvent tap, Frame frame) {
    if (tap != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
      for (HitResult hit : frame.hitTest(tap)) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
          // Create the Anchor.
          Anchor anchor = hit.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arSceneView.getScene());
          Node solarSystem = createSolarSystem();
          anchorNode.addChild(solarSystem);
          return true;
        }
      }
    }

    return false;
  }

  private Node createSolarSystem() {
    Node base = new Node();

    Node sun = new Node();
    sun.setParent(base);
    sun.setLocalPosition(new Vector3(0.0f, 0.5f, 0.0f));

    Node sunVisual = new Node();
    sunVisual.setParent(sun);
    sunVisual.setRenderable(sunRenderable);
    sunVisual.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));

    Node solarControls = new Node();
    solarControls.setParent(sun);
    solarControls.setRenderable(solarControlsRenderable);
    solarControls.setLocalPosition(new Vector3(0.0f, 0.25f, 0.0f));

    View solarControlsView = solarControlsRenderable.getView();
    SeekBar orbitSpeedBar = solarControlsView.findViewById(R.id.orbitSpeedBar);
    orbitSpeedBar.setProgress((int) (solarSettings.getOrbitSpeedMultiplier() * 10.0f));
    orbitSpeedBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float ratio = (float) progress / (float) orbitSpeedBar.getMax();
            solarSettings.setOrbitSpeedMultiplier(ratio * 10.0f);
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
          }

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
          }
        });

    SeekBar rotationSpeedBar = solarControlsView.findViewById(R.id.rotationSpeedBar);
    rotationSpeedBar.setProgress((int) (solarSettings.getRotationSpeedMultiplier() * 10.0f));
    rotationSpeedBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float ratio = (float) progress / (float) rotationSpeedBar.getMax();
            solarSettings.setRotationSpeedMultiplier(ratio * 10.0f);
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
          }

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
          }
        });

    // Toggle the solar controls on and off by tapping the sun.
    sunVisual.setOnTapListener(
        (hitTestResult, motionEvent) -> solarControls.setEnabled(!solarControls.isEnabled()));

    createPlanet("Mercury", sun, 0.4f, 47f, mercuryRenderable, 0.019f, 0.03f);

    createPlanet("Venus", sun, 0.7f, 35f, venusRenderable, 0.0475f, 2.64f);

    Node earth = createPlanet("Earth", sun, 1.0f, 29f, earthRenderable, 0.05f, 23.4f);

    createPlanet("Moon", earth, 0.15f, 100f, lunaRenderable, 0.018f, 6.68f);

    createPlanet("Mars", sun, 1.5f, 24f, marsRenderable, 0.0265f, 25.19f);

    createPlanet("Jupiter", sun, 2.2f, 13f, jupiterRenderable, 0.16f, 3.13f);

    createPlanet("Saturn", sun, 3.5f, 9f, saturnRenderable, 0.1325f, 26.73f);

    createPlanet("Uranus", sun, 5.2f, 7f, uranusRenderable, 0.1f, 82.23f);

    createPlanet("Neptune", sun, 6.1f, 5f, neptuneRenderable, 0.074f, 28.32f);

    return base;
  }
  private Node createNode(ViewRenderable viewRender,DogPoint point){
    Node node=new Node();
    node.setRenderable(viewRender);
    node.setLocalPosition(point.toPositionVector());
    node.setLocalRotation(point.toQuaternion());
    return node;
  }
  private CompletableFuture<ViewRenderable>  createFuture(int id){
    CompletableFuture<ViewRenderable> solarControlsStage =
            ViewRenderable.builder().setView(this,id).build();
    return solarControlsStage;
  }
  private Node createPlanet(
      String name,
      Node parent,
      float auFromParent,
      float orbitDegreesPerSecond,
      ModelRenderable renderable,
      float planetScale,
      float axisTilt) {
    // Orbit is a rotating node with no renderable positioned at the sun.
    // The planet is positioned relative to the orbit so that it appears to rotate around the sun.
    // This is done instead of making the sun rotate so each planet can orbit at its own speed.
    RotatingNode orbit = new RotatingNode(solarSettings, true, false, 0);
    orbit.setDegreesPerSecond(orbitDegreesPerSecond);
    orbit.setParent(parent);

    // Create the planet and position it relative to the sun.
    Planet planet =
        new Planet(
            this, name, planetScale, orbitDegreesPerSecond, axisTilt, renderable, solarSettings);
    planet.setParent(orbit);
    planet.setLocalPosition(new Vector3(auFromParent * AU_TO_METERS, 0.0f, 0.0f));

    return planet;
  }

  private void showLoadingMessage() {
    if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
      return;
    }

    loadingMessageSnackbar =
        Snackbar.make(
            SolarActivity.this.findViewById(android.R.id.content),
            R.string.plane_finding,
            Snackbar.LENGTH_INDEFINITE);
    loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
    loadingMessageSnackbar.show();
  }

  private void hideLoadingMessage() {
    if (loadingMessageSnackbar == null) {
      return;
    }

    loadingMessageSnackbar.dismiss();
    loadingMessageSnackbar = null;
  }
}
