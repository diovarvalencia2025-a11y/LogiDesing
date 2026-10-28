package com.example.logidesignai;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas unitarias para el modelo de conversación ConversationItem y el cálculo de tiempo relativo.
 */
public class ConversationItemTest {

    @Test
    public void testCreacionYGetters() {
        long now = System.currentTimeMillis();
        ConversationItem item = new ConversationItem(10L, "Web para Taller", now, "taller", "Último mensaje recibido");

        assertEquals(10L, item.getId());
        assertEquals("Web para Taller", item.getTitle());
        assertEquals(now, item.getTimestamp());
        assertEquals("taller", item.getNiche());
        assertEquals("Último mensaje recibido", item.getLastMessage());
    }

    @Test
    public void testTiempoRelativoAhora() {
        long now = System.currentTimeMillis();
        ConversationItem item = new ConversationItem(1L, "Conversación", now - 10000, "restaurantes", "Hola"); // 10 segundos atrás
        assertEquals("Ahora", item.getFormattedRelativeTime());
    }

    @Test
    public void testTiempoRelativoMinutos() {
        long now = System.currentTimeMillis();
        ConversationItem item = new ConversationItem(2L, "Conversación", now - (15 * 60 * 1000), "reformas", "Mensaje"); // 15 minutos atrás
        assertEquals("Hace 15 min", item.getFormattedRelativeTime());
    }

    @Test
    public void testTiempoRelativoHoras() {
        long now = System.currentTimeMillis();
        ConversationItem item1 = new ConversationItem(3L, "Conversación", now - (60 * 60 * 1000), "abogados", "Mensaje"); // 1 hora atrás
        assertEquals("Hace 1 hora", item1.getFormattedRelativeTime());

        ConversationItem item3 = new ConversationItem(4L, "Conversación", now - (3 * 60 * 60 * 1000), "abogados", "Mensaje"); // 3 horas atrás
        assertEquals("Hace 3 horas", item3.getFormattedRelativeTime());
    }

    @Test
    public void testTiempoRelativoDias() {
        long now = System.currentTimeMillis();
        ConversationItem itemAyer = new ConversationItem(5L, "Conversación", now - (24 * 60 * 60 * 1000), "veterinaria", "Mensaje");
        assertEquals("Ayer", itemAyer.getFormattedRelativeTime());

        ConversationItem itemDias = new ConversationItem(6L, "Conversación", now - (4 * 24 * 60 * 60 * 1000), "veterinaria", "Mensaje");
        assertEquals("Hace 4 días", itemDias.getFormattedRelativeTime());
    }

    @Test
    public void testTimestampInvalido() {
        ConversationItem itemCero = new ConversationItem(7L, "Conversación", 0, "dental", "Mensaje");
        assertEquals("", itemCero.getFormattedRelativeTime());
    }
}
