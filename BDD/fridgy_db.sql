CREATE DATABASE IF NOT EXISTS fridgy_db;
USE fridgy_db;

-- Table des mesures (Capteurs)
CREATE TABLE IF NOT EXISTS mesures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    temperature FLOAT,
    humidite FLOAT,
    porte_ouverte BOOLEAN,
    date_mesure DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Table de la liste de courses (Synchro Web/Mobile)
CREATE TABLE IF NOT EXISTS liste_courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    produit VARCHAR(255),
    quantite INT DEFAULT 1,
    est_achete BOOLEAN DEFAULT FALSE
);

-- Table de l'inventaire (Stock du frigo)
CREATE TABLE IF NOT EXISTS inventaire (
    id INT AUTO_INCREMENT PRIMARY KEY,
    produit VARCHAR(255),
    quantite INT DEFAULT 1,
    dlc DATE DEFAULT NULL
);