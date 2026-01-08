package com.example.weighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_BMI = "bmi";
    public static final String COLUMN_PHOTO_PATH = "photo_path";

    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_ENTRIES + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_WEIGHT + " REAL, " +
            COLUMN_DATE + " TEXT, " +
            COLUMN_BMI + " REAL, " +
            COLUMN_PHOTO_PATH + " TEXT" +
            ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Izveido tabulas, kad db pirmo reizi izveido
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    // Versijas maiņa: šeit jābūt migrācijai, lai nepazaudētu datus
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Šeit mēs rakstītu ALTER TABLE komandas, ja mainītos struktūra.
        // Piemēram:
        // if (oldVersion < 2) {
        // db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN jauna_kolonna
        // TEXT");
        // }
        // NEKAD nedzēst tabulu (DROP), ja gribam saglabāt lietotāja datus!
    }

    // pievieno svaru
    public long pievienotIerakstu(WeightEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_WEIGHT, entry.iegutSvaru());
            values.put(COLUMN_DATE, entry.iegutDatumu());
            values.put(COLUMN_BMI, entry.iegutKmi());
            values.put(COLUMN_PHOTO_PATH, entry.iegutFotoCelu());

            id = db.insert(TABLE_ENTRIES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();
        return id;
    }

    // Atjaunina esošu ierakstu
    public int atjauninatIerakstu(WeightEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, entry.iegutSvaru());
        values.put(COLUMN_DATE, entry.iegutDatumu());
        values.put(COLUMN_BMI, entry.iegutKmi());
        values.put(COLUMN_PHOTO_PATH, entry.iegutFotoCelu());

        int rowsAffected = db.update(TABLE_ENTRIES, values, COLUMN_ID + " = ?",
                new String[] { String.valueOf(entry.iegutId()) });
        db.close();
        return rowsAffected;
    }

    // ieraksta dzēšana
    public void dzestIerakstu(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ENTRIES + " WHERE " + COLUMN_ID + " = " + id);
        db.close();
    }

    // iegūst visus ierakstus
    public List<WeightEntry> iegutVisusIerakstus() {
        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Sakārtoti dilstošā secībā pēc ID
        Cursor cursor = db.query(TABLE_ENTRIES, null, null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long dbId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                double bmi = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BMI));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH));

                entries.add(new WeightEntry(dbId, weight, date, bmi, path));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return entries;
    }
}
