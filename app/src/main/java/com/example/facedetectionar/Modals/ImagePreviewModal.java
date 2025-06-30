package com.example.facedetectionar.Modals;

import java.io.File;

public class ImagePreviewModal {
    File imageFile;

    public ImagePreviewModal(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }
}
