package com.lasertag.mhacks11.lasertag;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class EndActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean has_won = true;
        if (has_won) {
            setContentView(R.layout.activity_won);
        }
        else {
            setContentView(R.layout.activity_lost);
        }
    }
}
