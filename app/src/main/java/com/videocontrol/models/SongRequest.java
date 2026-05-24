package com.videocontrol.models;

import com.google.gson.annotations.SerializedName;

public class SongRequest {
    @SerializedName("id")        private int    id;
    @SerializedName("songTitle") private String songTitle;
    @SerializedName("artistName") private String artistName;
    @SerializedName("tableNumber") private String tableNumber;
    @SerializedName("requestedBy") private String requestedBy;
    @SerializedName("requestedAt") private String requestedAt;
    @SerializedName("isPlayed")    private boolean isPlayed;

    public int    getId()          { return id; }
    public String getSongTitle()   { return songTitle; }
    public String getArtistName()  { return artistName; }
    public String getTableNumber() { return tableNumber; }
    public String getRequestedBy() { return requestedBy; }
    public String getRequestedAt() { return requestedAt; }
    public boolean isPlayed()      { return isPlayed; }
}
