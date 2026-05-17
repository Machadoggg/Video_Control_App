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
