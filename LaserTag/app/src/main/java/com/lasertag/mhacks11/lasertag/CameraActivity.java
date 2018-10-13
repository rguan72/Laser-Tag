package com.lasertag.mhacks11.lasertag;

import java.util.ArrayList;
import java.util.List;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;



public class CameraActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "CameraActivity";

    // Are we showing the filtered video feed?
    // set to FALSE when deploying
    private static final boolean IS_DEBUG_VIDEO = false;

    // Radius of accuracy needed to shoot a target
    private static final int SHOOT_THRESHOLD = 200;

    // The target that we find
    private RotatedRect target = null;
    // Are we locked on and able to shoot a target?
    private boolean lockedOn;

    // Audio handling
    private SoundPool soundPool;
    private int soundID_shoot,
                soundID_empty,
                soundID_reload;

    private ArrayList<Mat> channels;
    private Mat camInput;
    private Mat hueFiltered,
            satFiltered,
            valFiltered;

    private CameraBridgeViewBase mOpenCvCameraView;

    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.setOnTouchListener(CameraActivity.this);
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        //Ask for Camera Permission
        getCameraPermission();

        // Audio
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        // TODO: Can this be left commented out?
        /*soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                loaded = true;
            }
        });*/
        soundID_shoot  = soundPool.load(this, R.raw.shoot,  1);
        soundID_empty  = soundPool.load(this, R.raw.empty,  1);
        soundID_reload = soundPool.load(this, R.raw.reload, 1);

        mOpenCvCameraView = findViewById(R.id.camera_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        getCameraPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void getCameraPermission ()
    {
        //Log.d ("HI", "HI");
        int permissionCheck = getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA);
        //Log.d("Permission", "Permission: " + permissionCheck + ", and " + PackageManager.PERMISSION_DENIED);
        if (permissionCheck== PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},0);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        // TODO: Init CV Constants here
        channels = new ArrayList<>();
        camInput = new Mat(height, width, CvType.CV_8UC4);
        hueFiltered = new Mat();
        satFiltered = new Mat();
        valFiltered = new Mat();
    }

    public void onCameraViewStopped() {
        // TODO: Release all mats
        //mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        // TODO: Fire!
        playSound(soundID_shoot);

        return false; // don't need subsequent touch events
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        camInput = inputFrame.rgba();
        Mat mask = new Mat();
        // Convert to HSV

        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_RGB2HSV);

        // Split channels
        Core.split(camInput, channels);


//          //Filter HSV
        Core.inRange(channels.get(0), new Scalar(40), new Scalar(93), hueFiltered );
        Core.inRange(channels.get(1), new Scalar(60), new Scalar(255), satFiltered );
        Core.inRange(channels.get(2), new Scalar(48), new Scalar(238), valFiltered );

//        // Mix together
        Core.bitwise_and(hueFiltered, satFiltered, satFiltered);
        Core.bitwise_and(satFiltered, valFiltered, camInput);

        // Erode and Dilate
        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9));
        Imgproc.erode(camInput, camInput, erodeKernel);
        Imgproc.dilate(camInput, camInput, dilateKernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(camInput, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double largestArea = 0.0;
        RotatedRect largestRect = null;
        for(MatOfPoint mop : contours) {
            MatOfPoint2f mop2f = new MatOfPoint2f();
            mop.convertTo(mop2f, CvType.CV_32FC1);
            RotatedRect rect = Imgproc.minAreaRect(mop2f);

            if (rect.size.area() > largestArea) {
                largestArea = rect.size.area();
                largestRect = rect;
            }
        }

        // Set our target. NOTE: This can be null.
        setTarget(largestRect);

        // Are we positioned to actually HIT a target?
        setLockedOn(false);
        if (hasTarget()) {

            double dx = getTarget().center.x - camInput.width() / 2,
                   dy = getTarget().center.y - camInput.height() / 2;

            if (dx*dx + dy*dy < SHOOT_THRESHOLD*SHOOT_THRESHOLD) {
                setLockedOn(true);
            }
            if (IS_DEBUG_VIDEO) {
                // Draw a circle or whatever around our main rect!
                Imgproc.circle(camInput, getTarget().center, 10, new Scalar(255, 0, 0));
            }
        }

        if (isLockedOn()) {
            vibrate(100);
        }

        // Green screen, if we're playing the game
        if (!IS_DEBUG_VIDEO) {
            camInput.setTo(new Scalar(0, 255, 0));
        }

        // Clean up
        for(Mat mat : channels) {
            mat.release();
        }
        mask.release();
        erodeKernel.release();
        dilateKernel.release();
        System.gc();

        if (IS_DEBUG_VIDEO) {
            // Flip video so it's reasonable when displayed
            Mat dest = new Mat();
            Core.flip(camInput, dest, 1);
            dest.copyTo(camInput);
            dest.release();
        }

        return camInput;
    }

    /**
     * Vibrates phone for a given time in milliseconds
     * @param ms : Time (in milliseconds) to vibrate
     */
    private void vibrate(int ms) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            //deprecated in API 26
            v.vibrate(ms);
        }
    }

    /**
     * Plays a sound
     * @param soundID (one of three. See declarations above.)
     */
    private void playSound(int soundID) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = actualVolume / maxVolume;
        soundPool.play(soundID, volume, volume, 1, 0, 1f);
    }


    // Target getters and setters
    private void setTarget(RotatedRect target) {
        this.target = target;
    }
    private RotatedRect getTarget() {
        return target;
    }
    private boolean hasTarget() {
        return target != null;
    }

    // Locked on getters and setters
    private void setLockedOn(boolean lockedOn) {
        this.lockedOn = lockedOn;
    }
    private boolean isLockedOn() {
        return lockedOn;
    }
}
