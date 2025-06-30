package com.example.facedetectionar.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.facedetectionar.Modals.ImagePreviewModal;
import com.example.facedetectionar.R;

import java.io.File;
import java.util.ArrayList;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {

    private ArrayList<ImagePreviewModal> list;
    private Context context;
    private OnImageClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public ImagePreviewAdapter(ArrayList<ImagePreviewModal> list, Context context,OnImageClickListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File imageFile = list.get(position).getImageFile();



        Glide.with(context)
                .load(imageFile)
                .into(holder.imageView);

        // selected image border logic
        ViewGroup.LayoutParams params = holder.imageView.getLayoutParams();
        if (position == selectedPosition) {
                   holder.imageView.setBackgroundResource(R.drawable.image_white_border);

        } else {
            holder.imageView.setBackgroundResource(0);
        }


        holder.imageView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            int oldPosition = selectedPosition;
            selectedPosition = currentPosition;

            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onImageClick(list.get(currentPosition).getImageFile());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Get selected file from list
    public File getSelectedImageFile() {
        Log.d("getSelectedImageFile", "Selected position: " + selectedPosition);
        if (selectedPosition != RecyclerView.NO_POSITION) {
            Log.d("getSelectedImageFile", "Selected position: " + selectedPosition);
            Log.d("getSelectedImageFile", "Selected position: " + list.get(selectedPosition).getImageFile());
            return list.get(selectedPosition).getImageFile();
        }
        return null;
    }

    // Remove selected item from list
    public void removeSelectedImage() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            list.remove(selectedPosition);
            notifyItemRemoved(selectedPosition);
            selectedPosition = RecyclerView.NO_POSITION;
        }
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imagePreview);
        }
    }
}
