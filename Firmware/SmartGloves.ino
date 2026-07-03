#include <WiFi.h>
#include <HTTPClient.h>
#include <SD.h>
#include <SPI.h>
#include <ArduinoJson.h>
#include "MD5Builder.h"
#include "Audio.h"

// ================= WIFI =================
const char* ssid = "Lamvica";
const char* password = "2011204L";

// ================= SERVER =================
const char* serverIP = "http://192.168.0.103:3000";

// ================= SD SPI =================
#define SD_CS    17
#define SD_MOSI  18
#define SD_SCK   16
#define SD_MISO  15

// ================= FLEX PINS =================
#define FLEX_THUMB   9
#define FLEX_INDEX   10
#define FLEX_MIDDLE  11
#define FLEX_RING    12
#define FLEX_PINKY   13

#define R_FIXED 10000.0

Audio audio;

// ================= FLEX =================
float filteredValue[5] = {0};
int lastState[5] = {0};
unsigned long lastReadTime = 0;
const int readInterval = 40;

// ================= UPDATE =================
unsigned long lastUpdateCheck = 0;
const unsigned long updateInterval = 20000; // 20 giây

// =======================================================
// ================= MD5 ================================
String getFileMD5(String path)
{
  if (!SD.exists(path)) return "";

  File file = SD.open(path);
  if (!file) return "";

  MD5Builder md5;
  md5.begin();

  uint8_t buf[1024];
  int len;

  while ((len = file.read(buf, sizeof(buf))) > 0)
    md5.add(buf, len);

  md5.calculate();
  file.close();

  return md5.toString();
}

// =======================================================
// ================= WIFI ================================
void connectWiFi()
{
  Serial.println("Connecting WiFi...");
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi Connected!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// =======================================================
// ================= DOWNLOAD ============================
void downloadFile(String filename)
{
  Serial.println("Downloading: " + filename);

  HTTPClient http;
  String url = String(serverIP) + "/audio/" + filename;
  http.begin(url);

  int httpCode = http.GET();

  if (httpCode == HTTP_CODE_OK)
  {
    if (SD.exists("/" + filename))
      SD.remove("/" + filename);

    File file = SD.open("/" + filename, FILE_WRITE);
    WiFiClient * stream = http.getStreamPtr();

    uint8_t buffer[1024];
    while (http.connected())
    {
      size_t size = stream->available();
      if (size)
      {
        int c = stream->readBytes(buffer,
                 size > sizeof(buffer) ? sizeof(buffer) : size);
        file.write(buffer, c);
      }
      delay(1);
    }

    file.close();
    Serial.println("Download done");
  }

  http.end();
}

// =======================================================
// ================= CHECK UPDATE ========================
void checkForUpdates()
{
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  String url = String(serverIP) + "/file-list";
  http.begin(url);

  int httpCode = http.GET();
  if (httpCode != HTTP_CODE_OK)
  {
    http.end();
    return;
  }

  String payload = http.getString();
  http.end();

  DynamicJsonDocument doc(4096);
  if (deserializeJson(doc, payload))
    return;

  bool changed = false;

  for (JsonObject fileObj : doc.as<JsonArray>())
  {
    String name = fileObj["name"].as<String>();
    String serverHash = fileObj["hash"].as<String>();
    String localHash = getFileMD5("/" + name);

    if (localHash == "" || localHash != serverHash)
    {
      Serial.println("File changed: " + name);
      downloadFile(name);
      changed = true;
    }
  }

  if (changed)
    Serial.println("Update finished\n");
}

// =======================================================
// ================= AUDIO ===============================
void playMP3(const char* filename)
{
  if (audio.isRunning()) return;

  Serial.println("Playing: " + String(filename));
  audio.connecttoFS(SD, filename);
}

// =======================================================
// ================= FLEX ================================
int readFlexState(int index, int pin)
{
  int raw = analogRead(pin);

  filteredValue[index] =
      0.7 * filteredValue[index] + 0.3 * raw;

  float voltage = filteredValue[index] * 3.3 / 4095.0;
  float rFlex = (voltage * R_FIXED) / (3.3 - voltage);
  rFlex /= 1000.0;

  if (rFlex >= 7.0) return 0;
  else if (rFlex >= 5.0) return 1;
  else return 2;
}

void checkFinger(int index, int pin,
                 const char* file1,
                 const char* file2)
{
  int currentState = readFlexState(index, pin);

  if (currentState != lastState[index])
  {
    lastState[index] = currentState;

    if (currentState == 1)
      playMP3(file1);
    else if (currentState == 2)
      playMP3(file2);
  }
}

// =======================================================
// ================= SETUP ===============================
void setup()
{
  Serial.begin(115200);
  delay(2000);

  connectWiFi();

  SPI.begin(SD_SCK, SD_MISO, SD_MOSI, SD_CS);

  if (!SD.begin(SD_CS))
  {
    Serial.println("SD Failed!");
    return;
  }

  Serial.println("SD OK");

  checkForUpdates(); // update khi boot

  analogReadResolution(12);
  analogSetAttenuation(ADC_11db);

  audio.setPinout(5,4,6);
  audio.setVolume(20);
  audio.forceMono(true);

  Serial.println("System Ready!\n");
}

// =======================================================
// ================= LOOP ================================
void loop()
{
  audio.loop();

  // 1️⃣ Đọc flex
  if (millis() - lastReadTime > readInterval)
  {
    lastReadTime = millis();

    checkFinger(0, FLEX_PINKY,  "/ngonut_c1.mp3",   "/ngonut_c2.mp3");
    checkFinger(1, FLEX_RING,   "/ngonaput_c1.mp3", "/ngonaput_c2.mp3");
    checkFinger(2, FLEX_MIDDLE, "/ngongiua_c1.mp3", "/ngongiua_c2.mp3");
    checkFinger(3, FLEX_INDEX,  "/ngontro_c1.mp3",  "/ngontro_c2.mp3");
    checkFinger(4, FLEX_THUMB,  "/ngoncai_c1.mp3",  "/ngoncai_c2.mp3");
  }

  // 2️⃣ Check update 20s/lần
  if (!audio.isRunning() &&
      millis() - lastUpdateCheck > updateInterval)
  {
    lastUpdateCheck = millis();
    checkForUpdates();
  }
}