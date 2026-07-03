# QUICK FIX - Giải pháp nhanh cho lỗi kết nối

## Lỗi hiện tại
```
java.net.ConnectException: Failed to connect to /10.0.2.2:3000
```

## Giải pháp nhanh (3 bước)

### Bước 1: Setup ADB Port Forwarding

**Cách 1: Chạy file batch (nếu ADB có trong PATH)**
```bash
setup_port_forwarding.bat
```

**Cách 2: Chạy từ Android Studio Terminal**
1. Mở Android Studio
2. Mở tab **Terminal** (ở dưới cùng)
3. Chạy lệnh:
   ```bash
   adb reverse tcp:3000 tcp:3000
   ```

**Cách 3: Chạy từ Command Prompt/PowerShell (nếu có ADB trong PATH)**
```bash
adb reverse tcp:3000 tcp:3000
```

**Kết quả mong đợi:**
```
3000
```
(Không có lỗi, chỉ hiển thị số port)

### Bước 2: Code đã được cập nhật

✅ Code Android đã được cập nhật để dùng `http://localhost:3000` thay vì `http://10.0.2.2:3000`

✅ Server của bạn vẫn cần listen trên `0.0.0.0:3000` (đã có trong code)

### Bước 3: Restart App

1. Rebuild app trong Android Studio
2. Hoặc Stop và Run lại app
3. Test nút TTS

## Kiểm tra nhanh

### Test 1: Kiểm tra port forwarding
```bash
adb reverse --list
```
Phải thấy: `tcp:3000 tcp:3000`

### Test 2: Test từ emulator browser
1. Mở **Browser** trong emulator
2. Truy cập: `http://localhost:3000`
3. Phải hiển thị: "TTS Server is running"

**Nếu không hiển thị:**
- Port forwarding chưa được setup
- Server chưa chạy
- Server không listen trên 0.0.0.0

### Test 3: Kiểm tra server có đang chạy
```bash
netstat -ano | findstr :3000
```
Phải thấy: `TCP    0.0.0.0:3000` hoặc `TCP    [::]:3000`

### Test 4: Kiểm tra server có listen đúng không

**Trong server console, phải thấy:**
```
Server running at http://0.0.0.0:3000
Access from Android emulator: http://10.0.2.2:3000
Access from local browser: http://localhost:3000
Response URLs will use: http://10.0.2.2:3000
```

**Nếu chỉ thấy:**
```
Server running at http://localhost:3000
```
→ Server chỉ listen trên localhost, cần sửa thành `0.0.0.0`

## Nếu vẫn lỗi

### Option 1: Restart emulator và setup lại
1. Close emulator
2. Start emulator mới
3. Setup port forwarding lại: `adb reverse tcp:3000 tcp:3000`
4. Test lại

### Option 2: Kiểm tra server code

Đảm bảo server code có:
```javascript
app.listen(PORT, '0.0.0.0', () => {  // ← QUAN TRỌNG: '0.0.0.0'
  console.log(`Server running at http://0.0.0.0:${PORT}`);
});
```

**KHÔNG được:**
```javascript
app.listen(PORT);  // ← Mặc định là localhost
app.listen(PORT, 'localhost');  // ← Chỉ localhost
```

### Option 3: Test trên thiết bị thật

Nếu emulator vẫn không kết nối được:

1. Tìm IP máy tính:
   ```bash
   ipconfig
   ```
   Tìm IPv4 Address (ví dụ: 192.168.1.100)

2. Đổi `BASE_URL` trong `TTSService.java`:
   ```java
   private static final String BASE_URL = "http://192.168.1.100:3000";
   ```

3. Đổi `HOST_FOR_CLIENT` trong server:
   ```javascript
   const HOST_FOR_CLIENT = '192.168.1.100';
   ```

4. Đảm bảo máy tính và điện thoại cùng WiFi

5. Server vẫn listen trên `0.0.0.0:3000`

## Tóm tắt

✅ **Đã làm:**
- Code Android đã đổi sang `http://localhost:3000`
- Cần setup: `adb reverse tcp:3000 tcp:3000`
- Server cần listen trên `0.0.0.0:3000`

✅ **Cần làm:**
1. Chạy: `adb reverse tcp:3000 tcp:3000`
2. Rebuild app
3. Test lại

✅ **Kiểm tra:**
- Browser trong emulator: `http://localhost:3000` → "TTS Server is running"
- Logcat khi click TTS → Không còn `ConnectException`

