package com.videocontrol;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videocontrol.adapters.VideoAdapter;
import com.videocontrol.adapters.QueueAdapter;
import com.videocontrol.adapters.RequestsAdapter;
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

    // ── Vistas — Solicitudes ───────────────────────────────────────────────────
    private RecyclerView requestsRecyclerView;
    private RequestsAdapter requestsAdapter;
    private TextView requestsBadge;   // contador de solicitudes pendientes

    // ── Controlador de panel reproductor ──────────────────────────────────────
    private PlayerPanelController playerPanelController;

    // ── Panels ────────────────────────────────────────────────────────────────
    private View panelVideos;
    private View panelQueue;
    private View panelPlayer;
    private View panelRequests;

    // ── API ───────────────────────────────────────────────────────────────────
    private ApiService api;

    // ── Estado de reproducción ─────────────────────────────────────────────────
    private boolean isPlaying        = false;
    private boolean autoPlayEnabled  = false;
    private boolean awaitingNextPlay = false;

    /** Lista completa de videos disponibles para shuffle. */
    private List<Video> allAvailableVideos = new ArrayList<>();

    // ── Polling ───────────────────────────────────────────────────────────────
    private final Handler handler = new Handler();
    private final Runnable statusPoll = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 3000);
        }
    };
    // Polling de solicitudes: cada 8 segundos cuando la pestaña está activa
    private final Handler requestsHandler = new Handler();
    private boolean requestsPanelVisible = false;
    private final Runnable requestsPoll = new Runnable() {
        @Override public void run() {
            if (requestsPanelVisible) {
                loadRequests();
                requestsHandler.postDelayed(this, 8000);
            }
        }
    };
    // Polling de badge (siempre activo, cada 15 s)
    private final Handler badgeHandler = new Handler();
    private final Runnable badgePoll = new Runnable() {
        @Override public void run() {
            refreshRequestsBadge();
            badgeHandler.postDelayed(this, 15000);
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
        setupRequestsPanel();
        setupBottomNav();

        loadVideos();
        loadCategories();
        showPanel(panelVideos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(statusPoll);
        badgeHandler.post(badgePoll);
        loadQueue();
        refreshStatus();
        refreshRequestsBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statusPoll);
        badgeHandler.removeCallbacks(badgePoll);
        requestsHandler.removeCallbacks(requestsPoll);
        requestsPanelVisible = false;
    }

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

        Button clearBtn = findViewById(R.id.clearQueueButton);
        clearBtn.setOnClickListener(v -> clearQueue());

        Button playQueueBtn = findViewById(R.id.playQueueButton);
        playQueueBtn.setOnClickListener(v -> startAutoPlay());

        Button shuffleBtn = findViewById(R.id.shuffleButton);
        shuffleBtn.setOnClickListener(v -> shuffleAndPlay());

        Button playNextBtn = findViewById(R.id.playNextButton);
        playNextBtn.setOnClickListener(v -> playNext());
    }

    // ── Panel Reproductor ──────────────────────────────────────────────────────
    /*private void setupPlayerControls() {
        ViewGroup container = findViewById(R.id.panelPlayer);
        View playerView = getLayoutInflater().inflate(R.layout.fragment_player, container, false);
        container.addView(playerView);
        panelPlayer = container;
        playerPanelController = new PlayerPanelController(playerView, api, () -> playNext());
    }*/
    private void setupPlayerControls() {
        panelPlayer = findViewById(R.id.panelPlayer);
        playerPanelController = new PlayerPanelController(panelPlayer, api, () -> playNext());
    }

    // ── Panel Solicitudes ──────────────────────────────────────────────────────
    private void setupRequestsPanel() {
        panelRequests = findViewById(R.id.panelRequests);
        //requestsBadge = findViewById(R.id.requestsBadge);

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsAdapter = new RequestsAdapter(new RequestsAdapter.OnRequestListener() {
            @Override public void onMarkPlayed(SongRequest req) { markRequestPlayed(req); }
            @Override public void onDelete(SongRequest req)     { deleteRequest(req); }
        });
        requestsRecyclerView.setAdapter(requestsAdapter);

        Button refreshBtn = findViewById(R.id.requestsRefreshButton);
        refreshBtn.setOnClickListener(v -> loadRequests());
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_videos) {
                requestsPanelVisible = false;
                showPanel(panelVideos);
                return true;
            }
            if (id == R.id.nav_queue) {
                requestsPanelVisible = false;
                showPanel(panelQueue);
                loadQueue();
                return true;
            }
            if (id == R.id.nav_player) {
                requestsPanelVisible = false;
                showPanel(panelPlayer);
                refreshStatus();
                return true;
            }
            if (id == R.id.nav_requests) {
                requestsPanelVisible = true;
                showPanel(panelRequests);
                loadRequests();
                requestsHandler.removeCallbacks(requestsPoll);
                requestsHandler.postDelayed(requestsPoll, 8000);
                return true;
            }
            return false;
        });
    }

    private void showPanel(View panel) {
        panelVideos.setVisibility(View.GONE);
        panelQueue.setVisibility(View.GONE);
        panelPlayer.setVisibility(View.GONE);
        panelRequests.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    // ── Carga de videos ────────────────────────────────────────────────────────
    private void loadVideos() {
        api.getAllVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> c, Response<List<Video>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    allAvailableVideos = r.body();
                    videoAdapter.setVideos(allAvailableVideos);
                    if (allAvailableVideos.isEmpty()) toast("El servidor no tiene videos escaneados");
                } else {
                    toast("Error HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<List<Video>> c, Throwable t) {
                toast("Error: " + t.getClass().getSimpleName());
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
                    allAvailableVideos = r.body();
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
                toast("Scan error: " + t.getClass().getSimpleName());
            }
        });
    }

    // ── Cola ───────────────────────────────────────────────────────────────────
    private void addToQueue(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addToQueue(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🎬 " + video.getTitle() + " agregado");
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

    private void addNext(Video video) {
        ApiService.AddToQueueDto dto = new ApiService.AddToQueueDto(video.getId(), "Android");
        api.addNext(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("⏭ " + video.getTitle() + " sonará a continuación");
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

    /*private void clearQueue() {
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
    }*/
    private void clearQueue() {
        api.clearQueue().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    autoPlayEnabled  = false;
                    awaitingNextPlay = false;
                    PlayerService.autoPlayEnabled = false;
                    stopService(new Intent(MainActivity.this, PlayerService.class));
                    toast("🗑️ Lista limpia");
                    loadQueue();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }




    // ── Reproducción continua ──────────────────────────────────────────────────
    private void startAutoPlay() {
        autoPlayEnabled  = true;
        awaitingNextPlay = false;
        PlayerService.autoPlayEnabled = true;
        startPlayerService(true);      // ← inicia el servicio segundo plano
        toast("▶ Iniciando reproducción continua…");
        playNext();
    }

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

    private void doShuffleAndPlay() {
        List<Integer> ids = new ArrayList<>();
        for (Video v : allAvailableVideos) ids.add(v.getId());
        ApiService.ShuffleDto dto = new ApiService.ShuffleDto(ids, "Android");

        /*api.shuffleAndFill(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🔀 Cola aleatoria lista");
                    autoPlayEnabled  = true;
                    awaitingNextPlay = false;
                    loadQueue();
                    if (!isPlaying) playNext();
                } else {
                    toast("Error al mezclar: " + r.code());
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                toast("Sin conexión con el servidor");
            }
        });*/
        api.shuffleAndFill(dto).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    autoPlayEnabled = true;
                    PlayerService.autoPlayEnabled = true;
                    startPlayerService(true);   // ← inicia el servicio
                    loadQueue();
                    if (!isPlaying) playNext();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });



    }

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

    // ── Solicitudes de canciones ───────────────────────────────────────────────
    /*private void loadRequests() {
        api.getPendingRequests().enqueue(new Callback<List<SongRequest>>() {
            @Override public void onResponse(Call<List<SongRequest>> c, Response<List<SongRequest>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    requestsAdapter.setRequests(r.body());
                    updateBadge(r.body().size());
                }
            }
            @Override public void onFailure(Call<List<SongRequest>> c, Throwable t) {
                toast("Error cargando solicitudes");
            }
        });
    }*/
    private void loadRequests() {
        api.getAllRequests().enqueue(new Callback<List<SongRequest>>() {
            @Override public void onResponse(Call<List<SongRequest>> c, Response<List<SongRequest>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    requestsAdapter.setRequests(r.body());
                    updateBadge((int) r.body().stream()
                            .filter(req -> !req.isPlayed())
                            .count());
                }
            }
            @Override public void onFailure(Call<List<SongRequest>> c, Throwable t) {
                toast("Error cargando solicitudes");
            }
        });
    }

    /*private void refreshRequestsBadge() {
        api.getPendingRequests().enqueue(new Callback<List<SongRequest>>() {
            @Override public void onResponse(Call<List<SongRequest>> c, Response<List<SongRequest>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    updateBadge(r.body().size());
                }
            }
            @Override public void onFailure(Call<List<SongRequest>> c, Throwable t) {}
        });
    }*/
    private void refreshRequestsBadge() {
        api.getAllRequests().enqueue(new Callback<List<SongRequest>>() {
            @Override public void onResponse(Call<List<SongRequest>> c, Response<List<SongRequest>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    int pending = (int) r.body().stream()
                            .filter(req -> !req.isPlayed())
                            .count();
                    updateBadge(pending);
                }
            }
            @Override public void onFailure(Call<List<SongRequest>> c, Throwable t) {}
        });
    }


    /*private void updateBadge(int count) {
        if (requestsBadge == null) return;
        if (count > 0) {
            requestsBadge.setVisibility(View.VISIBLE);
            requestsBadge.setText(String.valueOf(count));
        } else {
            requestsBadge.setVisibility(View.GONE);
        }
    }*/
    private void updateBadge(int count) {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_requests);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }

        // Actualizar texto dentro del panel
        TextView countText = findViewById(R.id.pendingCountText);
        if (countText != null) {
            countText.setText(count > 0
                    ? "🎵 SOLICITUDES: " + count
                    : "✅ Sin solicitudes pendientes");
            /*countText.setTextColor(count > 0
                    ? Color.parseColor("#00CC44")
                    : Color.parseColor("#6b6b8a"));*/
        }
    }





    /*private void markRequestPlayed(SongRequest req) {
        api.markRequestPlayed(req.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("✅ Marcada como reproducida");
                    loadRequests();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }*/
    /*private void markRequestPlayed(SongRequest req) {
        api.markRequestPlayed(req.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("✅ Marcada como reproducida");
                    // NO llamar loadRequests() aquí — el adapter ya actualizó la UI
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }*/
    private void markRequestPlayed(SongRequest req) {
        // Solo notifica al servidor en background, no recarga la lista
        api.markRequestPlayed(req.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {}
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }





    private void deleteRequest(SongRequest req) {
        api.deleteRequest(req.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    toast("🗑️ Solicitud eliminada");
                    loadRequests();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { toast("Error"); }
        });
    }

    // ── Controles del reproductor ──────────────────────────────────────────────
    private void refreshStatus() {
        api.getStatus().enqueue(new Callback<PlayerStatus>() {
            @Override public void onResponse(Call<PlayerStatus> c, Response<PlayerStatus> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                PlayerStatus s = r.body();
                playerPanelController.updateStatus(s);
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
            @Override public void onFailure(Call<PlayerStatus> c, Throwable t) {}
        });
    }

    private void startPlayerService(boolean autoPlay) {
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtra("autoPlay", autoPlay);
        startForegroundService(intent);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
