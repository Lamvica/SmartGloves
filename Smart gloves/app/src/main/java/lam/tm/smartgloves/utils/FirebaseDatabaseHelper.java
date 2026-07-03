package lam.tm.smartgloves.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Helper class để quản lý kết nối Firebase Realtime Database
 * Dữ liệu được lưu theo user ID để mỗi tài khoản có dữ liệu riêng
 */
public class FirebaseDatabaseHelper {
    
    private static FirebaseDatabase database;
    private static final String DATABASE_URL = "https://smart-glove-1769e-default-rtdb.asia-southeast1.firebasedatabase.app";
    
    /**
     * Lấy instance của FirebaseDatabase
     * Sử dụng URL từ google-services.json hoặc URL tùy chỉnh
     */
    public static FirebaseDatabase getDatabase() {
        if (database == null) {
            // Nếu có google-services.json, Firebase sẽ tự động sử dụng URL từ đó
            // Nhưng nếu cần chỉ định URL cụ thể, dùng dòng dưới:
            // database = FirebaseDatabase.getInstance(DATABASE_URL);
            database = FirebaseDatabase.getInstance();
            
            // Bật persistence nếu cần (cho offline support)
            // database.setPersistenceEnabled(true);
        }
        return database;
    }
    
    /**
     * Lấy User ID hiện tại
     * @return User ID nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    public static String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Lấy reference đến node "users/{userId}/gestures"
     * Nếu chưa đăng nhập, trả về reference chung (backward compatibility)
     */
    public static DatabaseReference getGesturesReference() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child("gestures");
        }
        // Fallback to old path for backward compatibility
        return getDatabase().getReference("gestures");
    }
    
    /**
     * Lấy reference đến node "users/{userId}/categories"
     * Nếu chưa đăng nhập, trả về reference chung (backward compatibility)
     */
    public static DatabaseReference getCategoriesReference() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child("categories");
        }
        // Fallback to old path for backward compatibility
        return getDatabase().getReference("categories");
    }
    
    /**
     * Lấy reference đến node "users/{userId}/history"
     * Nếu chưa đăng nhập, trả về reference chung (backward compatibility)
     */
    public static DatabaseReference getHistoryReference() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child("history");
        }
        // Fallback to old path for backward compatibility
        return getDatabase().getReference("history");
    }
    
    /**
     * Lấy reference đến node "users/{userId}/speechHistory" (lịch sử giọng nói thành chữ)
     * Nếu chưa đăng nhập, trả về reference chung (backward compatibility)
     */
    public static DatabaseReference getSpeechHistoryReference() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child("speechHistory");
        }
        // Fallback to old path for backward compatibility
        return getDatabase().getReference("speechHistory");
    }
    
    /**
     * Lấy reference đến node tùy chỉnh
     */
    public static DatabaseReference getReference(String path) {
        return getDatabase().getReference(path);
    }
    
    /**
     * Lấy reference đến node tùy chỉnh theo user
     * @param path Đường dẫn tương đối (không bao gồm users/{userId}/)
     * @return Reference đến users/{userId}/{path} nếu đã đăng nhập, hoặc {path} nếu chưa đăng nhập
     */
    public static DatabaseReference getUserReference(String path) {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child(path);
        }
        return getDatabase().getReference(path);
    }
    
    /**
     * Lấy reference đến node "users/{userId}/wavFiles" (danh sách file WAV đã tạo)
     * Nếu chưa đăng nhập, trả về reference chung (backward compatibility)
     */
    public static DatabaseReference getWavFilesReference() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getDatabase().getReference("users").child(userId).child("wavFiles");
        }
        // Fallback to old path for backward compatibility
        return getDatabase().getReference("wavFiles");
    }
}




