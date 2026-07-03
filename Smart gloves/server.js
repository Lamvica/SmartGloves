console.log("Starting server...");

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const googleTTS = require('google-tts-api');
const axios = require('axios');

const app = express();
app.use(cors());
app.use(express.json());

// ================== AUDIO FOLDER ==================
const audioDir = path.join(__dirname, 'audio');
if (!fs.existsSync(audioDir)) {
  fs.mkdirSync(audioDir, { recursive: true });
}
app.use('/audio', express.static(audioDir));

// ================== CONFIG ==================
const PORT = 3000;

// ================== ROOT ==================
app.get('/', (req, res) => {
  res.send('TTS Server is running');
});

// ================== TTS API ==================
app.post('/tts', async (req, res) => {
  const { text, finger, gesture } = req.body;

  if (!text || !finger || !gesture) {
    return res.status(400).json({
      error: 'Missing text, finger or gesture'
    });
  }

  const safeFinger = finger.toLowerCase().trim();
  const safeGesture = gesture.toLowerCase().trim();

  // Google trả về MP3 → dùng đuôi .mp3
  const fileName = `${safeFinger}_${safeGesture}.mp3`;
  const filePath = path.join(audioDir, fileName);

  try {
    // ====== TẠO LINK AUDIO TỪ GOOGLE ======
    const url = googleTTS.getAudioUrl(text, {
      lang: 'vi',
      slow: false,
      host: 'https://translate.google.com',
    });

    // ====== TẢI FILE MP3 ======
    const response = await axios({
      method: 'GET',
      url: url,
      responseType: 'arraybuffer'
    });

    // ====== GHI FILE ======
    fs.writeFileSync(filePath, response.data);

    const host = req.headers.host;
    const fileUrl = `http://${host}/audio/${fileName}`;

    console.log(`Created audio: ${fileName}`);
    console.log(`Text: ${text}`);

    res.json({
      message: 'TTS file created',
      finger: safeFinger,
      gesture: safeGesture,
      file: fileName,
      url: fileUrl
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({
      error: 'TTS generation failed'
    });
  }
});

// ================== START SERVER ==================
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running at http://0.0.0.0:${PORT}`);
  console.log(`Local browser: http://localhost:${PORT}`);
});

// node server.js