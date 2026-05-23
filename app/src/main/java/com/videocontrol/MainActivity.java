/*
package com.videocontrol;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videocontrol.adapters.VideoAdapter;
import com.videocontrol.adapters.QueueAdapter;
import com.videocontrol.api.ApiClient;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Views — Video list
    private RecyclerView videosRecyclerView;
    private VideoAdapter videoAdapter;
    private EditText searchEditText;
    private Spinner categorySpinner;
    private ArrayAdapter<String> categoryAdapter;

    // Views — Queue
    private RecyclerView queueRecyclerView;
    private QueueAdapter queueAdapter;

    // Views — Player controls
    private TextView nowPlayingTitle;
    private TextView playerStateText;
    private TextView queueCountText;
    private ImageButton btnPlayPause;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnFullscreen;
    private SeekBar volumeSeekBar;

    // Layout panels
    private View panelVideos;
    private View panelQueue;
    private View panelPlayer;

    private ApiService api;
    private final Handler handler = new Handler();
    private boolean isPlaying = false;

    private final Runnable statusPoll = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = ApiClient.getClient().create(ApiService.class);

        setupToolbar();
        setupVideoPanel();
        setupQueuePanel();
        setupPlayerControls();
        setupBottomNav();

        // Carga inicial
        loadVideos();
        loadCategories();
        showPanel(panelVideos);
    }

    @Override protected void onResume() { super.onResume(); handler.post(statusPoll); }
    @Override protected void onPause()  { super.onPause();  handler.removeCallbacks(statusPoll); }

    // ── Toolbar ──────────────────────────────────────────────────────────────
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("🎬 Video Control");
    }

    // ── Panel Videos ─────────────────────────────────────────────────────────
    private void setupVideoPanel() {
        panelVideos = findViewById(R.id.panelVideos);

        videosRecyclerView = findViewById(R.id.videosRecyclerView);
        videosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(this::addToQueue);
        videosRecyclerView.setAdapter(videoAdapter);

        searchEditText = findViewById(R.id.searchEditText);
        Button searchBtn = findViewById(R.id.searchButton);
        searchBtn.setOnClickListener(v -> searchVideos());

        categorySpinner = findViewById(R.id.categorySpinner);
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String sel = categoryAdapter.getItem(pos);
                if (sel != null && !sel.equals("Todas")) loadByCategory(sel);
                else loadVideos();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        Button scanBtn = findViewById(R.id.scanButton);
        scanBtn.setOnClickListener(v -> scanVideos());
    }

    // ── Panel Cola ────────────────────────────────────────────────────────────
    private void setupQueuePanel() {
        panelQueue = findViewById(R.id.panelQueue);

        queueRecyclerView = findViewById(R.id.queueRecyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        queueAdapter = new QueueAdapter(this::removeFromQueue);
        queueRecyclerView.setAdapter(queueAdapter);

        Button clearBtn = findViewById(R.id.clearQueueButton);
        clearBtn.setOnClickListener(v -> clearQueue());

        Button playNextBtn = findViewById(R.id.playNextButton);
        playNextBtn.setOnClickListener(v -> playNext());
    }

    // ── Panel Reproductor ────────────────────────────────────────────────────
    private void setupPlayerControls() {
        panelPlayer = findViewById(R.id.panelPlayer);

        nowPlayingTitle = findViewById(R.id.nowPlayingTitle);
        playerStateText = findViewById(R.id.playerStateText);
        queueCountText  = findViewById(R.id.queueCountText);

        btnPlayPause  = findViewById(R.id.btnPlayPause);
        btnStop       = findViewById(R.id.btnStop);
        btnNext       = findViewById(R.id.btnNext);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        btnPlayPause.setOnClickListener(v -> togglePause());
        btnStop.setOnClickListener(v -> stopPlayer());
        btnNext.setOnClickListener(v -> nextVideo());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        volumeSeekBar.setMax(200);
        volumeSeekBar.setProgress(100);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) setVolume(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_videos) { showPanel(panelVideos); return true; }
            if (id == R.id.nav_queue)  { showPanel(panelQueue); loadQueue(); return true; }
            if (id == R.id.nav_player) { showPanel(panelPlayer); refreshStatus(); return true; }
            return false;
        });
    }

    private void showPanel(View panel) {
        panelVideos.setVisibility(View.GONE);
        panelQueue.setVisibility(View.GONE);
        panelPlayer.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    // ── Carga de videos ───────────────────────────────────────────────────────
    private void loadVideos() {
        api.getAllVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    videoAdapter.setVideos(r.body());
                    if (r.body().isEmpty()) toast("El servidor no tiene videos escaneados");
                } else {
                    toast("Error HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {
                toast("Error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    private void loadCategories() {
        api.getCategories().enqueue(new Callback<List<String>>() {
            @Override public void onResponse(Call<List<String>> c, Response<List<String>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    categoryAdapter.clear();
                    categoryAdapter.add("Todas");
                    categoryAdapter.addAll(r.body());
                }
            }
            @Override public void onFailure(Call<List<String>> c, Throwable t) {}
        });
    }

    private void searchVideos() {
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) { loadVideos(); return; }
        api.searchVideos(q).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    videoAdapter.setVideos(r.body());
                    if (r.body().isEmpty()) toast("Sin resultados para: " + q);
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) { toast("Error en búsqueda"); }
        });
    }

    private void loadByCategory(String cat) {
        api.getByCategory(cat).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) videoAdapter.setVideos(r.body());
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {}
        });
    }

    private void scanVideos() {
        api.scanVideos().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("📂 Escaneo completado");
                    loadVideos();
                    loadCategories();
                } else {
                    toast("Scan HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                toast("Scan error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    // ── Cola ──────────────────────────────────────────────────────────────────
    private void addToQueue(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addToQueue(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful())      toast("🎬 " + video.getTitle() + " agregado a la cola");
                else if (r.code() == 400)  toast("⚠️ Ya está en la cola");
                else                       toast("Error al agregar");
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Sin conexión"); }
        });
    }

    private void loadQueue() {
        api.getQueue().enqueue(new Callback<List<QueueItem>>() {
            @Override public void onResponse(Call<List<QueueItem>> c, Response<List<QueueItem>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    queueAdapter.setQueue(r.body());
                    if (r.body().isEmpty()) toast("La cola está vacía");
                }
            }
            @Override public void onFailure(Call<List<QueueItem>> c, Throwable t) { toast("Error cargando cola"); }
        });
    }

    private void removeFromQueue(QueueItem item) {
        api.removeFromQueue(item.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) { toast("Eliminado"); loadQueue(); }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void clearQueue() {
        api.clearQueue().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) { toast("🗑️ Cola limpiada"); loadQueue(); }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void playNext() {
        api.playNext().enqueue(new Callback<Video>() {
            @Override public void onResponse(Call<Video> c, Response<Video> r) {
                if (r.isSuccessful() && r.body() != null) {
                    toast("▶ Reproduciendo: " + r.body().getTitle());
                    loadQueue();
                } else {
                    toast("No hay más videos en la cola");
                }
            }
            @Override public void onFailure(Call<Video> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Controles reproductor ─────────────────────────────────────────────────
    private void togglePause() {
        api.pause().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { refreshStatus(); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al pausar"); }
        });
    }

    private void stopPlayer() {
        api.stop().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                toast("⏹ Detenido");
                refreshStatus();
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al detener"); }
        });
    }

    private void nextVideo() {
        api.next().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                toast("⏭ Siguiente video");
                refreshStatus();
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void setVolume(int value) {
        api.setVolume(value).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {}
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    private void toggleFullscreen() {
        api.toggleFullscreen().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { toast("🖥 Pantalla completa"); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void refreshStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();
                isPlaying = s.isPlaying();

                String title = s.getCurrentVideo() != null
                        ? s.getCurrentVideo().getTitle()
                        : "Ningún video";
                nowPlayingTitle.setText(title);
                playerStateText.setText(s.getMessage());
                queueCountText.setText("En cola: " + s.getQueueCount() + " video(s)");

                // Icono play/pause dinámico
                btnPlayPause.setImageResource(
                    isPlaying ? android.R.drawable.ic_media_pause
                              : android.R.drawable.ic_media_play);
            }
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {
                playerStateText.setText("Sin conexión con el servidor");
            }
        });
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
*/



