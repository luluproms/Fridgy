import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class FridgyServer {

    // --- CONFIGURATION ---
    private static final String PORT_NAME = "COM6"; // Vérifie ton port Arduino !
    
    // Config WampServer
    private static final String DB_URL = "jdbc:mysql://localhost:3306/fridgy_db?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    // Chemin du fichier de commande (créé par PHP)
    private static final String CMD_FILE_PATH = "C:/wamp64/www/fridgy/cmd.txt";

    public static void main(String[] args) {
        System.out.println("--- DEMARRAGE DU SERVEUR FRIDGY ---");

        // 1. Connexion au Port Série
        SerialPort arduinoPort = SerialPort.getCommPort(PORT_NAME);
        arduinoPort.setBaudRate(9600);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (arduinoPort.openPort()) {
            System.out.println("Port " + PORT_NAME + " ouvert avec succes.");
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
        } else {
            System.err.println("ERREUR: Impossible d'ouvrir le port " + PORT_NAME);
            return;
        }

        // Flux de sortie pour envoyer des ordres à l'Arduino
        OutputStream outStream = arduinoPort.getOutputStream();
        PrintWriter arduinoWriter = new PrintWriter(outStream, true);

        // 2. Thread d'écoute (Lecture des données Arduino -> BDD)
        Thread listenerThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(arduinoPort.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    traiterDonnees(line);
                }
            } catch (Exception e) {}
        });
        listenerThread.start();

        // 3. Thread de surveillance du fichier de commande (PHP -> Arduino)
        // C'est ici que la magie opère pour le bouton Cadenas !
        Thread cmdWatcherThread = new Thread(() -> {
            System.out.println("Surveillance des commandes Web activée...");
            while (true) {
                try {
                    File cmdFile = new File(CMD_FILE_PATH);
                    if (cmdFile.exists()) {
                        // Lire la commande
                        Scanner fReader = new Scanner(cmdFile);
                        if (fReader.hasNext()) {
                            String cmd = fReader.next().trim();
                            System.out.println(">>> Commande reçue du Web : " + cmd);
                            
                            // Envoyer à l'Arduino
                            arduinoWriter.println(cmd); 
                        }
                        fReader.close();
                        
                        // Supprimer le fichier pour ne pas répéter la commande en boucle
                        if(cmdFile.delete()) {
                            System.out.println("   (Fichier commande traité et supprimé)");
                        }
                    }
                    Thread.sleep(1000); // Vérifier toutes les secondes
                } catch (Exception e) {
                    System.err.println("Erreur lecture fichier CMD : " + e.getMessage());
                }
            }
        });
        cmdWatcherThread.start();

        // 4. Boucle principale pour le terminal (Clavier PC -> Arduino)
        try (Scanner consoleInput = new Scanner(System.in)) {
            System.out.println("Serveur prêt. Tapez LOCK/UNLOCK ici ou utilisez l'Appli.");
            
            while (true) {
                if (consoleInput.hasNextLine()) {
                    String command = consoleInput.nextLine().toUpperCase();
                    if (command.equals("EXIT")) break;
                    
                    if (command.equals("LOCK") || command.equals("UNLOCK")) {
                        arduinoWriter.println(command);
                        System.out.println("-> Commande manuelle envoyée : " + command);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        arduinoPort.closePort();
        System.out.println("Serveur arrêté.");
        System.exit(0); // Force l'arrêt de tous les threads
    }

    private static void traiterDonnees(String line) {
        System.out.println("[ARDUINO] " + line);

        if (line.startsWith("DATA;")) {
            String[] parts = line.split(";");
            if (parts.length == 4) {
                try {
                    float temp = Float.parseFloat(parts[1]);
                    float hum = Float.parseFloat(parts[2]);
                    boolean isClosed = parts[3].equals("1"); 
                    sauvegarderMesure(temp, hum, !isClosed);
                } catch (Exception e) {
                    System.err.println("Erreur lecture données: " + line);
                }
            }
        }
    }

    private static void sauvegarderMesure(float temp, float hum, boolean isDoorOpen) {
        String query = "INSERT INTO mesures (temperature, humidite, porte_ouverte) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setFloat(1, temp);
            pstmt.setFloat(2, hum);
            pstmt.setBoolean(3, isDoorOpen);
            pstmt.executeUpdate();
            System.out.println("   (Sauvegardé en BDD)");
        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
        }
    }
}