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
            return "¡Hola Diovar! 👋 ¿Qué quieres hacer hoy?";
        }

        String lower = userPrompt.toLowerCase().trim();

        // 1. Saludo natural puro
        if (esSaludoPuro(lower)) {
            return "¡Hola Diovar! 👋 ¿Qué quieres hacer hoy?";
        }

        // 2. Si el usuario pide algo ajeno a una página web
        if (esPeticionAjenaAWeb(lower)) {
            return "Disculpa, no puedo ayudarte con eso. Mi especialidad y único propósito es diseñar y crear páginas web profesionales para negocios locales. 😊\n\n¿Te gustaría que diseñemos una página web para tu negocio?";
        }

        // 3. Si el usuario ya terminó una web y pide otra o saluda
        if (isWebCreated && (lower.contains("otra") || lower.contains("nueva") || esSaludoPuro(lower))) {
            reset();
            return "¡Con mucho gusto! Vamos a crear una nueva página web desde cero. ✨\n\n" +
                    "¿De qué tipo de negocio te gustaría diseñar la nueva web?\n\n" +
                    "🔹 **Restaurantes / Bares**\n" +
                    "🔹 **Taller Mecánico**\n" +
                    "🔹 **Veterinaria**\n" +
                    "🔹 **Reformas & Obras**\n" +
                    "🔹 **Abogados & Legal**\n" +
                    "🔹 **Clínica Dental**";
        }

        String nichoDetectado = detectarNichoTexto(lower);

        // 4. Si el usuario envió un bloque con saltos de línea (entrada directa multilínea)
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
                return "🎉 ¡Excelente! He recibido y verificado los 4 parámetros clave de tu negocio:\n\n" +
                        "📍 **Nombre:** " + currentNombre + "\n" +
                        "📞 **Teléfono / WhatsApp:** " + currentTelefono + "\n" +
                        "🏠 **Dirección:** " + currentDireccion + "\n" +
                        "⭐ **Especialidades:** " + currentServicios + "\n\n" +
                        "Tu página web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido configurada con éxito. Pulsa el botón abajo para visualizarla y probarla:\n" +
                        "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
            }
        }

        // 5. Intención de crear web o especificar nicho (ej: 'una apgina web de dentista', 'web de taller')
        if (!nichoDetectado.isEmpty() && currentNiche.isEmpty()) {
            currentNiche = nichoDetectado;
            return "¡Excelente elección! Vamos a diseñar la página web profesional para tu **" + getNombreNichoAmigable(currentNiche) + "**. 🌟\n\n" +
                    "1️⃣ **¿Cuál es el nombre de tu negocio o marca?**\n" +
                    "(Por ejemplo: 'Restaurante El Rincón', 'Talleres Gómez')";
        }

        if (esPeticionCrearWeb(lower) && nichoDetectado.isEmpty() && currentNiche.isEmpty()) {
            return "¡Excelente! Me especializo en crear páginas web de alto impacto para negocios locales. 🚀\n\n" +
                    "Cuéntame, ¿de qué temática o negocio es la web? (Por ejemplo: Restaurantes, Taller Mecánico, Veterinaria, Reformas, Abogados o Clínica Dental)";
        }

        // Si no tenemos nicho seleccionado todavía, invitar a elegir nicho
        if (currentNiche.isEmpty()) {
            return "Disculpa, Diovar. En LogiDesign AI me dedico exclusivamente a diseñar y crear páginas web profesionales para negocios locales. 😊\n\n" +
                    "¿Para qué temática te gustaría diseñar tu página web hoy?\n\n" +
                    "🔹 **Restaurante / Bar**\n" +
                    "🔹 **Taller Mecánico**\n" +
                    "🔹 **Veterinaria**\n" +
                    "🔹 **Reformas & Obras**\n" +
                    "🔹 **Abogados & Legal**\n" +
                    "🔹 **Clínica Dental**";
        }

        // Flujo secuencial de preguntas
        // Nombre comercial
        if (currentNombre.isEmpty()) {
            // Verificar si el usuario proporcionó primero la dirección en lugar del nombre
            if (esDireccionTexto(userPrompt) && currentDireccion.isEmpty()) {
                currentDireccion = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                return "¡Dirección anotada! 🏠 Ahora, por favor indícame el **nombre comercial o marca** de tu negocio:";
            }

            // Verificar si el usuario proporcionó servicios primero
            if ((userPrompt.contains(",") || userPrompt.contains(" y ")) && currentServicios.isEmpty() && !esNombreNegocioValido(userPrompt)) {
                currentServicios = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                return "¡Servicios anotados! ⭐ Ahora, por favor indícame el **nombre comercial o marca** de tu negocio:";
            }

            String telDetectado = extraerTelefono(userPrompt);
            if (telDetectado != null && userPrompt.replaceAll("[0-9\\s+\\-().]", "").length() < 3) {
                if (currentTelefono.isEmpty()) {
                    currentTelefono = telDetectado;
                    return "¡Teléfono registrado! 📞 Ahora, por favor indícame el **nombre comercial o marca** de tu negocio:";
                }
                return "📍 Eso parece un número de teléfono. Para tu **" + getNombreNichoAmigable(currentNiche) + "**, por favor indícame primero el **nombre comercial o marca** de tu negocio:";
            }

            if (lower.equals(currentNiche) || lower.matches("^(restaurante|restaurantes|bar|taller|talleres|abogado|abogados|reforma|reformas|veterinaria|dental|dentista)$")) {
                return "📍 Ese es el tipo de negocio. ¿Cómo se llama tu local o empresa? (Por ejemplo: 'Restaurante El Rincón', 'Talleres Gómez'):";
            }

            if (esNombreNegocioValido(userPrompt)) {
                currentNombre = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                // Si el teléfono ya estaba presente (por orden alterno o entrada previa):
                if (!currentTelefono.isEmpty()) {
                    if (currentDireccion.isEmpty()) {
                        return "¡Nombre registrado, **" + currentNombre + "**! 🎉\n\n" +
                                "3️⃣ ¿Cuál es la **dirección física o ciudad** de tu negocio?\n" +
                                "*(Si trabajas a domicilio o prefieres no incluirla, escribe **'omitir'**).*";
                    } else if (currentServicios.isEmpty()) {
                        return "¡Nombre registrado, **" + currentNombre + "**! 🎉\n\n" +
                                "4️⃣ ¿Cuáles son tus **especialidades o servicios principales**?\n" +
                                "*(O escribe **'omitir'** para usar las recomendadas de " + getNombreNichoAmigable(currentNiche) + ").*";
                    } else {
                        aplicarFiltroAntiDuplicados();
                        isWebCreated = true;
                        return "🎉 ¡Excelente! He configurado la página web profesional para tu negocio:\n\n" +
                                "📍 **Nombre:** " + currentNombre + "\n" +
                                "📞 **Teléfono / WhatsApp:** " + currentTelefono + "\n" +
                                "🏠 **Dirección:** " + currentDireccion + "\n" +
                                "⭐ **Especialidades:** " + currentServicios + "\n\n" +
                                "Tu página web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con éxito. Pulsa el botón abajo para visualizarla y probarla:\n" +
                                "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
                    }
                }
                return "¡Genial, **" + currentNombre + "**! 🎉\n\n" +
                        "2️⃣ ¿A qué número de **teléfono o WhatsApp** podrán contactarte tus clientes?\n" +
                        "*(Este dato es obligatorio para configurar los botones de llamada y contacto de tu web)*";
            } else {
                return "Para tu **" + getNombreNichoAmigable(currentNiche) + "**, por favor indícame el **nombre comercial o marca** de tu negocio (ejemplo: 'Clínica Dental Basauri'):";
            }
        }

        // Teléfono o WhatsApp
        if (currentTelefono.isEmpty()) {
            String tel = extraerTelefono(userPrompt);
            if (tel != null) {
                currentTelefono = tel;
                if (!currentDireccion.isEmpty() && !currentServicios.isEmpty()) {
                    aplicarFiltroAntiDuplicados();
                    isWebCreated = true;
                    return "🎉 ¡Excelente! He configurado la página web profesional para tu negocio:\n\n" +
                            "📍 **Nombre:** " + currentNombre + "\n" +
                            "📞 **Teléfono / WhatsApp:** " + currentTelefono + "\n" +
                            "🏠 **Dirección:** " + currentDireccion + "\n" +
                            "⭐ **Especialidades:** " + currentServicios + "\n\n" +
                            "Tu página web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con éxito. Pulsa el botón abajo para visualizarla y probarla:\n" +
                            "[LOGIDESIGN_WEB_READY:" + currentNiche + "|" + currentNombre + "|" + currentTelefono + "|" + currentDireccion + "|" + currentServicios + "]";
                }
                return "¡Teléfono registrado con éxito! 📞 (**" + currentTelefono + "**)\n\n" +
                        "3️⃣ ¿Cuál es la **dirección física o ciudad** de tu negocio?\n" +
                        "*(Si trabajas a domicilio o prefieres no incluirla, escribe **'omitir'**).*";
            } else {
                return "📞 Para que los clientes puedan llamarte o escribirte por WhatsApp, el teléfono es indispensable.\n\n" +
                        "Por favor, facilítame un número de contacto válido para **" + currentNombre + "** (ejemplo: **600 123 456**):";
            }
        }

        // Dirección física o ciudad
        if (currentDireccion.isEmpty()) {
            if (esComandoOmitir(userPrompt)) {
                currentDireccion = getDireccionPorDefecto(currentNiche);
            } else {
                // Verificar que no sea un teléfono repetido ni el nombre
                String tel = extraerTelefono(userPrompt);
                if (tel != null && userPrompt.replaceAll("[0-9\\s+\\-().]", "").length() < 3) {
                    currentDireccion = getDireccionPorDefecto(currentNiche);
                } else if (userPrompt.trim().equalsIgnoreCase(currentNombre) || userPrompt.trim().equalsIgnoreCase(currentTelefono)) {
                    currentDireccion = getDireccionPorDefecto(currentNiche);
                } else {
                    currentDireccion = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                }
            }

            return "¡Dirección anotada! 🏠\n\n" +
                    "4️⃣ ¿Cuáles son tus **especialidades o servicios principales**?\n" +
                    "*(Por ejemplo: 'Menú diario, asados' o escribe **'omitir'** para usar las especialidades recomendadas de " + getNombreNichoAmigable(currentNiche) + ").*";
        }

        // Especialidades o servicios
        if (currentServicios.isEmpty()) {
            if (esComandoOmitir(userPrompt)) {
                currentServicios = getServiciosPorDefecto(currentNiche);
            } else {
                // Verificar que no sea el teléfono ni el nombre repetido
                if (userPrompt.trim().equalsIgnoreCase(currentNombre) || userPrompt.trim().equalsIgnoreCase(currentTelefono)) {
                    currentServicios = getServiciosPorDefecto(currentNiche);
                } else {
                    currentServicios = sanitizarTextoSeguro(limpiarPrefijo(userPrompt));
                }
            }
        }

        // Generación de la web
        aplicarFiltroAntiDuplicados();
        isWebCreated = true;

        return "🎉 ¡Excelente! He configurado la página web profesional para tu negocio:\n\n" +
                "📍 **Nombre:** " + currentNombre + "\n" +
                "📞 **Teléfono / WhatsApp:** " + currentTelefono + "\n" +
                "🏠 **Dirección:** " + currentDireccion + "\n" +
                "⭐ **Especialidades:** " + currentServicios + "\n\n" +
                "Tu página web para **" + getNombreNichoAmigable(currentNiche) + "** ha sido creada con éxito. Pulsa el botón abajo para visualizarla y probarla:\n" +
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

        // CASO 1: Exactamente 4 líneas limpias (Nombre, Teléfono, Dirección, Servicios)
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

        // CASO 2: Procesamiento secuencial inteligente de cada línea (con o sin etiquetas)
        for (String l : limpias) {
            String clean = limpiarPrefijo(l);
            if (clean.isEmpty() || esSaludoOGenerico(clean)) continue;
            String lower = clean.toLowerCase();

            // 1. Teléfono
            String tel = extraerTelefono(clean);
            if (tel != null && currentTelefono.isEmpty()) {
                currentTelefono = tel;
                continue;
            }

            // 2. Dirección física (con calle, avda, plaza o número de portal)
            if (lower.matches("^(3[.)\\-]|3️⃣|direcci[oó]n|ciudad|ubicaci[oó]n):?\\s*.*") || esDireccionTexto(clean)) {
                String val = clean.replaceFirst("^(?i)(3[.)\\-]|3️⃣|direcci[oó]n|ciudad|ubicaci[oó]n):?\\s*", "").trim();
                if (!val.isEmpty() && currentDireccion.isEmpty()) {
                    currentDireccion = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 3. Etiqueta explícita de Nombre
            if (lower.matches("^(1[.)\\-]|1️⃣|nombre|negocio|empresa|marca):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(1[.)\\-]|1️⃣|nombre|negocio|empresa|marca):?\\s*", "").trim();
                if (!val.isEmpty() && esNombreNegocioValido(val)) {
                    currentNombre = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 4. Etiqueta explícita de Servicios / Especialidades
            if (lower.matches("^(4[.)\\-]|4️⃣|servicios?|especialidades?|platos?):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(4[.)\\-]|4️⃣|servicios?|especialidades?|platos?):?\\s*", "").trim();
                if (!val.isEmpty()) {
                    currentServicios = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 5. Línea sin etiquetas:
            boolean pareceServicios = clean.contains(",") || clean.contains(" y ") || lower.matches(".*\\b(ortodoncia|ortodoncias|cirug[ií]a|cirugias|cirugías|implante|implantes|limpieza|blanqueamiento|pr[oó]tesis|revisi[oó]n|asado|asados|men[uú]|tapa|tapas|pizza|pizzas|reforma|reformas|fontaner[ií]a|electricidad|pintura|chapa|freno|frenos|aceite|neum[aá]tico|neumáticos|juicio|divorcio|herencia|laboral|penal|vacuna|vacunas|desparasitaci[oó]n|ecograf[ií]a)\\b.*");

            if (pareceServicios && currentServicios.isEmpty()) {
                currentServicios = sanitizarTextoSeguro(clean);
            }
            // Si falta el nombre y la línea es un nombre de negocio válido:
            else if (currentNombre.isEmpty() && esNombreNegocioValido(clean)) {
                currentNombre = sanitizarTextoSeguro(clean);
            }
            // Si falta la dirección y parece dirección:
            else if (currentDireccion.isEmpty() && (clean.matches(".*[0-9]+.*") || esDireccionTexto(clean))) {
                currentDireccion = sanitizarTextoSeguro(clean);
            }
            // En cualquier otro caso, si falta servicios y no coincide con el nombre:
            else if (currentServicios.isEmpty() && !clean.equalsIgnoreCase(currentNombre)) {
                currentServicios = sanitizarTextoSeguro(clean);
            }
        }
    }

    public String extraerTelefono(String text) {
        if (text == null) return null;
        // Permite prefijo opcional (+34 o 0034) y 9 dígitos con espacios, guiones o puntos
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

    public boolean esSaludoPuro(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().replaceAll("[,;.:¡!¿?]+", " ").trim().replaceAll("\\s+", " ");
        return lower.matches("^(hola|buenas|hey|saludos|buenos d[ií]as|buenas tardes|buenas noches)(\\s+(buenas tardes|buenas noches|buenos d[ií]as|qu[eé] tal|c[oó]mo est[aá]s?|buenas|amigo|diovar))?$")
                || lower.matches("^(qu[eé] tal|c[oó]mo est[aá]s?)$");
    }

    public boolean esPeticionAjenaAWeb(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim().replaceAll("^[¿¡?!\\s]+|[¿¡?!\\s]+$", "");
        // 1. Peticiones de búsqueda, investigación o consulta general
        if (lower.matches(".*\\b(busca|busques|buscar|buscame|informaci[oó]n|informacionn|investiga|investigar|averigua|averiguar|googlear|consulta|consultar|explica|cu[eé]ntame)\\b.*")) {
            return true;
        }
        // 2. Chistes, clima, recetas, tareas escolares, ocio
        if (lower.matches(".*\\b(chiste|chistes|broma|clima|tiempo|temperatura|receta|recetas|cocinar|tarea|tareas|matem[aá]ticas|ecuaci[oó]n|suma|resta|multiplica|divide|canci[oó]n|pel[ií]cula|noticia|noticias|f[uú]tbol|juego|adivinanza|poema|historia|cuento|traducci[oó]n|traduce)\\b.*")) {
            return true;
        }
        // 3. Preguntas conversacionales ajenas a webs ("quién es", "qué es", "cómo se hace", etc.)
        if (lower.matches("^(qui[eé]n|qui[eé]nes|qu[eé] es|c[oó]mo se hace|cu[aá]ndo|d[oó]nde est[aá]|por qu[eé]|cu[aá]l es el|cu[aá]l es la|dime la hora|dime el tiempo).*") 
                && !lower.contains("web") && !lower.contains("página") && !lower.contains("sitio")) {
            return true;
        }
        return false;
    }

    public boolean esNombreNegocioValido(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        if (lower.isEmpty() || lower.length() > 45) return false;

        // 1. Conceptos o menciones de páginas web / creación / apps (incluyendo erratas como 'apgina', 'pagna')
        if (lower.matches(".*\\b(web|p[aá]gina|apgina|pagna|pajina|sitio|diseño|diseña|crear|hacer|haz|app|aplicaci[oó]n)\\b.*")) {
            return false;
        }

        // 2. Frases que empiezan con artículos de petición, oraciones o preposiciones ("una de...", "para un...", "no sé...")
        if (lower.matches("^(un|una|el|la|los|las|de|para|quiero|necesito|busca|busques|buscar|dime|hazme|puedes|podr[ií]as|ayuda|ay[uú]dame|c[oó]mo|qu[eé]|qui[eé]n|d[oó]nde|por qu[eé]|para qu[eé]|no s[eé]|me gustar[ií]a|por favor|averigua|investiga|consulta|haz)(\\s+.*|$)")) {
            return false;
        }

        // 3. Nombres de nichos genéricos solos ("dentista", "restaurante", "abogados", etc.)
        if (lower.matches("^(dentista|dentistas|dental|odontolog[ií]a|restaurante|restaurantes|bar|bares|cafeter[ií]a|taller|talleres|mec[aá]nico|reformas|construcci[oó]n|abogado|abogados|legal|veterinaria|veterinario|cl[ií]nica)$")) {
            return false;
        }

        // 4. Frases con estructuras compuestas o comas indicando lista de servicios ("ortodoncias, cirugías")
        if (lower.contains(",") || lower.matches(".*\\b(que busques|que hagas|que me digas|una informaci[oó]n|porque|para que|no entiendo|estoy probando|de dentista|de restaurante|de taller|de abogado|de reformas|de veterinaria)\\b.*")) {
            return false;
        }

        // 5. Saludo o texto genérico
        if (esSaludoOGenerico(lower)) return false;

        return true;
    }

    public boolean esPeticionCrearWeb(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.matches(".*\\b(p[aá]gina|apgina|pagna|web|sitio|diseño web|crear web|hacer web|hacer una web|crear una web|hacer una p[aá]gina|crear una p[aá]gina|quiero una web|una web|una p[aá]gina|una apgina)\\b.*");
    }

    public boolean esSaludoOGenerico(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|buenos d[ií]as|buenas tardes|buenas noches|hey|qu[eé] tal|saludos|quiero hacer una p[aá]gina|quiero una web|hacer una web|crear web|necesito una web|ok|vale|est[aá] bien|comenzar|empezar|ayuda|ay[uú]dame).*");
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
        if (n == null) return "Restaurante & Gastronomía";
        switch (n) {
            case "taller": return "Taller Mecánico & Automoción";
            case "veterinaria": return "Clínica Veterinaria & Cuidado Animal";
            case "reformas": return "Reformas Integrales & Construcción";
            case "abogados": return "Despacho de Abogados & Asesoría Legal";
            case "dental": return "Clínica Dental & Odontología";
            case "restaurantes":
            default: return "Restaurante & Gastronomía";
        }
    }

    public String limpiarPrefijo(String s) {
        if (s == null) return "";
        return s.replaceFirst("^[1-4][.)\\-]?\\s*", "")
                .replaceFirst("^[-•*~–—]+\\s*", "")
                .replaceFirst("^(?i)(nombre|negocio|tel[eé]fono|whatsapp|direcci[oó]n|ciudad|servicios?|especialidades?|platos?):?\\s*", "")
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

        // 1. Prefijos y tipos de vías claros (calle, avda, plaza, polígono, etc.)
        if (lower.matches(".*\\b(calle|c/|c\\.|avda|av\\.|avenida|plaza|pza|paseo|poligono|polígono|crta|carretera|barrio|camino|urb|urbanizacion|urbanización|portal|piso)\\b.*")) {
            return true;
        }

        // 2. Nombre de vía que contenga un número de portal (ej: "Juan XXIII 6", "Abaroeta 10", "Mayor 12")
        if (lower.matches(".*\\b[a-záéíóúñ]+\\s+[0-9]{1,4}\\b.*") && !lower.matches(".*[0-9]{8,}.*")) {
            // Pero no si es un nombre comercial que empieza por Clínica, Restaurante, Taller, etc.
            if (!lower.startsWith("clínica") && !lower.startsWith("clinica") && !lower.startsWith("taller") && !lower.startsWith("restaurante")) {
                return true;
            }
        }

        return false;
    }

    public boolean esComandoOmitir(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim().replaceAll("[.!?,]+$", "");
        return lower.matches("^(omitir|omite|omito|saltar|salta|paso|pasar|siguiente|no|ninguno|ninguna|no tengo|no poner|nada|despu[eé]s)$");
    }

    public String getDireccionPorDefecto(String n) {
        if (n == null) return "Servicio local y a domicilio";
        switch (n.toLowerCase().trim()) {
            case "abogados":
            case "dental":
                return "Atención en despacho con cita previa";
            case "taller":
            case "reformas":
                return "Servicio local y a domicilio";
            case "veterinaria":
            case "restaurantes":
            default:
                return "Atención presencial en local";
        }
    }

    public String getServiciosPorDefecto(String n) {
        if (n == null) return "Servicios profesionales garantizados";
        switch (n.toLowerCase().trim()) {
            case "taller":
                return "Mecánica general, diagnosis electrónica y mantenimiento integral";
            case "veterinaria":
                return "Medicina general, vacunación, cirugías y urgencias";
            case "reformas":
                return "Reformas integrales, albañilería, fontanería y pintura";
            case "abogados":
                return "Asesoría jurídica, derecho civil, laboral y penal";
            case "dental":
                return "Odontología integral, ortodoncia, estética dental e implantes";
            case "restaurantes":
            default:
                return "Cocina de autor, menú del día y especialidades de temporada";
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
}
