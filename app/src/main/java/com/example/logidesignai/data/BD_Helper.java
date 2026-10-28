package com.example.logidesignai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BD_Helper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "BD_LogiDesignAI.db";

    public BD_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Estructura_BD.SQL_CREATE_CONVERSACIONES);
        db.execSQL(Estructura_BD.SQL_CREATE_MENSAJES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Estructura_BD.EntryMensaje.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Estructura_BD.EntryConversacion.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Estructura_BD.EntryHistorial.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
