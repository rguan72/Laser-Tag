package com.lasertag.mhacks11.lasertag;

import java.util.ArrayList;
import java.util.List;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
import android.view.KeyEvent;
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

    // How many milliseconds to wait for exposure before reloading
    private static final int EXPOSE_TIME_THRESHOLD = 500;

    // The Game ID of this user. Set when you "join" the game
    private int myGameID = -1;
    // Are we alive?
    private static boolean isAlive = true;

    // The target that we find
    private RotatedRect target = null;
    // Are we locked on and able to shoot a target?
    private boolean lockedOn;

    // Do we have ammo
    private boolean hasAmmo = false;

    // When's the last time we were exposed?
    private long lastTimeExposed = 0;

    // Audio handling
    private static SoundPool soundPool;
    private static AudioManager audioManager = null;
    private static int soundID_shoot,
                       soundID_empty,
                       soundID_reload,
                       soundID_death;

    private ArrayList<Mat> channels;
    private Mat camInput;
    private Mat hueFiltered,
            satFiltered,
            valFiltered;

    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Gets the color of a player given their id
     * @param name
     * @return
     */
    private static final Scalar getPlayerCenterColor(String name) {
        switch (name) {
            case "player1":
                return new Scalar(255, 0, 0);
            case "player2":
                return new Scalar(0, 0, 255);
            case "player3":
                return new Scalar(255, 0, 255);
        }
        return new Scalar(0, 0, 0);
    }

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

        // We're alive!
        isAlive = true;

        // Audio
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        soundID_shoot  = soundPool.load(this, R.raw.shoot,  1);
        soundID_empty  = soundPool.load(this, R.raw.empty,  1);
        soundID_reload = soundPool.load(this, R.raw.reload, 2);
        soundID_death  = soundPool.load(this, R.raw.death, 2);

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
        int permissionCheck = getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA);
        if (permissionCheck== PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},0);
        }
    }

    // CV Constants
    public void onCameraViewStarted(int width, int height) {
        channels = new ArrayList<>();
        camInput = new Mat(height, width, CvType.CV_8UC4);
        hueFiltered = new Mat();
        satFiltered = new Mat();
        valFiltered = new Mat();
    }

    // Final cleanup
    public void onCameraViewStopped() {
        camInput.release();
    }

    // INPUTS
    public boolean onTouch(View v, MotionEvent event) {
        shoot();
        return false; // don't need subsequent touch events
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            shoot();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     *  Verifies that a potential target has a valid central code thing
     * @param camInput
     * @param potentialTarget
     * @return
     */
    private int verifyCentralColor(Mat camInput, RotatedRect potentialTarget) {
        if (Database.getPlayer().getName().equals("player1")) {
            return 0;
        } else {
            return 1;
        }
        // TODO: FAR FUTURE/POST HACKATHON: Implement this
        /*Scalar[] colors = new Scalar[3];
        colors[0] = getPlayerCenterColor("player1");
        colors[1] = getPlayerCenterColor("player2");
        colors[2] = getPlayerCenterColor("player3");

        Rect r = potentialTarget.boundingRect();
        Log.i("Rectangle", "(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")");
        Mat check = null;
        try {
            check = camInput.submat(r);
        } catch (CvException e) {
            Log.i("Rectangle: ","Avoided an exception");
            return -1;
        }
        Mat checker = new Mat();
        Mat rMat = new Mat(),
            gMat = new Mat(),
            bMat = new Mat();

        int result = -1;
        int delta = 20;
        for(int i = 0; i < colors.length; i++) {
            Scalar col = colors[i];
            Core.split(check, channels);
            Core.inRange(channels.get(0), new Scalar(col.val[0] - delta), new Scalar(col.val[0] + delta), rMat);
            Core.inRange(channels.get(1), new Scalar(col.val[1] - delta), new Scalar(col.val[1] + delta), gMat);
            Core.inRange(channels.get(2), new Scalar(col.val[2] - delta), new Scalar(col.val[2] + delta), bMat);

            Core.bitwise_and(rMat, gMat, checker);
            Core.bitwise_and(bMat, checker, checker);
            // Is checker not empty? If so, we have a value.
            if (Core.countNonZero(checker) != 0) {
            //if (Core.checkRange(checker, true, 1, 255)) {
                result = i;
                break;
            }
        }
        // Is dis bad?
        check.release();
        rMat.release();
        gMat.release();
        bMat.release();
        checker.release();

        return result;
        */
        /*
        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_HSV2RGB);
        double[] rgb = camInput.get((int)potentialTarget.center.y,(int)potentialTarget.center.x);
        Log.i("CenterColor", "(" + rgb[0] + ", " + rgb[1] + ", " + rgb[2] + ")");
        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_RGB2HSV);

        int thresh = 100;
        for(int i = 0; i < colors.length; i++) {
            Scalar col = colors[i];
            double dr = col.val[0] - rgb[0];
            double dg = col.val[1] - rgb[1];
            double db = col.val[2] - rgb[2];
            double distanceSqr = dr*dr + dg*dg + db*db;
            if (distanceSqr < thresh*thresh) {
                // If our central pixel is close enough...
                return i;
            }
        }
        return -1;
        */
    }

    /**
     *   Grabs a rotated rect corresponding to a player target
     * @param camInput    The camera frame input
     * @param rectOutput  A rotated rect that we output containing the position
     * @return the ID of our target
     */
    private int getTarget(Mat camInput, RotatedRect rectOutput) {
        Mat mask = new Mat();
        // Convert to HSV

        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_RGB2HSV);

        // Split channels
        Core.split(camInput, channels);

//          //Filter HSV
        Core.inRange(channels.get(0), new Scalar(40), new Scalar(93), hueFiltered );
        Core.inRange(channels.get(1), new Scalar(154), new Scalar(255), satFiltered );
        Core.inRange(channels.get(2), new Scalar(67), new Scalar(238), valFiltered );

//        // Mix together
        Core.bitwise_and(hueFiltered, satFiltered, satFiltered);
        Core.bitwise_and(satFiltered, valFiltered, mask);

        // Erode and Dilate
        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.erode(mask, mask, erodeKernel);
        Imgproc.dilate(mask, mask, dilateKernel);

        // Contour analysis!
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        // Our rectangles must be above this threshold
        double areaThresh = 10.0;

        double largestArea = 0.0;
        RotatedRect largestRect = null;
        int targetPlayerID = -1;
        for(MatOfPoint mop : contours) {
            MatOfPoint2f mop2f = new MatOfPoint2f();
            mop.convertTo(mop2f, CvType.CV_32FC1);
            RotatedRect rect = Imgproc.minAreaRect(mop2f);

            // If our rect is big enough and the biggest....
            if (rect.size.area() > largestArea && rect.size.area() > areaThresh) {
                targetPlayerID = verifyCentralColor(camInput, rect);
                // If we detect a valid player color...
                if (targetPlayerID != -1) {
                    largestArea = rect.size.area();
                    largestRect = rect;
                }
            }
            mop.release();
            mop2f.release();
        }
        Log.i("Largest Area:", "area: " + largestArea);
        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_HSV2RGB);
        if (IS_DEBUG_VIDEO) {
            // Draw a circle or whatever around our main rect!
            Imgproc.cvtColor(mask, mask, Imgproc.COLOR_GRAY2RGB);
            if (largestRect != null) {
                Imgproc.circle(mask, largestRect.center, 10, new Scalar(255, 0, 0));
            }
            Core.bitwise_not(mask, mask);
            // Draw our green stuff, and only our green stuff!
            Core.subtract(camInput, mask, camInput);

            //mask.copyTo(camInput);
        }
        mask.release();
        erodeKernel.release();
        dilateKernel.release();

        if (largestRect != null) {
            // Set values of output rect so we can grab it
            double[] vals = {
                    largestRect.center.x,
                    largestRect.center.y,
                    largestRect.size.width,
                    largestRect.size.height,
                    largestRect.angle
            };
            rectOutput.set(vals);
        }

        return targetPlayerID;
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        camInput = inputFrame.rgba();

        RotatedRect target = new RotatedRect();
        int targetPlayer = getTarget(camInput, target);

        if (targetPlayer  == -1) {
            setTarget(null);
        } else {
            setTarget(target);
            Log.i("Player ID", "target: " + targetPlayer);
        }

        // Are we positioned to actually HIT a target?
        setLockedOn(false);
        if (hasTarget()) {
            // Reload
            if (!hasAmmo()) {
                // How much time has passed since we were exposed
                long dtime = System.currentTimeMillis() - lastTimeExposed;
                if (dtime > EXPOSE_TIME_THRESHOLD) {
                    reload();
                    // Reset our time exposed timer
                    lastTimeExposed = System.currentTimeMillis();
                }
            }

            double dx = getTarget().center.x - camInput.width() / 2,
                   dy = getTarget().center.y - camInput.height() / 2;

            if (dx*dx + dy*dy < SHOOT_THRESHOLD*SHOOT_THRESHOLD) {
                setLockedOn(true);
            }
        } else {
            // Reset our time exposed timer
            lastTimeExposed = System.currentTimeMillis();
        }

        if (isLockedOn()) {
            vibrate(200);
        }

        // Mask if we're debugging, Green screen, if we're playing the game
