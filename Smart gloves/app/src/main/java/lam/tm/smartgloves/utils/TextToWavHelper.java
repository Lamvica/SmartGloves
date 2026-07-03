package lam.tm.smartgloves.utils;

import android.content.Context;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

/**
 * Helper class để chuyển đổi TextToSpeech thành file WAV
 */
public class TextToWavHelper {
    private static final String TAG = "TextToWavHelper";
    private static final String WAV_DIR = "SmartGlovesWav";
    
    /**
     * Chuyển đổi text thành file WAV
     * @param context Context của ứng dụng
     * @param text Text cần chuyển đổi
     * @param gestureName Tên cử chỉ (để đặt tên file)
     * @param callback Callback để nhận kết quả
     */
    public static void convertToWav(Context context, String text, String gestureName, 
                                    TextToWavCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Text không được rỗng");
            }
            return;
        }
        
        // Tạo TextToSpeech instance
        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        ttsRef[0] = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set language to Vietnamese
                int result = ttsRef[0].setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default
                    ttsRef[0].setLanguage(Locale.getDefault());
                }
                
                // Generate unique filename
                String fileName = generateFileName(gestureName);
                File wavFile = createWavFile(context, fileName);
                
                if (wavFile == null) {
                    if (callback != null) {
                        callback.onError("Không thể tạo file WAV");
                    }
                    ttsRef[0].shutdown();
                    return;
                }
                
                // Synthesize to file
                int synthesizeResult = ttsRef[0].synthesizeToFile(
                    text,
                    null,
                    wavFile,
                    UUID.randomUUID().toString()
                );
                
                if (synthesizeResult == TextToSpeech.SUCCESS) {
                    // Wait longer for file to be written (TextToSpeech needs more time)
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        // Check multiple times with increasing delay
                        checkFileCreated(wavFile, fileName, callback, ttsRef[0], 0);
                    }, 1000);
                } else {
                    if (callback != null) {
                        callback.onError("Lỗi khi tạo file WAV: " + synthesizeResult);
                    }
                    ttsRef[0].shutdown();
                }
            } else {
                if (callback != null) {
                    callback.onError("TextToSpeech không khả dụng");
                }
            }
        });
    }
    
    /**
     * Tạo tên file từ gesture name
     */
    private static String generateFileName(String gestureName) {
        if (gestureName == null || gestureName.trim().isEmpty()) {
            gestureName = "gesture";
        }
        
        // Sanitize filename
        String sanitized = gestureName
            .replaceAll("[^a-zA-Z0-9\\s]", "")
            .replaceAll("\\s+", "_")
            .toLowerCase();
        
        // Add timestamp
        long timestamp = System.currentTimeMillis();
        return sanitized + "_" + timestamp + ".wav";
    }
    
    /**
     * Tạo file WAV trong thư mục app-specific
     */
    private static File createWavFile(Context context, String fileName) {
        try {
            // Get app-specific external directory
            File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (externalDir == null) {
                // Fallback to internal storage
                externalDir = new File(context.getFilesDir(), WAV_DIR);
            } else {
                externalDir = new File(externalDir, WAV_DIR);
            }
            
            // Create directory if not exists
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            
            // Create file
            File wavFile = new File(externalDir, fileName);
            if (wavFile.exists()) {
                wavFile.delete();
            }
            
            return wavFile;
        } catch (Exception e) {
            Log.e(TAG, "Error creating WAV file", e);
            return null;
        }
    }
    
    /**
     * Lấy đường dẫn thư mục chứa WAV files
     */
    public static String getWavDirectoryPath(Context context) {
        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (externalDir == null) {
            externalDir = new File(context.getFilesDir(), WAV_DIR);
        } else {
            externalDir = new File(externalDir, WAV_DIR);
        }
        return externalDir.getAbsolutePath();
    }
    
    /**
     * Xóa file WAV
     */
    public static boolean deleteWavFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        File file = new File(filePath);
        return file.exists() && file.delete();
    }
    
    /**
     * Kiểm tra file đã được tạo thành công chưa
     */
    private static void checkFileCreated(File wavFile, String fileName,
                                        TextToWavCallback callback, TextToSpeech tts, int retryCount) {
        if (retryCount > 5) {
            // Đã thử quá nhiều lần
            if (callback != null) {
                callback.onError("File WAV không được tạo thành công sau nhiều lần thử");
            }
            tts.shutdown();
            return;
        }

        if (wavFile.exists() && wavFile.length() > 0) {
            // File đã được tạo thành công
            if (callback != null) {
                callback.onSuccess(wavFile.getAbsolutePath(), fileName);
            }
            tts.shutdown();
        } else {
            // Chờ thêm một chút và thử lại
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                checkFileCreated(wavFile, fileName, callback, tts, retryCount + 1);
            }, 500);
        }
    }

    /**
     * Callback interface
     */
    public interface TextToWavCallback {
        void onSuccess(String filePath, String fileName);
        void onError(String error);
    }
}

