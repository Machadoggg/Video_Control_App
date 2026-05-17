package com.videocontrol.api;

import com.videocontrol.models.PlayerStatus;
import com.videocontrol.models.QueueItem;
import com.videocontrol.models.Video;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Videos ──────────────────────────────────────────────────────────────
    @GET("api/Videos")
    Call<List<Video>> getAllVideos();

    @GET("api/Videos/search")
    Call<List<Video>> searchVideos(@Query("query") String query);

    @GET("api/Videos/categories")
    Call<List<String>> getCategories();

    @GET("api/Videos/category/{cat}")
    Call<List<Video>> getByCategory(@Path("cat") String category);

    @POST("api/Videos/scan")
    Call<Void> scanVideos();

    // ── Cola ─────────────────────────────────────────────────────────────────
    @GET("api/Queue")
    Call<List<QueueItem>> getQueue();

    @POST("api/Queue/add")
    Call<Void> addToQueue(@Body AddToQueueDto dto);

    @DELETE("api/Queue/remove/{id}")
    Call<Void> removeFromQueue(@Path("id") int id);

    @POST("api/Queue/play-next")
    Call<Video> playNext();

    @GET("api/Queue/now-playing")
    Call<QueueItem> getNowPlaying();

    @DELETE("api/Queue/clear")
    Call<Void> clearQueue();

    // ── Reproductor ──────────────────────────────────────────────────────────
    @GET("api/Player/status")
    Call<PlayerStatus> getStatus();

    @POST("api/Player/pause")
    Call<Void> pause();

    @POST("api/Player/stop")
    Call<Void> stop();

    @POST("api/Player/next")
    Call<Void> next();

    @POST("api/Player/volume/{value}")
    Call<Void> setVolume(@Path("value") int value);

    @POST("api/Player/fullscreen")
    Call<Void> toggleFullscreen();

    // ── DTO ──────────────────────────────────────────────────────────────────
    class AddToQueueDto {
        public int videoId;
        public String requestedBy;

        public AddToQueueDto(int videoId, String requestedBy) {
            this.videoId = videoId;
            this.requestedBy = requestedBy;
        }
    }
}
