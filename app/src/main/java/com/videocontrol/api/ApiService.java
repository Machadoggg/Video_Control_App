/*
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
*/


/*package com.videocontrol.api;

import com.videocontrol.models.PlayerStatus;
import com.videocontrol.models.QueueItem;
import com.videocontrol.models.Video;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── Videos ────────────────────────────────────────────────────────────────
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

    // ── Cola ──────────────────────────────────────────────────────────────────
    @GET("api/Queue")
    Call<List<QueueItem>> getQueue();

    @POST("api/Queue/add")
    Call<Void> addToQueue(@Body AddToQueueDto dto);

    @DELETE("api/Queue/remove/{id}")
    Call<Void> removeFromQueue(@Path("id") int id);

    @POST("api/Queue/play-next")
    Call<Video> playNext();

    @DELETE("api/Queue/clear")
    Call<Void> clearQueue();

    // ── Reproductor ───────────────────────────────────────────────────────────
    @GET("api/Player/status")
    Call<PlayerStatus> getStatus();

    @POST("api/Player/pause")
    Call<Void> pause();

    @POST("api/Player/stop")
    Call<Void> stop();

    @POST("api/Player/next")
    Call<Void> next();

    // Seek absoluto (saltar a segundo exacto)
    @POST("api/Player/seek/{seconds}")
    Call<Void> seek(@Path("seconds") int seconds);

    // Seek relativo (+10, -30, etc.)
    @POST("api/Player/seek-relative/{delta}")
    Call<Void> seekRelative(@Path("delta") int delta);

    // Volumen 0-200
    @POST("api/Player/volume/{value}")
    Call<Void> setVolume(@Path("value") int value);

    // Velocidad (0.25, 0.5, 1.0, 1.5, 2.0, etc.)
    @POST("api/Player/rate/{value}")
    Call<Void> setRate(@Path("value") double value);

    // Pantalla completa
    @POST("api/Player/fullscreen")
    Call<Void> toggleFullscreen();

    // Modos de reproducción
    @POST("api/Player/repeat")
    Call<Void> toggleRepeat();

    @POST("api/Player/random")
    Call<Void> toggleRandom();

    @POST("api/Player/loop")
    Call<Void> toggleLoop();

    // ── DTOs ──────────────────────────────────────────────────────────────────
    class AddToQueueDto {
        public int    videoId;
        public String requestedBy;
        public AddToQueueDto(int videoId, String requestedBy) {
            this.videoId = videoId;
            this.requestedBy = requestedBy;
        }
    }
}*/



/*package com.videocontrol.api;

import com.videocontrol.models.PlayerStatus;
import com.videocontrol.models.QueueItem;
import com.videocontrol.models.Video;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── Videos ────────────────────────────────────────────────────────────────
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

    // ── Cola ──────────────────────────────────────────────────────────────────
    @GET("api/Queue")
    Call<List<QueueItem>> getQueue();

    *//** Agrega un video al final de la cola. *//*
    @POST("api/Queue/add")
    Call<Void> addToQueue(@Body AddToQueueDto dto);

    *//** Inserta un video justo después del que se está reproduciendo. *//*
    @POST("api/Queue/add-next")
    Call<Void> addNext(@Body AddToQueueDto dto);

    *//**
     * Limpia los pendientes, mezcla la lista recibida y llena la cola
     * con hasta 20 videos aleatorios.
     *//*
    @POST("api/Queue/shuffle-and-fill")
    Call<Void> shuffleAndFill(@Body ShuffleDto dto);

    @DELETE("api/Queue/remove/{id}")
    Call<Void> removeFromQueue(@Path("id") int id);

    @POST("api/Queue/play-next")
    Call<Video> playNext();

    @DELETE("api/Queue/clear")
    Call<Void> clearQueue();

    // ── Reproductor ───────────────────────────────────────────────────────────
    @GET("api/Player/status")
    Call<PlayerStatus> getStatus();

    @POST("api/Player/pause")
    Call<Void> pause();

    @POST("api/Player/stop")
    Call<Void> stop();

    @POST("api/Player/next")
    Call<Void> next();

    @POST("api/Player/seek/{seconds}")
    Call<Void> seek(@Path("seconds") int seconds);

    @POST("api/Player/seek-relative/{delta}")
    Call<Void> seekRelative(@Path("delta") int delta);

    @POST("api/Player/volume/{value}")
    Call<Void> setVolume(@Path("value") int value);

    @POST("api/Player/rate/{value}")
    Call<Void> setRate(@Path("value") double value);

    @POST("api/Player/fullscreen")
    Call<Void> toggleFullscreen();

    @POST("api/Player/repeat")
    Call<Void> toggleRepeat();

    @POST("api/Player/random")
    Call<Void> toggleRandom();

    @POST("api/Player/loop")
    Call<Void> toggleLoop();

    // ── DTOs ──────────────────────────────────────────────────────────────────
    class AddToQueueDto {
        public int    videoId;
        public String requestedBy;
        public AddToQueueDto(int videoId, String requestedBy) {
            this.videoId     = videoId;
            this.requestedBy = requestedBy;
        }
    }

    class ShuffleDto {
        public java.util.List<Integer> videoIds;
        public String                  requestedBy;
        public ShuffleDto(java.util.List<Integer> videoIds, String requestedBy) {
            this.videoIds    = videoIds;
            this.requestedBy = requestedBy;
        }
    }
}*/





