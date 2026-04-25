# Fridgy : Serveur Web & Objet Connecté

**Fridgy** est un projet d'objet connecté (IoT) développé dans le cadre de mon BUT Réseaux et Télécommunications (SAE 33). Il a pour objectif de surveiller et d'afficher des données en temps réel via une interface web, en faisant le lien entre du matériel électronique et un serveur d'application.

![Maquette Web](maquette%20web.png)

## 📋 Architecture du Projet

Le projet est divisé en deux grandes parties :

1. **La partie Matérielle (IoT)** : Située dans le dossier `arduino`. Elle gère les capteurs et l'acquisition des données. Le code est écrit en C++ pour microcontrôleur Arduino.
2. **La partie Serveur / Web** : Située dans le dossier `FridgyServer` (et `pageweb.html`). Elle est chargée de récupérer les données envoyées par l'objet connecté et de les restituer via une interface web fluide et intuitive.

## 🚀 Technologies Utilisées

- **Matériel** : Arduino, divers capteurs.
- **Backend** : Serveur Web (Python / CherryPy / Flask selon les itérations).
- **Frontend** : HTML5, CSS3, JavaScript.

## 🛠️ Installation et Utilisation

*(Ces instructions sont données à titre indicatif)*

1. **Serveur** : 
   - Naviguez dans le dossier `FridgyServer`.
   - Lancez le script principal (ex: `python main.py`).
   - Accédez à la page web via votre navigateur local (généralement `http://localhost:8080`).

2. **Arduino** : 
   - Ouvrez le dossier `arduino` avec l'IDE Arduino.
   - Téléversez le code sur votre carte après l'avoir connectée.

## 👤 Auteur
**Lucas Promayon**
Étudiant en BUT Réseaux et Télécommunications.
[Mon Profil LinkedIn](https://www.linkedin.com/in/lucas-promayon/)
