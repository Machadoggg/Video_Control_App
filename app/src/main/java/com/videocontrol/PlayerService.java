package com.videocontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.videocontrol.api.ApiClient;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.PlayerStatus;
import com.videocontrol.models.Video;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerService extends Service {

    private static final String CHANNEL_ID    = "player_channel";
    private static final int    NOTIF_ID      = 1;
    private static final long   POLL_INTERVAL = 4000;

    private ApiService api;
    private Handler    handler;
    private boolean    wasPlaying       = false;
    private boolean    awaitingNext     = false;

    // Acceso estático para que MainActivity pueda activar/desactivar auto-play
    public static boolean autoPlayEnabled = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            checkStatus();
            handler.postDelayed(this, POLL_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        api     = ApiClient.getClient().create(ApiService.class);
        handler = new Handler();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("Iniciando…"));
        handler.post(pollRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("autoPlay")) {
            autoPlayEnabled = intent.getBooleanExtra("autoPlay", false);
        }
        return START_STICKY;   // ← se reinicia solo si Android lo mata
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollRunnable);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    // ── Polling de estado ──────────────────────────────────────────────────
    private void checkStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();

                updateNotification(s.isPlaying
                        ? "▶ " + (s.title != null ? s.title : "Reproduciendo")
                        : "⏸ Pausado");

                // Auto-play: si terminó el video y hay cola, avanzar
                if (autoPlayEnabled && wasPlaying && !s.isPlaying
                        && !awaitingNext && s.queueCount > 0) {
                    playNext();
                }

                if (autoPlayEnabled && !s.isPlaying && s.queueCount == 0) {
                    autoPlayEnabled = false;
                }

                wasPlaying = s.isPlaying;
            }
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {}
        });
    }

    private void playNext() {
        awaitingNext = true;
        api.playNext().enqueue(new Callback<Video>() {
            @Override public void onResponse(Call<Video> c, Response<Video> r) {
                awaitingNext = false;
                if (r.isSuccessful() && r.body() != null) {
                    wasPlaying = true;
                    updateNotification("▶ " + r.body().getTitle());
                } else {
                    autoPlayEnabled = false;
                }
            }
            @Override public void onFailure(Call<Video> c, Throwable t) {
                awaitingNext = false;
            }
        });
    }

    // ── Notificación persistente ───────────────────────────────────────────
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Reproductor", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Control de reproducción en segundo plano");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎬 Video Control")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID, buildNotification(text));
    }
}