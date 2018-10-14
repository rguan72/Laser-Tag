package com.lasertag.mhacks11.lasertag;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Database
{
    private static DatabaseReference myRef = FirebaseDatabase.getInstance ().getReference();
    private static long numPlayers;
    private static long totalDeaths;
    private static Player player = null;

    public static DatabaseReference getDatabaseReference ()
    {
        return myRef;
    }

    public static long getNumPlayers ()
    {
        return numPlayers;
    }

    public static long getTotalDeaths ()
    {
        return totalDeaths;
    }

    public static Player getPlayer ()
    {
        return player;
    }

    //EFFECTS: Increase numPlayers by 1, adds a player into the database
    //MODIFIES: numPlayers, database
    public static void addPlayer (String name)
    {
        ++numPlayers;
        myRef.child ("numPlayers").setValue(numPlayers);

        player = new Player("player" + numPlayers, name);
        myRef.child ("players").child ("player" + numPlayers).setValue(player);
    }

    //Kill player with id
    public static void killPlayer (final String id)
    {
        //if (id.equals(player.getName()))
        //    CameraActivity.die();

        myRef.child("players").child (id).child ("deaths").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                Object tmp = snapshot.getValue();
                long death = ((long) tmp) + 1;
                myRef.child("players").child (id).child ("deaths").setValue(death);

                ++totalDeaths;
                myRef.child ("totalDeaths").setValue(totalDeaths);
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    //EFFECTS: updates numPlayers to the correct size
    //MODIFIES: numPlayers
    public static void updateNumPlayers ()
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

    public static void updateTotalDeaths ()
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

                if (player != null && player.getDeaths() > 0)
                    CameraActivity.die ();

                if (numPlayers > 1 && numPlayers - totalDeaths <= 1)
                {
                    numPlayers = 0;
                    totalDeaths = 0;
                    myRef.child ("numPlayers").setValue(numPlayers);
                    myRef.child ("totalDeaths").setValue(totalDeaths);
                    myRef.child ("players").removeValue();
                    //CameraActivity.startEnding();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    public static void updatePlayers ()
    {
        myRef.child("players").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (player != null)
                    Log.d ("hotpotato", (long) snapshot.child (player.getId()).child ("deaths").getValue() + "");
                if (player != null && (long) snapshot.child (player.getId()).child ("deaths").getValue() > 0)
                    CameraActivity.die();

                /*if (numPlayers - totalDeaths <= 1)
                {
                    numPlayers = 0;
                    totalDeaths = 0;
                    myRef.child ("numPlayers").setValue(numPlayers);
                    myRef.child ("totalDeaths").setValue(totalDeaths);
                    myRef.child ("players").removeValue();
                }*/
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }
}
