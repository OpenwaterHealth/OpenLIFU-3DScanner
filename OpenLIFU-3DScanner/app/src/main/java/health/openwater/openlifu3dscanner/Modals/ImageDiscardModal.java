package health.openwater.openlifu3dscanner.Modals;
import java.io.File;

public class ImageDiscardModal {
    File imageFile;

    public ImageDiscardModal(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }
}
