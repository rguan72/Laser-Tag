package com.lasertag.mhacks11.lasertag;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;


public class MainActivity extends AppCompatActivity {
    private static final String  TAG              = "MainActivity";

    public void startCamera(View view)
    {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


}