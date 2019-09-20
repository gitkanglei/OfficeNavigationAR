package com.ardog.models;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.ardog.utils.FileUtils;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.solarsystem.DemoUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class ModelLoaderManager {

    RxPermissions rxPermissions;

    private Context context;

    public ModelLoaderManager(FragmentActivity activity) {
        this.context = activity;
        rxPermissions = new RxPermissions(activity);
    }

    public ModelLoaderManager(Fragment fragment) {
        this.context = fragment.getContext();
        rxPermissions = new RxPermissions(fragment);
    }

    public void loadModelRenderablesFromDirectory(
            String directoryPath,
            String[] modelArrays,
            Map<String, CompletableFuture<ViewRenderable>> extrasRenders,
            Consumer<Map<String, Renderable>> hander) {
        if (modelArrays == null || modelArrays.length <= 0) {
            return;
        }

        List<String> list = new ArrayList<>();
        for (String s : modelArrays) {
            list.add(s);
        }
        loadModelRenderablesFromDirectory(directoryPath, list, extrasRenders, hander);
    }

    public void loadModelRenderablesFromDirectory(
            String directoryPath,
            List<String> modelList,
            Map<String, CompletableFuture<ViewRenderable>> extrasRenders,
            Consumer<Map<String, Renderable>> hander) {

        if (TextUtils.isEmpty(directoryPath) || modelList == null || modelList.isEmpty()) {
            return;
        }

        Map<String, String> map = new LinkedHashMap<>();
        List<String> assertModelMap = new LinkedList<>();
        for (String name : modelList) {
            String path = directoryPath + "/" + name;
            if (!FileUtils.isFileExists(path)) {
                assertModelMap.add(name);
                continue;
            }
            map.put(name, path);
        }
        loadModelRenderables(map, extrasRenders, assertModelMap, hander);
    }

    public void loadModelRenderables(
            Map<String, String> sourcePaths,
            Map<String, CompletableFuture<ViewRenderable>> extrasRenders,
            List<String> fileSourceList,
            Consumer<Map<String, Renderable>> hander) {
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(granted -> {
                    if (!granted) {
                        Toast.makeText(context, "请授予读写权限", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadModels(sourcePaths, extrasRenders, fileSourceList, hander);
                });
    }

    private void loadModels(Map<String, String> sourcePaths,
                            Map<String, CompletableFuture<ViewRenderable>> extrasRenders,
                            List<String> fileSourceList,
                            Consumer<Map<String, Renderable>> hander) {
        Map<CompletableFuture<ModelRenderable>, String> map = new LinkedHashMap<>();

        CompletableFuture<ModelRenderable> array[] = new CompletableFuture[sourcePaths.size() + fileSourceList.size()];
        int index = 0;
        for (Map.Entry<String, String> e : sourcePaths.entrySet()) {
            CompletableFuture<ModelRenderable> future =
                    ModelRenderable.builder().setSource(
                            context,
                            readModelFromSDCard(e.getValue()))
                            .build();
            map.put(future, e.getKey());
            array[index++] = future;
        }

        for (String assertModel : fileSourceList) {
            CompletableFuture<ModelRenderable> future =
                    ModelRenderable.builder().setSource(
                            context,
                            readModelFromAssert(assertModel))
                            .build();
            map.put(future, assertModel);
            array[index++] = future;
        }

        CompletableFuture.allOf(array)
                .handle((notUsed, throwable) -> {

                    // When you build a Renderable, Sceneform loads its resources in the background while
                    // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                    // before calling get().

                    if (throwable != null) {
                        DemoUtils.displayError(context, "Unable to load renderable", throwable);
                        return null;
                    }

                    try {
                        Map<String, Renderable> result = new LinkedHashMap<>();
                        for (int i = 0; i < array.length; i++) {
                            String name = map.get(array[i]);
                            ModelRenderable renderable = array[i].get();
                            result.put(name, renderable);
                        }

                        if (extrasRenders != null || !extrasRenders.isEmpty()) {
                            for (Map.Entry<String, CompletableFuture<ViewRenderable>> e : extrasRenders.entrySet()) {
                                Renderable r = e.getValue().get();
                                result.put(e.getKey(), r);
                            }
                        }

                        if (hander == null) {
                            return null;
                        }
                        hander.accept(result);

                    } catch (InterruptedException | ExecutionException ex) {
                        DemoUtils.displayError(context, "Unable to load renderable", ex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;

                });
    }

    private Uri readModelFromAssert(String path) {
        return Uri.parse(path);
    }

    private Callable<InputStream> readModelFromSDCard(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return null;
        }
        return new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                try {
                    InputStream inputStream = new FileInputStream(f);
                    return inputStream;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }


}
