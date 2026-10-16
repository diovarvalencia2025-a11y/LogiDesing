package com.example.logidesignai.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class BD_Helper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "BD_Historial.db";


    public BD_Helper(Context context) {super(context, DATABASE_NAME,null, DATABASE_VERSION);}

    public void onCreate(SQLiteDatabase db){db.execSQL(Estructura_BD.SQL_CREATE_ENTRIES);}

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

        db.execSQL(Estructura_BD.SQL_DELETE_ENTRIES);
        onCreate(db);

    }

public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        onUpgrade(db,oldVersion, newVersion);

}

}





