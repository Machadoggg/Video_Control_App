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
import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    private List<SongRequest> requests = new ArrayList<>();
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

        h.songTitle.setText(req.getSongTitle());

        String artist = req.getArtistName() != null && !req.getArtistName().isEmpty()
                ? "🎤 " + req.getArtistName() : "";
        h.artistName.setText(artist);
        h.artistName.setVisibility(artist.isEmpty() ? View.GONE : View.VISIBLE);

        String table = req.getTableNumber() != null && !req.getTableNumber().isEmpty()
                ? "🪑 Mesa " + req.getTableNumber() : "Sin mesa";
        String who = req.getRequestedBy() != null && !req.getRequestedBy().isEmpty()
                ? "  •  " + req.getRequestedBy() : "";
        h.tableName.setText(table + who);

        // Hora — viene como ISO, mostramos solo HH:mm
        String time = req.getRequestedAt() != null && req.getRequestedAt().length() >= 16
                ? req.getRequestedAt().substring(11, 16) : "";
        h.timeText.setText(time);

        h.markPlayedBtn.setOnClickListener(v -> listener.onMarkPlayed(req));
        h.deleteBtn.setOnClickListener(v -> listener.onDelete(req));
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
