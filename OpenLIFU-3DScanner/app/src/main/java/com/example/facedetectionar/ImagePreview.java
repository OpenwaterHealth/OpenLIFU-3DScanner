package com.example.facedetectionar;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.with;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.Transition;
import com.example.facedetectionar.Adapters.ImagePreviewAdapter;
import com.example.facedetectionar.Adapters.OnImageClickListener;
import com.example.facedetectionar.Modals.ImagePreviewModal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImagePreview extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImagePreviewAdapter adapter;
    private int currentPosition = 0; // 🧠 Track current image position
    private ImageView imageViewPreview;
    private String referenceNumber;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        recyclerView = findViewById(R.id.recyclerViewImagePreview);
        imageViewPreview = findViewById(R.id.imageViewInReview);

        Button reviewDoneBtn = findViewById(R.id.reviewDoneButton);
        Button deleteButton = findViewById(R.id.deleteButton);
        ImageButton btnPreviousForReview = findViewById(R.id.btnPreviousForReview);
        ImageButton btnNextForReview = findViewById(R.id.btnNextForReview);
        TextView referenceIdText = findViewById(R.id.referenceIdText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        referenceNumber = getIntent().getStringExtra("REFERENCE_ID");


        referenceIdText.setText(referenceNumber != null ? referenceNumber : "No Ref");


        List<File> imageFiles = getImagesForReference(referenceNumber);
        ArrayList<ImagePreviewModal> list = new ArrayList<>();

        for (File file : imageFiles) {
            list.add(new ImagePreviewModal(file));
        }

        adapter = new ImagePreviewAdapter(list, this, imageFile -> {
            currentPosition = adapter.getSelectedPosition(); // Keep in sync
            Glide.with(ImagePreview.this).load(imageFile).into(imageViewPreview);
        });

        recyclerView.setAdapter(adapter);
        updateCaptureCount();

        // Set initial preview if list is not empty
        if (!list.isEmpty()) {
            currentPosition = 0;
            adapter.setSelectedPosition(currentPosition);
            Glide.with(this).load(list.get(currentPosition).getImageFile()).into(imageViewPreview);
            recyclerView.scrollToPosition(currentPosition);
        }

        reviewDoneBtn.setOnClickListener(v ->{
            Intent intent = new Intent(this, ReviewCaptures.class);
            startActivity(intent);
            finish();
        });

        deleteButton.setOnClickListener(v -> showDeleteDialog());

        btnPreviousForReview.setOnClickListener(v -> {
            if (currentPosition > 0) {
                currentPosition--;
                updatePreviewAndSelection();
            } else {
//                Toast.makeText(this, "First image reached", Toast.LENGTH_SHORT).show();
            }
        });

        btnNextForReview.setOnClickListener(v -> {
            if (currentPosition < adapter.getItemCount() - 1) {
                currentPosition++;
                updatePreviewAndSelection();
            } else {
//                Toast.makeText(this, "Last image reached", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void updatePreviewAndSelection() {
        recyclerView.smoothScrollToPosition(currentPosition);
        adapter.setSelectedPosition(currentPosition);
        File imageFile = adapter.getSelectedImageFile();
        if (imageFile != null) {
            Glide.with(this).load(imageFile).into(imageViewPreview);
        }
    }

    private void showDeleteDialog() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.modal_capture_delete, null);

        Button deleteYesBtn = view.findViewById(R.id.deleteYesBtn);
        Button deleteNoBtn = view.findViewById(R.id.deleteNoBtn);
        TextView deleteWarningText = view.findViewById(R.id.deleteWarningText);

        String text = getString(R.string.deleteText, referenceNumber != null ? referenceNumber.split("_")[0] : "Scan ID");

        deleteWarningText.setText(text);

        deleteNoBtn.setOnClickListener(v -> dialog.dismiss());

        deleteYesBtn.setOnClickListener(v -> {
            File selectedImage = adapter.getSelectedImageFile();
            if (selectedImage != null && selectedImage.exists()) {
                boolean deleted = selectedImage.delete();
                if (deleted) {
                    adapter.removeSelectedImage();
                    updateCaptureCount();
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();

                    // Adjust current position
                    if (currentPosition >= adapter.getItemCount()) {
                        currentPosition = Math.max(adapter.getItemCount() - 1, 0);
                    }

                    updatePreviewAndSelection();
                } else {
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int marginInPx = (int) (20 * metrics.density); // 20dp to pixels
            int dialogWidth = screenWidth - (marginInPx * 2);

            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        }

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void updateCaptureCount() {
        TextView captureCountText = findViewById(R.id.CaptureCountTextOnReview);
        if (adapter != null && captureCountText != null) {
            int imageCount = adapter.getItemCount();

            captureCountText.setText(String.valueOf(imageCount));
        }
    }

    public static List<File> getImagesForReference(String referenceNumber) {
        List<File> imageList = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/" + referenceNumber);








        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles(file ->
                    file.getName().toLowerCase().endsWith(".jpeg") ||
                            file.getName().toLowerCase().endsWith(".jpg") ||
                            file.getName().toLowerCase().endsWith(".png")
            );

            if (files != null) {
                Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
                imageList.addAll(Arrays.asList(files));
            }
        }

        return imageList;
    }
}