//        Imgproc.cvtColor(camInput, camInput, Imgproc.COLOR_HSV2RGB);
        if (!IS_DEBUG_VIDEO) {
            if (isAlive()) {
                int w = 200,
                    h = 200;
                String name = Database.getPlayer ().getId();
                camInput.setTo(new Scalar(0, 255, 0));
                // Draw color rectangle in middle
                /*
                Imgproc.rectangle(camInput, new Point(camInput.width()/2 - w/2, camInput.height()/2 - h/2),
                                            new Point(camInput.width()/2 + w/2, camInput.height()/2 + h/2),
                                            getPlayerCenterColor(name),
                                            -1 // -1 means fill the rectangle
                );
                */
            } else {
                camInput.setTo(new Scalar(255, 100, 100));
            }
        }

        // Clean up
        for(Mat mat : channels) {
            mat.release();
        }
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
     * Pew Pew!
     */
    void shoot() {
        if (isAlive()) {
            if (hasAmmo()) {
                loseAmmo();
                // If we're locked on, kill the other player
                if (isLockedOn()) {
                    String name = Database.getPlayer ().getId();

                    // 1v1 ONLY!!!
                    if (name.equals("player2"))
                        Database.killPlayer("player1");
                    else
                        Database.killPlayer("player2");

                }
                playSound(soundID_shoot);
            } else {
                playSound(soundID_empty);
            }
        }
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
    private static void playSound(int soundID) {
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
        return target != null && isAlive();
    }

    // Locked on getters and setters
    private void setLockedOn(boolean lockedOn) {
        this.lockedOn = lockedOn;
    }
    private boolean isLockedOn() {
        return lockedOn && isAlive();
    }

    // Ammo
    private boolean hasAmmo() {
        return hasAmmo && isAlive();
    }
    private void reload() {
        playSound(soundID_reload);
        hasAmmo = true;
    }
    private void loseAmmo() {
        hasAmmo = false;
    }

    // Is alive?
    private boolean isAlive() {
        return isAlive;
    }
    public static void die() {
        isAlive = false;
        playSound(soundID_death);
    }
}
