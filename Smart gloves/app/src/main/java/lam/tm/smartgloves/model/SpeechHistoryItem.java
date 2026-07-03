package lam.tm.smartgloves.model;

/**
 * Model để lưu lịch sử chuyển đổi giọng nói thành chữ
 */
public class SpeechHistoryItem {
    public String id;
    public String text;  // Nội dung văn bản được nhận diện
    public long timestamp;  // Thời gian nhận diện

    public SpeechHistoryItem() {
        // Default constructor required for Firebase
    }

    public SpeechHistoryItem(String text, long timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }
}

