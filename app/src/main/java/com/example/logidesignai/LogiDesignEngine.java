package com.example.logidesignai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogiDesignEngine {

    private String currentNiche = "";
    private String currentNombre = "";
    private String currentTelefono = "";
    private String currentDireccion = "";
    private String currentServicios = "";
    private boolean isWebCreated = false;

    public LogiDesignEngine() {
        reset();
    }

    public void reset() {
        this.currentNiche = "";
        this.currentNombre = "";
        this.currentTelefono = "";
        this.currentDireccion = "";
        this.currentServicios = "";
        this.isWebCreated = false;
    }

    // Getters y Setters
    public String getCurrentNiche() { return currentNiche; }
    public void setCurrentNiche(String niche) { this.currentNiche = niche != null ? niche : ""; }

    public String getCurrentNombre() { return currentNombre; }
    public void setCurrentNombre(String nombre) { this.currentNombre = nombre != null ? nombre : ""; }

    public String getCurrentTelefono() { return currentTelefono; }
    public void setCurrentTelefono(String telefono) { this.currentTelefono = telefono != null ? telefono : ""; }

    public String getCurrentDireccion() { return currentDireccion; }
    public void setCurrentDireccion(String direccion) { this.currentDireccion = direccion != null ? direccion : ""; }

    public String getCurrentServicios() { return currentServicios; }
    public void setCurrentServicios(String servicios) { this.currentServicios = servicios != null ? servicios : ""; }

    public boolean isWebCreated() { return isWebCreated; }
    public void setWebCreated(boolean webCreated) { this.isWebCreated = webCreated; }

    public String processMessage(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "Hola Diovar! Que quieres hacer hoy?";
        }

        String lower = userPrompt.toLowerCase().trim();

        // 1. Saludo natural puro
        if (esSaludoPuro(lower)) {
            return "Hola Diovar! Que quieres hacer hoy?";
        }

        // 2. Si el usuario pide algo ajeno a una pagina web
        if (esPeticionAjenaAWeb(lower)) {
            return "Disculpa, no puedo ayudarte con eso. Mi especialidad y unico proposito es disenar y crear paginas web profesionales para negocios locales.\n\nTe gustaria que disenemos una pagina web para tu negocio?";
        }

        // 3. Si el usuario ya termino una web y pide otra o saluda
        if (isWebCreated && (lower.contains("otra") || lower.contains("nueva") || esSaludoPuro(lower))) {
            reset();
            return "Con mucho gusto! Vamos a crear una nueva pagina web desde cero.\n\n" +
                    "De que tipo de negocio te gustaria disenar la nueva web?\n\n" +
                    "- **Restaurantes / Bares**\n" +
                    "- **Taller Mecanico**\n" +
                    "- **Veterinaria**\n" +
                    "- **Reformas & Obras**\n" +
                    "- **Abogados & Legal**\n" +
                    "- **Clinica Dental**";
        }

        String nichoDetectado = detectarNichoTexto(lower);

        // 4. Si el usuario envio un bloque con saltos de linea (entrada directa multilinea)
        if (userPrompt.contains("\n")) {
            extraerDatosRiguroso(userPrompt);
            if (currentNiche.isEmpty()) {
                String nichoDeducido = detectarNichoTexto((currentNombre + " " + currentServicios + " " + userPrompt).toLowerCase());
                if (!nichoDeducido.isEmpty()) {
                    currentNiche = nichoDeducido;
                } else if (!nichoDetectado.isEmpty()) {
                    currentNiche = nichoDetectado;
                } else {
                    currentNiche = "restaurantes";
                }
            }
            if (!currentNombre.isEmpty() && !currentTelefono.isEmpty()) {
                aplicarFiltroAntiDuplicados();
                isWebCreated = true;
                return "Excelente! He recibido y verificado los 4 parametros clave de tu negocio:\n\n" +
                        "- **Nombre:** " + currentNombre + "\n" +
                        "- **Telefono / WhatsApp:** " + currentTelefono + "\n" +
                        "- **Direccion:** " + currentDireccion + "\n" +
                        "- **Especialidades:** " + currentServicios + "\n\n" +
                        "Tu pagina web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido configurada con exito. Pulsa el boton abajo para visualizarla y probarla:\n" +
                        "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
            }
        }

        // 5. Intencion de crear web o especificar nicho
        if (!nichoDetectado.isEmpty() && currentNiche.isEmpty()) {
            currentNiche = nichoDetectado;
            return "Excelente eleccion! Vamos a disenar la pagina web profesional para tu **" + getNombreNichoAmigable(currentNiche) + "**.\n\n" +
                    "1. **Cual es el nombre de tu negocio o marca?**\n" +
                    "(Por ejemplo: 'Restaurante El Rincon', 'Talleres Gomez')";
        }

        if (esPeticionCrearWeb(lower) && nichoDetectado.isEmpty() && currentNiche.isEmpty()) {
            return "Excelente! Me especializo en crear paginas web de alto impacto para negocios locales.\n\n" +
                    "Cuentame, de que tematica o negocio es la web? (Por ejemplo: Restaurantes, Taller Mecanico, Veterinaria, Reformas, Abogados o Clinica Dental)";
        }

        // Si no tenemos nicho seleccionado todavia, invitar a elegir nicho
        if (currentNiche.isEmpty()) {
            return "Disculpa, Diovar. En LogiDesign AI me dedico exclusivamente a disenar y crear paginas web profesionales para negocios locales.\n\n" +
                    "Para que tematica te gustaria disenar tu pagina web hoy?\n\n" +
                    "- **Restaurante / Bar**\n" +
                    "- **Taller Mecanico**\n" +
                    "- **Veterinaria**\n" +
                    "- **Reformas & Obras**\n" +
                    "- **Abogados & Legal**\n" +
                    "- **Clinica Dental**";
        }

        // Flujo secuencial de preguntas
        // Nombre comercial
        if (currentNombre.isEmpty()) {
            // Verificar si el usuario proporciono primero la direccion en lugar del nombre
            if (esDireccionTexto(userPrompt) && currentDireccion.isEmpty()) {
                currentDireccion = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                return "Direccion anotada! Ahora, por favor indicame el **nombre comercial o marca** de tu negocio:";
            }

            // Verificar si el usuario proporciono servicios primero
            if ((userPrompt.contains(",") || userPrompt.contains(" y ")) && currentServicios.isEmpty() && !esNombreNegocioValido(userPrompt)) {
                currentServicios = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                return "Servicios anotados! Ahora, por favor indicame el **nombre comercial o marca** de tu negocio:";
            }

            String telDetectado = extraerTelefono(userPrompt);
            if (telDetectado != null && userPrompt.replaceAll("[0-9\\s+\\-().]", "").length() < 3) {
                if (currentTelefono.isEmpty()) {
                    currentTelefono = telDetectado;
                    return "Telefono registrado! Ahora, por favor indicame el **nombre comercial o marca** de tu negocio:";
                }
                return "Eso parece un numero de telefono. Para tu **" + getNombreNichoAmigable(currentNiche) + "**, por favor indicame primero el **nombre comercial o marca** de tu negocio:";
            }

            if (lower.equals(currentNiche) || lower.matches("^(restaurante|restaurantes|bar|taller|talleres|abogado|abogados|reforma|reformas|veterinaria|dental|dentista)$")) {
                return "Ese es el tipo de negocio. Como se llama tu local o empresa? (Por ejemplo: 'Restaurante El Rincon', 'Talleres Gomez'):";
            }

            if (esNombreNegocioValido(userPrompt)) {
                currentNombre = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                if (!currentTelefono.isEmpty()) {
                    if (currentDireccion.isEmpty()) {
                        return "Nombre registrado, **" + currentNombre + "**!\n\n" +
                                "3. Cual es la **direccion fisica o ciudad** de tu negocio?\n" +
                                "*(Si trabajas a domicilio o prefieres no incluirla, escribe **'omitir'**).*";
                    } else if (currentServicios.isEmpty()) {
                        return "Nombre registrado, **" + currentNombre + "**!\n\n" +
                                "4. Cuales son tus **especialidades o servicios principales**?\n" +
                                "*(O escribe **'omitir'** para usar las recomendadas de " + getNombreNichoAmigable(currentNiche) + ").*";
                    } else {
                        aplicarFiltroAntiDuplicados();
                        isWebCreated = true;
                        return "Excelente! He configurado la pagina web profesional para tu negocio:\n\n" +
                                "- **Nombre:** " + currentNombre + "\n" +
                                "- **Telefono / WhatsApp:** " + currentTelefono + "\n" +
                                "- **Direccion:** " + currentDireccion + "\n" +
                                "- **Especialidades:** " + currentServicios + "\n\n" +
                                "Tu pagina web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con exito. Pulsa el boton abajo para visualizarla y probarla:\n" +
                                "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
                    }
                }
                return "Genial, **" + currentNombre + "**!\n\n" +
                        "2. A que numero de **telefono o WhatsApp** podran contactarte tus clientes?\n" +
                        "*(Este dato es obligatorio para configurar los botones de llamada y contacto de tu web)*";
            } else {
                return "Para tu **" + getNombreNichoAmigable(currentNiche) + "**, por favor indicame el **nombre comercial o marca** de tu negocio (ejemplo: 'Clinica Dental Basauri'):";
            }
        }

        // Telefono o WhatsApp
        if (currentTelefono.isEmpty()) {
            String tel = extraerTelefono(userPrompt);
            if (tel != null) {
                currentTelefono = tel;
                if (!currentDireccion.isEmpty() && !currentServicios.isEmpty()) {
                    aplicarFiltroAntiDuplicados();
                    isWebCreated = true;
                    return "Excelente! He configurado la pagina web profesional para tu negocio:\n\n" +
                            "- **Nombre:** " + currentNombre + "\n" +
                            "- **Telefono / WhatsApp:** " + currentTelefono + "\n" +
                            "- **Direccion:** " + currentDireccion + "\n" +
                            "- **Especialidades:** " + currentServicios + "\n\n" +
                            "Tu pagina web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con exito. Pulsa el boton abajo para visualizarla y probarla:\n" +
                            "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
                }
                return "Telefono registrado con exito! (**" + currentTelefono + "**)\n\n" +
                        "3. Cual es la **direccion fisica o ciudad** de tu negocio?\n" +
                        "*(Si trabajas a domicilio o prefieres no incluirla, escribe **'omitir'**).*";
            } else {
                return "Para que los clientes puedan llamarte o escribirte por WhatsApp, el telefono es indispensable.\n\n" +
                        "Por favor, facilitame un numero de contacto valido para **" + currentNombre + "** (ejemplo: **600 123 456**):";
            }
        }

        // Direccion fisica o ciudad
        if (currentDireccion.isEmpty()) {
            if (esComandoOmitir(userPrompt)) {
                currentDireccion = getDireccionPorDefecto(currentNiche);
            } else {
                String tel = extraerTelefono(userPrompt);
                if (tel != null && userPrompt.replaceAll("[0-9\\s+\\-().]", "").length() < 3) {
                    currentDireccion = getDireccionPorDefecto(currentNiche);
                } else if (userPrompt.trim().equalsIgnoreCase(currentNombre) || userPrompt.trim().equalsIgnoreCase(currentTelefono)) {
                    currentDireccion = getDireccionPorDefecto(currentNiche);
                } else {
                    currentDireccion = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                }
            }

            return "Direccion anotada!\n\n" +
                    "4. Cuales son tus **especialidades o servicios principales**?\n" +
                    "*(Por ejemplo: 'Menu diario, asados' o escribe **'omitir'** para usar las especialidades recomendadas de " + getNombreNichoAmigable(currentNiche) + ").*";
        }

        // Especialidades o servicios
        if (currentServicios.isEmpty()) {
            if (esComandoOmitir(userPrompt)) {
                currentServicios = getServiciosPorDefecto(currentNiche);
            } else {
                if (userPrompt.trim().equalsIgnoreCase(currentNombre) || userPrompt.trim().equalsIgnoreCase(currentTelefono)) {
                    currentServicios = getServiciosPorDefecto(currentNiche);
                } else {
                    currentServicios = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                }
            }
        }

        // Generacion de la web
        aplicarFiltroAntiDuplicados();
        isWebCreated = true;

        return "Excelente! He configurado la pagina web profesional para tu negocio:\n\n" +
                "- **Nombre:** " + currentNombre + "\n" +
                "- **Telefono / WhatsApp:** " + currentTelefono + "\n" +
                "- **Direccion:** " + currentDireccion + "\n" +
                "- **Especialidades:** " + currentServicios + "\n\n" +
                "Tu pagina web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con exito. Pulsa el boton abajo para visualizarla y probarla:\n" +
                "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
    }

    public void extraerDatosRiguroso(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String[] lineas = text.split("\n");
        List<String> limpias = new ArrayList<>();
        for (String l : lineas) {
            String t = l.trim();
            if (!t.isEmpty()) limpias.add(t);
        }
        if (limpias.isEmpty()) return;

        // CASO 1: Exactamente 4 lineas limpias (Nombre, Telefono, Direccion, Servicios)
        if (limpias.size() == 4) {
            String l0 = limpiarPrefijo(limpias.get(0));
            String l1 = limpiarPrefijo(limpias.get(1));
            String l2 = limpiarPrefijo(limpias.get(2));
            String l3 = limpiarPrefijo(limpias.get(3));

            String telDetectado = extraerTelefono(l1);
            if (telDetectado == null) telDetectado = extraerTelefono(l0);

            if (telDetectado != null && esNombreNegocioValido(l0)) {
                currentNombre = sanitizarTextoSeguro(l0);
                currentTelefono = telDetectado;
                currentDireccion = sanitizarTextoSeguro(l2);
                currentServicios = sanitizarTextoSeguro(l3);
                return;
            }
        }

        // CASO 2: Procesamiento secuencial inteligente de cada linea
        for (String l : limpias) {
            String clean = limpiarPrefijo(l);
            if (clean.isEmpty() || esSaludoOGenerico(clean)) continue;
            String lower = clean.toLowerCase();

            // 1. Telefono
            String tel = extraerTelefono(clean);
            if (tel != null && currentTelefono.isEmpty()) {
                currentTelefono = tel;
                continue;
            }

            // 2. Direccion fisica
            if (lower.matches("^(3[.)\\-]|direccion|ciudad|ubicacion):?\\s*.*") || esDireccionTexto(clean)) {
                String val = clean.replaceFirst("^(?i)(3[.)\\-]|direccion|ciudad|ubicacion):?\\s*", "").trim();
                if (!val.isEmpty() && currentDireccion.isEmpty()) {
                    currentDireccion = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 3. Etiqueta explicita de Nombre
            if (lower.matches("^(1[.)\\-]|nombre|negocio|empresa|marca):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(1[.)\\-]|nombre|negocio|empresa|marca):?\\s*", "").trim();
                if (!val.isEmpty() && esNombreNegocioValido(val)) {
                    currentNombre = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 4. Etiqueta explicita de Servicios / Especialidades
            if (lower.matches("^(4[.)\\-]|servicios?|especialidades?|platos?):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(4[.)\\-]|servicios?|especialidades?|platos?):?\\s*", "").trim();
                if (!val.isEmpty()) {
                    currentServicios = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 5. Linea sin etiquetas:
            boolean pareceServicios = clean.contains(",") || clean.contains(" y ") || lower.matches(".*\\b(ortodoncia|ortodoncias|cirugia|cirugias|implante|implantes|limpieza|blanqueamiento|protesis|revision|asado|asados|menu|tapa|tapas|pizza|pizzas|reforma|reformas|fontaneria|electricidad|pintura|chapa|freno|frenos|aceite|neumatico|neumaticos|juicio|divorcio|herencia|laboral|penal|vacuna|vacunas|desparasitacion|ecografia)\\b.*");

            if (pareceServicios && currentServicios.isEmpty()) {
                currentServicios = sanitizarTextoSeguro(clean);
            }
            else if (currentNombre.isEmpty() && esNombreNegocioValido(clean)) {
                currentNombre = sanitizarTextoSeguro(clean);
            }
            else if (currentDireccion.isEmpty() && (clean.matches(".*[0-9]+.*") || esDireccionTexto(clean))) {
                currentDireccion = sanitizarTextoSeguro(clean);
            }
            else if (currentServicios.isEmpty() && !clean.equalsIgnoreCase(currentNombre)) {
                currentServicios = sanitizarTextoSeguro(clean);
            }
        }
    }

    public String extraerTelefono(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("(\\+34\\s*|0034\\s*)?([6789][0-9\\s.-]{7,13}[0-9])");
        Matcher m = p.matcher(text.trim());
        if (m.find()) {
            String cand = m.group().trim();
            String digitsOnly = cand.replaceAll("[^0-9]", "");
            if (digitsOnly.length() == 9 || (digitsOnly.length() == 11 && digitsOnly.startsWith("34"))) {
                return cand;
            }
        }
        return null;
    }

    public String limpiarPrefijo(String s) {
        if (s == null) return "";
        return s.replaceFirst("^[1-4][.)\\-]?\\s*", "")
                .replaceFirst("^[-*~]+\\s*", "")
                .replaceFirst("^(?i)(nombre|negocio|telefono|whatsapp|direccion|ciudad|servicios?|especialidades?|platos?):?\\s*", "")
                .trim();
    }

    public String sanitizarTextoSeguro(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)<script.*?>.*?</script>", "")
                    .replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)on[a-z]+\\s*=", "")
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .trim();
    }

    public boolean esDireccionTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase().trim();

        if (lower.matches(".*\\b(calle|c/|c\\.|avda|av\\.|avenida|plaza|pza|paseo|poligono|crta|carretera|barrio|camino|urb|urbanizacion|portal|piso)\\b.*")) {
            return true;
        }

        if (lower.matches(".*\\b[a-z]+\\s+[0-9]{1,4}\\b.*") && !lower.matches(".*[0-9]{8,}.*")) {
            if (!lower.startsWith("clinica") && !lower.startsWith("taller") && !lower.startsWith("restaurante")) {
                return true;
            }
        }

        return false;
    }

    public boolean esComandoOmitir(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim().replaceAll("[.!?,]+$", "");
        return lower.matches("^(omitir|omite|omito|saltar|salta|paso|pasar|siguiente|no|ninguno|ninguna|no tengo|no poner|nada|despues)$");
    }

    public String getDireccionPorDefecto(String n) {
        if (n == null) return "Servicio local y a domicilio";
        switch (n.toLowerCase().trim()) {
            case "abogados":
            case "dental":
                return "Atencion en despacho con cita previa";
            case "taller":
            case "reformas":
                return "Servicio local y a domicilio";
            case "veterinaria":
            case "restaurantes":
            default:
                return "Atencion presencial en local";
        }
    }

    public String getServiciosPorDefecto(String n) {
        if (n == null) return "Servicios profesionales garantizados";
        switch (n.toLowerCase().trim()) {
            case "taller":
                return "Mecanica general, diagnosis electronica y mantenimiento integral";
            case "veterinaria":
                return "Medicina general, vacunacion, cirugias y urgencias";
            case "reformas":
                return "Reformas integrales, albanileria, fontaneria y pintura";
            case "abogados":
                return "Asesoria juridica, derecho civil, laboral y penal";
            case "dental":
                return "Odontologia integral, ortodoncia, estetica dental e implantes";
            case "restaurantes":
            default:
                return "Cocina de autor, menu del dia y especialidades de temporada";
        }
    }

    public void aplicarFiltroAntiDuplicados() {
        if (currentDireccion == null || currentDireccion.trim().isEmpty() 
                || currentDireccion.equalsIgnoreCase(currentTelefono) 
                || currentDireccion.equalsIgnoreCase(currentNombre)
                || currentDireccion.equalsIgnoreCase(currentNiche)) {
            currentDireccion = getDireccionPorDefecto(currentNiche);
        }
        if (currentServicios == null || currentServicios.trim().isEmpty() 
                || currentServicios.equalsIgnoreCase(currentTelefono) 
                || currentServicios.equalsIgnoreCase(currentNombre)
                || currentServicios.equalsIgnoreCase(currentNiche)) {
            currentServicios = getServiciosPorDefecto(currentNiche);
        }
    }

    public boolean esSaludoPuro(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().replaceAll("[,;.:!?]+", " ").trim().replaceAll("\\s+", " ");
        return lower.matches("^(hola|buenas|hey|saludos|buenos dias|buenas tardes|buenas noches)(\\s+(buenas tardes|buenas noches|buenos dias|que tal|como estas?|buenas|amigo|diovar))?$")
                || lower.matches("^(que tal|como estas?)$");
    }

    public boolean esPeticionAjenaAWeb(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim().replaceAll("^[?!\\s]+|[?!\\s]+$", "");
        if (lower.matches(".*\\b(busca|busques|buscar|buscame|informacion|informacionn|investiga|investigar|averigua|averiguar|googlear|consulta|consultar|explica|cuentame)\\b.*")) {
            return true;
        }
        if (lower.matches(".*\\b(chiste|chistes|broma|clima|tiempo|temperatura|receta|recetas|cocinar|tarea|tareas|matematicas|ecuacion|suma|resta|multiplica|divide|cancion|pelicula|noticia|noticias|futbol|juego|adivinanza|poema|historia|cuento|traduccion|traduce)\\b.*")) {
            return true;
        }
        if (lower.matches("^(quien|quienes|que es|como se hace|cuando|donde esta|por que|cual es el|cual es la|dime la hora|dime el tiempo).*") 
                && !lower.contains("web") && !lower.contains("pagina") && !lower.contains("sitio")) {
            return true;
        }
        return false;
    }

    public boolean esNombreNegocioValido(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        if (lower.isEmpty() || lower.length() > 45) return false;

        if (lower.matches(".*\\b(web|pagina|apgina|pagna|pajina|sitio|diseno|disena|crear|hacer|haz|app|aplicacion)\\b.*")) {
            return false;
        }

        if (lower.matches("^(un|una|el|la|los|las|de|para|quiero|necesito|busca|busques|buscar|dime|hazme|puedes|podrias|ayuda|ayudame|como|que|quien|donde|por que|para que|no se|me gustaria|por favor|averigua|investiga|consulta|haz)(\\s+.*|$)")) {
            return false;
        }

        if (lower.matches("^(dentista|dentistas|dental|odontologia|restaurante|restaurantes|bar|bares|cafeteria|taller|talleres|mecanico|reformas|construccion|abogado|abogados|legal|veterinaria|veterinario|clinica)$")) {
            return false;
        }

        if (lower.contains(",") || lower.matches(".*\\b(que busques|que hagas|que me digas|una informacion|porque|para que|no entiendo|estoy probando|de dentista|de restaurante|de taller|de abogado|de reformas|de veterinaria)\\b.*")) {
            return false;
        }

        if (esSaludoOGenerico(lower)) return false;

        return true;
    }

    public boolean esPeticionCrearWeb(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.matches(".*\\b(pagina|apgina|pagna|web|sitio|diseno web|crear web|hacer web|hacer una web|crear una web|hacer una pagina|crear una pagina|quiero una web|una web|una pagina|una apgina)\\b.*");
    }

    public boolean esSaludoOGenerico(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|buenos dias|buenas tardes|buenas noches|hey|que tal|saludos|quiero hacer una pagina|quiero una web|hacer una web|crear web|necesito una web|ok|vale|esta bien|comenzar|empezar|ayuda|ayudame).*");
    }

    public String detectarNichoTexto(String lower) {
        if (lower == null) return "";
        if (lower.contains("restauran") || lower.contains("bar") || lower.contains("comida") || lower.contains("pizza") || lower.contains("asado") || lower.contains("tapa") || lower.contains("cafeteria")) {
            return "restaurantes";
        } else if (lower.contains("taller") || lower.contains("mecanic") || lower.contains("coche") || lower.contains("auto") || lower.contains("motor") || lower.contains("chapa")) {
            return "taller";
        } else if (lower.contains("veterina") || lower.contains("mascota") || lower.contains("animal") || lower.contains("perro") || lower.contains("gato")) {
            return "veterinaria";
        } else if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construc") || lower.contains("pintor") || lower.contains("fontaner")) {
            return "reformas";
        } else if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho") || lower.contains("jurid") || lower.contains("bufete")) {
            return "abogados";
        } else if (lower.contains("dental") || lower.contains("dentist") || lower.contains("diente") || lower.contains("odontol") || lower.contains("sonrisa")) {
            return "dental";
        }
        return "";
    }

    public String getNombreNichoAmigable(String n) {
        if (n == null) return "Restaurante & Gastronomia";
        switch (n) {
            case "taller": return "Taller Mecanico & Automocion";
            case "veterinaria": return "Clinica Veterinaria & Cuidado Animal";
            case "reformas": return "Reformas Integrales & Construccion";
            case "abogados": return "Despacho de Abogados & Asesoria Legal";
            case "dental": return "Clinica Dental & Odontologia";
            case "restaurantes":
            default: return "Restaurante & Gastronomia";
        }
    }
}
