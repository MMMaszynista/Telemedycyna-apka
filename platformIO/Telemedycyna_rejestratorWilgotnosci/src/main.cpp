#include <BLEDevice.h>          // Biblioteka do obsługi BLE (Bluetooth Low Energy)
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "DHT.h"                // Biblioteka do obsługi czujników DHT

// Definicje UUID dla usługi BLE i charakterystyk
#define SERVICE_UUID                  "4182dfcc-13bf-40e1-8067-a46b38030ecf"
#define HUMIDITY_CHARACTERISTIC_UUID  "3a75655b-3da0-474d-b387-eccb8f5fad0a"
#define TEMP_CHARACTERISTIC_UUID      "9861f68f-b20b-45c3-a1be-c6627d245391"

// Nazwa serwera BLE oraz konfiguracja czujnika DHT
#define BLE_SERVER_NAME "24hWilgotnosc_ESP32"
#define DHTPIN 4                   // Pin do którego podłączony jest czujnik DHT
#define DHTTYPE DHT11              // Typ czujnika DHT (DHT11)

// Zmienna do śledzenia, czy urządzenie jest połączone przez BLE
bool deviceConnected = false;

// Tworzenie obiektu dla czujnika DHT
DHT dht(DHTPIN, DHTTYPE);

// Klasa dla obsługi zdarzeń połączenia i rozłączenia urządzenia z serwerem BLE
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;       // Ustawia zmienną, gdy urządzenie się połączy
    Serial.println("Telefon połączone");
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;      // Ustawia zmienną, gdy urządzenie się rozłączy
    Serial.println("Telefon rozłączone");
  }
};

// Inicjalizacja zmiennych do odczytu
uint64_t lastMills = 0;
uint16_t readHumDelay = 2000; // Czas między odczytami czujnika (w milisekundach)

// Charakterystyka wilgotności z funkcjami powiadomienia i odczytu
BLECharacteristic *humCharacteristics= new BLECharacteristic(HUMIDITY_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_READ);

// Deskryptory dla charakterystyk wilgotności
BLEDescriptor *humDescriptor = new BLEDescriptor(BLEUUID((uint16_t)0x2902));

void setup() {
  Serial.begin(115200);         // Ustawienie prędkości transmisji dla portu szeregowego
  Serial.println(F("DHTxx test!"));
  dht.begin();                  // Inicjalizacja czujnika DHT
  Serial.println("Starting BLE work!");

  BLEDevice::init(BLE_SERVER_NAME); // Inicjalizacja BLE z nazwą serwera
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks()); // Ustawienie callbacków połączenia

  // Tworzenie usługi BLE i dodanie charakterystyki wilgotności
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pService->addCharacteristic(humCharacteristics);
  
  humDescriptor->setValue("Wilgotnosc"); // Ustawienie opisu dla wilgotności
  humCharacteristics->addDescriptor(humDescriptor);

  pService->start();  // Uruchomienie usługi

  // Ustawienie reklamowania usługi BLE
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  BLEDevice::startAdvertising(); // Rozpoczęcie reklamowania (urządzenie jest widoczne)
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  // Sprawdzenie, czy urządzenie jest połączone
  if (deviceConnected) {
    // Odczyt danych co ustalony czas
    if (millis() > lastMills + readHumDelay) {
      float h = (dht.readHumidity()*1.51269584)-(15.69421934);       // Odczyt wilgotności
      float t = dht.readTemperature();    // Odczyt temperatury

      // Sprawdzenie poprawności odczytanych wartości
      if (isnan(h) || isnan(t)) {
        Serial.println(F("Failed to read from DHT sensor!"));
        return;
      }

      humCharacteristics->setValue(h); // Ustawienie wartości wilgotności
      humCharacteristics->notify();    // Powiadomienie urządzenia o nowej wartości wilgotności
      
      // Wyświetlanie wartości wilgotności i temperatury w konsoli
      Serial.print(F("Humidity: "));
      Serial.print(h);
      Serial.print(F("%  Temperature: "));
      Serial.print(t);
      Serial.println(F("°C "));

      lastMills = millis(); // Aktualizacja czasu ostatniego odczytu
    }
  } else {
    delay(200);                   // Krótkie opóźnienie, gdy nie ma połączenia
    BLEDevice::startAdvertising(); // Ponowne reklamowanie usługi BLE
  }
}