/*package com.videocontrol;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videocontrol.adapters.VideoAdapter;
import com.videocontrol.adapters.QueueAdapter;
import com.videocontrol.api.ApiClient;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Views — Video list
    private RecyclerView videosRecyclerView;
    private VideoAdapter videoAdapter;
    private EditText searchEditText;
    private Spinner categorySpinner;
    private ArrayAdapter<String> categoryAdapter;

    // Views — Queue
    private RecyclerView queueRecyclerView;
    private QueueAdapter queueAdapter;

    // Views — Player controls
    private TextView nowPlayingTitle;
    private TextView playerStateText;
    private TextView queueCountText;
    private ImageButton btnPlayPause;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnFullscreen;
    private SeekBar volumeSeekBar;

    private PlayerPanelController playerPanelController;

    // Layout panels
    private View panelVideos;
    private View panelQueue;
    private View panelPlayer;

    private ApiService api;
    private final Handler handler = new Handler();
    private boolean isPlaying = false;

    // ── Control de reproducción continua ────────────────────────────────────────
    // Cuando true, el poller avanzará al siguiente video al detectar que el
    // reproductor quedó inactivo pero aún hay items en la cola.
    private boolean autoPlayEnabled = false;
    // Evita que se llame a playNext() varias veces seguidas mientras la API responde
    private boolean awaitingNextPlay = false;

    // ── Polling de estado (cada 3 s) ─────────────────────────────────────────────
    private final Runnable statusPoll = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = ApiClient.getClient().create(ApiService.class);

        setupToolbar();
        setupVideoPanel();
        setupQueuePanel();
        setupPlayerControls();
        setupBottomNav();

        loadVideos();
        loadCategories();
        showPanel(panelVideos);

        // En onCreate(), después de inflar panelPlayer:
        PlayerPanelController playerPanelController = new PlayerPanelController(
                panelPlayer,
                api,
                () -> playNext()   // callback para el botón "Siguiente"
        );

    }

    @Override protected void onResume() { super.onResume(); handler.post(statusPoll); }
    @Override protected void onPause()  { super.onPause();  handler.removeCallbacks(statusPoll); }

    // ── Toolbar ──────────────────────────────────────────────────────────────────
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("🎬 Video Control");
    }

    // ── Panel Videos ─────────────────────────────────────────────────────────────
    private void setupVideoPanel() {
        panelVideos = findViewById(R.id.panelVideos);

        videosRecyclerView = findViewById(R.id.videosRecyclerView);
        videosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(this::addToQueue);
        videosRecyclerView.setAdapter(videoAdapter);

        searchEditText = findViewById(R.id.searchEditText);
        Button searchBtn = findViewById(R.id.searchButton);
        searchBtn.setOnClickListener(v -> searchVideos());

        categorySpinner = findViewById(R.id.categorySpinner);
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String sel = categoryAdapter.getItem(pos);
                if (sel != null && !sel.equals("Todos los géneros")) loadByCategory(sel);
                else loadVideos();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        Button scanBtn = findViewById(R.id.scanButton);
        scanBtn.setOnClickListener(v -> scanVideos());
    }

    // ── Panel Cola ────────────────────────────────────────────────────────────────
    private void setupQueuePanel() {
        panelQueue = findViewById(R.id.panelQueue);

        queueRecyclerView = findViewById(R.id.queueRecyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        queueAdapter = new QueueAdapter(this::removeFromQueue);
        queueRecyclerView.setAdapter(queueAdapter);

        Button clearBtn = findViewById(R.id.clearQueueButton);
        clearBtn.setOnClickListener(v -> clearQueue());

        // "Reproducir Cola" — inicia la reproducción continua desde el primer item
        Button playQueueBtn = findViewById(R.id.playQueueButton);
        playQueueBtn.setOnClickListener(v -> startAutoPlay());

        // "Siguiente" — avanza manualmente sin detener el modo auto-play
        Button playNextBtn = findViewById(R.id.playNextButton);
        playNextBtn.setOnClickListener(v -> playNext());
    }

    // ── Panel Reproductor ─────────────────────────────────────────────────────────
    *//*private void setupPlayerControls() {
        panelPlayer = findViewById(R.id.panelPlayer);

        nowPlayingTitle = findViewById(R.id.nowPlayingTitle);
        playerStateText = findViewById(R.id.playerStateText);
        queueCountText  = findViewById(R.id.queueCountText);

        btnPlayPause  = findViewById(R.id.btnPlayPause);
        btnStop       = findViewById(R.id.btnStop);
        btnNext       = findViewById(R.id.btnNext);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        btnPlayPause.setOnClickListener(v -> togglePause());
        btnStop.setOnClickListener(v -> stopPlayer());
        // El botón ⏭ en el reproductor también respeta el modo auto-play
        btnNext.setOnClickListener(v -> nextVideo());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        volumeSeekBar.setMax(200);
        volumeSeekBar.setProgress(100);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) setVolume(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }*//*
    *//*private void setupPlayerControls() {
        panelPlayer = findViewById(R.id.panelPlayer);
        playerPanelController = new PlayerPanelController(
                panelPlayer,
                api,
                () -> playNext()
        );
    }*//*
    *//*private void setupPlayerControls() {
        // Si panelPlayer es un ViewGroup contenedor (FrameLayout, etc.)
        View playerView = getLayoutInflater().inflate(R.layout.fragment_player,
                (ViewGroup) findViewById(R.id.panelPlayer), true);
        panelPlayer = playerView;
        playerPanelController = new PlayerPanelController(playerView, api, () -> playNext());
    }*//*
    private void setupPlayerControls() {
        ViewGroup container = findViewById(R.id.panelPlayer);
        View playerView = getLayoutInflater().inflate(R.layout.fragment_player, container, false);
        container.addView(playerView);
        panelPlayer = container;
        playerPanelController = new PlayerPanelController(playerView, api, () -> playNext());
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────────
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_videos) { showPanel(panelVideos); return true; }
            if (id == R.id.nav_queue)  { showPanel(panelQueue); loadQueue(); return true; }
            if (id == R.id.nav_player) { showPanel(panelPlayer); refreshStatus(); return true; }
            return false;
        });
    }

    private void showPanel(View panel) {
        panelVideos.setVisibility(View.GONE);
        panelQueue.setVisibility(View.GONE);
        panelPlayer.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    // ── Carga de videos ───────────────────────────────────────────────────────────
    private void loadVideos() {
        api.getAllVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    videoAdapter.setVideos(r.body());
                    if (r.body().isEmpty()) toast("El servidor no tiene videos escaneados");
                } else {
                    toast("Error HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {
                toast("Error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    private void loadCategories() {
        api.getCategories().enqueue(new Callback<List<String>>() {
            @Override public void onResponse(Call<List<String>> c, Response<List<String>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    categoryAdapter.clear();
                    categoryAdapter.add("Todos los géneros");
                    categoryAdapter.addAll(r.body());
                }
            }
            @Override public void onFailure(Call<List<String>> c, Throwable t) {}
        });
    }

    private void searchVideos() {
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) { loadVideos(); return; }
        api.searchVideos(q).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    videoAdapter.setVideos(r.body());
                    if (r.body().isEmpty()) toast("Sin resultados para: " + q);
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) { toast("Error en búsqueda"); }
        });
    }

    private void loadByCategory(String cat) {
        api.getByCategory(cat).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) videoAdapter.setVideos(r.body());
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {}
        });
    }

    private void scanVideos() {
        api.scanVideos().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("📂 Escaneo completado");
                    loadVideos();
                    loadCategories();
                } else {
                    toast("Scan HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                toast("Scan error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    // ── Cola ───────────────────────────────────────────────────────────────────────

    *//**
     * Agrega un video a la cola. Si el modo auto-play está activo y el reproductor
     * estaba inactivo (cola vacía), arranca la reproducción de inmediato.
     *//*
    private void addToQueue(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addToQueue(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🎬 " + video.getTitle() + " agregado a la cola");
                    // Si auto-play está ON y el player estaba parado, arranca el nuevo item
                    if (autoPlayEnabled && !isPlaying && !awaitingNextPlay) {
                        playNext();
                    }
                } else if (r.code() == 400) {
                    toast("⚠️ Ya está en la cola");
                } else {
                    toast("Error al agregar");
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Sin conexión"); }
        });
    }

    private void loadQueue() {
        api.getQueue().enqueue(new Callback<List<QueueItem>>() {
            @Override public void onResponse(Call<List<QueueItem>> c, Response<List<QueueItem>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    queueAdapter.setQueue(r.body());
                    // Desplaza al primer item de la cola (prioridad más alta / índice 0)
                    if (!r.body().isEmpty()) {
                        queueRecyclerView.scrollToPosition(0);
                    }
                }
            }
            @Override public void onFailure(Call<List<QueueItem>> c, Throwable t) { toast("Error cargando cola"); }
        });
    }

    private void removeFromQueue(QueueItem item) {
        api.removeFromQueue(item.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) { toast("Eliminado"); loadQueue(); }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void clearQueue() {
        api.clearQueue().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    autoPlayEnabled = false;    // detiene el auto-play al limpiar
                    awaitingNextPlay = false;
                    toast("🗑️ Lista limpia");
                    loadQueue();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Reproducción continua ──────────────────────────────────────────────────────

    *//**
     * Activa el modo auto-play y lanza el primer video de la cola.
     * El poller de estado se encargará de llamar a playNext() cuando cada
     * video termine, hasta que la cola quede vacía.
     *//*
    private void startAutoPlay() {
        autoPlayEnabled = true;
        awaitingNextPlay = false;
        toast("▶ Iniciando reproducción continua…");
        playNext();
        nextVideo();
    }

    *//**
     * Solicita al servidor que reproduzca el siguiente item de la cola.
     * Actualiza la lista y hace scroll para mostrar el item actual arriba.
     *//*
    private void playNext() {
        awaitingNextPlay = true;
        api.playNext().enqueue(new Callback<Video>() {
            @Override public void onResponse(Call<Video> c, Response<Video> r) {
                awaitingNextPlay = false;
                if (r.isSuccessful() && r.body() != null) {
                    isPlaying = true;
                    toast("▶ Reproduciendo: " + r.body().getTitle());
                    loadQueue();        // refresca y hace scroll a posición 0
                    refreshStatus();
                } else {
                    // Cola vacía — fin de la reproducción continua
                    isPlaying = false;
                    autoPlayEnabled = false;
                    toast("✅ Lista finalizada");
                    loadQueue();
                    refreshStatus();
                }
            }
            @Override public void onFailure(Call<Video> c, Throwable t) {
                awaitingNextPlay = false;
                toast("Error al avanzar");
            }
        });
    }

    // ── Controles reproductor ──────────────────────────────────────────────────────
    private void togglePause() {
        api.pause().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { refreshStatus(); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al pausar"); }
        });
    }

    *//**
     * Detener implica cancelar también el auto-play para no arrancar
     * el siguiente video automáticamente.
     *//*
    private void stopPlayer() {
        autoPlayEnabled = false;
        awaitingNextPlay = false;
        api.stop().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                toast("⏹ Detenido");
                refreshStatus();
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al detener"); }
        });
    }

    *//**
     * Avance manual con el botón ⏭.
     * Si auto-play está activo llama a playNext() (que también recarga la cola).
     * Si no, sólo envía el comando next al servidor.
     *//*
    private void nextVideo() {
        if (autoPlayEnabled) {
            playNext();
        } else {
            api.next().enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                    toast("⏭ Siguiente video");
                    loadQueue();
                    refreshStatus();
                }
                @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
            });
        }
    }

    private void setVolume(int value) {
        api.setVolume(value).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {}
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    private void toggleFullscreen() {
        api.toggleFullscreen().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { toast("🖥 Pantalla completa"); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Polling de estado ──────────────────────────────────────────────────────────

    *//**
     * Refresca estado del reproductor. Si auto-play está activo y el servidor
     * reporta que ya no está reproduciendo (video terminó) y aún hay items en
     * la cola, dispara automáticamente el siguiente.
     *//*
    *//*private void refreshStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();
                playerPanelController.updateStatus(s);

                *//**//*boolean wasPlaying = isPlaying;
                isPlaying = s.isPlaying();

                String title = s.getCurrentVideo() != null
                        ? s.getCurrentVideo().getTitle()
                        : "Ningún video";
                nowPlayingTitle.setText(title);
                playerStateText.setText(s.getMessage());
                queueCountText.setText("En cola: " + s.getQueueCount() + " video(s)");

                btnPlayPause.setImageResource(
                        isPlaying ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play);*//**//*

                // ── Lógica de reproducción continua ─────────────────────────────
                // Si el video anterior terminó (wasPlaying → !isPlaying),
                // auto-play está ON, no hay una solicitud en vuelo y aún hay cola:
                // avanzamos al siguiente automáticamente.
                *//**//*if (autoPlayEnabled
                        && wasPlaying
                        && !isPlaying
                        && !awaitingNextPlay
                        && s.getQueueCount() > 0) {
                    playNext();
                }

                // Si la cola se agotó, desactivamos auto-play
                if (autoPlayEnabled && !isPlaying && s.getQueueCount() == 0) {
                    autoPlayEnabled = false;
                }*//**//*




            }
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {
                playerStateText.setText("Sin conexión con el servidor");
            }
        });
    }*//*
    private void refreshStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();

                // Actualiza toda la UI del reproductor
                playerPanelController.updateStatus(s);

                // Lógica de auto-play (vive en MainActivity, no en el controlador)
                boolean wasPlaying = isPlaying;
                isPlaying = s.isPlaying;

                if (autoPlayEnabled && wasPlaying && !isPlaying
                        && !awaitingNextPlay && s.queueCount > 0) {
                    playNext();
                }
                if (autoPlayEnabled && !isPlaying && s.queueCount == 0) {
                    autoPlayEnabled = false;
                }
            }
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {
                // sin conexión — no hay playerStateText directo, el panel lo maneja
            }
        });
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}*/





