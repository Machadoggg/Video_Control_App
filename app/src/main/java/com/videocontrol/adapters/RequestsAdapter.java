package com.videocontrol.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.videocontrol.R;
import com.videocontrol.models.SongRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    private List<SongRequest> requests = new ArrayList<>();

    // IDs marcados localmente como reproducidos (sin borrarlos de la lista)
    private final Set<Integer> markedPlayed = new HashSet<>();

    private final OnRequestListener listener;

    public interface OnRequestListener {
        void onMarkPlayed(SongRequest req);
        void onDelete(SongRequest req);
    }

    public RequestsAdapter(OnRequestListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<SongRequest> requests) {
        this.requests = requests != null ? requests : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder h, int position) {
        SongRequest req = requests.get(position);
        boolean played = markedPlayed.contains(req.getId());

        // Título
        h.songTitle.setText(req.getSongTitle());
        // Si está marcada, texto tachado visualmente con color muted
        h.songTitle.setTextColor(played
                ? Color.parseColor("#6b6b8a")
                : Color.parseColor("#f0f0ff"));
        h.songTitle.setPaintFlags(played
                ? h.songTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                : h.songTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        // Artista
        String artist = req.getArtistName() != null && !req.getArtistName().isEmpty()
                ? "🎤 " + req.getArtistName() : "";
        h.artistName.setText(artist);
        h.artistName.setVisibility(artist.isEmpty() ? View.GONE : View.VISIBLE);
        h.artistName.setTextColor(played
                ? Color.parseColor("#444455")
                : Color.parseColor("#9999aa"));

        // Mesa y nombre
        String table = req.getTableNumber() != null && !req.getTableNumber().isEmpty()
                ? "🪑 Mesa " + req.getTableNumber() : "Sin mesa";
        String who = req.getRequestedBy() != null && !req.getRequestedBy().isEmpty()
                ? "  •  " + req.getRequestedBy() : "";
        h.tableName.setText(table + who);
        h.tableName.setTextColor(played
                ? Color.parseColor("#444455")
                : Color.parseColor("#0099FF"));

        // Hora
        /*String time = req.getRequestedAt() != null && req.getRequestedAt().length() >= 16
                ? req.getRequestedAt().substring(11, 16) : "";
        h.timeText.setText(time);*/
        String time = "";
        if (req.getRequestedAt() != null && !req.getRequestedAt().isEmpty()) {
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(
                        req.getRequestedAt().replace(" ", "T").endsWith("Z")
                                ? req.getRequestedAt().replace(" ", "T")
                                : req.getRequestedAt().replace(" ", "T") + "Z");
                java.time.ZonedDateTime local = odt.atZoneSameInstant(java.time.ZoneId.systemDefault());
                time = String.format("%02d:%02d", local.getHour(), local.getMinute());
            } catch (Exception e) {
                // fallback: mostrar tal cual
                time = req.getRequestedAt().length() >= 16
                        ? req.getRequestedAt().substring(11, 16) : "";
            }
        }
        h.timeText.setText(time);




        // Botón ✅ — verde si ya marcada, gris si pendiente
        h.markPlayedBtn.setBackgroundColor(played
                ? Color.parseColor("#1a4a2a")   // verde oscuro = ya marcada
                : Color.parseColor("#00CC44")); // verde brillante = pendiente
        h.markPlayedBtn.setAlpha(played ? 0.5f : 1.0f);

        h.markPlayedBtn.setOnClickListener(v -> {
            if (!played) {
                // Marcar localmente sin quitar de la lista
                markedPlayed.add(req.getId());
                notifyItemChanged(position);
                // Notificar al servidor
                listener.onMarkPlayed(req);
            }
            // Si ya está marcada, no hace nada al volver a tocar
        });

        h.deleteBtn.setOnClickListener(v -> {
            markedPlayed.remove(req.getId());
            listener.onDelete(req);
        });
    }

    @Override
    public int getItemCount() { return requests.size(); }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView    songTitle, artistName, tableName, timeText;
        ImageButton markPlayedBtn, deleteBtn;

        RequestViewHolder(@NonNull View v) {
            super(v);
            songTitle     = v.findViewById(R.id.reqSongTitle);
            artistName    = v.findViewById(R.id.reqArtistName);
            tableName     = v.findViewById(R.id.reqTableName);
            timeText      = v.findViewById(R.id.reqTime);
            markPlayedBtn = v.findViewById(R.id.reqMarkPlayedBtn);
            deleteBtn     = v.findViewById(R.id.reqDeleteBtn);
        }
    }
}
