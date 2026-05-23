/*
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
import com.videocontrol.models.QueueItem;
import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private List<QueueItem> queue = new ArrayList<>();
    private final OnQueueListener listener;

    public interface OnQueueListener {
        void onRemove(QueueItem item);
    }

    public QueueAdapter(OnQueueListener listener) {
        this.listener = listener;
    }

    public void setQueue(List<QueueItem> queue) {
        this.queue = queue;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder h, int position) {
        QueueItem item = queue.get(position);

        if (item.isPlaying()) {
            h.posText.setText("▶");
            h.posText.setTextColor(Color.parseColor("#00CC44"));
            h.titleText.setTextColor(Color.parseColor("#00CC44"));
        } else {
            h.posText.setText("#" + item.getPosition());
            h.posText.setTextColor(Color.parseColor("#0099FF"));
            h.titleText.setTextColor(Color.WHITE);
        }

        h.titleText.setText(item.getVideo() != null ? item.getVideo().getTitle() : "...");
        h.categoryText.setText(item.getVideo() != null ? item.getVideo().getCategory() : "");
        h.removeButton.setOnClickListener(v -> listener.onRemove(item));

        // No mostrar botón eliminar si está reproduciendo
        h.removeButton.setVisibility(item.isPlaying() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return queue.size(); }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView posText, titleText, categoryText;
        ImageButton removeButton;

        QueueViewHolder(@NonNull View v) {
            super(v);
            posText      = v.findViewById(R.id.positionText);
            titleText    = v.findViewById(R.id.titleText);
            categoryText = v.findViewById(R.id.categoryText);
            removeButton = v.findViewById(R.id.removeButton);
        }
    }
}
*/



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
import com.videocontrol.models.QueueItem;
import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    /** Número máximo de items que se muestran en la lista (incluye el que suena). */
    private static final int MAX_VISIBLE = 20;

    private List<QueueItem> queue = new ArrayList<>();
    private final OnQueueListener listener;

    public interface OnQueueListener {
        void onRemove(QueueItem item);
    }

    public QueueAdapter(OnQueueListener listener) {
        this.listener = listener;
    }

    /**
     * Actualiza la lista recortando a {@link #MAX_VISIBLE} items como máximo.
     * El recorte se aplica a la vista solamente; el servidor mantiene la cola completa.
     */
    public void setQueue(List<QueueItem> queue) {
        if (queue != null && queue.size() > MAX_VISIBLE) {
            this.queue = queue.subList(0, MAX_VISIBLE);
        } else {
            this.queue = queue != null ? queue : new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder h, int position) {
        QueueItem item = queue.get(position);

        if (item.isPlaying()) {
            h.posText.setText("▶");
            h.posText.setTextColor(Color.parseColor("#00CC44"));
            h.titleText.setTextColor(Color.parseColor("#00CC44"));
        } else {
            h.posText.setText("#" + item.getPosition());
            h.posText.setTextColor(Color.parseColor("#0099FF"));
            h.titleText.setTextColor(Color.WHITE);
        }

        h.titleText.setText(item.getVideo() != null ? item.getVideo().getTitle() : "...");
        h.categoryText.setText(item.getVideo() != null ? item.getVideo().getCategory() : "");
        h.removeButton.setOnClickListener(v -> listener.onRemove(item));

        // No mostrar botón eliminar si está reproduciendo
        h.removeButton.setVisibility(item.isPlaying() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return queue.size(); }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView posText, titleText, categoryText;
        ImageButton removeButton;

        QueueViewHolder(@NonNull View v) {
            super(v);
            posText      = v.findViewById(R.id.positionText);
            titleText    = v.findViewById(R.id.titleText);
            categoryText = v.findViewById(R.id.categoryText);
            removeButton = v.findViewById(R.id.removeButton);
        }
    }
}
