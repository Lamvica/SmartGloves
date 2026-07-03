package lam.tm.smartgloves.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để gọi API TTS từ Node.js server và download file audio (MP3)
 */
public class TTSService {
    private static final String TAG = "TTSService";
    // QUAN TRỌNG: 
    // - Trên Android Emulator: sử dụng "http://10.0.2.2:3000" (10.0.2.2 là địa chỉ đặc biệt của emulator để truy cập host machine)
    // - Trên thiết bị thật: sử dụng IP máy tính chạy server, ví dụ "http://192.168.1.100:3000"
    // - Nếu dùng ADB port forwarding: có thể dùng "http://localhost:3000" (chạy: adb reverse tcp:3000 tcp:3000)
    private static final String AUDIO_DIR = "SmartGlovesAudio";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static String BASE_URL = null; // Lazy initialization
    
    /**
     * Lấy BASE_URL của server TTS
     * Cấu hình IP server tại đây
     */
    private static String getServerBaseUrl() {
        if (BASE_URL == null) {
            // IP máy tính chạy server TTS
            // - Nếu dùng Android Emulator: sử dụng "10.0.2.2:3000"
            // - Nếu dùng thiết bị thật: sử dụng IP máy tính, ví dụ "10.32.65.25:3000"
            BASE_URL = "http://192.168.0.103:3000";
            
            // Phát hiện emulator (optional - để log thông tin)
            if (isEmulator()) {
                Log.d(TAG, "Detected Android Emulator - using " + BASE_URL);
            } else {
                Log.d(TAG, "Detected physical device - using " + BASE_URL);
            }
        }
        return BASE_URL;
    }
    
    /**
     * Phát hiện xem có đang chạy trên emulator không
     */
    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
    
