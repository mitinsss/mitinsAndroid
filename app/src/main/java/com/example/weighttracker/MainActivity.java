package com.example.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WeightAdapter adapter;
    private List<WeightEntry> entryList;
    private DatabaseHelper dbHelper;

    // Apstrādā rezultātu no AddEntryActivity (pievienošana/rediģēšana)
    private final ActivityResultLauncher<Intent> addEntryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    atjaunotSarakstu();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        entryList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new WeightAdapter(entryList);

        adapter.setOnItemClickListener(this::atvertRedigesanasSkatu);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        atjaunotSarakstu();

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        FloatingActionButton fabHeight = findViewById(R.id.fabHeight);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
            addEntryLauncher.launch(intent);
        });

        fabHeight.setOnClickListener(v -> raditAugumaIevadesDialogu());

        // Pārbauda, vai augums ir iestatīts; ja nē - piedāvā to ievadīt
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        float currentHeight = prefs.getFloat(Constants.KEY_USER_HEIGHT, Constants.DEFAULT_HEIGHT);

        if (currentHeight <= 0) {
            raditAugumaIevadesDialogu();
        }
    }

    // Atver rediģēšanas skatu ar esošajiem datiem
    private void atvertRedigesanasSkatu(WeightEntry entry) {
        Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
        intent.putExtra(Constants.EXTRA_ID, entry.iegutId());
        intent.putExtra(Constants.EXTRA_WEIGHT, entry.iegutSvaru());
        intent.putExtra(Constants.EXTRA_DATE, entry.iegutDatumu());
        intent.putExtra(Constants.EXTRA_BMI, entry.iegutKmi());
        intent.putExtra(Constants.EXTRA_PHOTO_PATH, entry.iegutFotoCelu());
        intent.putExtra(Constants.EXTRA_IS_EDIT, true);
        addEntryLauncher.launch(intent);
    }

    // Parāda promptu auguma ievadei
    private void raditAugumaIevadesDialogu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Iestatīt augumu");
        builder.setMessage("Lūdzu, ievadiet savu augumu metros (piem., 1.75):");

        final EditText inputFrame = new EditText(this);
        inputFrame.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        float currentHeight = prefs.getFloat(Constants.KEY_USER_HEIGHT, Constants.DEFAULT_HEIGHT);

        if (currentHeight > 0) {
            inputFrame.setText(String.valueOf(currentHeight));
        }

        builder.setView(inputFrame);

        builder.setPositiveButton("Saglabāt",
                (dialog, which) -> apstradatAugumaSaglabasanu(inputFrame.getText().toString()));
        builder.setNegativeButton("Atcelt", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Apstrādā ievadīto augumu un pārrēķina datus
    private void apstradatAugumaSaglabasanu(String heightStr) {
        if (heightStr.isEmpty())
            return;

        try {
            float height = Float.parseFloat(heightStr);
            if (height > 0) {
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putFloat(Constants.KEY_USER_HEIGHT, height).apply();

                parrekinatKmiIerakstiem(height);
                Toast.makeText(this, "Augums saglabāts un ĶMI atjaunināts", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // Pārrēķina ĶMI visiem ierakstiem
    private void parrekinatKmiIerakstiem(float height) {
        List<WeightEntry> allEntries = dbHelper.iegutVisusIerakstus();
        for (WeightEntry entry : allEntries) {
            double newBmi = entry.iegutSvaru() / (height * height);

            WeightEntry updatedEntry = new WeightEntry(
                    entry.iegutId(),
                    entry.iegutSvaru(),
                    entry.iegutDatumu(),
                    newBmi,
                    entry.iegutFotoCelu());
            dbHelper.atjauninatIerakstu(updatedEntry);
        }
        atjaunotSarakstu();
    }

    // Atjaunina sarakstu no datu bāzes
    private void atjaunotSarakstu() {
        entryList.clear();
        entryList.addAll(dbHelper.iegutVisusIerakstus());
        adapter.notifyDataSetChanged();
    }
}
