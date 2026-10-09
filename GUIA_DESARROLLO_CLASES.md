# 📘 LOGIDESIGN AI — MANUAL DE DESARROLLO Y GUÍA PARA CLASE

**Proyecto:** LogiDesign AI (Creador de Sitios Web para Negocios con Inteligencia Artificial)  
**Entorno:** Android Studio | Java | OkHttp | Ollama Local (`qwen2.5-coder:7b`) | Google Gemini (`gemini-3.8-flash`)  
**Autor:** Diovar Valencia  

---

## 📌 1. ESTADO ACTUAL (LO QUE YA ESTÁ LISTO Y FUNCIONANDO)

1. **Pantalla Principal y Chat Conversacional (`MainActivity.java`):**
   - Transición suave de la pantalla de bienvenida al chat de mensajes en tiempo real.
   - Burbujas de chat estilizadas (Usuario a la derecha en tono oscuro, IA a la izquierda con avatar).
   - Apertura automática del teclado al pulsar sobre la caja de entrada (`InputMethodManager`).
   - Botón de envío inteligente (`btnSend`): se activa y aparece automáticamente al escribir texto.

2. **Arquitectura Tri-Motor (Híbrida e Independiente):**
   - 🖥️ **Ollama Local (PC):** Conexión vía HTTP a `http://10.0.2.2:11434` usando tu modelo local `qwen2.5-coder:7b`. **100% gratis, privado y sin gastar saldo de APIs.**
   - ☁️ **Google Gemini Online:** Conexión oficial y directa a `gemini-3.8-flash` mediante llamadas REST HTTPS sin plataformas intermediarias.
   - 📱 **Modo Local (Offline Autónomo):** Motor de contingencia interno dentro de la app para que funcione incluso en Modo Avión o sin red Wi-Fi.

3. **Flujo de Negocio sin Tecnicismos:**
   - La IA pregunta primero la temática (*Restaurante, Taller Mecánico, Veterinaria, Reformas, Abogados*).
   - Luego pide de una sola vez los datos clave (*Nombre, Teléfono/WhatsApp, Dirección, Servicios*).
   - Confirma que la web está creada y personalizada (sin mencionar nunca las palabras *"demo"* o *"plantilla"*).

---

## 🚀 2. SIGUIENTE PASO: CONECTAR LAS PLANTILLAS DE PÁGINAS WEB

### Paso 2.1: Dónde guardar los archivos web en Android Studio
Crea la carpeta de recursos `assets` dentro de `app/src/main/`:
```text
app/src/main/assets/
└── templates/
    ├── restaurante/
    │   └── index.html
    ├── taller/
    │   └── index.html
    ├── veterinaria/
    │   └── index.html
    └── reformas/
        └── index.html
```

### Paso 2.2: Colocar etiquetas de reemplazo en cada archivo `index.html`
En el código HTML de cada demo, sustituye los textos fijos por marcadores variables:
- `{{NOMBRE_NEGOCIO}}` ➔ Título principal, logo y pie de página.
- `{{TELEFONO}}` ➔ Número visible en el encabezado.
- `{{ENLACE_WHATSAPP}}` ➔ Enlace directo del botón flotante (`https://wa.me/34...`).
- `{{DIRECCION}}` ➔ Dirección física del local.
- `{{SERVICIOS}}` ➔ Lista de especialidades o platos.

### Paso 2.3: Inyectar datos y mostrar en pantalla con WebView
Crea una clase auxiliar en Java para leer el archivo desde assets y reemplazar las variables:
```java
public class TemplateManager {
    public static String cargarYPersonalizar(Context context, String rutaTemplate, 
                                             String nombre, String telefono, String direccion) {
        try {
            InputStream is = context.getAssets().open(rutaTemplate);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            
            String html = new String(buffer, "UTF-8");
            html = html.replace("{{NOMBRE_NEGOCIO}}", nombre)
                       .replace("{{TELEFONO}}", telefono)
                       .replace("{{DIRECCION}}", direccion);
            return html;
        } catch (Exception e) {
            return "<html><body>Error al cargar plantilla: " + e.getMessage() + "</body></html>";
        }
    }
}
```

Para previsualizar la web en pantalla:
```java
WebView webView = findViewById(R.id.miWebView);
webView.getSettings().setJavaScriptEnabled(true);
webView.loadDataWithBaseURL("file:///android_asset/templates/restaurante/", htmlPersonalizado, "text/html", "UTF-8", null);
```

---

## 🗂️ 3. SIGUIENTE PASO: HISTORIAL DE CHATS (`Chat.java`)

Actualmente la pantalla `Chat.java` tiene el diseño visual de la lista (`activity_chat.xml`).
Para conectarla en clase:

1. **Almacenamiento Local (SQLite o SharedPreferences con Gson):**
   - Guardar cada conversación con: `id`, `titulo` (ej: *"Pizzería Don Juan"*), `fecha` y lista de mensajes.
2. **Cargar en la lista:**
   - En `Chat.java`, asociar un `RecyclerView` que liste las conversaciones guardadas.
3. **Botón Flotante `[+ Nuevo chat]`:**
   - Ya configurado en el layout; al pulsarlo, debe abrir `MainActivity` con el chat limpio para iniciar un nuevo proyecto.

---

## 💼 4. SIGUIENTE PASO: GESTIÓN DE PROYECTOS (`ProjectsActivity.java`)

En `ProjectsActivity.java`:
1. Mostrar una lista tipo tarjetas de las páginas webs ya creadas por el usuario.
2. Cada tarjeta tendrá:
   - Nombre de la empresa.
   - Categoría (Restaurante, Taller, etc.).
   - Botón **"Ver Web"**: abre el WebView con el diseño completo.
   - Botón **"Compartir"**: exporta el archivo HTML o comparte el enlace por WhatsApp.

---

## 🎓 5. CÓMO DEFENDER EL PROYECTO ANTE EL PROFESOR

Cuando te evalúe el profesor, resalta estos 3 puntos técnicos clave:

1. **Arquitectura Híbrida Inteligente:**
   - *"Profesor, la aplicación integra un modelo de IA local en mi PC (Ollama) para pruebas ilimitadas de desarrollo a coste cero, y se conecta directamente a Google Gemini en la nube oficial cuando se requiere despliegue en producción, sin depender de plataformas intermediarias de terceros."*
2. **Resiliencia ante fallos (Zero-Crash Policy):**
   - *"Si el dispositivo pierde la señal de internet o se activa el modo avión, la aplicación no falla: cuenta con un motor autónomo local que garantiza la continuidad del flujo."*
3. **Generación Determinista de Alta Calidad:**
   - *"En lugar de dejar que la IA genere código HTML/CSS impredecible desde cero que pueda verse roto en el móvil, la IA actúa como un extractor semántico de requerimientos que inyecta los datos de negocio en estructuras web adaptables y optimizadas."*
