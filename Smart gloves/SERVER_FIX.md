# Hướng dẫn sửa Server Node.js để kết nối với Android Emulator

## Vấn đề

Server hiện tại không thể kết nối từ Android emulator vì:
1. Server chỉ listen trên `localhost` (127.0.0.1), không phải `0.0.0.0`
2. URL trong response dùng `localhost`, nhưng từ emulator cần dùng `10.0.2.2`

## Code Server đã sửa

```javascript
console.log("Starting server...");

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());

// Cho phép truy cập file trong thư mục audio
app.use('/audio', express.static(path.join(__dirname, 'audio')));

const PORT = 3000;

// Xác định HOST để trả về URL đúng
// Khi chạy từ Android emulator, cần dùng 10.0.2.2
// Khi chạy từ trình duyệt local, có thể dùng localhost
const HOST_FOR_CLIENT = process.env.HOST_FOR_CLIENT || '10.0.2.2'; // Mặc định cho emulator

app.get('/', (req, res) => {
  res.send('TTS Server is running');
});

// API nhận text (tạm tạo file WAV giả)
app.post('/tts', (req, res) => {
  const text = req.body.text;

  if (!text) {
    return res.status(400).json({ error: 'No text provided' });
  }

  const fileName = `tts_${Date.now()}.wav`;
  const filePath = path.join(__dirname, 'audio', fileName);

  // Đảm bảo thư mục audio tồn tại
  const audioDir = path.join(__dirname, 'audio');
  if (!fs.existsSync(audioDir)) {
    fs.mkdirSync(audioDir, { recursive: true });
  }

  // WAV giả (chỉ để test luồng) - tạo file WAV đơn giản
  const wavHeader = Buffer.alloc(44);
  // WAV header cơ bản
  wavHeader.write('RIFF', 0);
  wavHeader.writeUInt32LE(36, 4); // File size - 8
  wavHeader.write('WAVE', 8);
  wavHeader.write('fmt ', 12);
  wavHeader.writeUInt32LE(16, 16); // Subchunk1Size
  wavHeader.writeUInt16LE(1, 20); // AudioFormat (PCM)
  wavHeader.writeUInt16LE(1, 22); // NumChannels
  wavHeader.writeUInt32LE(16000, 24); // SampleRate
  wavHeader.writeUInt32LE(32000, 28); // ByteRate
  wavHeader.writeUInt16LE(2, 32); // BlockAlign
  wavHeader.writeUInt16LE(16, 34); // BitsPerSample
  wavHeader.write('data', 36);
  wavHeader.writeUInt32LE(0, 40); // Subchunk2Size
  
  fs.writeFileSync(filePath, wavHeader);

  // QUAN TRỌNG: URL phải dùng HOST_FOR_CLIENT (10.0.2.2 cho emulator)
  // không được dùng localhost
  res.json({
    message: 'TTS file created',
    file: fileName,
    url: `http://${HOST_FOR_CLIENT}:${PORT}/audio/${fileName}`
  });
});

// QUAN TRỌNG: Listen trên 0.0.0.0 để emulator có thể kết nối
// không phải localhost (127.0.0.1)
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running at http://0.0.0.0:${PORT}`);
  console.log(`Access from Android emulator: http://10.0.2.2:${PORT}`);
  console.log(`Access from local browser: http://localhost:${PORT}`);
});

// node server.js
```

## Thay đổi chính

1. **Listen trên `0.0.0.0`**: 
   ```javascript
   app.listen(PORT, '0.0.0.0', () => {
   ```
   Thay vì:
   ```javascript
   app.listen(PORT, () => {  // Mặc định là localhost
   ```

2. **URL trong response dùng `10.0.2.2`**:
   ```javascript
   url: `http://${HOST_FOR_CLIENT}:${PORT}/audio/${fileName}`
   ```
   Với `HOST_FOR_CLIENT = '10.0.2.2'` (mặc định cho emulator)

3. **Đảm bảo thư mục audio tồn tại**: Thêm code tạo thư mục nếu chưa có

## Test

1. Chạy server: `node server.js`
2. Test từ trình duyệt: `http://localhost:3000` (phải hoạt động)
3. Test từ emulator: App Android sẽ gọi `http://10.0.2.2:3000/tts`

## Lưu ý

- Nếu test trên thiết bị thật (không phải emulator), cần:
  - Tìm IP máy tính: `ipconfig` (Windows) hoặc `ifconfig` (Mac/Linux)
  - Đổi `HOST_FOR_CLIENT` thành IP máy tính (ví dụ: `192.168.1.100`)
  - Hoặc set environment variable: `HOST_FOR_CLIENT=192.168.1.100 node server.js`

