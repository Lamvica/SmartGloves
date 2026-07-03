package lam.tm.smartgloves.model;

/**
 * Model để lưu thông tin file WAV đã tạo
 */
public class WavFileItem {
    public String id;
    public String gestureName;      // Tên cử chỉ
    public String text;             // Câu nói
    public String fileName;         // Tên file
    public String filePath;         // Đường dẫn file local
    public String storageUrl;        // URL trên Firebase Storage (nếu upload)
    public long timestamp;          // Thời gian tạo
    public long fileSize;            // Kích thước file (bytes)

    public WavFileItem() {
        // Default constructor required for Firebase
    }

    public WavFileItem(String gestureName, String text, String fileName, 
                       String filePath, long timestamp, long fileSize) {
        this.gestureName = gestureName;
        this.text = text;
        this.fileName = fileName;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.fileSize = fileSize;
    }

    public WavFileItem(String gestureName, String text, String fileName, 
                       String filePath, String storageUrl, long timestamp, long fileSize) {
        this.gestureName = gestureName;
        this.text = text;
        this.fileName = fileName;
        this.filePath = filePath;
        this.storageUrl = storageUrl;
        this.timestamp = timestamp;
        this.fileSize = fileSize;
    }
}

