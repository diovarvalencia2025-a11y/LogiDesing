package com.example.logidesignai.data;

import android.provider.BaseColumns;

public final class Estructura_BD {

    private Estructura_BD() {}

    public static class EntryHistorial implements BaseColumns {
        public static final String TABLE_NAME = "Historial";
        public static final String COLUMN_TEXTO = "texto";
        public static final String COLUMN_TIPO = "tipo";
        public static final String COLUMN_FECHA = "fecha";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + EntryHistorial.TABLE_NAME + " (" +
                    EntryHistorial._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EntryHistorial.COLUMN_TEXTO + " TEXT NOT NULL, " +
                    EntryHistorial.COLUMN_TIPO + " INTEGER NOT NULL, " +
                    EntryHistorial.COLUMN_FECHA + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + EntryHistorial.TABLE_NAME;
}
