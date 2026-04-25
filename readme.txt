# FRIDGY - Système de Frigo Connecté et Intelligent

## Description du Projet
Fridgy est un système IoT complet développé dans le cadre de la SAÉ 3.03 ("Développer des applications communicantes"). Il permet de transformer un réfrigérateur classique en un appareil intelligent capable de :
* Surveiller les constantes vitales (Température, Humidité, État de la porte).
* Gérer l'inventaire et la liste de courses de manière synchronisée (Web & Mobile).
* Sécuriser l'accès via un verrouillage à distance et un déverrouillage par digicode physique.
* Simuler l'envoi de commandes vers un service de Drive.

Ce projet met en œuvre une architecture distribuée complète : un objet connecté (Arduino), un serveur métier (Java), une base de données (MySQL), une API (PHP) et deux interfaces utilisateurs (Web & Android).

## Table des Matières
1. Architecture et Technologies
2. Prérequis
3. Installation
4. Utilisation
5. Fonctionnalités
6. Crédits

## Structure du Dossier Rendu

PROMAYON_LUCAS_L3.ZIP
├── readme.txt              (Documentation du projet)
├── android                 (Application Mobile)
│   ├── MainActivity.java
│   ├── activity_main.xml
│   ├── AndroidManifest.xml
│   └── build.gradle
├── arduino                 (Code Objet Connecté)
│   └── Fridgy_Main.ino
├── BDD                     (Base de Données)
│   └── fridgy_db.sql
├── FridgyServer            (Serveur Métier Java - Projet Maven)
│   ├── pom.xml
│   └── src
│       └── main
│           └── java
│               └── FridgyServer.java
└── web+api                 (Interface Web & API)
    ├── api.php
    └── index.html


## 1. Architecture et Technologies
* **Objet Connecté :** Arduino UNO, Capteur DHT11, Écran LCD I2C, Clavier Matriciel 4x4, LEDs, Buzzer.
* **Serveur Métier :** Java 17 (gestion du Port Série et persistance BDD).
* **Base de Données :** MySQL (hébergée via WampServer).
* **API Backend :** PHP (interface entre la BDD et les clients).
* **Clients :**
    * Interface Web : HTML5, CSS3 (Glassmorphism), JavaScript.
    * Application Mobile : Android (Java), ZXing (Scanner), Volley/HttpUrlConnection.

## 2. Prérequis
Avant de lancer le projet, assurez-vous d'avoir :
* **Matériel :** Kit Arduino Adeept (UNO R3, Câble USB, Modules).
* **Logiciels :**
    * Arduino IDE (pour téléverser le code sur l'objet).
    * JDK 17 et Maven (pour le serveur Java).
    * WampServer ou XAMPP (pour Apache/MySQL/PHP).
    * Android Studio (pour l'application mobile).
    * Un navigateur Web moderne.

## 3. Installation

### Étape 1 : Base de Données
1. Lancez WampServer.
2. Accédez à PhpMyAdmin (`http://localhost/phpmyadmin`).
3. Importez le fichier `fridgy_db.sql` pour créer la base `fridgy_db` et les tables `mesures`, `inventaire` et `liste_courses`.

### Étape 2 : L'Objet Connecté (Arduino)
1. Réalisez le montage électronique.
2. Ouvrez le fichier `Fridgy_Main.ino` dans l'IDE Arduino.
3. Installez les bibliothèques requises : `DHT sensor library`, `LiquidCrystal I2C`, `Keypad`.
4. Téléversez le code sur la carte Arduino connectée en USB.
5. **Important :** Fermez l'IDE Arduino pour libérer le port COM.

### Étape 3 : Le Serveur Java
1. Ouvrez un terminal dans le dossier `FridgyServer`.
2. Vérifiez le port COM dans le fichier `src/main/java/FridgyServer.java` (ex: "COM3").
3. Compilez et lancez le serveur avec Maven :
   > mvn compile exec:java

### Étape 4 : L'Interface Web et l'API
1. Copiez le dossier `fridgy` (contenat `index.html` et `api.php`) dans le répertoire `www` de WampServer (`C:\wamp64\www\`).
2. Vérifiez que l'API est accessible via `http://localhost/fridgy/api.php`.

### Étape 5 : L'Application Android
1. Ouvrez le projet dans Android Studio.
2. Dans `MainActivity.java`, modifiez la constante `API_URL` avec l'adresse IP locale de votre PC (ex: `http://192.168.1.15/fridgy/api.php`).
3. Compilez et installez l'APK sur votre téléphone.

## 4. Utilisation (Scénario de Démonstration)

1. **Démarrage :** Assurez-vous que Wamp est vert, l'Arduino branché, et le serveur Java lancé ("Port ouvert avec succès").
2. **Surveillance :** Ouvrez l'interface Web ou l'application. Observez la température changer en chauffant le capteur DHT11.
3. **Gestion des courses :**
   * Scannez un code-barres avec l'application mobile : le produit s'ajoute à l'inventaire.
   * Ajoutez manuellement un produit à la liste de courses sur le Web : il apparaît instantanément sur le mobile.
4. **Sécurité :**
   * Sur l'application, cliquez sur l'icône "Cadenas".
   * L'Arduino passe en mode "VERROUILLE" (LED Rouge, Écran bloqué).
   * Pour déverrouiller, tapez le code PIN (`1234#`) sur le clavier physique de l'Arduino.

## 5. Fonctionnalités Clés
* **Synchronisation Temps Réel :** Les actions sur le mobile se répercutent immédiatement sur le web et vice-versa.
* **Scan de Produits :** Intégration de l'API OpenFoodFacts pour récupérer le vrai nom des produits scannés.
* **Contrôle Bidirectionnel :** Le serveur Java agit comme une passerelle, traduisant les commandes Web (fichiers texte) en signaux Série pour l'Arduino.
* **Persistance :** Historique des températures et état des stocks conservés en base de données SQL.

## 6. Crédits
* **Auteur :** Lucas PROMAYON (2e Année de BUT R&T - IUT de Valence)
* **Encadrants :** M. Jamont
* **Ressources :** Kit Adeept Arduino, Documentation OpenFoodFacts API.