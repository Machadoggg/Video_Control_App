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
