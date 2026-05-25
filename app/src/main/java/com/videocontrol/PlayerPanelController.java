/*
package com.videocontrol;

// ── PlayerPanelController ──────────────────────────────────────────────────────
// Maneja toda la UI del panel Reproductor en MainActivity.
// Se instancia una vez desde MainActivity y recibe el rootView del panel.
//
// Controles implementados:
//   ▸ Barra de progreso con seek táctil
//   ▸ Tiempo transcurrido / restante / total
//   ▸ Play/Pause, Stop, Siguiente, Pantalla completa
//   ▸ Retroceder/Adelantar 10s y 30s
//   ▸ Volumen (SeekBar 0-200) con etiqueta %
//   ▸ Velocidad (botones: 0.25× 0.5× 0.75× 1× 1.25× 1.5× 2×)
//   ▸ Modos: Repetir, Aleatorio, Bucle (ToggleButton)
//   ▸ Contador de cola
// ─────────────────────────────────────────────────────────────────────────────

import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import com.videocontrol.api.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerPanelController {

    private final ApiService api;
    private final Runnable onNextRequested; // callback → MainActivity llama a playNext()

    // ── Vistas ────────────────────────────────────────────────────────────────
    private final TextView  nowPlayingTitle;
    private final TextView  playerStateText;
    private final TextView  timeElapsed;
    private final TextView  timeRemaining;
    private final TextView  timeTotal;
    private final TextView  volumeValue;
    private final TextView  rateValue;
    private final TextView  queueCountText;

    private final SeekBar   progressSeekBar;
    private final SeekBar   volumeSeekBar;

    private final ImageButton btnPlayPause;
    private final ImageButton btnStop;
    private final ImageButton btnNext;
    private final ImageButton btnFullscreen;
    private final ImageButton btnSeekBack10;
    private final ImageButton btnSeekBack30;
    private final ImageButton btnSeekFwd10;
    private final ImageButton btnSeekFwd30;

    private final ToggleButton btnRepeat;
    private final ToggleButton btnRandom;
    private final ToggleButton btnLoop;

    // Estado local
    private int    currentLength   = 0;
    private boolean seekingByUser  = false;

    public PlayerPanelController(@NonNull View root,
                                 @NonNull ApiService api,
                                 @NonNull Runnable onNextRequested) {
        this.api = api;
        this.onNextRequested = onNextRequested;

        // Bind vistas
        nowPlayingTitle  = root.findViewById(R.id.nowPlayingTitle);
        playerStateText  = root.findViewById(R.id.playerStateText);
        timeElapsed      = root.findViewById(R.id.timeElapsed);
        timeRemaining    = root.findViewById(R.id.timeRemaining);
        timeTotal        = root.findViewById(R.id.timeTotal);
        volumeValue      = root.findViewById(R.id.volumeValue);
        rateValue        = root.findViewById(R.id.rateValue);
        queueCountText   = root.findViewById(R.id.queueCountText);

        progressSeekBar  = root.findViewById(R.id.progressSeekBar);
        volumeSeekBar    = root.findViewById(R.id.volumeSeekBar);

        btnPlayPause     = root.findViewById(R.id.btnPlayPause);
        btnStop          = root.findViewById(R.id.btnStop);
        btnNext          = root.findViewById(R.id.btnNext);
        btnFullscreen    = root.findViewById(R.id.btnFullscreen);
        btnSeekBack10    = root.findViewById(R.id.btnSeekBack10);
        btnSeekBack30    = root.findViewById(R.id.btnSeekBack30);
        btnSeekFwd10     = root.findViewById(R.id.btnSeekFwd10);
        btnSeekFwd30     = root.findViewById(R.id.btnSeekFwd30);

        btnRepeat        = root.findViewById(R.id.btnRepeat);
        btnRandom        = root.findViewById(R.id.btnRandom);
        btnLoop          = root.findViewById(R.id.btnLoop);

        setupListeners(root);
    }

    // ── Actualiza toda la UI con el status recibido del servidor ─────────────
    public void updateStatus(PlayerStatus s) {
        // Título y estado
        String title = s.currentVideo != null ? s.currentVideo.getTitle() : "Sin video";
        nowPlayingTitle.setText(title);
        playerStateText.setText(s.message);

        // Tiempos
        timeElapsed.setText(s.timeFormatted);
        timeTotal.setText(s.lengthFormatted);
        timeRemaining.setText("-" + s.remainingFormatted);

        // Barra de progreso (solo si el usuario no la está arrastrando)
        currentLength = s.length;
        if (!seekingByUser) {
            int prog = s.length > 0 ? (int)(s.progress * 1000) : 0;
            progressSeekBar.setProgress(prog);
        }

        // Icono Play/Pause
        btnPlayPause.setImageResource(
                s.isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);

        // Volumen
        volumeSeekBar.setProgress(s.volume);
        volumeValue.setText(s.volume + "%");

        // Velocidad
        rateValue.setText(formatRate(s.rate));

        // Modos
        btnRepeat.setChecked(s.repeat);
        btnRandom.setChecked(s.random);
        btnLoop.setChecked(s.loop);

        // Cola
        queueCountText.setText("En cola: " + s.queueCount + " video(s)");
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private void setupListeners(View root) {

        // Play / Pause
        btnPlayPause.setOnClickListener(v ->
                api.pause().enqueue(simpleCallback("Error al pausar")));

        // Stop
        btnStop.setOnClickListener(v ->
                api.stop().enqueue(simpleCallback("Error al detener")));

        // Siguiente
        btnNext.setOnClickListener(v -> onNextRequested.run());

        // Pantalla completa
        btnFullscreen.setOnClickListener(v ->
                api.toggleFullscreen().enqueue(simpleCallback("Error fullscreen")));

        // Seek ±10s / ±30s
        btnSeekBack10.setOnClickListener(v -> seekRelative(-10));
        btnSeekBack30.setOnClickListener(v -> seekRelative(-30));
        btnSeekFwd10.setOnClickListener(v  -> seekRelative(+10));
        btnSeekFwd30.setOnClickListener(v  -> seekRelative(+30));

        // Barra de progreso — seek táctil
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { seekingByUser = true; }
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                seekingByUser = false;
                if (currentLength > 0) {
                    int targetSec = (int)((s.getProgress() / 1000.0) * currentLength);
                    api.seek(targetSec).enqueue(simpleCallback("Error al buscar posición"));
                }
            }
        });

        // Volumen
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) volumeValue.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                api.setVolume(s.getProgress()).enqueue(simpleCallback("Error al ajustar volumen"));
            }
        });

        // Velocidad
        root.findViewById(R.id.btnRate025).setOnClickListener(v -> setRate(0.25));
        root.findViewById(R.id.btnRate05) .setOnClickListener(v -> setRate(0.5));
        root.findViewById(R.id.btnRate075).setOnClickListener(v -> setRate(0.75));
        root.findViewById(R.id.btnRate1)  .setOnClickListener(v -> setRate(1.0));
        root.findViewById(R.id.btnRate125).setOnClickListener(v -> setRate(1.25));
        root.findViewById(R.id.btnRate15) .setOnClickListener(v -> setRate(1.5));
        root.findViewById(R.id.btnRate2)  .setOnClickListener(v -> setRate(2.0));

        // Modos
        btnRepeat.setOnClickListener(v ->
                api.toggleRepeat().enqueue(simpleCallback("Error repeat")));
        btnRandom.setOnClickListener(v ->
                api.toggleRandom().enqueue(simpleCallback("Error random")));
        btnLoop.setOnClickListener(v ->
                api.toggleLoop().enqueue(simpleCallback("Error loop")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void seekRelative(int delta) {
        api.seekRelative(delta).enqueue(simpleCallback("Error al buscar"));
    }

    private void setRate(double rate) {
        rateValue.setText(formatRate(rate));
        api.setRate(rate).enqueue(simpleCallback("Error al ajustar velocidad"));
    }

    private String formatRate(double rate) {
        if (rate == (int) rate) return (int) rate + "×";
        return rate + "×";
    }

    private <T> Callback<T> simpleCallback(String errorMsg) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {}
            @Override public void onFailure(Call<T> c, Throwable t) {
                android.util.Log.e("Player", errorMsg + ": " + t.getMessage());
            }
        };
    }

    // ── Modelo de status que recibe desde MainActivity ─────────────────────
    public static class PlayerStatus {
        public String  state;
        public boolean isPlaying;
        public boolean isPaused;
        public String  message;
        public int     time;
        public int     length;
        public int     remaining;
        public double  progress;
        public String  timeFormatted;
        public String  lengthFormatted;
        public String  remainingFormatted;
        public int     volume;
        public double  rate;
        public boolean fullscreen;
        public boolean repeat;
        public boolean random;
        public boolean loop;
        public int     queueCount;
        public VideoModel currentVideo;
    }

    public static class VideoModel {
        private String title;
        public String getTitle() { return title != null ? title : ""; }
    }
}

*/






