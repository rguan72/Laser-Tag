package com.example.adris.opencvtest;

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
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "MainActivity";

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
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
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

        setContentView(R.layout.activity_main);

        //Ask for Camera Permission
        getCameraPermission();

        //Search for the "Join" Button
        Button button = findViewById(R.id.join);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                //Creating text dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Username");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String name = input.getText().toString();
                        Player player = new Player (name);

                        //Get Database
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        //Select "path" in database
                        DatabaseReference myRef = database.getReference("players");
                        //Set Value
                        myRef.setValue(player);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
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

//        // Filter RGB
//        Core.inRange(channels.get(0), new Scalar(0), new Scalar(150), hueFiltered);
//        Core.inRange(channels.get(0), new Scalar(100), new Scalar(255), satFiltered);
//        Core.inRange(channels.get(0), new Scalar(0), new Scalar(150), valFiltered);

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

        if (largestRect != null) {
            // Draw a circle or whatever around our main rect!
            Imgproc.circle(camInput, largestRect.center, 10, new Scalar(255, 0, 0));
        }

        // Free up
        for(Mat mat : channels) {
            mat.release();
        }
        mask.release();
        erodeKernel.release();
        dilateKernel.release();
        System.gc();

        // Flip video so it's reasonable when displayed
        Mat dest = new Mat();
        Core.flip(camInput, dest, 1);
        dest.copyTo(camInput);
        dest.release();

        // Green screen

        return camInput;
    }
}