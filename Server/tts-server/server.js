console.log("Starting server...");

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const googleTTS = require('google-tts-api');
const axios = require('axios');
const crypto = require('crypto');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = 3000;

// ================== AUDIO FOLDER ==================
const audioDir = path.join(__dirname, 'audio');

if (!fs.existsSync(audioDir)) {
  fs.mkdirSync(audioDir, { recursive: true });
  console.log("Created audio folder");
}

// ================== STATIC AUDIO (NO CACHE) ==================
app.use('/audio', express.static(audioDir, {
  etag: false,
  lastModified: false,
  setHeaders: (res) => {
    res.setHeader('Cache-Control', 'no-store');
  }
}));

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

  const fileName = `${safeFinger}_${safeGesture}.mp3`;
  const filePath = path.join(audioDir, fileName);

  try {

    // Tạo URL Google TTS
    const ttsUrl = googleTTS.getAudioUrl(text, {
      lang: 'vi',
      slow: false,
      host: 'https://translate.google.com',
    });

    // Tải mp3
    const response = await axios({
      method: 'GET',
      url: ttsUrl,
      responseType: 'arraybuffer'
    });

    // Ghi file (ghi đè nếu tồn tại)
    fs.writeFileSync(filePath, response.data);

    const host = req.headers.host;
    const fileUrl = `http://${host}/audio/${fileName}`;

    console.log(`Updated audio: ${fileName}`);
    console.log(`Text: ${text}`);

    res.json({
      message: 'TTS file updated',
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

// ================== CHECK UPDATE (SINGLE FILE MD5) ==================
app.get('/check-update/:file', (req, res) => {

  const fileName = req.params.file;
  const filePath = path.join(audioDir, fileName);

  if (!fs.existsSync(filePath)) {
    return res.json({ exists: false });
  }

  try {

    const fileBuffer = fs.readFileSync(filePath);

    const hash = crypto
      .createHash('md5')
      .update(fileBuffer)
      .digest('hex');

    const stats = fs.statSync(filePath);

    res.json({
      exists: true,
      hash: hash,
      size: stats.size
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Hash check failed' });
  }

});

// ================== FILE LIST (FULL SYNC FOR ESP) ==================
app.get('/file-list', (req, res) => {

  try {

    const files = fs.readdirSync(audioDir);

    let result = [];

    files.forEach(file => {

      if (!file.endsWith('.mp3')) return;

      const filePath = path.join(audioDir, file);
      const fileBuffer = fs.readFileSync(filePath);

      const hash = crypto
        .createHash('md5')
        .update(fileBuffer)
        .digest('hex');

      result.push({
        name: file,
        hash: hash
      });

    });

    res.json(result);

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'File list failed' });
  }

});

// ================== START SERVER ==================
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running at http://0.0.0.0:${PORT}`);
  console.log(`Local: http://localhost:${PORT}`);
});
// node server.js