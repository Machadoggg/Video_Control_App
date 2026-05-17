package com.videocontrol.models;

public class QueueItem {
    private int id;
    private int videoId;
    private int position;
    private boolean isPlaying;
    private boolean isFinished;
    private String requestedBy;
    private String addedAt;
    private Video video;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVideoId() { return videoId; }
    public void setVideoId(int videoId) { this.videoId = videoId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { isPlaying = playing; }

    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getAddedAt() { return addedAt; }
    public void setAddedAt(String addedAt) { this.addedAt = addedAt; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }
}
