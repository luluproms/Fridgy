#include <Wire.h> 
#include <LiquidCrystal_I2C.h>
#include <Keypad.h>
#include <DHT.h>

// --- CONFIGURATION PIN ---
#define PIN_DHT 2
#define PIN_PORTE 3
#define PIN_LED_VERT 4
#define PIN_LED_ROUGE 5
#define PIN_BUZZER 6

#define DHTTYPE DHT11
DHT dht(PIN_DHT, DHTTYPE);
LiquidCrystal_I2C lcd(0x27, 16, 2);

// Configuration Clavier 4x4
const byte ROWS = 4; const byte COLS = 4; 
char keys[ROWS][COLS] = {{'1','2','3','A'},{'4','5','6','B'},{'7','8','9','C'},{'*','0','#','D'}};
byte rowPins[ROWS] = {7, 8, 9, 10}; 
byte colPins[COLS] = {11, 12, A0, A1};
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS);

// Variables d'état
bool isLocked = false;
String inputCode = "";
const String SECRET_CODE = "1234"; // Code PIN
unsigned long lastTime = 0;

void setup() {
  Serial.begin(9600); // Communication Série avec Java
  pinMode(PIN_PORTE, INPUT_PULLUP);
  pinMode(PIN_LED_VERT, OUTPUT); pinMode(PIN_LED_ROUGE, OUTPUT); pinMode(PIN_BUZZER, OUTPUT);
  
  lcd.init(); lcd.backlight(); 
  dht.begin();
}

void loop() {
  gestionSerie(); // Écoute les ordres du PC (LOCK/UNLOCK)
  
  if (isLocked) {
    modeVerrouille();
  } else {
    modeNormal();
  }
}

void modeNormal() {
  digitalWrite(PIN_LED_VERT, HIGH); digitalWrite(PIN_LED_ROUGE, LOW);
  
  // Envoi des données toutes les 2 secondes
  if (millis() - lastTime >= 2000) {
    lastTime = millis();
    float t = dht.readTemperature(); 
    float h = dht.readHumidity();
    bool isClosed = (digitalRead(PIN_PORTE) == LOW);
    
    // Affichage LCD
    lcd.setCursor(0, 0); lcd.print("Temp: " + String(t, 1) + "C    ");
    lcd.setCursor(0, 1); lcd.print(isClosed ? "FERMEE H:" : "OUVERTE H:"); lcd.print(String(h, 0) + "%");
    
    // Envoi au Serveur Java (Protocole: DATA;temp;hum;porte)
    Serial.print("DATA;"); Serial.print(t); Serial.print(";"); Serial.print(h); Serial.print(";"); Serial.println(isClosed ? "1" : "0");
  }
}

void modeVerrouille() {
  digitalWrite(PIN_LED_VERT, LOW); digitalWrite(PIN_LED_ROUGE, HIGH);
  lcd.setCursor(0, 0); lcd.print("!! VERROUILLE !!");
  lcd.setCursor(0, 1); lcd.print("Code: " + inputCode + "    ");
  
  char key = keypad.getKey();
  if (key) {
    tone(PIN_BUZZER, 2000, 50);
    if (key == '#') { // Validation
      if (inputCode == SECRET_CODE) {
        isLocked = false; inputCode = ""; lcd.clear();
        Serial.println("EVENT;UNLOCKED_BY_USER");
      } else {
        tone(PIN_BUZZER, 200, 500); inputCode = ""; // Erreur
      }
    } else if (key == '*') { 
      inputCode = ""; 
    } else { 
      inputCode += key; 
    }
  }
}

void gestionSerie() {
  if (Serial.available() > 0) {
    String cmd = Serial.readStringUntil('\n'); 
    cmd.trim();
    if (cmd == "LOCK") { isLocked = true; inputCode = ""; lcd.clear(); }
    if (cmd == "UNLOCK") { isLocked = false; lcd.clear(); }
  }
}