package com.example.logidesignai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.logidesignai.ChatMessage;
import com.example.logidesignai.ConversationItem;

import java.util.ArrayList;
import java.util.List;

public class HistorialDao {

    private final BD_Helper dbHelper;

    public HistorialDao(Context context) {
        this.dbHelper = new BD_Helper(context);
    }

    // Gestion de conversaciones
    public long crearConversacion(String titulo, String nicho) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Estructura_BD.EntryConversacion.COLUMN_TITULO, (titulo != null && !titulo.isEmpty()) ? titulo : "Nueva conversacion");
        values.put(Estructura_BD.EntryConversacion.COLUMN_FECHA, System.currentTimeMillis());
        values.put(Estructura_BD.EntryConversacion.COLUMN_NICHO, nicho);
        values.put(Estructura_BD.EntryConversacion.COLUMN_WEB_READY, 0);

        long id = db.insert(Estructura_BD.EntryConversacion.TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public void actualizarConversacion(long id, String titulo, String nicho, String nombre, String telefono, String direccion, String servicios, boolean isWebReady) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (titulo != null && !titulo.isEmpty()) {
            values.put(Estructura_BD.EntryConversacion.COLUMN_TITULO, titulo);
        }
        if (nicho != null) values.put(Estructura_BD.EntryConversacion.COLUMN_NICHO, nicho);
        if (nombre != null) values.put(Estructura_BD.EntryConversacion.COLUMN_NOMBRE, nombre);
        if (telefono != null) values.put(Estructura_BD.EntryConversacion.COLUMN_TELEFONO, telefono);
        if (direccion != null) values.put(Estructura_BD.EntryConversacion.COLUMN_DIRECCION, direccion);
        if (servicios != null) values.put(Estructura_BD.EntryConversacion.COLUMN_SERVICIOS, servicios);
        values.put(Estructura_BD.EntryConversacion.COLUMN_WEB_READY, isWebReady ? 1 : 0);
        values.put(Estructura_BD.EntryConversacion.COLUMN_FECHA, System.currentTimeMillis());

        db.update(Estructura_BD.EntryConversacion.TABLE_NAME, values, Estructura_BD.EntryConversacion._ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void actualizarTitulo(long id, String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Estructura_BD.EntryConversacion.COLUMN_TITULO, titulo.trim());
        db.update(Estructura_BD.EntryConversacion.TABLE_NAME, values, Estructura_BD.EntryConversacion._ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void actualizarTimestamp(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Estructura_BD.EntryConversacion.COLUMN_FECHA, System.currentTimeMillis());
        db.update(Estructura_BD.EntryConversacion.TABLE_NAME, values, Estructura_BD.EntryConversacion._ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<ConversationItem> obtenerConversaciones(String filtro) {
        List<ConversationItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;
        if (filtro != null && !filtro.trim().isEmpty()) {
            selection = Estructura_BD.EntryConversacion.COLUMN_TITULO + " LIKE ?";
            selectionArgs = new String[]{"%" + filtro.trim() + "%"};
        }

        String orden = Estructura_BD.EntryConversacion.COLUMN_FECHA + " DESC, " + Estructura_BD.EntryConversacion._ID + " DESC";

        Cursor cursor = db.query(
                Estructura_BD.EntryConversacion.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orden
        );

        if (cursor != null) {
            int idIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion._ID);
            int titIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_TITULO);
            int fecIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_FECHA);
            int nicIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_NICHO);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIdx);
                String titulo = cursor.getString(titIdx);
                long fecha = cursor.getLong(fecIdx);
                String nicho = cursor.getString(nicIdx);

                // Obtener el ultimo mensaje de la conversacion para previsualizar si se desea
                lista.add(new ConversationItem(id, titulo, fecha, nicho, ""));
            }
            cursor.close();
        }
        db.close();
        return lista;
    }

    public ConversationItem obtenerConversacion(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ConversationItem item = null;

        Cursor cursor = db.query(
                Estructura_BD.EntryConversacion.TABLE_NAME,
                null,
                Estructura_BD.EntryConversacion._ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int titIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_TITULO);
                int fecIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_FECHA);
                int nicIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryConversacion.COLUMN_NICHO);

                String titulo = cursor.getString(titIdx);
                long fecha = cursor.getLong(fecIdx);
                String nicho = cursor.getString(nicIdx);

                item = new ConversationItem(id, titulo, fecha, nicho, "");
            }
            cursor.close();
        }
        db.close();
        return item;
    }

    public void borrarConversacion(long conversacionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Estructura_BD.EntryMensaje.TABLE_NAME, Estructura_BD.EntryMensaje.COLUMN_CONVERSACION_ID + " = ?", new String[]{String.valueOf(conversacionId)});
        db.delete(Estructura_BD.EntryConversacion.TABLE_NAME, Estructura_BD.EntryConversacion._ID + " = ?", new String[]{String.valueOf(conversacionId)});
        db.close();
    }

    // Gestion de mensajes
    public long insertarMensaje(long conversacionId, ChatMessage mensaje) {
        if (conversacionId <= 0) {
            String primerTitulo = (mensaje.getText() != null && mensaje.getText().length() > 30)
                    ? mensaje.getText().substring(0, 30) + "..."
                    : mensaje.getText();
            conversacionId = crearConversacion(primerTitulo, mensaje.getNiche());
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Estructura_BD.EntryMensaje.COLUMN_CONVERSACION_ID, conversacionId);
        values.put(Estructura_BD.EntryMensaje.COLUMN_TEXTO, mensaje.getText());
        values.put(Estructura_BD.EntryMensaje.COLUMN_TIPO, mensaje.getType());
        values.put(Estructura_BD.EntryMensaje.COLUMN_FECHA, System.currentTimeMillis());
        values.put(Estructura_BD.EntryMensaje.COLUMN_WEB_READY, mensaje.isWebsiteReady() ? 1 : 0);
        values.put(Estructura_BD.EntryMensaje.COLUMN_NICHO, mensaje.getNiche());
        values.put(Estructura_BD.EntryMensaje.COLUMN_NOMBRE, mensaje.getBusinessName());
        values.put(Estructura_BD.EntryMensaje.COLUMN_TELEFONO, mensaje.getPhone());
        values.put(Estructura_BD.EntryMensaje.COLUMN_DIRECCION, mensaje.getAddress());
        values.put(Estructura_BD.EntryMensaje.COLUMN_SERVICIOS, mensaje.getServices());

        long nuevoId = db.insert(Estructura_BD.EntryMensaje.TABLE_NAME, null, values);

        // Actualizar fecha de la conversacion
        ContentValues updateConv = new ContentValues();
        updateConv.put(Estructura_BD.EntryConversacion.COLUMN_FECHA, System.currentTimeMillis());
        if (mensaje.isWebsiteReady()) {
            updateConv.put(Estructura_BD.EntryConversacion.COLUMN_WEB_READY, 1);
            if (mensaje.getNiche() != null) updateConv.put(Estructura_BD.EntryConversacion.COLUMN_NICHO, mensaje.getNiche());
            if (mensaje.getBusinessName() != null) {
                updateConv.put(Estructura_BD.EntryConversacion.COLUMN_NOMBRE, mensaje.getBusinessName());
                updateConv.put(Estructura_BD.EntryConversacion.COLUMN_TITULO, "Web: " + mensaje.getBusinessName());
            }
            if (mensaje.getPhone() != null) updateConv.put(Estructura_BD.EntryConversacion.COLUMN_TELEFONO, mensaje.getPhone());
            if (mensaje.getAddress() != null) updateConv.put(Estructura_BD.EntryConversacion.COLUMN_DIRECCION, mensaje.getAddress());
            if (mensaje.getServices() != null) updateConv.put(Estructura_BD.EntryConversacion.COLUMN_SERVICIOS, mensaje.getServices());
        }
        db.update(Estructura_BD.EntryConversacion.TABLE_NAME, updateConv, Estructura_BD.EntryConversacion._ID + " = ?", new String[]{String.valueOf(conversacionId)});

        db.close();
        return nuevoId;
    }

    public List<ChatMessage> obtenerMensajesPorConversacion(long conversacionId) {
        List<ChatMessage> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                Estructura_BD.EntryMensaje.TABLE_NAME,
                null,
                Estructura_BD.EntryMensaje.COLUMN_CONVERSACION_ID + " = ?",
                new String[]{String.valueOf(conversacionId)},
                null,
                null,
                Estructura_BD.EntryMensaje._ID + " ASC"
        );

        if (cursor != null) {
            int idIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje._ID);
            int txtIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_TEXTO);
            int tipIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_TIPO);
            int fecIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_FECHA);
            int webIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_WEB_READY);
            int nicIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_NICHO);
            int nomIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_NOMBRE);
            int telIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_TELEFONO);
            int dirIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_DIRECCION);
            int serIdx = cursor.getColumnIndexOrThrow(Estructura_BD.EntryMensaje.COLUMN_SERVICIOS);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIdx);
                String texto = cursor.getString(txtIdx);
                int tipo = cursor.getInt(tipIdx);
                long fecha = cursor.getLong(fecIdx);
                boolean isWebReady = cursor.getInt(webIdx) == 1;
                String nicho = cursor.getString(nicIdx);
                String nombre = cursor.getString(nomIdx);
                String telefono = cursor.getString(telIdx);
                String direccion = cursor.getString(dirIdx);
                String servicios = cursor.getString(serIdx);

                ChatMessage msg = new ChatMessage(conversacionId, texto, tipo);
                msg.setId(id);
                msg.setTimestamp(fecha);
                msg.setWebsiteReady(isWebReady);
                msg.setNiche(nicho);
                msg.setBusinessName(nombre);
                msg.setPhone(telefono);
                msg.setAddress(direccion);
                msg.setServices(servicios);

                lista.add(msg);
            }
            cursor.close();
        }
        db.close();
        return lista;
    }

    public long obtenerUltimaConversacionId() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long id = -1;
        Cursor cursor = db.query(
                Estructura_BD.EntryConversacion.TABLE_NAME,
                new String[]{Estructura_BD.EntryConversacion._ID},
                null,
                null,
                null,
                null,
                Estructura_BD.EntryConversacion.COLUMN_FECHA + " DESC, " + Estructura_BD.EntryConversacion._ID + " DESC",
                "1"
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0);
            }
            cursor.close();
        }
        db.close();
        return id;
    }

    // Metodos de compatibilidad
    public long insertarMensaje(ChatMessage mensaje) {
        long convId = mensaje.getConversationId();
        if (convId <= 0) {
            convId = obtenerUltimaConversacionId();
            if (convId <= 0) {
                String tit = (mensaje.getText() != null && mensaje.getText().length() > 25)
                        ? mensaje.getText().substring(0, 25) + "..."
                        : mensaje.getText();
                convId = crearConversacion(tit, mensaje.getNiche());
            }
            mensaje.setConversationId(convId);
        }
        return insertarMensaje(convId, mensaje);
    }

    public List<ChatMessage> obtenerHistorial() {
        long ultimaConvId = obtenerUltimaConversacionId();
        if (ultimaConvId > 0) {
            return obtenerMensajesPorConversacion(ultimaConvId);
        }
        return new ArrayList<>();
    }

    public int borrarHistorial() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int f1 = db.delete(Estructura_BD.EntryMensaje.TABLE_NAME, null, null);
        int f2 = db.delete(Estructura_BD.EntryConversacion.TABLE_NAME, null, null);
        db.close();
        return f1 + f2;
    }
}
