package com.example.logidesignai.data;

import android.provider.BaseColumns;

public final class Estructura_BD {

    private Estructura_BD() {}

    // Tabla 1: Conversaciones (Sesiones de chat independientes)
    public static class EntryConversacion implements BaseColumns {
        public static final String TABLE_NAME = "Conversaciones";
        public static final String COLUMN_TITULO = "titulo";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_NICHO = "nicho";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_TELEFONO = "telefono";
        public static final String COLUMN_DIRECCION = "direccion";
        public static final String COLUMN_SERVICIOS = "servicios";
        public static final String COLUMN_WEB_READY = "is_web_ready";
    }

    // Tabla 2: Mensajes (Burbujas asociadas a una conversacion especifica)
    public static class EntryMensaje implements BaseColumns {
        public static final String TABLE_NAME = "Mensajes";
        public static final String COLUMN_CONVERSACION_ID = "conversacion_id";
        public static final String COLUMN_TEXTO = "texto";
        public static final String COLUMN_TIPO = "tipo";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_WEB_READY = "is_web_ready";
        public static final String COLUMN_NICHO = "nicho";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_TELEFONO = "telefono";
        public static final String COLUMN_DIRECCION = "direccion";
        public static final String COLUMN_SERVICIOS = "servicios";
    }

    // Compatibilidad con la tabla anterior
    public static class EntryHistorial implements BaseColumns {
        public static final String TABLE_NAME = "Historial";
        public static final String COLUMN_TEXTO = "texto";
        public static final String COLUMN_TIPO = "tipo";
        public static final String COLUMN_FECHA = "fecha";
    }

    public static final String SQL_CREATE_CONVERSACIONES =
            "CREATE TABLE " + EntryConversacion.TABLE_NAME + " (" +
                    EntryConversacion._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EntryConversacion.COLUMN_TITULO + " TEXT NOT NULL, " +
                    EntryConversacion.COLUMN_FECHA + " INTEGER NOT NULL, " +
                    EntryConversacion.COLUMN_NICHO + " TEXT, " +
                    EntryConversacion.COLUMN_NOMBRE + " TEXT, " +
                    EntryConversacion.COLUMN_TELEFONO + " TEXT, " +
                    EntryConversacion.COLUMN_DIRECCION + " TEXT, " +
                    EntryConversacion.COLUMN_SERVICIOS + " TEXT, " +
                    EntryConversacion.COLUMN_WEB_READY + " INTEGER DEFAULT 0)";

    public static final String SQL_CREATE_MENSAJES =
            "CREATE TABLE " + EntryMensaje.TABLE_NAME + " (" +
                    EntryMensaje._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EntryMensaje.COLUMN_CONVERSACION_ID + " INTEGER NOT NULL, " +
                    EntryMensaje.COLUMN_TEXTO + " TEXT NOT NULL, " +
                    EntryMensaje.COLUMN_TIPO + " INTEGER NOT NULL, " +
                    EntryMensaje.COLUMN_FECHA + " INTEGER NOT NULL, " +
                    EntryMensaje.COLUMN_WEB_READY + " INTEGER DEFAULT 0, " +
                    EntryMensaje.COLUMN_NICHO + " TEXT, " +
                    EntryMensaje.COLUMN_NOMBRE + " TEXT, " +
                    EntryMensaje.COLUMN_TELEFONO + " TEXT, " +
                    EntryMensaje.COLUMN_DIRECCION + " TEXT, " +
                    EntryMensaje.COLUMN_SERVICIOS + " TEXT)";

    public static final String SQL_CREATE_ENTRIES = SQL_CREATE_CONVERSACIONES + "; " + SQL_CREATE_MENSAJES;

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + EntryMensaje.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + EntryConversacion.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + EntryHistorial.TABLE_NAME;
}
