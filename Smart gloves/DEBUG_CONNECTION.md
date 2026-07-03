# Hướng dẫn Debug Kết nối Server

## Server đã chạy (đã xác nhận)
- Server đang chạy trên port 3000 (process 26884)
- Server đang listen trên `0.0.0.0:3000`

## Các bước Debug

### Bước 1: Kiểm tra server hoạt động từ trình duyệt

Mở trình duyệt trên máy tính và truy cập:
- `http://localhost:3000` → Phải hiển thị "TTS Server is running"
- `http://127.0.0.1:3000` → Phải hiển thị "TTS Server is running"

**Nếu không mở được:** Server có vấn đề, cần kiểm tra lại.

### Bước 2: Test API từ trình duyệt/Postman

Test API POST `/tts`:

**Cách 1: Dùng cURL (trong terminal/command prompt)**
```bash
curl -X POST http://localhost:3000/tts \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"test\"}"
```

**Cách 2: Dùng Postman hoặc trình duyệt với extension REST Client**

**Kết quả mong đợi:**
```json
{
  "message": "TTS file created",
  "file": "tts_1234567890.wav",
  "url": "http://10.0.2.2:3000/audio/tts_1234567890.wav"
}
```

### Bước 3: Test kết nối từ Android Emulator

**Cách 1: Mở Browser trong Emulator**
1. Mở app Browser trong Android Emulator
2. Truy cập: `http://10.0.2.2:3000`
3. **Phải hiển thị:** "TTS Server is running"

**Nếu không mở được:**
- Emulator không thể truy cập host machine
- Có thể cần restart emulator
- Hoặc cần kiểm tra cấu hình network của emulator

**Cách 2: Kiểm tra Logcat trong Android Studio**

Khi click nút TTS trong app, xem Logcat với filter `TTSService`:
```
adb logcat -s TTSService
```

Hoặc trong Android Studio:
1. Mở tab Logcat
2. Filter: `TTSService`
3. Click nút TTS trong app
4. Xem logs để tìm lỗi cụ thể

### Bước 4: Kiểm tra Firewall

Windows Firewall có thể chặn port 3000:

1. Mở Windows Defender Firewall
2. Advanced settings
3. Inbound Rules → New Rule
4. Port → TCP → 3000
5. Allow connection
6. Apply cho tất cả profiles

Hoặc tạm thời tắt firewall để test:
- **Chỉ để test, không khuyến nghị cho production**

### Bước 5: Kiểm tra Emulator Network

1. Trong Android Studio → AVD Manager
2. Edit emulator
3. Advanced Settings
4. Đảm bảo Network Speed: Full
5. Network Latency: None

### Bước 6: Restart Emulator

Đôi khi emulator cần restart để kết nối network đúng:
1. Close emulator
2. Restart Android Studio
3. Start emulator mới
4. Test lại

## Logs cần kiểm tra

### Trong Android Studio Logcat (filter: TTSService):

**Khi thành công:**
```
D/TTSService: Testing connection to server...
D/TTSService: Connection test result: 200
D/TTSService: Opening connection to: http://10.0.2.2:3000/tts
D/TTSService: Sending TTS request to: http://10.0.2.2:3000/tts
D/TTSService: Request body: {"text":"test"}
D/TTSService: Request sent, waiting for response...
D/TTSService: Response code: 200
D/TTSService: Response message: OK
D/TTSService: Response: {"message":"...","file":"...","url":"..."}
```

**Khi lỗi kết nối:**
```
E/TTSService: Connection test failed
java.net.ConnectException: failed to connect to /10.0.2.2 (port 3000)
```

**Khi lỗi timeout:**
```
E/TTSService: Timeout error
java.net.SocketTimeoutException: failed to connect to /10.0.2.2 (port 3000) after 15000ms
```

## Giải pháp thay thế

### Nếu vẫn không kết nối được từ emulator:

**Option 1: Test trên thiết bị thật**
1. Tìm IP máy tính: `ipconfig` (Windows) → IPv4 Address
2. Đảm bảo máy tính và điện thoại cùng mạng WiFi
3. Đổi `BASE_URL` trong `TTSService.java`:
   ```java
   private static final String BASE_URL = "http://192.168.1.100:3000"; // IP máy tính
   ```
4. Server cần listen trên `0.0.0.0` (đã có)
5. URL response trong server cần dùng IP máy tính:
   ```javascript
   const HOST_FOR_CLIENT = '192.168.1.100'; // IP máy tính
   ```

**Option 2: Dùng ngrok để tạo public URL**
```bash
ngrok http 3000
```
Sẽ tạo URL public như `https://abc123.ngrok.io`
- Đổi `BASE_URL` trong Android app
- Đổi URL trong server response
- **Nhưng cần HTTPS, phức tạp hơn**

## Kiểm tra nhanh

Chạy các lệnh sau để kiểm tra:

1. **Kiểm tra server có chạy:**
   ```bash
   netstat -ano | findstr :3000
   ```

2. **Test từ trình duyệt:**
   - Mở: `http://localhost:3000`

3. **Test từ emulator browser:**
   - Mở Browser trong emulator
   - Truy cập: `http://10.0.2.2:3000`

4. **Test API:**
   ```bash
   curl -X POST http://localhost:3000/tts -H "Content-Type: application/json" -d "{\"text\":\"test\"}"
   ```

## Thông tin cần cung cấp nếu vẫn lỗi

1. Log từ Android Studio Logcat (filter: TTSService)
2. Log từ server console (khi gọi API)
3. Kết quả test từ trình duyệt (`http://localhost:3000`)
4. Kết quả test từ emulator browser (`http://10.0.2.2:3000`)
5. Loại emulator đang dùng (AVD, Genymotion, etc.)
6. Android version của emulator

