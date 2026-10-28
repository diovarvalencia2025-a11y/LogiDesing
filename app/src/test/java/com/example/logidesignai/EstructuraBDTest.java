package com.example.logidesignai;

import org.junit.Test;
import com.example.logidesignai.data.Estructura_BD;

import static org.junit.Assert.*;

public class EstructuraBDTest {

    @Test
    public void testNombresDeTablas() {
        assertEquals("Conversaciones", Estructura_BD.EntryConversacion.TABLE_NAME);
        assertEquals("Mensajes", Estructura_BD.EntryMensaje.TABLE_NAME);
        assertEquals("Historial", Estructura_BD.EntryHistorial.TABLE_NAME);
    }

    @Test
    public void testColumnasConversacion() {
        assertEquals("titulo", Estructura_BD.EntryConversacion.COLUMN_TITULO);
        assertEquals("fecha", Estructura_BD.EntryConversacion.COLUMN_FECHA);
        assertEquals("nicho", Estructura_BD.EntryConversacion.COLUMN_NICHO);
        assertEquals("nombre", Estructura_BD.EntryConversacion.COLUMN_NOMBRE);
        assertEquals("telefono", Estructura_BD.EntryConversacion.COLUMN_TELEFONO);
        assertEquals("direccion", Estructura_BD.EntryConversacion.COLUMN_DIRECCION);
        assertEquals("servicios", Estructura_BD.EntryConversacion.COLUMN_SERVICIOS);
        assertEquals("is_web_ready", Estructura_BD.EntryConversacion.COLUMN_WEB_READY);
    }

    @Test
    public void testColumnasMensajes() {
        assertEquals("conversacion_id", Estructura_BD.EntryMensaje.COLUMN_CONVERSACION_ID);
        assertEquals("texto", Estructura_BD.EntryMensaje.COLUMN_TEXTO);
        assertEquals("tipo", Estructura_BD.EntryMensaje.COLUMN_TIPO);
        assertEquals("fecha", Estructura_BD.EntryMensaje.COLUMN_FECHA);
        assertEquals("is_web_ready", Estructura_BD.EntryMensaje.COLUMN_WEB_READY);
        assertEquals("nicho", Estructura_BD.EntryMensaje.COLUMN_NICHO);
        assertEquals("nombre", Estructura_BD.EntryMensaje.COLUMN_NOMBRE);
        assertEquals("telefono", Estructura_BD.EntryMensaje.COLUMN_TELEFONO);
        assertEquals("direccion", Estructura_BD.EntryMensaje.COLUMN_DIRECCION);
        assertEquals("servicios", Estructura_BD.EntryMensaje.COLUMN_SERVICIOS);
    }

    @Test
    public void testSentenciasSQLCreacionTablas() {
        String sqlConv = Estructura_BD.SQL_CREATE_CONVERSACIONES;
        assertNotNull(sqlConv);
        assertTrue(sqlConv.contains("CREATE TABLE " + Estructura_BD.EntryConversacion.TABLE_NAME));
        assertTrue(sqlConv.contains("INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(sqlConv.contains(Estructura_BD.EntryConversacion.COLUMN_TITULO));
        assertTrue(sqlConv.contains(Estructura_BD.EntryConversacion.COLUMN_FECHA));

        String sqlMsg = Estructura_BD.SQL_CREATE_MENSAJES;
        assertNotNull(sqlMsg);
        assertTrue(sqlMsg.contains("CREATE TABLE " + Estructura_BD.EntryMensaje.TABLE_NAME));
        assertTrue(sqlMsg.contains(Estructura_BD.EntryMensaje.COLUMN_CONVERSACION_ID));
        assertTrue(sqlMsg.contains(Estructura_BD.EntryMensaje.COLUMN_TEXTO));
    }
}
