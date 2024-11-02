#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "DHT.h"

#define SERVICE_UUID                  "4182dfcc-13bf-40e1-8067-a46b38030ecf"
#define HUMIDITY_CHARACTERISTIC_UUID  "3a75655b-3da0-474d-b387-eccb8f5fad0a"
#define TEMP_CHARACTERISTIC_UUID      "9861f68f-b20b-45c3-a1be-c6627d245391"

#define BLE_SERVER_NAME "24hWilgotnosc_ESP32"
#define DHTPIN 4
#define DHTTYPE DHT11   // DHT 11

bool deviceConnected =false;

DHT dht(DHTPIN, DHTTYPE);
  class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("Telefon polaczone");
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("Telefon rozlaczony");
  }
};


uint64_t lastMills = 0;
uint16_t readHumDelay = 2000;
BLECharacteristic *humCharacteristics= new BLECharacteristic(HUMIDITY_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_READ);
BLECharacteristic tempCharacteristics(TEMP_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor *humDescriptor= new BLEDescriptor(BLEUUID((uint16_t)0x2902));
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
  pService->addCharacteristic(humCharacteristics);
  humDescriptor->setValue("Wilgotnosc");
  humCharacteristics->addDescriptor(humDescriptor);
  
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  if(deviceConnected)
  {
    if(millis() > lastMills+readHumDelay){
      float h = dht.readHumidity();
      float t = dht.readTemperature();

      if (isnan(h) || isnan(t)) {
        Serial.println(F("Failed to read from DHT sensor!"));
        return;
      }
      humCharacteristics->setValue(h);
      humCharacteristics->notify();
      

      Serial.print(F("Humidity: "));
      Serial.print(h);
      Serial.print(F("%  Temperature: "));
      Serial.print(t);
      Serial.println(F("°C "));

      lastMills=millis();
    }
  }
  else{
    delay(200);
    BLEDevice::startAdvertising();
  }
}