    /**
     * Gọi API TTS và download file MP3
     * @param context Context của ứng dụng
     * @param text Text cần chuyển đổi thành audio
     * @param fingerName Tên ngón tay (ngoncai, ngontro, ngongiua, ngonaput, ngonut)
     * @param cValue Giá trị c (c1 hoặc c2)
     * @param callback Callback để nhận kết quả
     */
    public static void convertTextToAudio(Context context, String text, String fingerName, String cValue,
                                          TTSCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Text không được rỗng");
            }
            return;
        }
        
        // Execute on background thread
        executor.execute(() -> {
            TTSResult result = performTTSTask(context, text, fingerName, cValue);
            
            // Post result to main thread
            mainHandler.post(() -> {
                if (callback == null) return;
                
                if (result.error != null) {
                    callback.onError(result.error);
                } else {
                    callback.onSuccess(result.filePath, result.fileName);
                }
            });
        });
    }
    
    /**
     * Test kết nối đến server trước khi gọi API chính
     */
    private static boolean testConnection() {
        try {
            URL testUrl = new URL(getServerBaseUrl());
            HttpURLConnection testConnection = (HttpURLConnection) testUrl.openConnection();
            testConnection.setRequestMethod("GET");
            testConnection.setConnectTimeout(5000); // 5 seconds for test
            testConnection.setReadTimeout(5000);
            testConnection.connect();
            
            int responseCode = testConnection.getResponseCode();
            testConnection.disconnect();
            
            Log.d(TAG, "Connection test result: " + responseCode);
            return responseCode == HttpURLConnection.HTTP_OK || responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return false;
        }
    }
    
    /**
     * Thực hiện task TTS trên background thread
     */
    private static TTSResult performTTSTask(Context context, String text, String fingerName, String cValue) {
        try {
            // Test kết nối trước
            Log.d(TAG, "Testing connection to server...");
            if (!testConnection()) {
                Log.w(TAG, "Connection test failed, but continuing anyway...");
            }
            
            // Step 1: Gọi API POST /tts
            String baseUrl = getServerBaseUrl();
            URL url = new URL(baseUrl + "/tts");
            Log.d(TAG, "=== Starting TTS Request ===");
            Log.d(TAG, "Opening connection to: " + url.toString());
            Log.d(TAG, "Base URL: " + baseUrl);
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            try {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Connection", "close"); // Đóng connection sau khi xong
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(30000); // Tăng timeout lên 30 seconds
                connection.setReadTimeout(30000); // 30 seconds
                connection.setInstanceFollowRedirects(true);
                
                Log.d(TAG, "Connection configured, connecting...");
                
                // Tạo JSON body với thông tin đầy đủ
                // Server expect: text, finger, gesture
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("text", text);
                jsonBody.put("finger", fingerName != null ? fingerName : "ngoncai");
                jsonBody.put("gesture", cValue != null ? cValue : "c1");
                String body = jsonBody.toString();
                
                Log.d(TAG, "Sending TTS request to: " + baseUrl + "/tts");
                Log.d(TAG, "Request body: " + body);
                
                // Gửi request
                Log.d(TAG, "Writing request body...");
                OutputStream os = connection.getOutputStream();
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
                os.close();
                
                Log.d(TAG, "Request sent (" + input.length + " bytes), waiting for response...");
                
                // Đọc response
                Log.d(TAG, "Reading response code...");
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                Log.d(TAG, "Response message: " + connection.getResponseMessage());
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream errorStream = connection.getErrorStream();
                    String errorMessage = readStream(errorStream);
                    Log.e(TAG, "Server error: " + responseCode + " - " + errorMessage);
                    return new TTSResult(null, null, null, "Server error: " + responseCode + " - " + errorMessage);
                }
                
                InputStream inputStream = connection.getInputStream();
                String response = readStream(inputStream);
                inputStream.close();
                
                Log.d(TAG, "Response: " + response);
                
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response);
                String fileName = jsonResponse.optString("file", null);
                String audioUrl = jsonResponse.optString("url", null);
                
                if (fileName == null || audioUrl == null) {
                    Log.e(TAG, "Invalid response: missing 'file' or 'url'");
                    return new TTSResult(null, null, null, "Invalid server response: missing 'file' or 'url'");
                }
                
                // QUAN TRỌNG: Chuyển đổi localhost/127.0.0.1 thành IP thực tế từ BASE_URL
                // Server có thể trả về URL với localhost, cần chuyển thành IP mà client có thể truy cập
                String baseUrlForDownload = getServerBaseUrl();
                try {
                    // Lấy IP/host từ BASE_URL (ví dụ: http://10.32.65.25:3000 -> 10.32.65.25)
                    URL baseUrlObj = new URL(baseUrlForDownload);
                    String serverHost = baseUrlObj.getHost();
                    
                    Log.d(TAG, "Original audio URL from server: " + audioUrl);
                    
                    // Chuyển đổi localhost thành IP thực tế
                if (audioUrl != null && audioUrl.contains("localhost")) {
                        audioUrl = audioUrl.replace("localhost", serverHost);
                        Log.d(TAG, "Converted localhost to " + serverHost + ": " + audioUrl);
                }
                    // Chuyển đổi 127.0.0.1 thành IP thực tế
                if (audioUrl != null && audioUrl.contains("127.0.0.1")) {
                        audioUrl = audioUrl.replace("127.0.0.1", serverHost);
                        Log.d(TAG, "Converted 127.0.0.1 to " + serverHost + ": " + audioUrl);
                    }
                    // Nếu server trả về 10.0.2.2 nhưng đang dùng thiết bị thật, cần chuyển đổi
                    if (audioUrl != null && audioUrl.contains("10.0.2.2") && !serverHost.equals("10.0.2.2")) {
                        audioUrl = audioUrl.replace("10.0.2.2", serverHost);
                        Log.d(TAG, "Converted 10.0.2.2 to " + serverHost + ": " + audioUrl);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing base URL, using original audioUrl", e);
                }
                
                Log.d(TAG, "File name: " + fileName);
                Log.d(TAG, "Audio URL (final): " + audioUrl);
                
                // Step 2: Download file audio (MP3) từ URL
                // Sử dụng tên file từ server trực tiếp (đã có format đúng: fingerName_cValue.mp3)
                String savedFilePath = downloadAudioFile(context, audioUrl, fileName);
                
                if (savedFilePath != null) {
                    return new TTSResult(savedFilePath, fileName, audioUrl, null);
                } else {
                    return new TTSResult(null, null, null, "Không thể download file audio");
                }
                
            } finally {
                connection.disconnect();
            }
            
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Unknown host error", e);
            String errorMsg = "Không thể tìm thấy server tại " + getServerBaseUrl() + 
                ". Vui lòng kiểm tra:\n" +
                "1. Server có đang chạy không?\n" +
                "2. Đang test trên emulator hay thiết bị thật?\n" +
                "3. URL có đúng không? (Emulator: 10.0.2.2:3000, Thiết bị thật: IP máy tính:3000)";
            return new TTSResult(null, null, null, errorMsg);
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "Connection error", e);
            String errorMsg = "Không thể kết nối đến server tại " + getServerBaseUrl() + 
                ". Vui lòng kiểm tra:\n" +
                "1. Server Node.js có đang chạy ở port 3000 không?\n" +
                "2. Server có lắng nghe trên 0.0.0.0:3000 (không phải localhost:3000) không?\n" +
                "3. Firewall có đang chặn port 3000 không?\n" +
                "4. Đang test trên emulator hay thiết bị thật? (10.0.2.2 chỉ hoạt động trong emulator)";
            return new TTSResult(null, null, null, errorMsg);
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Timeout error", e);
            String errorMsg = "Kết nối quá thời gian đến server tại " + getServerBaseUrl() + 
                ". Vui lòng kiểm tra:\n" +
                "1. Server có đang chạy không?\n" +
                "2. IP server có đúng không? (Hiện tại: " + getServerBaseUrl() + ")\n" +
                "3. Máy tính và điện thoại có cùng mạng WiFi không?\n" +
                "4. Firewall có chặn port 3000 không?\n" +
                "5. Thử ping IP server từ điện thoại";
            return new TTSResult(null, null, null, errorMsg);
        } catch (java.io.IOException e) {
            Log.e(TAG, "IO error", e);
            return new TTSResult(null, null, null, "Lỗi kết nối mạng: " + e.getMessage());
        } catch (org.json.JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
            return new TTSResult(null, null, null, "Lỗi định dạng response từ server: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error calling TTS API", e);
            e.printStackTrace();
            String errorMsg = "Lỗi không xác định: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += "\nNguyên nhân: " + e.getCause().getMessage();
            }
            return new TTSResult(null, null, null, errorMsg);
        }
    }
    
    /**
     * Đọc InputStream thành String
     */
    private static String readStream(InputStream inputStream) {
        try {
            if (inputStream == null) return "";
            java.io.BufferedReader reader = 
                new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading stream", e);
            return "";
        }
    }
    
    /**
     * Download file audio (MP3) từ URL và lưu vào thư mục app
     */
    private static String downloadAudioFile(Context context, String audioUrl, String serverFileName) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        
        try {
            // Tạo thư mục nếu chưa có
            File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (externalDir == null) {
                externalDir = new File(context.getFilesDir(), AUDIO_DIR);
            } else {
                externalDir = new File(externalDir, AUDIO_DIR);
            }
            
            if (!externalDir.exists()) {
                boolean created = externalDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create directory: " + externalDir.getAbsolutePath());
                    return null;
                }
            }
            
            // Sử dụng tên file từ server trực tiếp (đã có format đúng: fingerName_cValue.mp3)
            String localFileName = serverFileName != null ? serverFileName : "audio.mp3";
            File outputFile = new File(externalDir, localFileName);
            
            // Xóa file cũ nếu tồn tại
            if (outputFile.exists()) {
                boolean deleted = outputFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete existing file: " + outputFile.getAbsolutePath());
                }
            }
            
            Log.d(TAG, "Downloading file from: " + audioUrl);
            Log.d(TAG, "Saving to: " + outputFile.getAbsolutePath());
            
            // Download file
            URL url = new URL(audioUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Download response code: " + responseCode);
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + responseCode + " " + connection.getResponseMessage());
                return null;
            }
            
            input = connection.getInputStream();
            output = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            output.flush();
            output.close();
            input.close();
            
            Log.d(TAG, "File downloaded successfully: " + outputFile.getAbsolutePath() + " (" + totalBytes + " bytes)");
            
            // Verify file exists and has content
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile.getAbsolutePath();
            } else {
                Log.e(TAG, "Downloaded file is empty or doesn't exist");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading audio file", e);
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing streams", e);
            }
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Tạo tên file local từ gesture name và server file name
     */
    private static String generateLocalFileName(String gestureName, String serverFileName) {
        String baseName;
        if (gestureName != null && !gestureName.trim().isEmpty()) {
            // Sanitize gesture name
            baseName = gestureName
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", "_")
                .toLowerCase();
        } else {
            baseName = "gesture";
        }
        
        // Giữ extension từ server file name nếu có
        String extension = ".mp3";
        if (serverFileName != null && serverFileName.contains(".")) {
            extension = serverFileName.substring(serverFileName.lastIndexOf("."));
            if (!extension.equalsIgnoreCase(".mp3")) {
                extension = ".mp3"; // Force .mp3 extension
            }
        }
        
        long timestamp = System.currentTimeMillis();
        return baseName + "_" + timestamp + extension;
    }
    
    /**
     * Class để lưu kết quả TTS
     */
    private static class TTSResult {
        String filePath;
        String fileName;
        String url;
        String error;
        
        TTSResult(String filePath, String fileName, String url, String error) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.url = url;
            this.error = error;
        }
    }
    
    /**
     * Callback interface
     */
    public interface TTSCallback {
        void onSuccess(String filePath, String fileName);
        void onError(String error);
    }
    
        /**
         * Lấy đường dẫn thư mục chứa audio files
         */
        public static String getAudioDirectoryPath(Context context) {
            File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (externalDir == null) {
                externalDir = new File(context.getFilesDir(), AUDIO_DIR);
            } else {
                externalDir = new File(externalDir, AUDIO_DIR);
            }
            return externalDir.getAbsolutePath();
        }
}

