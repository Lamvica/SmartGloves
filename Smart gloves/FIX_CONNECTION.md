# Giải pháp kết nối Server từ Android Emulator

## Vấn đề
`ConnectException: Failed to connect to /10.0.2.2:3000`

Emulator không thể kết nối đến host machine qua `10.0.2.2:3000`.

## Giải pháp: Sử dụng ADB Port Forwarding

### Cách 1: ADB Port Forwarding (KHUYẾN NGHỊ - Đã cập nhật trong code)

**Bước 1: Setup port forwarding**

Chạy file `setup_port_forwarding.bat` hoặc chạy lệnh:
```bash
adb reverse tcp:3000 tcp:3000
```

Lệnh này sẽ forward port 3000 từ emulator đến port 3000 trên host machine.

**Bước 2: Code đã được cập nhật**

Code Android đã được cập nhật để dùng `http://localhost:3000` thay vì `http://10.0.2.2:3000`.

**Bước 3: Server vẫn listen trên 0.0.0.0:3000**

Server Node.js của bạn vẫn cần listen trên `0.0.0.0:3000` (đã có trong code).

**Bước 4: Test**

1. Chạy lệnh port forwarding
2. Chạy server: `node server.js`
3. Chạy app Android
4. Click nút TTS

### Cách 2: Kiểm tra server có đang listen đúng không

**Kiểm tra xem server có đang listen trên 0.0.0.0:**

```bash
netstat -ano | findstr :3000
```

Phải thấy:
```
TCP    0.0.0.0:3000           0.0.0.0:0              LISTENING       <PID>
```

Nếu chỉ thấy `TCP    127.0.0.1:3000` → Server chỉ listen trên localhost, cần sửa.

**Restart server sau khi sửa:**

1. Dừng server hiện tại (Ctrl+C)
2. Đảm bảo code server có `app.listen(PORT, '0.0.0.0', ...)`
3. Chạy lại: `node server.js`

### Cách 3: Kiểm tra Firewall

Windows Firewall có thể chặn kết nối:

1. Mở **Windows Defender Firewall with Advanced Security**
2. **Inbound Rules** → **New Rule**
3. Chọn **Port** → **Next**
4. TCP → Specific local ports: `3000` → **Next**
5. **Allow the connection** → **Next**
6. Check all (Domain, Private, Public) → **Next**
7. Name: "Node.js Server Port 3000" → **Finish**

### Cách 4: Test từ emulator browser

1. Mở **Browser** trong Android Emulator
2. Truy cập: `http://localhost:3000` (nếu đã setup port forwarding)
   Hoặc: `http://10.0.2.2:3000` (nếu chưa setup port forwarding)
3. Phải hiển thị: "TTS Server is running"

### Cách 5: Kiểm tra lại code server

Đảm bảo server code có:

```javascript
// QUAN TRỌNG: Listen trên '0.0.0.0'
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running at http://0.0.0.0:${PORT}`);
});
```

**KHÔNG được:**
```javascript
app.listen(PORT); // Mặc định là localhost (127.0.0.1)
app.listen(PORT, 'localhost'); // Chỉ localhost
app.listen(PORT, '127.0.0.1'); // Chỉ 127.0.0.1
```

## Thứ tự thực hiện (theo thứ tự)

1. ✅ **Kiểm tra server có chạy:**
   ```bash
   netstat -ano | findstr :3000
   ```

2. ✅ **Kiểm tra server có listen trên 0.0.0.0:**
   Phải thấy `TCP    0.0.0.0:3000` trong netstat

3. ✅ **Setup ADB port forwarding:**
   ```bash
   adb reverse tcp:3000 tcp:3000
   ```

4. ✅ **Test từ emulator browser:**
   - Mở Browser trong emulator
   - Truy cập `http://localhost:3000`
   - Phải hiển thị "TTS Server is running"

5. ✅ **Chạy app và test TTS button**

## Nếu vẫn lỗi sau khi làm tất cả các bước trên

**Thử restart emulator:**
1. Close emulator
2. Trong Android Studio → AVD Manager
3. **Cold Boot Now** hoặc **Wipe Data** → Start
4. Setup port forwarding lại
5. Test lại

**Hoặc thử trên thiết bị thật:**
1. Tìm IP máy tính: `ipconfig` → IPv4 Address (ví dụ: 192.168.1.100)
2. Đảm bảo máy tính và điện thoại cùng WiFi
3. Đổi `BASE_URL` trong `TTSService.java`:
   ```java
   private static final String BASE_URL = "http://192.168.1.100:3000";
   ```
4. Server vẫn listen trên `0.0.0.0:3000`
5. URL response trong server cần dùng IP máy tính:
   ```javascript
   const HOST_FOR_CLIENT = '192.168.1.100';
   ```

## Lệnh ADB cần chạy mỗi lần restart emulator

Port forwarding sẽ mất khi emulator restart. Cần chạy lại:
```bash
adb reverse tcp:3000 tcp:3000
```

Hoặc thêm vào script khởi động tự động.

