#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "DHT.h"

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID        "4182dfcc-13bf-40e1-8067-a46b38030ecf"
#define HUMIDITY_CHARACTERISTIC_UUID "3a75655b-3da0-474d-b387-eccb8f5fad0a"
#define TEMP_CHARACTERISTIC_UUID "9861f68f-b20b-45c3-a1be-c6627d245391"

#define BLE_SERVER_NAME "24hWilgotnosc_ESP32"
#define DHTPIN 4
// Uncomment whatever type you're using!
#define DHTTYPE DHT11   // DHT 11
//#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
//#define DHTTYPE DHT21   // DHT 21 (AM2301)
bool deviceConnected =false;

DHT dht(DHTPIN, DHTTYPE);
  class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    BLEDevice::startAdvertising();
  }
};


uint64_t lastMills = 0;
uint16_t readHumDelay = 2000;
BLECharacteristic humCharacteristics(HUMIDITY_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic tempCharacteristics(TEMP_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor humDescriptor(BLEUUID((uint16_t)0x2902));
BLEDescriptor tempDescriptor(BLEUUID((uint16_t)0x2903));

void setup() {
  Serial.begin(115200);
  Serial.println(F("DHTxx test!"));
  dht.begin();
  Serial.println("Starting BLE work!");

  BLEDevice::init(BLE_SERVER_NAME);
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pService->addCharacteristic(&humCharacteristics);
  humCharacteristics.setValue("Wilgotnosc");
  
  pService->start();

  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  // Wait a few seconds between measurements.
  if(deviceConnected)
  {
    if(millis() > lastMills+readHumDelay){
      // Reading temperature or humidity takes about 250 milliseconds!
      // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
      float h = dht.readHumidity();
      // Read temperature as Celsius (the default)
      float t = dht.readTemperature();
      // Read temperature as Fahrenheit (isFahrenheit = true)
      //float f = dht.readTemperature(true);

      // Check if any reads failed and exit early (to try again).
      if (isnan(h) || isnan(t)) {
        Serial.println(F("Failed to read from DHT sensor!"));
        return;
      }

      // Compute heat index in Fahrenheit (the default)
      //float hif = dht.computeHeatIndex(f, h);
      // Compute heat index in Celsius (isFahreheit = false)
      //float hic = dht.computeHeatIndex(t, h, false);
      humCharacteristics.setValue(std::to_string(h));
      humCharacteristics.notify();

      Serial.print(F("Humidity: "));
      Serial.print(h);
      Serial.print(F("%  Temperature: "));
      Serial.print(t);
      Serial.println(F("°C "));

      // Serial.print(f);
      // Serial.print(F("°F  Heat index: "));
      // Serial.print(hic);
      // Serial.print(F("°C "));
      // Serial.print(hif);
      // Serial.println(F("°F"));
      lastMills=millis();
    }
  }
}