package com.videocontrol.api;

import com.videocontrol.models.PlayerStatus;
import com.videocontrol.models.QueueItem;
import com.videocontrol.models.SongRequest;
import com.videocontrol.models.Video;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── Videos ────────────────────────────────────────────────────────────────
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

    // ── Cola ──────────────────────────────────────────────────────────────────
    @GET("api/Queue")
    Call<List<QueueItem>> getQueue();

    @POST("api/Queue/add")
    Call<Void> addToQueue(@Body AddToQueueDto dto);

    @POST("api/Queue/add-next")
    Call<Void> addNext(@Body AddToQueueDto dto);

    @POST("api/Queue/shuffle-and-fill")
    Call<Void> shuffleAndFill(@Body ShuffleDto dto);

    @DELETE("api/Queue/remove/{id}")
    Call<Void> removeFromQueue(@Path("id") int id);

    @POST("api/Queue/play-next")
    Call<Video> playNext();

    @DELETE("api/Queue/clear")
    Call<Void> clearQueue();

    // ── Reproductor ───────────────────────────────────────────────────────────
    @GET("api/Player/status")
    Call<PlayerStatus> getStatus();

    @POST("api/Player/pause")
    Call<Void> pause();

    @POST("api/Player/stop")
    Call<Void> stop();

    @POST("api/Player/next")
    Call<Void> next();

    @POST("api/Player/seek/{seconds}")
    Call<Void> seek(@Path("seconds") int seconds);

    @POST("api/Player/seek-relative/{delta}")
    Call<Void> seekRelative(@Path("delta") int delta);

    @POST("api/Player/volume/{value}")
    Call<Void> setVolume(@Path("value") int value);

    @POST("api/Player/rate/{value}")
    Call<Void> setRate(@Path("value") double value);

    @POST("api/Player/fullscreen")
    Call<Void> toggleFullscreen();

    @POST("api/Player/repeat")
    Call<Void> toggleRepeat();

    @POST("api/Player/random")
    Call<Void> toggleRandom();

    @POST("api/Player/loop")
    Call<Void> toggleLoop();

    // ── Solicitudes de canciones ──────────────────────────────────────────────
    @GET("api/SongRequest")
    Call<List<SongRequest>> getPendingRequests();

    @POST("api/SongRequest/{id}/mark-played")
    Call<Void> markRequestPlayed(@Path("id") int id);

    @DELETE("api/SongRequest/{id}")
    Call<Void> deleteRequest(@Path("id") int id);

    // ── DTOs ──────────────────────────────────────────────────────────────────
    class AddToQueueDto {
        public int    videoId;
        public String requestedBy;
        public AddToQueueDto(int videoId, String requestedBy) {
            this.videoId     = videoId;
            this.requestedBy = requestedBy;
        }
    }

    class ShuffleDto {
        public java.util.List<Integer> videoIds;
        public String                  requestedBy;
        public ShuffleDto(java.util.List<Integer> videoIds, String requestedBy) {
            this.videoIds    = videoIds;
            this.requestedBy = requestedBy;
        }
    }
}

