import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.sql.*;
import java.util.Scanner;

public class FridgyServer {
    // A ADAPTER SELON LE PC
    private static final String PORT_NAME = "COM6"; 
    private static final String DB_URL = "jdbc:mysql://localhost:3306/fridgy_db?serverTimezone=UTC";
    private static final String CMD_FILE = "C:/wamp64/www/fridgy/cmd.txt";

    public static void main(String[] args) {
        System.out.println("--- FRIDGY SERVER ---");
        
        SerialPort port = SerialPort.getCommPort(PORT_NAME);
        port.setBaudRate(9600);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        
        if(!port.openPort()) { System.err.println("Erreur: Impossible d'ouvrir " + PORT_NAME); return; }
        
        PrintWriter writer = new PrintWriter(port.getOutputStream(), true);

        // 1. Thread Lecture Arduino -> BDD
        new Thread(() -> {
            try(Scanner s = new Scanner(port.getInputStream())) {
                while(s.hasNextLine()) processData(s.nextLine().trim());
            } catch(Exception e){}
        }).start();

        // 2. Thread Surveillance Fichier Web -> Arduino (Commandes)
        new Thread(() -> {
            while(true) {
                try {
                    File f = new File(CMD_FILE);
                    if(f.exists()) {
                        Scanner fs = new Scanner(f);
                        if(fs.hasNext()) {
                            String cmd = fs.next();
                            System.out.println("Web Command: " + cmd);
                            writer.println(cmd);
                        }
                        fs.close(); f.delete();
                    }
                    Thread.sleep(1000);
                } catch(Exception e){}
            }
        }).start();
    }

    private static void processData(String line) {
        System.out.println("[ARDUINO] " + line);
        if(line.startsWith("DATA;")) {
            String[] p = line.split(";");
            if(p.length==4) saveDB(Float.parseFloat(p[1]), Float.parseFloat(p[2]), p[3].equals("1"));
        }
    }

    private static void saveDB(float t, float h, boolean closed) {
        try(Connection c = DriverManager.getConnection(DB_URL, "root", "");
            PreparedStatement ps = c.prepareStatement("INSERT INTO mesures (temperature,humidite,porte_ouverte) VALUES (?,?,?)")) {
            ps.setFloat(1, t); ps.setFloat(2, h); ps.setBoolean(3, !closed); 
            ps.executeUpdate();
        } catch(Exception e) { e.printStackTrace(); }
    }
}