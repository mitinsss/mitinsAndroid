package com.example.weighttracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddEntryActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String API_URL = "https://jsonplaceholder.typicode.com/posts";

    private EditText etWeight;
    private ImageView ivPreview;
    private Button btnSave;
    private Button btnDelete;

    private String currentPhotoPath;
    private DatabaseHelper dbHelper;

    // Kameras rezultāta apstrāde
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    raditUznemtoAttelu(); // Parāda uzņemto attēlu
                }
            });

    // Izsaukts izveides brīdī. Iestata skatu un mainīgos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        // Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etWeight = findViewById(R.id.etWeight);
        ivPreview = findViewById(R.id.ivPreview);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        dbHelper = new DatabaseHelper(this);

        // Edit mode check
        if (getIntent().getBooleanExtra(Constants.EXTRA_IS_EDIT, false)) {
            long id = getIntent().getLongExtra(Constants.EXTRA_ID, -1);
            double weight = getIntent().getDoubleExtra(Constants.EXTRA_WEIGHT, 0);
            currentPhotoPath = getIntent().getStringExtra(Constants.EXTRA_PHOTO_PATH);

            etWeight.setText(String.valueOf(weight));
            btnSave.setText("Atjaunināt ierakstu"); // Update Entry

            btnDelete.setVisibility(android.view.View.VISIBLE);
            btnDelete.setOnClickListener(v -> dzestIerakstu(id));

            if (currentPhotoPath != null) {
                ivPreview.post(this::raditUznemtoAttelu);
            }
        }

        // Listeners
        findViewById(R.id.btnCamera).setOnClickListener(v -> parbauditAtlaujuUnAtvertKameru());
        btnSave.setOnClickListener(v -> parbauditUnSaglabat());
    }

    // Dzēš ierakstu no datu bāzes
    private void dzestIerakstu(long id) {
        dbHelper.dzestIerakstu(id);
        Toast.makeText(this, "Ieraksts izdzēsts", Toast.LENGTH_SHORT).show(); // Entry Deleted
        setResult(RESULT_OK);
        finish();
    }

    // Pārbauda kameras atļauju un atver kameru
    private void parbauditAtlaujuUnAtvertKameru() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    PERMISSION_REQUEST_CODE);
        } else {
            atvertKameru();
        }
    }

    // Apstrādā lietotāja piešķirtās (vai noraidītās) atļaujas
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                atvertKameru();
            } else {
                Toast.makeText(this, "Nepieciešama kameras atļauja", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Atver kameras lietotni
    private void atvertKameru() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = izveidotAttelaFailu();
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.weighttracker.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    cameraLauncher.launch(takePictureIntent);
                }
            } catch (IOException ex) {
                Toast.makeText(this, "Kļūda izveidojot failu", Toast.LENGTH_SHORT).show(); // Error creating file
            }
        }
    }

    // Izveido pagaidu failu attēlam
    private File izveidotAttelaFailu() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Parāda attēlu
    private void raditUznemtoAttelu() {
        if (ivPreview.getWidth() == 0)
            return;

        int targetW = ivPreview.getWidth();
        if (targetW == 0)
            targetW = 500;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int scaleFactor = Math.min(photoW / targetW, photoW / targetW);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        ivPreview.setImageBitmap(bitmap);
    }

    // Validē ievadi un saglabā (vai atjaunina)
    private void parbauditUnSaglabat() {
        String weightStr = etWeight.getText().toString();
        if (weightStr.isEmpty()) {
            etWeight.setError("Ievadiet svaru"); // Enter weight
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double height = (double) getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getFloat(Constants.KEY_USER_HEIGHT, Constants.DEFAULT_HEIGHT);

        if (height <= 0) {
            Toast.makeText(this, "Lūdzu, iestatiet savu augumu galvenajā skatā", Toast.LENGTH_LONG).show();
            return;
        }

        double bmi = weight / (height * height);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        izpilditApiPieprasijumu(weight, date, bmi);
    }

    // Sūta datus uz API
    private void izpilditApiPieprasijumu(double weight, String date, double bmi) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", "Weight Entry");
            jsonBody.put("body", "Weight: " + weight + ", Date: " + date);
            jsonBody.put("userId", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                response -> {
                    new AlertDialog.Builder(this)
                            .setTitle("API Atbilde")
                            .setMessage(response.toString())
                            .setPositiveButton("Labi", (d, w) -> saglabatDatuBaze(weight, date, bmi))
                            .show();
                },
                error -> {
                    new AlertDialog.Builder(this)
                            .setTitle("API Kļūda")
                            .setMessage(error.getMessage())
                            .setPositiveButton("Labi", (d, w) -> saglabatDatuBaze(weight, date, bmi))
                            .show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    // Saglabā datus lokālajā datu bāzē
    private void saglabatDatuBaze(double weight, String date, double bmi) {
        if (getIntent().getBooleanExtra(Constants.EXTRA_IS_EDIT, false)) {
            long id = getIntent().getLongExtra(Constants.EXTRA_ID, -1);
            WeightEntry entry = new WeightEntry(id, weight, date, bmi, currentPhotoPath);
            dbHelper.atjauninatIerakstu(entry);
            Toast.makeText(this, "Ieraksts atjaunināts", Toast.LENGTH_SHORT).show(); // Entry Updated
        } else {
            WeightEntry entry = new WeightEntry(weight, date, bmi, currentPhotoPath);
            dbHelper.pievienotIerakstu(entry);
            Toast.makeText(this, "Ieraksts pievienots", Toast.LENGTH_SHORT).show(); // Entry Added
        }

        setResult(RESULT_OK, new Intent());
        finish();
    }

    // Apstrādā 'Atpakaļ' pogu; atgriežas iepriekšējā skatā
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
