/*
package com.videocontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.videocontrol.R;
import com.videocontrol.models.Video;
import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos = new ArrayList<>();
    private final OnVideoListener listener;

    public interface OnVideoListener {
        void onAddToQueue(Video video);
    }

    public VideoAdapter(OnVideoListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder h, int position) {
        Video video = videos.get(position);
        h.titleText.setText(video.getTitle() != null ? video.getTitle() : "(sin título)");

        String ext = video.getExtension() != null ? video.getExtension().toUpperCase() : "";
        String size = video.getFileSizeFormatted() != null ? video.getFileSizeFormatted() : "";

        h.categoryText.setText("📁 " + (video.getCategory() != null ? video.getCategory() : "General") + "  •  " + (ext.isEmpty() ? "" : "" + ext) + "  •  " + size);

        //h.sizeText.setText(size + (ext.isEmpty() ? "" : "  •  " + ext));

        h.addButton.setOnClickListener(v -> listener.onAddToQueue(video));
    }

    @Override
    public int getItemCount() { return videos.size(); }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, categoryText, sizeText;
        Button addButton;

        VideoViewHolder(@NonNull View v) {
            super(v);
            titleText    = v.findViewById(R.id.videoTitle);
            categoryText = v.findViewById(R.id.videoCategory);
            //sizeText     = v.findViewById(R.id.videoSize);
            addButton    = v.findViewById(R.id.addButton);
        }
    }
}
*/


package com.videocontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.videocontrol.R;
import com.videocontrol.models.Video;
import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos = new ArrayList<>();
    private final OnVideoListener listener;

    public interface OnVideoListener {
        /** Agrega al final de la cola (comportamiento original). */
        void onAddToQueue(Video video);

        /**
         * Inserta el video justo después del que se está reproduciendo,
         * de modo que suene a continuación.
         */
        void onAddNext(Video video);
    }

    public VideoAdapter(OnVideoListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder h, int position) {
        Video video = videos.get(position);

        h.titleText.setText(video.getTitle() != null ? video.getTitle() : "(sin título)");

        String ext  = video.getExtension() != null ? video.getExtension().toUpperCase() : "";
        String size = video.getFileSizeFormatted() != null ? video.getFileSizeFormatted() : "";
        h.categoryText.setText(
                "📁 " + (video.getCategory() != null ? video.getCategory() : "General")
                        + "  •  " + ext + "  •  " + size);

        // Botón "Agregar" → al final de la cola
        h.addButton.setOnClickListener(v -> listener.onAddToQueue(video));

        // Botón "▶ Siguiente" → inserta justo después del que suena
        h.addNextButton.setOnClickListener(v -> listener.onAddNext(video));
    }

    @Override
    public int getItemCount() { return videos.size(); }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, categoryText;
        Button   addButton, addNextButton;

        VideoViewHolder(@NonNull View v) {
            super(v);
            titleText    = v.findViewById(R.id.videoTitle);
            categoryText = v.findViewById(R.id.videoCategory);
            addButton    = v.findViewById(R.id.addButton);
            addNextButton = v.findViewById(R.id.addNextButton);
        }
    }
}
