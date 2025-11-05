package health.openwater.openlifu3dscanner.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import health.openwater.openlifu3dscanner.Modals.ImageDiscardModal;
import health.openwater.openlifu3dscanner.R;

import java.io.File;
import java.util.ArrayList;

public class ImageDiscardAdapter extends RecyclerView.Adapter<ImageDiscardAdapter.ViewHolder> {

    private ArrayList<ImageDiscardModal> list;
    private Context context;
    private OnImageClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public ImageDiscardAdapter(ArrayList<ImageDiscardModal> list, Context context, OnImageClickListener listener) {
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

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }



    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imagePreview);
        }
    }
}
