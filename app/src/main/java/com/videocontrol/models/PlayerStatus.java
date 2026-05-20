/*
package com.videocontrol.models;

public class PlayerStatus {
    private String state;   // playing, paused, stopped
    private Video currentVideo;
    private int queueCount;
    private String message;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Video getCurrentVideo() { return currentVideo; }
    public void setCurrentVideo(Video currentVideo) { this.currentVideo = currentVideo; }

    public int getQueueCount() { return queueCount; }
    public void setQueueCount(int queueCount) { this.queueCount = queueCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isPlaying() { return "playing".equals(state); }
    public boolean isPaused()  { return "paused".equals(state); }
}
*/



package com.videocontrol.models;

import com.google.gson.annotations.SerializedName;

public class PlayerStatus {

    // Estado
    @SerializedName("state")       public String  state;
    @SerializedName("isPlaying")   public boolean isPlaying;
    @SerializedName("isPaused")    public boolean isPaused;
    @SerializedName("message")     public String  message;

    // Tiempo (segundos)
    @SerializedName("time")        public int     time;
    @SerializedName("length")      public int     length;
    @SerializedName("remaining")   public int     remaining;
    @SerializedName("progress")    public double  progress;   // 0.0 – 1.0

    // Tiempo formateado (MM:SS o HH:MM:SS)
    @SerializedName("timeFormatted")      public String timeFormatted;
    @SerializedName("lengthFormatted")    public String lengthFormatted;
    @SerializedName("remainingFormatted") public String remainingFormatted;

    // Audio / velocidad
    @SerializedName("volume")      public int     volume;    // 0-200 (100 = normal)
    @SerializedName("rate")        public double  rate;      // 1.0 = normal

    // Flags
    @SerializedName("fullscreen")  public boolean fullscreen;
    @SerializedName("repeat")      public boolean repeat;
    @SerializedName("random")      public boolean random;
    @SerializedName("loop")        public boolean loop;

    // Video y cola
    @SerializedName("currentVideo") public Video  currentVideo;
    @SerializedName("queueCount")   public int    queueCount;
}


