package com.lasertag.mhacks11.lasertag;

public class Player
{
    private String id;
    private String name;
    private int kills, deaths;

    public Player ()
    {

    }

    public Player (String id, String name)
    {
        this.id = id;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
    }

    public String getId ()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getDeaths()
    {
        return deaths;
    }

    public void setDeaths(int deaths)
    {
        this.deaths = deaths;
    }

    public int getKills()
    {
        return kills;
    }

    public void setKills(int kills)
    {
        this.kills = kills;
    }
}
