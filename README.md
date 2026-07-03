# 🧤 SmartGloves

A smart glove system for recognizing sign language based on Flex Sensor voltage measurements and converting gestures into speech using ESP32-S3, IoT, an Android application, Firebase, and a Text-to-Speech (TTS) server.

---

## 📌 Features

- ✋ Real-time hand gesture recognition using Flex Sensors
- 🎤 Convert recognized gestures into speech
- 📱 Android application for system management
- ☁️ Firebase integration for cloud data synchronization
- 🌐 IoT communication between ESP32-S3 and Android
- 🔊 Node.js Text-to-Speech (TTS) server
- 📜 Gesture history management

---

## 🛠 Technologies

### Hardware
- ESP32-S3
- Flex Sensors
- Speaker
- Power Supply

### Software
- Arduino IDE
- Android Studio
- Node.js
- Firebase Realtime Database
- VS Code

---

## 📂 Project Structure

```
SmartGloves
│
├── Firmware/          # ESP32-S3 source code
├── AndroidApp/        # Android application
├── Server/            # Node.js TTS server
├── Documents/         # Reports and documents
├── Images/            # Images and diagrams
└── README.md
```

---

## ⚙️ Installation

### 1. Firmware (ESP32-S3)

- Open the `Firmware` folder using Arduino IDE.
- Install required libraries.
- Select your ESP32-S3 board.
- Upload the code.

---

### 2. Android Application

Open the `AndroidApp` project in Android Studio.

Add your Firebase configuration:

```
app/google-services.json
```

Sync Gradle and run the application.

---

### 3. TTS Server

Go to the Server folder.

Install dependencies:

```bash
npm install
```

Run the server:

```bash
npm start
```

---

## 🚀 System Workflow

1. User performs a hand gesture.
2. Flex Sensors detect finger bending.
3. ESP32-S3 reads sensor voltage values.
4. Gesture is identified.
5. Data is sent through IoT.
6. Android receives the gesture.
7. TTS Server generates speech.
8. Audio is played to the user.

---

## 📸 Demo

### Hardware

![Smart Glove 1](Images/Gloves1.jpg)

![Smart Glove 2](Images/Gloves2.jpg)

### Android Application

![Application 1](Images/App1.png)

![Application 2](Images/App2.png)

---

## 👨‍💻 Author

**Lam Vica**

Graduation Project

University of Natural Resources and Environment Hanoi

---

## 📄 License

This project is intended for educational and research purposes.
