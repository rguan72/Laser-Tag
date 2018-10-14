package com.lasertag.mhacks11.lasertag;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class EndActivity extends AppCompatActivity{
    boolean has_won;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Database.getDatabaseReference().child(Database.getPlayer().getId()).child ("deaths").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                Object tmp = snapshot.getValue();
                if ((long) tmp == 0) {
                    has_won = false;
                }
                else {
                    has_won = true;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
        if (has_won) {
            setContentView(R.layout.activity_won);
        }
        else {
            setContentView(R.layout.activity_lost);
        }
    }
}
