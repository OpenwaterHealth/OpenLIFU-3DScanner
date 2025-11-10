package health.openwater.openlifu3dscanner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import health.openwater.openlifu3dscanner.Adapters.ImagePreviewAdapter;
import health.openwater.openlifu3dscanner.Modals.ImagePreviewModal;
import health.openwater.openlifu3dscanner.api.repository.CloudRepository;
import health.openwater.openlifu3dscanner.dialogs.DeleteCaptureDialog;
import health.openwater.openlifu3dscanner.dialogs.PhotoscanDownloadDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImagePreview extends BaseActivity {

    @Inject
    CloudRepository cloudRepository;

    private RecyclerView recyclerView;
    private ImagePreviewAdapter adapter;
    private int currentPosition = 0; // 🧠 Track current image position
    private ImageView imageViewPreview;
    private String referenceNumber;
    private long photoscanId;
    private long photocollectionId;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        referenceNumber = getIntent().getStringExtra("REFERENCE_ID");
        photocollectionId = getIntent().getLongExtra("PHOTOCOLLECTION_ID", -1);
        photoscanId = getIntent().getLongExtra("PHOTOSCAN_ID", -1);

        recyclerView = findViewById(R.id.recyclerViewImagePreview);
        imageViewPreview = findViewById(R.id.imageViewInReview);

        Button reviewDoneBtn = findViewById(R.id.reviewDoneButton);
        Button optionsButton = findViewById(R.id.optionsButton);
        ImageButton btnPreviousForReview = findViewById(R.id.btnPreviousForReview);
        ImageButton btnNextForReview = findViewById(R.id.btnNextForReview);
        TextView referenceIdText = findViewById(R.id.referenceIdText);

        //options
        View optionsLayout = findViewById(R.id.optionsLayout);
        Button hideOptionsButton = findViewById(R.id.hideReviewOptionsButton);
        Button deleteScanButton = findViewById(R.id.deleteScanButton);
        Button downloadMeshButton = findViewById(R.id.downloadMeshButton);
        Button reconstructMeshButton = findViewById(R.id.reconstructMeshButton);

        if (!cloudRepository.isLoggedInAndOnline() || photocollectionId == -1) {
            reconstructMeshButton.setEnabled(false);
            downloadMeshButton.setEnabled(false);
        }
        if (photoscanId == -1) downloadMeshButton.setEnabled(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


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
            Intent intent = new Intent(this, ReviewCapturesActivity.class);
            startActivity(intent);
            finish();
        });

        optionsButton.setOnClickListener(v -> optionsLayout.setVisibility(View.VISIBLE));
        hideOptionsButton.setOnClickListener(v -> optionsLayout.setVisibility(View.GONE));
        deleteScanButton.setOnClickListener(v -> {
            DeleteCaptureDialog dialog = new DeleteCaptureDialog(referenceNumber, photocollectionId);
            dialog.show(getSupportFragmentManager(), DeleteCaptureDialog.class.getSimpleName());
        });

        downloadMeshButton.setOnClickListener(v -> {
            PhotoscanDownloadDialog dialog = new PhotoscanDownloadDialog(photoscanId);
            dialog.show(getSupportFragmentManager(), PhotoscanDownloadDialog.class.getSimpleName());
        });
        reconstructMeshButton.setOnClickListener(v -> {
            Long newPhotoscanId = CoroutineHelper.startReconstruction(cloudRepository, photocollectionId);
            if (newPhotoscanId != null) {
                Intent intent = new Intent(getApplicationContext(), ReconstructionActivity.class)
                        .putExtra(ReconstructionActivity.EXTRA_PHOTOSCAN_ID, newPhotoscanId);
                startActivity(intent);
                finish();
            }
        });

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



        try {
            deleteYesBtn.setOnClickListener(v -> {
                File selectedImage = adapter.getSelectedImageFile();
                if (selectedImage==null){
                    Intent intent = new Intent(this, ReviewCaptures.class);
                    startActivity(intent);
                    finish();

                }
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
        } catch (Exception e) {
         Log.d("ErrorDeleting","${e.message}");
        }

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
            if (imageCount==0){
                Intent intent = new Intent(this, ReviewCaptures.class);
                startActivity(intent);
                finish();

            }
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
