/*
 * PROJET FRIDGY - LIVRABLE 3 (SAE 3.03)
 * Auteur : Lucas PROMAYON
 * Description : Gestion du dispositif connecté (Arduino UNO).
 * Fonctionnalités : Lecture capteurs, Affichage LCD, Verrouillage Clavier, Com Série.
 */

#include <Wire.h> 
#include <LiquidCrystal_I2C.h> // Installer bibliothèque "LiquidCrystal I2C"
#include <Keypad.h>            // Installer bibliothèque "Keypad"
#include <DHT.h>               // Installer bibliothèque "DHT sensor library"

// --- 1. CONFIGURATION DU MATÉRIEL ---

// Actionneurs et Capteurs
#define PIN_DHT 2        // Capteur Température sur Pin 2
#define PIN_PORTE 3      // Bouton Porte sur Pin 3 (GND + Pin 3)
#define PIN_LED_VERT 4   // LED Verte sur Pin 4
#define PIN_LED_ROUGE 5  // LED Rouge sur Pin 5
#define PIN_BUZZER 6     // Buzzer sur Pin 6

// Configuration du DHT
#define DHTTYPE DHT11
DHT dht(PIN_DHT, DHTTYPE);

// Configuration Écran LCD (Adresse I2C généralement 0x27)
LiquidCrystal_I2C lcd(0x27, 16, 2);

// Configuration Clavier 4x4
const byte ROWS = 4; 
const byte COLS = 4; 
char keys[ROWS][COLS] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};
// Branchement du clavier : Lignes sur 7-10, Colonnes sur 11-A1
byte rowPins[ROWS] = {7, 8, 9, 10}; 
byte colPins[COLS] = {11, 12, A0, A1}; // A0 et A1 utilisés en mode digital
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS);

// --- 2. VARIABLES GLOBALES ---

bool isLocked = false;             // État de verrouillage du frigo
String inputCode = "";             // Code en cours de saisie
const String SECRET_CODE = "1234"; // Code pour déverrouiller

unsigned long lastTime = 0;
const long interval = 2000;        // Envoi des données toutes les 2s

// --- 3. INITIALISATION ---
void setup() {
  Serial.begin(9600); // Vitesse de communication avec le PC
  
  pinMode(PIN_PORTE, INPUT_PULLUP); // Le bouton utilise la résistance interne
  pinMode(PIN_LED_VERT, OUTPUT);
  pinMode(PIN_LED_ROUGE, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);

  lcd.init();
  lcd.backlight();
  dht.begin();

  // Animation de démarrage
  lcd.setCursor(0, 0);
  lcd.print("FRIDGY SYSTEM");
  lcd.setCursor(0, 1);
  lcd.print("Demarrage...");
  delay(2000);
  lcd.clear();
}

// --- 4. BOUCLE PRINCIPALE ---
void loop() {
  
  // 1. Écouter si le serveur Java envoie un ordre
  gestionSerie();

  // 2. Comportement selon l'état
  if (isLocked) {
    modeVerrouille();
  } else {
    modeNormal();
  }
}

// --- 5. FONCTIONS ---

void modeNormal() {
  // LED Verte ON
  digitalWrite(PIN_LED_VERT, HIGH);
  digitalWrite(PIN_LED_ROUGE, LOW);

  // Gestion du temps (sans utiliser delay pour ne pas bloquer)
  unsigned long currentMillis = millis();
  
  if (currentMillis - lastTime >= interval) {
    lastTime = currentMillis;
    
    // Lecture Capteurs
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    // Lecture Porte (INPUT_PULLUP : LOW = Appuyé = Fermé)
    bool isClosed = (digitalRead(PIN_PORTE) == LOW); 
    String etatPorte = isClosed ? "FERMEE" : "OUVERTE";
    
    // Affichage LCD
    lcd.setCursor(0, 0);
    lcd.print("Temp: " + String(t, 1) + "C    ");
    lcd.setCursor(0, 1);
    lcd.print(etatPorte + " H:" + String(h, 0) + "%  ");

    // Envoi au Serveur (Protocole : DATA;Temp;Hum;Porte)
    Serial.print("DATA;");
    Serial.print(t);
    Serial.print(";");
    Serial.print(h);
    Serial.print(";");
    Serial.println(isClosed ? "1" : "0");
  }
}

void modeVerrouille() {
  // LED Rouge ON
  digitalWrite(PIN_LED_VERT, LOW);
  digitalWrite(PIN_LED_ROUGE, HIGH);

  lcd.setCursor(0, 0);
  lcd.print("!! VERROUILLE !!");
  lcd.setCursor(0, 1);
  lcd.print("Code: " + inputCode + "    ");

  // Gestion du Clavier
  char key = keypad.getKey();
  if (key) {
    tone(PIN_BUZZER, 2000, 50); // Bip touche
    
    if (key == '#') { // Touche de validation
      verifierCode();
    } 
    else if (key == '*') { // Touche effacer
      inputCode = "";
    } 
    else {
      inputCode += key; // Ajouter chiffre
    }
  }
}

void verifierCode() {
  if (inputCode == SECRET_CODE) {
    // Code Bon
    isLocked = false;
    inputCode = "";
    lcd.clear();
    lcd.print("DEVERROUILLAGE");
    tone(PIN_BUZZER, 1000, 100); delay(150);
    tone(PIN_BUZZER, 1500, 100); delay(150);
    delay(1000);
    lcd.clear();
    Serial.println("EVENT;UNLOCKED_BY_USER"); // Info pour le Java
  } else {
    // Code Faux
    lcd.setCursor(0, 1);
    lcd.print("CODE FAUX !     ");
    tone(PIN_BUZZER, 200, 500); // Son grave
    delay(1000);
    inputCode = "";
  }
}

void gestionSerie() {
  if (Serial.available() > 0) {
    String commande = Serial.readStringUntil('\n');
    commande.trim(); // Nettoyer les espaces

    if (commande == "LOCK") {
      isLocked = true;
      inputCode = ""; // Reset du code
      lcd.clear();
      Serial.println("ACK;LOCKED");
    }
    else if (commande == "UNLOCK") {
      isLocked = false;
      lcd.clear();
      Serial.println("ACK;UNLOCKED");
    }
  }
}