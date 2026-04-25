<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *'); // Autorise l'app Android
$mysqli = new mysqli("localhost", "root", "", "fridgy_db");

$action = $_GET['action'] ?? '';

// Commandes Arduino (via fichier texte)
if ($action == 'lock') { file_put_contents('cmd.txt', 'LOCK'); }
if ($action == 'unlock') { file_put_contents('cmd.txt', 'UNLOCK'); }

// Gestion Liste de Courses
if ($action == 'add_item') $mysqli->query("INSERT INTO liste_courses (produit) VALUES ('".$_POST['nom']."')");
if ($action == 'toggle_item') $mysqli->query("UPDATE liste_courses SET est_achete = NOT est_achete WHERE id=".$_POST['id']);
if ($action == 'delete_item') $mysqli->query("DELETE FROM liste_courses WHERE id=".$_POST['id']);

// Gestion Inventaire
if ($action == 'add_inv') $mysqli->query("INSERT INTO inventaire (produit, dlc) VALUES ('".$_POST['nom']."', DATE_ADD(NOW(), INTERVAL 7 DAY))");
if ($action == 'del_inv') $mysqli->query("DELETE FROM inventaire WHERE id=".$_POST['id']);

// Récupération des données (JSON final)
$data = $mysqli->query("SELECT * FROM mesures ORDER BY id DESC LIMIT 1")->fetch_assoc();
$list = $mysqli->query("SELECT * FROM liste_courses ORDER BY id DESC")->fetch_all(MYSQLI_ASSOC);
$inv = $mysqli->query("SELECT * FROM inventaire ORDER BY dlc ASC")->fetch_all(MYSQLI_ASSOC);
$isLockedCmd = file_exists('cmd.txt') && file_get_contents('cmd.txt') === 'LOCK';

echo json_encode([
    "temps" => ["fridge" => floatval($data['temperature']), "humidity" => floatval($data['humidite'])],
    "door" => ["open" => ($data['porte_ouverte'] == 1)],
    "status" => ["locked" => $isLockedCmd], // État théorique
    "shopping_list" => $list,
    "inventory_list" => $inv,
    "updatedAt" => $data['date_mesure']
]);
?>