/*package com.videocontrol;

// ── PlayerPanelController ──────────────────────────────────────────────────────
// Maneja toda la UI del panel Reproductor en MainActivity.
// Se instancia una vez desde MainActivity y recibe el rootView del panel.
//
// Controles implementados:
//   ▸ Barra de progreso con seek táctil
//   ▸ Tiempo transcurrido / restante / total
//   ▸ Play/Pause, Stop, Siguiente, Pantalla completa
//   ▸ Retroceder/Adelantar 10s y 30s
//   ▸ Volumen (SeekBar 0-200) con etiqueta %
//   ▸ Velocidad (botones: 0.25× 0.5× 0.75× 1× 1.25× 1.5× 2×)
//   ▸ Modos: Repetir, Aleatorio, Bucle (ToggleButton)
//   ▸ Contador de cola
// ─────────────────────────────────────────────────────────────────────────────

import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.PlayerStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerPanelController {

    private final ApiService api;
    private final Runnable onNextRequested; // callback → MainActivity llama a playNext()

    // ── Vistas ────────────────────────────────────────────────────────────────
    private final TextView  nowPlayingTitle;
    private final TextView  playerStateText;
    private final TextView  timeElapsed;
    private final TextView  timeRemaining;
    private final TextView  timeTotal;
    private final TextView  volumeValue;
    private final TextView  rateValue;
    private final TextView  queueCountText;

    private final SeekBar   progressSeekBar;
    private final SeekBar   volumeSeekBar;

    private final ImageButton btnPlayPause;
    private final ImageButton btnStop;
    private final ImageButton btnNext;
    private final ImageButton btnFullscreen;
    private final ImageButton btnSeekBack10;
    private final ImageButton btnSeekBack30;
    private final ImageButton btnSeekFwd10;
    private final ImageButton btnSeekFwd30;

    private final ToggleButton btnRepeat;
    private final ToggleButton btnRandom;
    private final ToggleButton btnLoop;

    // Estado local
    private int    currentLength   = 0;
    private boolean seekingByUser  = false;

    public PlayerPanelController(@NonNull View root,
                                 @NonNull ApiService api,
                                 @NonNull Runnable onNextRequested) {
        this.api = api;
        this.onNextRequested = onNextRequested;

        // Bind vistas
        nowPlayingTitle  = root.findViewById(R.id.nowPlayingTitle);
        playerStateText  = root.findViewById(R.id.playerStateText);
        timeElapsed      = root.findViewById(R.id.timeElapsed);
        timeRemaining    = root.findViewById(R.id.timeRemaining);
        timeTotal        = root.findViewById(R.id.timeTotal);
        volumeValue      = root.findViewById(R.id.volumeValue);
        *//*rateValue        = root.findViewById(R.id.rateValue);*//*
        queueCountText   = root.findViewById(R.id.queueCountText);

        progressSeekBar  = root.findViewById(R.id.progressSeekBar);
        volumeSeekBar    = root.findViewById(R.id.volumeSeekBar);

        btnPlayPause     = root.findViewById(R.id.btnPlayPause);
        btnStop          = root.findViewById(R.id.btnStop);
        btnNext          = root.findViewById(R.id.btnNext);
        btnFullscreen    = root.findViewById(R.id.btnFullscreen);
        *//*btnSeekBack10    = root.findViewById(R.id.btnSeekBack10);
        btnSeekBack30    = root.findViewById(R.id.btnSeekBack30);
        btnSeekFwd10     = root.findViewById(R.id.btnSeekFwd10);
        btnSeekFwd30     = root.findViewById(R.id.btnSeekFwd30);*//*

        btnRepeat        = root.findViewById(R.id.btnRepeat);
        btnRandom        = root.findViewById(R.id.btnRandom);
        btnLoop          = root.findViewById(R.id.btnLoop);

        setupListeners(root);
    }

    // ── Actualiza toda la UI con el status recibido del servidor ─────────────
    public void updateStatus(PlayerStatus s) {
        // Título y estado
        String title = s.currentVideo != null ? s.currentVideo.getTitle() : "Sin video";
        nowPlayingTitle.setText(title);
        playerStateText.setText(s.message);

        // Tiempos
        timeElapsed.setText(s.timeFormatted != null ? s.timeFormatted : "00:00");
        timeTotal.setText(s.lengthFormatted != null ? s.lengthFormatted : "00:00");
        timeRemaining.setText("-" + (s.remainingFormatted != null ? s.remainingFormatted : "00:00"));

        // Barra de progreso (solo si el usuario no la está arrastrando)
        currentLength = s.length;
        if (!seekingByUser) {
            int prog = s.length > 0 ? (int)(s.progress * 1000) : 0;
            progressSeekBar.setProgress(prog);
        }

        // Icono Play/Pause
        btnPlayPause.setImageResource(
                s.isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);

        // Volumen
        volumeSeekBar.setProgress(s.volume);
        volumeValue.setText(s.volume + "%");

        // Velocidad
        rateValue.setText(formatRate(s.rate));

        // Modos
        btnRepeat.setChecked(s.repeat);
        btnRandom.setChecked(s.random);
        btnLoop.setChecked(s.loop);

        // Cola
        queueCountText.setText("En cola: " + s.queueCount + " video(s)");
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private void setupListeners(View root) {

        // Play / Pause
        btnPlayPause.setOnClickListener(v ->
                api.pause().enqueue(simpleCallback("Error al pausar")));

        // Stop
        btnStop.setOnClickListener(v ->
                api.stop().enqueue(simpleCallback("Error al detener")));

        // Siguiente
        btnNext.setOnClickListener(v -> onNextRequested.run());

        // Pantalla completa
        btnFullscreen.setOnClickListener(v ->
                api.toggleFullscreen().enqueue(simpleCallback("Error fullscreen")));

        // Seek ±10s / ±30s
        btnSeekBack10.setOnClickListener(v -> seekRelative(-10));
        btnSeekBack30.setOnClickListener(v -> seekRelative(-30));
        btnSeekFwd10.setOnClickListener(v  -> seekRelative(+10));
        btnSeekFwd30.setOnClickListener(v  -> seekRelative(+30));

        // Barra de progreso — seek táctil
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { seekingByUser = true; }
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                seekingByUser = false;
                if (currentLength > 0) {
                    int targetSec = (int)((s.getProgress() / 1000.0) * currentLength);
                    api.seek(targetSec).enqueue(simpleCallback("Error al buscar posición"));
                }
            }
        });

        // Volumen
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) volumeValue.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                api.setVolume(s.getProgress()).enqueue(simpleCallback("Error al ajustar volumen"));
            }
        });

        // Velocidad
        *//*root.findViewById(R.id.btnRate025).setOnClickListener(v -> setRate(0.25));
        root.findViewById(R.id.btnRate05) .setOnClickListener(v -> setRate(0.5));
        root.findViewById(R.id.btnRate075).setOnClickListener(v -> setRate(0.75));
        root.findViewById(R.id.btnRate1)  .setOnClickListener(v -> setRate(1.0));
        root.findViewById(R.id.btnRate125).setOnClickListener(v -> setRate(1.25));
        root.findViewById(R.id.btnRate15) .setOnClickListener(v -> setRate(1.5));
        root.findViewById(R.id.btnRate2)  .setOnClickListener(v -> setRate(2.0));*//*

        // Modos
        btnRepeat.setOnClickListener(v ->
                api.toggleRepeat().enqueue(simpleCallback("Error repeat")));
        btnRandom.setOnClickListener(v ->
                api.toggleRandom().enqueue(simpleCallback("Error random")));
        btnLoop.setOnClickListener(v ->
                api.toggleLoop().enqueue(simpleCallback("Error loop")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void seekRelative(int delta) {
        api.seekRelative(delta).enqueue(simpleCallback("Error al buscar"));
    }

    private void setRate(double rate) {
        rateValue.setText(formatRate(rate));
        api.setRate(rate).enqueue(simpleCallback("Error al ajustar velocidad"));
    }

    private String formatRate(double rate) {
        if (rate == (int) rate) return (int) rate + "×";
        return rate + "×";
    }

    private <T> Callback<T> simpleCallback(String errorMsg) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {}
            @Override public void onFailure(Call<T> c, Throwable t) {
                android.util.Log.e("Player", errorMsg + ": " + t.getMessage());
            }
        };
    }

}*/