package com.videocontrol;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videocontrol.adapters.VideoAdapter;
import com.videocontrol.adapters.QueueAdapter;
import com.videocontrol.api.ApiClient;
import com.videocontrol.api.ApiService;
import com.videocontrol.models.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ── Vistas — Lista de videos ───────────────────────────────────────────────
    private RecyclerView videosRecyclerView;
    private VideoAdapter videoAdapter;
    private EditText searchEditText;
    private Spinner categorySpinner;
    private ArrayAdapter<String> categoryAdapter;

    // ── Vistas — Cola ─────────────────────────────────────────────────────────
    private RecyclerView queueRecyclerView;
    private QueueAdapter queueAdapter;

    // ── Controlador de panel reproductor ──────────────────────────────────────
    private PlayerPanelController playerPanelController;

    // ── Panels ────────────────────────────────────────────────────────────────
    private View panelVideos;
    private View panelQueue;
    private View panelPlayer;

    // ── API ───────────────────────────────────────────────────────────────────
    private ApiService api;

    // ── Estado de reproducción ─────────────────────────────────────────────────
    private boolean isPlaying       = false;
    private boolean autoPlayEnabled = false;
    private boolean awaitingNextPlay = false;

    /**
     * Lista completa de videos disponibles (se actualiza con loadVideos/loadByCategory).
     * Se usa para construir la lista de IDs al hacer shuffle.
     */
    private List<Video> allAvailableVideos = new ArrayList<>();

    // ── Polling de estado (cada 3 s) ───────────────────────────────────────────
    private final Handler handler = new Handler();
    private final Runnable statusPoll = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 3000);
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = ApiClient.getClient().create(ApiService.class);

        setupToolbar();
        setupVideoPanel();
        setupQueuePanel();
        setupPlayerControls();
        setupBottomNav();

        loadVideos();
        loadCategories();
        showPanel(panelVideos);
    }

    @Override protected void onResume() { super.onResume(); handler.post(statusPoll); }
    @Override protected void onPause()  { super.onPause();  handler.removeCallbacks(statusPoll); }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("🎬 Video Control");
    }

    // ── Panel Videos ──────────────────────────────────────────────────────────
    private void setupVideoPanel() {
        panelVideos = findViewById(R.id.panelVideos);

        videosRecyclerView = findViewById(R.id.videosRecyclerView);
        videosRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // VideoAdapter ahora recibe dos callbacks: agregar al final y agregar como siguiente
        videoAdapter = new VideoAdapter(new VideoAdapter.OnVideoListener() {
            @Override public void onAddToQueue(Video video) { addToQueue(video); }
            @Override public void onAddNext(Video video)    { addNext(video); }
        });
        videosRecyclerView.setAdapter(videoAdapter);

        searchEditText = findViewById(R.id.searchEditText);
        Button searchBtn = findViewById(R.id.searchButton);
        searchBtn.setOnClickListener(v -> searchVideos());

        categorySpinner = findViewById(R.id.categorySpinner);
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String sel = categoryAdapter.getItem(pos);
                if (sel != null && !sel.equals("Todos los géneros")) loadByCategory(sel);
                else loadVideos();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        Button scanBtn = findViewById(R.id.scanButton);
        scanBtn.setOnClickListener(v -> scanVideos());
    }

    // ── Panel Cola ────────────────────────────────────────────────────────────
    private void setupQueuePanel() {
        panelQueue = findViewById(R.id.panelQueue);

        queueRecyclerView = findViewById(R.id.queueRecyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        queueAdapter = new QueueAdapter(this::removeFromQueue);
        queueRecyclerView.setAdapter(queueAdapter);

        // "Limpiar cola"
        Button clearBtn = findViewById(R.id.clearQueueButton);
        clearBtn.setOnClickListener(v -> clearQueue());

        // "▶ Reproducir" — inicia reproducción continua desde el primer item
        Button playQueueBtn = findViewById(R.id.playQueueButton);
        playQueueBtn.setOnClickListener(v -> startAutoPlay());

        // "🔀 Aleatorio" — mezcla todos los videos disponibles y llena la cola
        Button shuffleBtn = findViewById(R.id.shuffleButton);
        shuffleBtn.setOnClickListener(v -> shuffleAndPlay());

        // "⏭ Siguiente" — avanza manualmente
        Button playNextBtn = findViewById(R.id.playNextButton);
        playNextBtn.setOnClickListener(v -> playNext());
    }

    // ── Panel Reproductor ──────────────────────────────────────────────────────
    private void setupPlayerControls() {
        ViewGroup container = findViewById(R.id.panelPlayer);
        View playerView = getLayoutInflater().inflate(R.layout.fragment_player, container, false);
        container.addView(playerView);
        panelPlayer = container;
        playerPanelController = new PlayerPanelController(playerView, api, () -> playNext());
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_videos) { showPanel(panelVideos); return true; }
            if (id == R.id.nav_queue)  { showPanel(panelQueue); loadQueue(); return true; }
            if (id == R.id.nav_player) { showPanel(panelPlayer); refreshStatus(); return true; }
            return false;
        });
    }

    private void showPanel(View panel) {
        panelVideos.setVisibility(View.GONE);
        panelQueue.setVisibility(View.GONE);
        panelPlayer.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    // ── Carga de videos ────────────────────────────────────────────────────────
    private void loadVideos() {
        api.getAllVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    allAvailableVideos = r.body();   // ← guardar para shuffle
                    videoAdapter.setVideos(allAvailableVideos);
                    if (allAvailableVideos.isEmpty()) toast("El servidor no tiene videos escaneados");
                } else {
                    toast("Error HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {
                toast("Error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    private void loadCategories() {
        api.getCategories().enqueue(new Callback<List<String>>() {
            @Override public void onResponse(Call<List<String>> c, Response<List<String>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    categoryAdapter.clear();
                    categoryAdapter.add("Todos los géneros");
                    categoryAdapter.addAll(r.body());
                }
            }
            @Override public void onFailure(Call<List<String>> c, Throwable t) {}
        });
    }

    private void searchVideos() {
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) { loadVideos(); return; }
        api.searchVideos(q).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    // Los resultados de búsqueda también actualizan allAvailableVideos
                    allAvailableVideos = r.body();
                    videoAdapter.setVideos(allAvailableVideos);
                    if (allAvailableVideos.isEmpty()) toast("Sin resultados para: " + q);
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) { toast("Error en búsqueda"); }
        });
    }

    private void loadByCategory(String cat) {
        api.getByCategory(cat).enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    allAvailableVideos = r.body();   // ← guardar para shuffle
                    videoAdapter.setVideos(allAvailableVideos);
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {}
        });
    }

    private void scanVideos() {
        api.scanVideos().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("📂 Escaneo completado");
                    loadVideos();
                    loadCategories();
                } else {
                    toast("Scan HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                toast("Scan error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    // ── Cola ───────────────────────────────────────────────────────────────────

    /** Agrega el video al final de la cola (botón "Agregar" en la lista de videos). */
    private void addToQueue(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addToQueue(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🎬 " + video.getTitle() + " agregado a la cola");
                    if (autoPlayEnabled && !isPlaying && !awaitingNextPlay) playNext();
                } else if (r.code() == 400) {
                    toast("⚠️ Ya está en la cola");
                } else {
                    toast("Error al agregar");
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Sin conexión"); }
        });
    }

    /**
     * Inserta el video justo después del que se está reproduciendo,
     * para que suene a continuación (botón "▶ Siguiente" en la lista de videos).
     */
    private void addNext(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addNext(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("⏭ " + video.getTitle() + " sonará a continuación");
                    // Si la cola estaba vacía y auto-play está ON, arrancar reproducción
                    if (autoPlayEnabled && !isPlaying && !awaitingNextPlay) playNext();
                } else if (r.code() == 400) {
                    toast("⚠️ Ya está en la cola");
                } else {
                    toast("Error al insertar");
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Sin conexión"); }
        });
    }

    private void loadQueue() {
        api.getQueue().enqueue(new Callback<List<QueueItem>>() {
            @Override public void onResponse(Call<List<QueueItem>> c, Response<List<QueueItem>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    queueAdapter.setQueue(r.body());
                    if (!r.body().isEmpty()) queueRecyclerView.scrollToPosition(0);
                }
            }
            @Override public void onFailure(Call<List<QueueItem>> c, Throwable t) {
                toast("Error cargando cola");
            }
        });
    }

    private void removeFromQueue(QueueItem item) {
        api.removeFromQueue(item.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) { toast("Eliminado"); loadQueue(); }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    private void clearQueue() {
        api.clearQueue().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    autoPlayEnabled  = false;
                    awaitingNextPlay = false;
                    toast("🗑️ Lista limpia");
                    loadQueue();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Reproducción continua ──────────────────────────────────────────────────

    /**
     * Activa auto-play y lanza el primer video de la cola existente.
     * No mezcla; sólo empieza a reproducir en el orden actual.
     */
    private void startAutoPlay() {
        autoPlayEnabled  = true;
        awaitingNextPlay = false;
        toast("▶ Iniciando reproducción continua…");
        playNext();
    }

    /**
     * Mezcla todos los videos visibles en la pestaña "Videos", llena la cola
     * con hasta 20 aleatorios, activa auto-play y arranca la reproducción.
     *
     * Si no hay videos cargados en la vista, intenta cargarlos primero.
     */
    private void shuffleAndPlay() {
        if (allAvailableVideos.isEmpty()) {
            toast("Cargando videos…");
            api.getAllVideos().enqueue(new Callback<List<Video>>() {
                @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                    if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                        allAvailableVideos = r.body();
                        videoAdapter.setVideos(allAvailableVideos);
                        doShuffleAndPlay();
                    } else {
                        toast("No hay videos disponibles");
                    }
                }
                @Override public void onFailure(Call<List<Video>> c, Throwable t) {
                    toast("Sin conexión con el servidor");
                }
            });
        } else {
            doShuffleAndPlay();
        }
    }

    /**
     * Envía la petición shuffle-and-fill al servidor con los IDs actuales
     * y luego arranca la reproducción continua.
     */
    private void doShuffleAndPlay() {
        List<Integer> ids = new ArrayList<>();
        for (Video v : allAvailableVideos) ids.add(v.getId());

        ApiService.ShuffleDto dto = new ApiService.ShuffleDto(ids, "Android");
        api.shuffleAndFill(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🔀 Cola aleatoria lista");
                    autoPlayEnabled  = true;
                    awaitingNextPlay = false;
                    loadQueue();      // refresca la vista de la cola
                    // Si no hay nada sonando, arranca el primero
                    if (!isPlaying) playNext();
                } else {
                    toast("Error al mezclar: " + r.code());
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                toast("Sin conexión con el servidor");
            }
        });
    }

    /** Avanza al siguiente item de la cola. */
    private void playNext() {
        awaitingNextPlay = true;
        api.playNext().enqueue(new Callback<Video>() {
            @Override public void onResponse(Call<Video> c, Response<Video> r) {
                awaitingNextPlay = false;
                if (r.isSuccessful() && r.body() != null) {
                    isPlaying = true;
                    toast("▶ Reproduciendo: " + r.body().getTitle());
                    loadQueue();
                    refreshStatus();
                } else {
                    // Cola vacía → fin de reproducción continua
                    isPlaying        = false;
                    autoPlayEnabled  = false;
                    toast("✅ Lista finalizada");
                    loadQueue();
                    refreshStatus();
                }
            }
            @Override public void onFailure(Call<Video> c, Throwable t) {
                awaitingNextPlay = false;
                toast("Error al avanzar");
            }
        });
    }

    // ── Controles del reproductor ──────────────────────────────────────────────
    private void togglePause() {
        api.pause().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { refreshStatus(); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al pausar"); }
        });
    }

    private void stopPlayer() {
        autoPlayEnabled  = false;
        awaitingNextPlay = false;
        api.stop().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                toast("⏹ Detenido");
                refreshStatus();
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error al detener"); }
        });
    }

    private void nextVideo() {
        if (autoPlayEnabled) {
            playNext();
        } else {
            api.next().enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                    toast("⏭ Siguiente video");
                    loadQueue();
                    refreshStatus();
                }
                @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
            });
        }
    }

    private void setVolume(int value) {
        api.setVolume(value).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {}
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    private void toggleFullscreen() {
        api.toggleFullscreen().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { toast("🖥 Pantalla completa"); }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Polling de estado ──────────────────────────────────────────────────────
    private void refreshStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();

                playerPanelController.updateStatus(s);

                boolean wasPlaying = isPlaying;
                isPlaying = s.isPlaying;

                // Auto-play: si el video terminó y aún hay cola, avanzar
                if (autoPlayEnabled && wasPlaying && !isPlaying
                        && !awaitingNextPlay && s.queueCount > 0) {
                    playNext();
                }
                // Apagar auto-play si la cola se agotó
                if (autoPlayEnabled && !isPlaying && s.queueCount == 0) {
                    autoPlayEnabled = false;
                }
            }
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {
                // Sin conexión — el panel reproductor lo maneja
            }
        });
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}

