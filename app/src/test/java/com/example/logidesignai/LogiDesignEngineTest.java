package com.example.logidesignai;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas unitarias completas para el motor de lógica conversacional de LogiDesign AI.
 * Valida el flujo conversacional, extracción de parámetros, filtros y disparador de creación web.
 */
public class LogiDesignEngineTest {

    private LogiDesignEngine engine;

    @Before
    public void setUp() {
        engine = new LogiDesignEngine();
    }

    @Test
    public void testEstadoInicial() {
        assertNotNull(engine);
        assertEquals("", engine.getCurrentNiche());
        assertEquals("", engine.getCurrentNombre());
        assertEquals("", engine.getCurrentTelefono());
        assertEquals("", engine.getCurrentDireccion());
        assertEquals("", engine.getCurrentServicios());
        assertFalse(engine.isWebCreated());
    }

    @Test
    public void testSaludoInicial() {
        String respNull = engine.processMessage(null);
        assertTrue(respNull.contains("¡Hola Diovar!"));

        String respVacio = engine.processMessage("   ");
        assertTrue(respVacio.contains("¡Hola Diovar!"));

        String respHola = engine.processMessage("Hola, buenas tardes");
        assertTrue(respHola.toLowerCase().contains("hola") || respHola.toLowerCase().contains("hacer hoy"));
    }

    @Test
    public void testRechazoTemasAjenos() {
        String respChiste = engine.processMessage("Cuéntame un chiste de pepito");
        assertTrue(respChiste.contains("exclusivamente") || respChiste.contains("páginas web"));

        String respClima = engine.processMessage("¿Qué clima hace hoy en Madrid?");
        assertTrue(respClima.contains("exclusivamente") || respClima.contains("páginas web"));

        String respReceta = engine.processMessage("Dame una receta de pasta carbonara");
        assertTrue(respReceta.contains("exclusivamente") || respReceta.contains("páginas web"));
    }

    @Test
    public void testDeteccionNicho() {
        String resp = engine.processMessage("Quiero crear una página web para un restaurante");
        assertEquals("restaurantes", engine.getCurrentNiche());
        assertTrue(resp.toLowerCase().contains("nombre"));

        engine.reset();
        engine.processMessage("Necesito una web para mi taller mecánico");
        assertEquals("taller", engine.getCurrentNiche());

        engine.reset();
        engine.processMessage("Quiero diseñar una web para una clínica dental");
        assertEquals("dental", engine.getCurrentNiche());
    }

    @Test
    public void testValidacionNombreNoConfundirConTelefono() {
        engine.setCurrentNiche("taller");
        // El usuario envía un teléfono cuando se le pide el nombre
        String resp = engine.processMessage("642430195");
        assertTrue(resp.toLowerCase().contains("teléfono") || resp.toLowerCase().contains("nombre"));
        assertEquals("", engine.getCurrentNombre());

        // Ahora envía un nombre real
        engine.processMessage("Taller Mecánico El Motor");
        assertEquals("Taller Mecánico El Motor", engine.getCurrentNombre());
    }

    @Test
    public void testExtraccionTelefono() {
        engine.setCurrentNiche("reformas");
        engine.setCurrentNombre("Reformas Integrales Express");

        engine.processMessage("+34 654 987 321");
        assertTrue(engine.getCurrentTelefono().contains("654987321") || engine.getCurrentTelefono().contains("654"));
    }

    @Test
    public void testOmitirDireccionYServicios() {
        engine.setCurrentNiche("veterinaria");
        engine.setCurrentNombre("Clínica San Antón");
        engine.setCurrentTelefono("944123456");

        // Omitir dirección
        engine.processMessage("omitir");
        assertFalse(engine.getCurrentDireccion().isEmpty());

        // Omitir servicios
        String respFinal = engine.processMessage("omitir");
        assertTrue(engine.isWebCreated());
        assertTrue(respFinal.contains("[LOGIDESIGN_WEB_READY:"));
        assertTrue(respFinal.contains("veterinaria|Clínica San Antón|944123456|"));
    }

    @Test
    public void testFlujoCompletoExitoso() {
        // Paso 1: Nicho
        engine.processMessage("Quiero hacer una web para un despacho de abogados");
        assertEquals("abogados", engine.getCurrentNiche());

        // Paso 2: Nombre
        engine.processMessage("Bufete Jurídico Valencia");
        assertEquals("Bufete Jurídico Valencia", engine.getCurrentNombre());

        // Paso 3: Teléfono
        engine.processMessage("600 11 22 33");
        assertEquals("600 11 22 33", engine.getCurrentTelefono());

        // Paso 4: Dirección
        engine.processMessage("Gran Vía 45, Planta 2, Bilbao");
        assertEquals("Gran Vía 45, Planta 2, Bilbao", engine.getCurrentDireccion());

        // Paso 5: Servicios
        String respuestaFinal = engine.processMessage("Derecho Civil, Laboral y Penal");
        assertEquals("Derecho Civil, Laboral y Penal", engine.getCurrentServicios());

        // Verificación de estado final y disparador
        assertTrue(engine.isWebCreated());
        assertTrue(respuestaFinal.contains("[LOGIDESIGN_WEB_READY:abogados|Bufete Jurídico Valencia|600 11 22 33|Gran Vía 45, Planta 2, Bilbao|Derecho Civil, Laboral y Penal]"));
    }

    @Test
    public void testResetLimpiaTodosLosCampos() {
        engine.setCurrentNiche("restaurantes");
        engine.setCurrentNombre("Asador El Roble");
        engine.setCurrentTelefono("611222333");
        engine.setCurrentDireccion("Calle Mayor 10");
        engine.setCurrentServicios("Carnes a la brasa, Menú del día");
        engine.setWebCreated(true);

        engine.reset();

        assertEquals("", engine.getCurrentNiche());
        assertEquals("", engine.getCurrentNombre());
        assertEquals("", engine.getCurrentTelefono());
        assertEquals("", engine.getCurrentDireccion());
        assertEquals("", engine.getCurrentServicios());
        assertFalse(engine.isWebCreated());
    }
}
