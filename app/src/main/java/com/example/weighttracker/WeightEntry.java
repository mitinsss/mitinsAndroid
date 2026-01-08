package com.example.weighttracker;

public class WeightEntry {
    private long id;
    private double svars;
    private String datums;
    private double kmi;
    private String fotoCels;

    // konstruktors, lai izveidotu objektu ar visiem datiem no db
    public WeightEntry(long id, double svars, String datums, double kmi, String fotoCels) {
        this.id = id;
        this.svars = svars;
        this.datums = datums;
        this.kmi = kmi;
        this.fotoCels = fotoCels;
    }

    public WeightEntry(double svars, String datums, double kmi, String fotoCels) {
        this.svars = svars;
        this.datums = datums;
        this.kmi = kmi;
        this.fotoCels = fotoCels;
    }

    public long iegutId() {
        return id;
    }

    public double iegutSvaru() {
        return svars;
    }

    public String iegutDatumu() {
        return datums;
    }

    public double iegutKmi() {
        return kmi;
    }

    public String iegutFotoCelu() {
        return fotoCels;
    }
}
