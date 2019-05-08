package com.example.dan87.livestreamrobot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends Activity implements Camera.PreviewCallback,GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private static final int MAX_BUFFER = 15;
    Camera camera;
    int width,height,length;
    FrameLayout frameLayout;
    private int previewFormat;
    ShowCamera showCamera;
    private Button mBtnConnect;
    private SocketClient mClient;
    MainActivity main = this;
    private boolean connected = false;
    private Queue<byte[]> framesQueue;
    boolean taken = true;
    private byte[] lastFrame,imageBytes;
    private GestureDetector gestureDetector;
    //private Handler handler = new Handler();
    //</editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        frameLayout = findViewById(R.id.frameLayout);
        mBtnConnect = findViewById(R.id.buttonConnect);
        framesQueue = new LinkedList<>();
        gestureDetector = new GestureDetector(this, this);

        //Start SocketClient - Thread
        mClient = new SocketClient(main);
        mClient.start();

        verifyPermissions();

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if (mBtnConnect.getText().equals("Connect")) {
                    mBtnConnect.setText("Disconnect");
                    connected = true;

                    //Start SocketClient communication
                    mClient.runningClient = true;
                    mClient.runningThread = true;

                    //Make button "GONE"
                    mBtnConnect.setVisibility(View.GONE);
                } else {
                    mBtnConnect.setText("Connect");
                    connected = false;

                    //Stop SocketClient communication
                    mClient.runningThread = false;
                    mClient.runningClient = false;
                }
            }
        });

    }

    private void verifyPermissions() {
        String[] permissions = {Manifest.permission.CAMERA};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[0]) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            setupAfterAcceptedPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera preview started !", Toast.LENGTH_LONG).show();
            setupAfterAcceptedPermissions();
        } else {
            Toast.makeText(this, "Camera preview stopped !", Toast.LENGTH_LONG).show();
            finish();
            System.exit(0);
        }
    }

    private void setupAfterAcceptedPermissions() {
        //Open camera
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        camera.setPreviewCallback(this);
        showCamera = new ShowCamera(this,camera,main);
        frameLayout.addView(showCamera);
        width = parameters.getPreviewSize().width;
        height = parameters.getPreviewSize().height;
        previewFormat = parameters.getPreviewFormat();
        length = width * height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //reset camera
        if(camera != null){
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }

        //reset variables
        mBtnConnect.setText("Connect");
        connected = false;
        framesQueue.clear();
        taken = true;
        lastFrame = null;
        imageBytes = null;

        //close socketclient
        if( mClient != null){
            mClient.runningThread = false;
            mClient.runningClient = false;
            try {
                mClient.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mClient = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //set camera
        if(camera == null){
            camera = Camera.open();
            camera.setPreviewCallback(this);
            showCamera.setCamera(camera);
        }
    }

    private void byteArrayToBitmap(byte[] data) {
        if(data != null){
            YuvImage yuvImage = new YuvImage(data, previewFormat, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            imageBytes = out.toByteArray();
        }
    }

    public byte[] receive(){
        taken = true;
        return lastFrame;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if(mBtnConnect.getVisibility()==View.VISIBLE){
            mBtnConnect.setVisibility(View.GONE);
        }else{
            mBtnConnect.setVisibility(View.VISIBLE);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //preview livestream
        byteArrayToBitmap(data.clone());

        //Add frames to queue if connected
        if(connected){
            if (framesQueue.size() == MAX_BUFFER) {
                framesQueue.poll();
            }
            framesQueue.add(imageBytes);
            if(taken){
                lastFrame = framesQueue.poll();
                taken = false;
            }
        }
    }

    public void showToast(String toast) {
        synchronized (mClient){
            runOnUiThread(() -> {
                mBtnConnect.setText("Connect");
                connected = false;

                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
