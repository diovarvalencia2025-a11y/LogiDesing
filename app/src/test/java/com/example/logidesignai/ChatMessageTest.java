package com.example.logidesignai;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas unitarias para el modelo de datos de mensajes ChatMessage.
 */
public class ChatMessageTest {

    @Test
    public void testCreacionMensajeUsuario() {
        ChatMessage msg = new ChatMessage("Hola, quiero una web", ChatMessage.TYPE_USER);
        assertEquals("Hola, quiero una web", msg.getText());
        assertEquals(ChatMessage.TYPE_USER, msg.getType());
        assertTrue(msg.getTimestamp() > 0);
        assertFalse(msg.isWebsiteReady());
    }

    @Test
    public void testCreacionMensajeIAConConversacion() {
        ChatMessage msg = new ChatMessage(42L, "¡Por supuesto! ¿Para qué tipo de negocio?", ChatMessage.TYPE_AI);
        assertEquals(42L, msg.getConversationId());
        assertEquals("¡Por supuesto! ¿Para qué tipo de negocio?", msg.getText());
        assertEquals(ChatMessage.TYPE_AI, msg.getType());
    }

    @Test
    public void testParametrosWebReady() {
        ChatMessage msg = new ChatMessage("Tu web está lista", ChatMessage.TYPE_AI);
        msg.setWebsiteReady(true);
        msg.setNiche("restaurantes");
        msg.setBusinessName("Restaurante Bilbao");
        msg.setPhone("+34 600 123 456");
        msg.setAddress("Calle Hurtado de Amézaga 12");
        msg.setServices("Pintxos, Paella, Carnes");

        assertTrue(msg.isWebsiteReady());
        assertEquals("restaurantes", msg.getNiche());
        assertEquals("Restaurante Bilbao", msg.getBusinessName());
        assertEquals("+34 600 123 456", msg.getPhone());
        assertEquals("Calle Hurtado de Amézaga 12", msg.getAddress());
        assertEquals("Pintxos, Paella, Carnes", msg.getServices());
    }

    @Test
    public void testSettersYGettersId() {
        ChatMessage msg = new ChatMessage("Texto", ChatMessage.TYPE_USER);
        msg.setId(101L);
        msg.setConversationId(55L);
        long fixedTime = 1729450000000L;
        msg.setTimestamp(fixedTime);

        assertEquals(101L, msg.getId());
        assertEquals(55L, msg.getConversationId());
        assertEquals(fixedTime, msg.getTimestamp());
    }
}
