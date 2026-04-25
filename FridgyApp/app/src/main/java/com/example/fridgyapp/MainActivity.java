package com.example.fridgyapp;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // --- CONFIGURATION IP (Mets ton IP ici) ---
    private static final String API_URL = "http://192.168.0.24/fridgy/api.php";

    // UI Elements
    private TextView tvTemp, tvDoor, tvLastUpdate;
    private LinearLayout layoutListContainer, layoutInventoryContainer, cardTemp;
    private EditText etNewItem;
    private Button btnAddItem, btnDrive;
    private ImageButton btnScan, btnLock;

    // Données locales
    private double currentHumidity = 0.0;
    private boolean isLocked = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // --- GESTION DU SCANNER ---
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Scan annulé", Toast.LENGTH_SHORT).show();
                } else {
                    // Code scanné -> On cherche le nom sur OpenFoodFacts
                    fetchRealProductName(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Liaison des éléments graphiques
        tvTemp = findViewById(R.id.tvTemp);
        tvDoor = findViewById(R.id.tvDoor);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        layoutListContainer = findViewById(R.id.layoutListContainer);
        layoutInventoryContainer = findViewById(R.id.layoutInventoryContainer);
        etNewItem = findViewById(R.id.etNewItem);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnDrive = findViewById(R.id.btnDrive);
        btnScan = findViewById(R.id.btnScan);
        btnLock = findViewById(R.id.btnLock);
        cardTemp = findViewById(R.id.cardTemp);

        // Configuration des boutons
        btnAddItem.setOnClickListener(v -> addItem()); // Ajout manuel liste courses
        btnDrive.setOnClickListener(v -> showDriveSimulation());
        cardTemp.setOnClickListener(v -> showSensorDetails());
        btnLock.setOnClickListener(v -> toggleLock());

        // Lancer le scanner
        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Ajout à l'Inventaire");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Démarrage de la boucle de synchronisation
        startAutoRefresh();
    }

    // --- 1. FONCTIONS RÉSEAU (API & OpenFoodFacts) ---

    // Recherche produit sur OpenFoodFacts
    private void fetchRealProductName(String barcode) {
        Toast.makeText(this, "Recherche du produit...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            String productName = "Produit " + barcode;
            try {
                String urlStr = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
                String jsonResponse = httpGet(urlStr);

                if (jsonResponse != null) {
                    JSONObject root = new JSONObject(jsonResponse);
                    if (root.has("status") && root.getInt("status") == 1) {
                        productName = root.getJSONObject("product").getString("product_name");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalName = productName;
            // Une fois le nom trouvé, on l'ajoute à l'inventaire BDD
            handler.post(() -> addInventoryItem(finalName));
        });
    }

    // Ajouter item dans l'inventaire (BDD)
    private void addInventoryItem(String name) {
        executor.execute(() -> {
            try {
                String params = "nom=" + URLEncoder.encode(name, "UTF-8");
                httpPost(API_URL + "?action=add_inv", params);
                fetchData(); // Rafraichir l'écran
                handler.post(() -> Toast.makeText(this, "Ajouté : " + name, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Récupérer toutes les données (GET)
    private void fetchData() {
        executor.execute(() -> {
            String result = httpGet(API_URL);
            handler.post(() -> {
                if (result != null) updateUI(result);
            });
        });
    }

    // Outil générique GET
    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Outil générique POST
    private void httpPost(String urlStr, String params) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            conn.getResponseCode(); // Déclenche la requête
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 2. MISE À JOUR DE L'INTERFACE (UI) ---

    private void updateUI(String json) {
        try {
            JSONObject data = new JSONObject(json);

            // A. Capteurs
            JSONObject temps = data.getJSONObject("temps");
            JSONObject door = data.getJSONObject("door");

            double tFridge = temps.getDouble("fridge");
            currentHumidity = temps.getDouble("humidity"); // Sauvegarde pour popup détail

            tvTemp.setText(tFridge + " °C");
            tvLastUpdate.setText("Mise à jour : " + data.getString("updatedAt"));

            if (door.getBoolean("open")) {
                tvDoor.setText("OUVERTE");
                tvDoor.setTextColor(Color.parseColor("#EF4444")); // Rouge
            } else {
                tvDoor.setText("FERMÉE");
                tvDoor.setTextColor(Color.WHITE);
            }

            // B. Liste de courses
            layoutListContainer.removeAllViews();
            JSONArray list = data.getJSONArray("shopping_list");
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                addCheckView(item.getInt("id"), item.getString("produit"), item.getInt("est_achete") == 1);
            }

            // C. Inventaire
            layoutInventoryContainer.removeAllViews();
            JSONArray inv = data.getJSONArray("inventory_list");
            for (int i = 0; i < inv.length(); i++) {
                JSONObject item = inv.getJSONObject(i);
                addInventoryView(item.getInt("id"), item.getString("produit"), item.getString("dlc"));
            }

        } catch (Exception e) {
            // tvLastUpdate.setText("Erreur lecture données");
        }
    }

    // --- 3. GÉNÉRATION DES VUES AVEC STYLE MODERNE ---

    // Style moderne pour l'inventaire (Carte sombre)
    private void addInventoryView(int id, String name, String dlc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(40, 30, 30, 30);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);

        // Fond style "Glass"
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#10FFFFFF"));
        shape.setCornerRadius(30);
        card.setBackground(shape);

        // Texte Produit + DLC
        TextView tv = new TextView(this);
        tv.setText(name + "\nDLC: " + dlc);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // Bouton Supprimer (Croix)
        ImageButton btnDel = new ImageButton(this);
        btnDel.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnDel.setBackgroundColor(Color.TRANSPARENT);
        btnDel.setColorFilter(Color.parseColor("#EF4444"));
        btnDel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Consommé ?")
                    .setMessage("Retirer " + name + " du frigo ?")
                    .setPositiveButton("Oui", (d, w) -> executor.execute(() -> httpPost(API_URL + "?action=del_inv", "id=" + id)))
                    .setNegativeButton("Non", null).show();
        });

        card.addView(tv);
        card.addView(btnDel);
        layoutInventoryContainer.addView(card);
    }

    // Style moderne pour la liste de courses
    private void addCheckView(int id, String text, boolean isChecked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setChecked(isChecked);
        cb.setTextColor(Color.parseColor(isChecked ? "#64748B" : "#FFFFFF"));
        cb.setTextSize(16);
        cb.setPadding(20, 20, 20, 20);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor(isChecked ? "#05FFFFFF" : "#10FFFFFF"));
        shape.setCornerRadius(30);
        cb.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        cb.setLayoutParams(params);

        if (isChecked) {
            cb.setPaintFlags(cb.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // Clic simple = Cocher
        cb.setOnClickListener(v -> {
            executor.execute(() -> httpPost(API_URL + "?action=toggle_item", "id=" + id));
            cb.setAlpha(0.6f);
        });

        // Clic long = Supprimer
        cb.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer ?")
                    .setPositiveButton("Oui", (d, w) -> {
                        executor.execute(() -> httpPost(API_URL + "?action=delete_item", "id=" + id));
                        layoutListContainer.removeView(cb);
                    }).setNegativeButton("Non", null).show();
            return true;
        });

        layoutListContainer.addView(cb);
    }

    // --- 4. AUTRES ACTIONS UTILISATEUR ---

    private void addItem() {
        String name = etNewItem.getText().toString().trim();
        if (name.isEmpty()) return;
        etNewItem.setText("");
        executor.execute(() -> {
            try {
                String params = "nom=" + URLEncoder.encode(name, "UTF-8");
                httpPost(API_URL + "?action=add_item", params);
                fetchData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void toggleLock() {
        isLocked = !isLocked;
        updateLockIcon();
        String action = isLocked ? "lock" : "unlock";
        executor.execute(() -> httpPost(API_URL + "?action=" + action, ""));
        Toast.makeText(this, isLocked ? "Verrouillage envoyé..." : "Déverrouillage envoyé...", Toast.LENGTH_SHORT).show();
    }

    private void updateLockIcon() {
        if (isLocked) {
            btnLock.setColorFilter(Color.RED);
            btnLock.setBackgroundColor(0x33FF0000);
        } else {
            btnLock.setColorFilter(Color.WHITE);
            btnLock.setBackgroundColor(0x33FFFFFF);
        }
    }

    private void showSensorDetails() {
        new AlertDialog.Builder(this)
                .setTitle("Données Capteurs")
                .setMessage("Température : " + tvTemp.getText() + "\n" +
                        "Humidité : " + currentHumidity + " %\n" +
                        "Capteur : DHT-11\n")
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showDriveSimulation() {
        int count = layoutListContainer.getChildCount();
        new AlertDialog.Builder(this)
                .setTitle("Drive Carrefour")
                .setMessage("Commande de " + count + " articles envoyée !\nMontant estimé : " + (count * 2.5) + " €")
                .setPositiveButton("Super", null)
                .show();
    }

    // Boucle de rafraîchissement (2s)
    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchData();
                handler.postDelayed(this, 2000);
            }
        }, 2000);
    }
}