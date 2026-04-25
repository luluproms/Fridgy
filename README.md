#  FRIDGY - Système de Frigo Connecté et Intelligent

![Maquette Web](maquette%20web.png)

##  Description du Projet
**Fridgy** est un système IoT complet développé dans le cadre de la SAÉ 3.03 ("Développer des applications communicantes"). Il permet de transformer un réfrigérateur classique en un appareil intelligent capable de :

- **Surveiller les constantes vitales** (Température, Humidité, État de la porte).
- **Gérer l'inventaire et la liste de courses** de manière synchronisée (Interface Web & Application Mobile).
- **Sécuriser l'accès** via un verrouillage à distance (depuis l'app) et un déverrouillage par digicode physique.
- **Simuler l'envoi de commandes** vers un service de Drive.

Ce projet met en œuvre une architecture distribuée complète : un objet connecté (Arduino), un serveur métier (Java), une base de données (MySQL), une API (PHP) et deux interfaces utilisateurs (Web & Android).

---

##  Architecture et Technologies

- **Objet Connecté :** Arduino UNO, Capteur DHT11, Écran LCD I2C, Clavier Matriciel 4x4, LEDs, Buzzer.
- **Serveur Métier :** Java 17 (gestion du Port Série et persistance en BDD).
- **Base de Données :** MySQL (hébergée via WampServer).
- **API Backend :** PHP (interface entre la BDD et les clients).
- **Clients :**
  - *Interface Web* : HTML5, CSS3 (Glassmorphism), JavaScript.
  - *Application Mobile* : Android (Java), ZXing (Scanner de code-barres), Volley/HttpUrlConnection.

---

##  Installation et Démarrage

###  Prérequis
- **Matériel :** Kit Arduino Adeept (UNO R3, Câble USB, Modules).
- **Logiciels :** Arduino IDE, JDK 17 et Maven, WampServer/XAMPP, Android Studio.

###  Base de Données
1. Lancez WampServer et accédez à PhpMyAdmin (`http://localhost/phpmyadmin`).
2. Importez le fichier `BDD/fridgy_db.sql` pour créer la base `fridgy_db`.

###  L'Objet Connecté (Arduino)
1. Réalisez le montage électronique.
2. Ouvrez le fichier `arduino/Fridhy_Main/Fridhy_Main.ino` dans l'IDE Arduino.
3. Installez les bibliothèques requises (`DHT sensor library`, `LiquidCrystal I2C`, `Keypad`).
4. Téléversez le code et fermez l'IDE pour libérer le port série.

###  Le Serveur Java
1. Ouvrez un terminal dans le dossier `serveur metier fichiers source` (ou le dossier Maven).
2. Vérifiez le port COM dans `FridgyServer.java` (ex: "COM3").
3. Compilez et lancez avec Maven : `mvn compile exec:java`

###  Web & App Mobile
1. Copiez le contenu de `web+api` dans le répertoire `www` de WampServer.
2. Ouvrez `FridgyApp` dans Android Studio, modifiez l'URL de l'API avec l'IP locale de votre PC, compilez et installez l'APK.

---

##  Fonctionnalités Clés

- **Synchronisation Temps Réel :** Les actions sur le mobile se répercutent immédiatement sur le web et vice-versa.
- **Scan de Produits :** Intégration de l'API *OpenFoodFacts* pour récupérer le vrai nom des produits scannés sur l'app.
- **Contrôle Bidirectionnel :** Le serveur Java agit comme une passerelle, traduisant les requêtes des clients en signaux Série pour l'Arduino.
- **Persistance :** Historique des températures et état des stocks conservés en base de données SQL.

---

##  Auteur et Crédits
* **Auteur :** Lucas (2e Année de BUT R&T)
* **[Mon Profil LinkedIn](https://www.linkedin.com/in/lucas-promayon/)**
* **Encadrants :** M. Jamont
* **Ressources :** Kit Adeept Arduino, Documentation OpenFoodFacts API.
