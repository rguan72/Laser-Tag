package com.lasertag.mhacks11.lasertag;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Database
{
    private DatabaseReference myRef = FirebaseDatabase.getInstance ().getReference();
    private static long numPlayers;
    private static long totalDeaths;

    public Database ()
    {

    }

    public long getNumPlayers ()
    {
        return numPlayers;
    }

    public long getTotalDeaths ()
    {
        return totalDeaths;
    }

    //EFFECTS: Increase numPlayers by 1, adds a player into the database
    //MODIFIES: numPlayers, database
    public void addPlayer (String name)
    {
        ++numPlayers;
        myRef.child ("numPlayers").setValue(numPlayers);

        Player player = new Player(name);
        myRef.child ("players").child ("player" + numPlayers).setValue(player);
    }

    //Kill player with id
    public void killPlayer (final String id)
    {
        myRef.child("players").child (id).child ("deaths").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                Object tmp = snapshot.getValue();
                long death = ((long) tmp) + 1;
                ++totalDeaths;
                myRef.child ("totalDeaths").setValue(totalDeaths);
                myRef.child("players").child (id).child ("deaths").setValue(death);
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    //EFFECTS: updates numPlayers to the correct size
    //MODIFIES: numPlayers
    public void updateNumPlayers ()
    {
        myRef.child("numPlayers").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                Object tmp = snapshot.getValue();
                if (tmp == null)
                    myRef.child ("numPlayers").setValue(numPlayers);
                else
                    numPlayers = (long) tmp;
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    public void updateTotalDeaths ()
    {
        myRef.child("totalDeaths").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                Object tmp = snapshot.getValue();
                if (tmp == null)
                    myRef.child ("totalDeaths").setValue(totalDeaths);
                else
                    totalDeaths = (long) tmp;
                Log.d ("totalDeaths", "" + totalDeaths);

                if (numPlayers - totalDeaths <= 1)
                {
                    numPlayers = 0;
                    totalDeaths = 0;
                    myRef.child ("numPlayers").setValue(numPlayers);
                    myRef.child ("totalDeaths").setValue(totalDeaths);
                    myRef.child ("players").removeValue();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }
}