package com.videocontrol;

// ── PlayerPanelController ──────────────────────────────────────────────────────
// Maneja toda la UI del panel Reproductor en MainActivity.
// Se instancia una vez desde MainActivity y recibe el rootView del panel.
//
// Controles implementados:
//   ▸ Barra de progreso con seek táctil
//   ▸ Tiempo transcurrido / restante / total
//   ▸ Play/Pause, Stop, Siguiente, Pantalla completa
//   ▸ Retroceder/Adelantar 10s y 30s
//   ▸ Volumen (SeekBar 0-200) con etiqueta %
//   ▸ Velocidad (botones: 0.25× 0.5× 0.75× 1× 1.25× 1.5× 2×)
//   ▸ Modos: Repetir, Aleatorio, Bucle (ToggleButton)
//   ▸ Contador de cola
// ─────────────────────────────────────────────────────────────────────────────

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.PlayerStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerPanelController {

    private final ApiService api;
    private final Runnable onNextRequested; // callback → MainActivity llama a playNext()

    // ── Vistas ────────────────────────────────────────────────────────────────
    private TextView  nowPlayingTitle;
    private TextView  playerStateText;
    private TextView  timeElapsed;
    private TextView  timeRemaining;
    private TextView  timeTotal;
    private TextView  volumeValue;
    private TextView  rateValue;
    private TextView  queueCountText;

    private SeekBar   progressSeekBar;
    private SeekBar   volumeSeekBar;

    private ImageButton btnPlayPause;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnFullscreen;
    private ImageButton btnSeekBack10;
    private ImageButton btnSeekBack30;
    private ImageButton btnSeekFwd10;
    private ImageButton btnSeekFwd30;

    private ToggleButton btnRepeat;
    private ToggleButton btnRandom;
    private ToggleButton btnLoop;

    // Estado local
    private int    currentLength   = 0;
    private boolean seekingByUser  = false;

    public PlayerPanelController(@NonNull View root,
                                 @NonNull ApiService api,
                                 @NonNull Runnable onNextRequested) {
        this.api = api;
        this.onNextRequested = onNextRequested;

        // Bind vistas
        nowPlayingTitle  = root.findViewById(R.id.nowPlayingTitle);
        playerStateText  = root.findViewById(R.id.playerStateText);
        timeElapsed      = root.findViewById(R.id.timeElapsed);
        timeRemaining    = root.findViewById(R.id.timeRemaining);
        timeTotal        = root.findViewById(R.id.timeTotal);
        volumeValue      = root.findViewById(R.id.volumeValue);
        rateValue        = root.findViewById(R.id.rateValue);
        queueCountText   = root.findViewById(R.id.queueCountText);

        progressSeekBar  = root.findViewById(R.id.progressSeekBar);
        volumeSeekBar    = root.findViewById(R.id.volumeSeekBar);

        btnPlayPause     = root.findViewById(R.id.btnPlayPause);
        btnStop          = root.findViewById(R.id.btnStop);
        btnNext          = root.findViewById(R.id.btnNext);
        btnFullscreen    = root.findViewById(R.id.btnFullscreen);
        btnSeekBack10    = root.findViewById(R.id.btnSeekBack10);
        btnSeekBack30    = root.findViewById(R.id.btnSeekBack30);
        btnSeekFwd10     = root.findViewById(R.id.btnSeekFwd10);
        btnSeekFwd30     = root.findViewById(R.id.btnSeekFwd30);

        btnRepeat        = root.findViewById(R.id.btnRepeat);
        btnRandom        = root.findViewById(R.id.btnRandom);
        btnLoop          = root.findViewById(R.id.btnLoop);

        ToggleButton btnRepeat = root.findViewById(R.id.btnRepeat);
        ToggleButton btnRandom = root.findViewById(R.id.btnRandom);
        ToggleButton btnLoop   = root.findViewById(R.id.btnLoop);


        setupListeners(root);
    }

    // ── Actualiza toda la UI con el status recibido del servidor ─────────────
    public void updateStatus(PlayerStatus s) {
        // Título y estado
        String title = s.currentVideo != null ? s.currentVideo.getTitle() : "Sin video";
        if (nowPlayingTitle != null) nowPlayingTitle.setText(title);
        if (playerStateText != null) playerStateText.setText(s.message);

        // Tiempos
        if (timeElapsed  != null) timeElapsed.setText(s.timeFormatted != null ? s.timeFormatted : "00:00");
        if (timeTotal    != null) timeTotal.setText(s.lengthFormatted != null ? s.lengthFormatted : "00:00");
        if (timeRemaining!= null) timeRemaining.setText("-" + (s.remainingFormatted != null ? s.remainingFormatted : "00:00"));

        // Barra de progreso (solo si el usuario no la está arrastrando)
        currentLength = s.length;
        if (progressSeekBar != null && !seekingByUser) {
            int prog = s.length > 0 ? (int)(s.progress * 1000) : 0;
            progressSeekBar.setProgress(prog);
        }

        // Icono Play/Pause
        if (btnPlayPause != null) btnPlayPause.setImageResource(
                s.isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);

        // Volumen
        if (volumeSeekBar != null) volumeSeekBar.setProgress(s.volume);
        if (volumeValue   != null) volumeValue.setText(s.volume + "%");

        // Velocidad
        if (rateValue != null) rateValue.setText(formatRate(s.rate));

        // Modos
        if (btnRepeat != null) btnRepeat.setChecked(s.repeat);
        if (btnRandom != null) btnRandom.setChecked(s.random);
        if (btnLoop   != null) btnLoop.setChecked(s.loop);

        // Cola
        if (queueCountText != null) queueCountText.setText("En cola: " + s.queueCount + " video(s)");
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private void setupListeners(View root) {

        // Play / Pause
        btnPlayPause.setOnClickListener(v ->
                api.pause().enqueue(simpleCallback("Error al pausar")));

        // Stop
        btnStop.setOnClickListener(v ->
                api.stop().enqueue(simpleCallback("Error al detener")));

        // Siguiente
        btnNext.setOnClickListener(v -> onNextRequested.run());

        // Pantalla completa
        btnFullscreen.setOnClickListener(v ->
                api.toggleFullscreen().enqueue(simpleCallback("Error fullscreen")));

        // Seek ±10s / ±30s
        btnSeekBack10.setOnClickListener(v -> seekRelative(-10));
        btnSeekBack30.setOnClickListener(v -> seekRelative(-30));
        btnSeekFwd10.setOnClickListener(v  -> seekRelative(+10));
        btnSeekFwd30.setOnClickListener(v  -> seekRelative(+30));

        // Barra de progreso — seek táctil
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { seekingByUser = true; }
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                seekingByUser = false;
                if (currentLength > 0) {
                    int targetSec = (int)((s.getProgress() / 1000.0) * currentLength);
                    api.seek(targetSec).enqueue(simpleCallback("Error al buscar posición"));
                }
            }
        });

        // Volumen
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) volumeValue.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                api.setVolume(s.getProgress()).enqueue(simpleCallback("Error al ajustar volumen"));
            }
        });

        // Velocidad
        /*root.findViewById(R.id.btnRate025).setOnClickListener(v -> setRate(0.25));
        root.findViewById(R.id.btnRate05) .setOnClickListener(v -> setRate(0.5));
        root.findViewById(R.id.btnRate075).setOnClickListener(v -> setRate(0.75));
        root.findViewById(R.id.btnRate1)  .setOnClickListener(v -> setRate(1.0));
        root.findViewById(R.id.btnRate125).setOnClickListener(v -> setRate(1.25));
        root.findViewById(R.id.btnRate15) .setOnClickListener(v -> setRate(1.5));
        root.findViewById(R.id.btnRate2)  .setOnClickListener(v -> setRate(2.0));*/

        // Modos
        btnRepeat.setOnClickListener(v ->
                api.toggleRepeat().enqueue(simpleCallback("Error repeat")));
        btnRandom.setOnClickListener(v ->
                api.toggleRandom().enqueue(simpleCallback("Error random")));
        btnLoop.setOnClickListener(v ->
                api.toggleLoop().enqueue(simpleCallback("Error loop")));

        btnRepeat.setOnCheckedChangeListener((btn, isChecked) -> {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    isChecked ? Color.parseColor("#00CC44") : Color.parseColor("#1a1a2a")));
            btn.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#0099FF"));
            api.toggleRepeat().enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        });

        btnRandom.setOnCheckedChangeListener((btn, isChecked) -> {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    isChecked ? Color.parseColor("#00CC44") : Color.parseColor("#1a1a2a")));
            btn.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#0099FF"));
            api.toggleRandom().enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        });

        btnLoop.setOnCheckedChangeListener((btn, isChecked) -> {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    isChecked ? Color.parseColor("#00CC44") : Color.parseColor("#1a1a2a")));
            btn.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#0099FF"));
            api.toggleLoop().enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void seekRelative(int delta) {
        api.seekRelative(delta).enqueue(simpleCallback("Error al buscar"));
    }

    private void setRate(double rate) {
        if (rateValue != null) rateValue.setText(formatRate(rate));
        api.setRate(rate).enqueue(simpleCallback("Error al ajustar velocidad"));
    }

    private String formatRate(double rate) {
        if (rate == (int) rate) return (int) rate + "×";
        return rate + "×";
    }

    private <T> Callback<T> simpleCallback(String errorMsg) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {}
            @Override public void onFailure(Call<T> c, Throwable t) {
                android.util.Log.e("Player", errorMsg + ": " + t.getMessage());
            }
        };
    }








}




