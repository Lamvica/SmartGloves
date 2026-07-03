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

// Xác định HOST để trả về URL đúng cho client
// Có thể set qua environment variable HOST_FOR_CLIENT
// Mặc định: 10.32.65.25 (IP máy tính cho thiết bị thật)
// Hoặc 10.0.2.2 cho Android emulator
const HOST_FOR_CLIENT = process.env.HOST_FOR_CLIENT || '10.32.65.25';

app.get('/', (req, res) => {
  res.send('TTS Server is running');
});

// API nhận text
app.post('/tts', (req, res) => {
  const text = req.body.text;

  if (!text) {
    return res.status(400).json({ error: 'No text provided' });
  }

  const fileName = `tts_${Date.now()}.wav`;
  const audioDir = path.join(__dirname, 'audio');
  
  // Đảm bảo thư mục audio tồn tại
  if (!fs.existsSync(audioDir)) {
    fs.mkdirSync(audioDir, { recursive: true });
  }
  
  const filePath = path.join(audioDir, fileName);

  // Tạo file WAV đơn giản (header cơ bản)
  const wavHeader = Buffer.alloc(44);
  // WAV header cơ bản
  wavHeader.write('RIFF', 0);
  wavHeader.writeUInt32LE(36, 4); // File size - 8
  wavHeader.write('WAVE', 8);
  wavHeader.write('fmt ', 12);
  wavHeader.writeUInt32LE(16, 16); // Subchunk1Size
  wavHeader.writeUInt16LE(1, 20); // AudioFormat (PCM)
  wavHeader.writeUInt16LE(1, 22); // NumChannels (Mono)
  wavHeader.writeUInt32LE(16000, 24); // SampleRate
  wavHeader.writeUInt32LE(32000, 28); // ByteRate
  wavHeader.writeUInt16LE(2, 32); // BlockAlign
  wavHeader.writeUInt16LE(16, 34); // BitsPerSample
  wavHeader.write('data', 36);
  wavHeader.writeUInt32LE(0, 40); // Subchunk2Size (no data yet)
  
  fs.writeFileSync(filePath, wavHeader);

  console.log(`Created WAV file: ${fileName}`);
  console.log(`File saved to: ${filePath}`);

  // Xác định IP để trả về URL đúng cho client
  // Ưu tiên: lấy từ request header, sau đó dùng HOST_FOR_CLIENT
  let hostForUrl = HOST_FOR_CLIENT;
  
  // Thử lấy IP từ request (từ client connection)
  const clientIp = req.connection.remoteAddress || req.socket.remoteAddress;
  const serverAddress = req.connection.localAddress || req.socket.localAddress;
  
  // Nếu có thể, dùng IP của server interface mà client kết nối đến
  // Nhưng để đơn giản, dùng HOST_FOR_CLIENT (đã cấu hình)
  const fileUrl = `http://${hostForUrl}:${PORT}/audio/${fileName}`;
  
  console.log(`Client connected from: ${clientIp}`);
  console.log(`Response URL: ${fileUrl}`);

  res.json({
    message: 'TTS file created',
    file: fileName,
    url: fileUrl
  });
});

// QUAN TRỌNG: Listen trên '0.0.0.0' để client có thể kết nối từ bất kỳ interface nào
// '0.0.0.0' có nghĩa là listen trên tất cả network interfaces
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running at http://0.0.0.0:${PORT}`);
  console.log(`Access from Android emulator: http://10.0.2.2:${PORT}`);
  console.log(`Access from physical device: http://10.32.65.25:${PORT}`);
  console.log(`Access from local browser: http://localhost:${PORT}`);
  console.log(`Response URLs will use: http://${HOST_FOR_CLIENT}:${PORT}`);
});

// node server_fixed.js

