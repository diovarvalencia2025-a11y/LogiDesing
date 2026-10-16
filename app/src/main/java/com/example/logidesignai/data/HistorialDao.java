package com.example.logidesignai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.logidesignai.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class HistorialDao {

    private final BD_Helper dbHelper;

    public HistorialDao(Context context) {
        this.dbHelper = new BD_Helper(context);
    }

    public long insertarMensaje(ChatMessage mensaje) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Estructura_BD.EntryHistorial.COLUMN_TEXTO, mensaje.getText());
        values.put(Estructura_BD.EntryHistorial.COLUMN_TIPO, mensaje.getType());

        long nuevoId = db.insert(Estructura_BD.EntryHistorial.TABLE_NAME, null, values);
        db.close();
        return nuevoId;
    }

    public List<ChatMessage> obtenerHistorial() {
        List<ChatMessage> listaMensajes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columnas = {
                Estructura_BD.EntryHistorial._ID,
                Estructura_BD.EntryHistorial.COLUMN_TEXTO,
                Estructura_BD.EntryHistorial.COLUMN_TIPO,
                Estructura_BD.EntryHistorial.COLUMN_FECHA
        };

        String orden = Estructura_BD.EntryHistorial._ID + " ASC";

        Cursor cursor = db.query(
                Estructura_BD.EntryHistorial.TABLE_NAME,
                columnas,
                null,
                null,
                null,
                null,
                orden
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int colTextoIndex = cursor.getColumnIndexOrThrow(Estructura_BD.EntryHistorial.COLUMN_TEXTO);
                int colTipoIndex = cursor.getColumnIndexOrThrow(Estructura_BD.EntryHistorial.COLUMN_TIPO);

                String texto = cursor.getString(colTextoIndex);
                int tipo = cursor.getInt(colTipoIndex);

                listaMensajes.add(new ChatMessage(texto, tipo));
            }
            cursor.close();
        }

        db.close();
        return listaMensajes;
    }

    public int borrarHistorial() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int filasBorradas = db.delete(Estructura_BD.EntryHistorial.TABLE_NAME, null, null);
        db.close();
        return filasBorradas;
    }
}
