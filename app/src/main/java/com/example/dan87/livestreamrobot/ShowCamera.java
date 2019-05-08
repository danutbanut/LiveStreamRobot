package com.example.dan87.livestreamrobot;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback {
    //<editor-fold defaultstate="collapsed" desc="Variables">
    Camera camera;
    SurfaceHolder holder;
    MainActivity main;
    private static final int AUTO_FOCUS_DELAY = 7500;
    private Runnable doAutoFocusRunnable = new Runnable() {
        @Override
        public void run() {
            camera.cancelAutoFocus();
            camera.autoFocus((success, camera) -> postDelayed(doAutoFocusRunnable, AUTO_FOCUS_DELAY));
        }
    };
    //</editor-fold>

    public ShowCamera(Context context, Camera camera, MainActivity main){
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);
        this.main = main;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = camera.getParameters();
        //Change the orientation of the camera
        if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
            params.set("orientation","portrait");
            camera.setDisplayOrientation(90);
            params.setRotation(90);
        }else{
            params.set("orientation","landscape");
            camera.setDisplayOrientation(0);
            params.setRotation(0);
        }

        camera.setParameters(params);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        boolean autoFocus = getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        if (holder.getSurface() == null){
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        try {
            camera.setPreviewCallback(main);
            camera.setPreviewDisplay(holder);
            camera.startPreview();

        } catch (Exception e){
            e.printStackTrace();
        }

        if (autoFocus) {
            postDelayed(doAutoFocusRunnable, AUTO_FOCUS_DELAY);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }
}
