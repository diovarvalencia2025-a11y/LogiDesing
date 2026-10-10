package com.example.logidesignai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu;
    private ImageView btnAvatar;
    private ImageView ivLogo;
    private TextView tvGreeting;
    private TextView tvSlogan;
    private TextView tvSloganAnimated;
    private Handler sloganAnimHandler;
    private Runnable sloganAnimRunnable;
    private int currentNicheIndex = 0;

    // Colección de nichos con nombre, color y estilo de fuente (Typeface)
    private static class DemoNiche {
        final String name;
        final int color;
        final android.graphics.Typeface typeface;

        DemoNiche(String name, int color, android.graphics.Typeface typeface) {
            this.name = name;
            this.color = color;
            this.typeface = typeface;
        }
    }

    private DemoNiche[] demoNiches;
    private LinearLayout bannerPromo;
    private TextView tvBannerAction;
    private EditText etMessagePrompt;
    private ImageButton btnAddAttachment;
    private LinearLayout pillModelSelector;
    private TextView tvModelName;
    private ImageButton btnMic;
    private ImageButton btnVoiceMode;
    private ImageButton btnSend;

    private LinearLayout centerHeroLayout;
    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private GeminiService geminiService;
    private OllamaService ollamaService;

    private final String[] logiDesignModels = {
            "Ollama Local (PC)",
            "Google Gemini (Online)",
            "Modo Local (Offline)"
    };
    private int selectedModelIndex = 0; // Por defecto: Ollama Local en tu PC para pruebas ilimitadas gratis

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initViews();
        setupWindowInsets();
        setupDynamicGreeting();
        setupListeners();
        setupNavigationDrawer();
        setupBackPressHandler();
    }

    private void initViews() {
        geminiService = new GeminiService();
        ollamaService = new OllamaService();

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnAvatar = findViewById(R.id.btnAvatar);
        ivLogo = findViewById(R.id.ivLogo);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvSlogan = findViewById(R.id.tvSlogan);
        tvSloganAnimated = findViewById(R.id.tvSloganAnimated);
        setupNicheSloganAnimation();
        bannerPromo = findViewById(R.id.bannerPromo);
        tvBannerAction = findViewById(R.id.tvBannerAction);
        etMessagePrompt = findViewById(R.id.etMessagePrompt);
        btnAddAttachment = findViewById(R.id.btnAddAttachment);
        pillModelSelector = findViewById(R.id.pillModelSelector);
        tvModelName = findViewById(R.id.tvModelName);
        tvModelName.setText(logiDesignModels[selectedModelIndex]);
        btnMic = findViewById(R.id.btnMic);
        btnVoiceMode = findViewById(R.id.btnVoiceMode);
        btnSend = findViewById(R.id.btnSend);

        centerHeroLayout = findViewById(R.id.centerHeroLayout);
        rvChatMessages = findViewById(R.id.rvChatMessages);

        chatAdapter = new ChatAdapter(chatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupDynamicGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 6) {
            tvGreeting.setText("¿Trasnochando, Diovar?");
        } else if (hour >= 6 && hour < 12) {
            tvGreeting.setText("¿Qué web crearemos hoy, Diovar?");
        } else if (hour >= 12 && hour < 19) {
            tvGreeting.setText("Buenas tardes, Diovar");
        } else {
            tvGreeting.setText("¿Qué diseñamos hoy, Diovar?");
        }
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnAvatar.setOnClickListener(v -> Toast.makeText(this, "LogiDesign Studio", Toast.LENGTH_SHORT).show());

        bannerPromo.setOnClickListener(v -> showProDialog());
        tvBannerAction.setOnClickListener(v -> showProDialog());

        btnAddAttachment.setOnClickListener(v -> Toast.makeText(this, "Adjuntar archivo o imagen", Toast.LENGTH_SHORT).show());

        pillModelSelector.setOnClickListener(v -> showModelSelectorDialog());

        btnMic.setOnClickListener(v -> Toast.makeText(this, "Dictado por voz", Toast.LENGTH_SHORT).show());

        btnVoiceMode.setOnClickListener(v -> Toast.makeText(this, "Modo de voz", Toast.LENGTH_SHORT).show());

        // 1. Desplegar teclado inmediatamente al hacer clic en el chat
        etMessagePrompt.setOnClickListener(v -> showKeyboard());
        etMessagePrompt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard();
            }
        });

        // 2. Mostrar botón de enviar automáticamente cuando el usuario escribe
        etMessagePrompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                btnVoiceMode.setVisibility(hasText ? View.GONE : View.VISIBLE);
                btnMic.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Enviar mensaje ÚNICAMENTE con el botón Enviar
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void showKeyboard() {
        etMessagePrompt.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etMessagePrompt, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void sendMessage() {
        String text = etMessagePrompt.getText().toString().trim();
        if (text.isEmpty()) return;

        // Si es el primer mensaje, ocultamos el hero central y el banner promocional
        if (chatMessages.isEmpty()) {
            centerHeroLayout.setVisibility(View.GONE);
            bannerPromo.setVisibility(View.GONE);
            rvChatMessages.setVisibility(View.VISIBLE);
        }

        // Agregar mensaje del usuario a la derecha
        chatMessages.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.smoothScrollToPosition(chatMessages.size() - 1);
        etMessagePrompt.setText("");

        // Respuesta conversacional de LogiDesign AI
        respondWithAi(text);
    }

    private void respondWithAi(String userPrompt) {
        // 1. Añadimos burbuja temporal de "pensando..."
        ChatMessage aiBubble = new ChatMessage("LogiDesign está pensando...", ChatMessage.TYPE_AI);
        chatMessages.add(aiBubble);
        int aiIndex = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(aiIndex);
        rvChatMessages.smoothScrollToPosition(aiIndex);

        // 2. Historial de mensajes previos
        List<ChatMessage> previousHistory = new ArrayList<>();
        if (chatMessages.size() > 2) {
            previousHistory.addAll(chatMessages.subList(0, chatMessages.size() - 2));
        }

        if (selectedModelIndex == 0) {
            // MOTOR 1: OLLAMA LOCAL EN TU PC (qwen2.5-coder:7b)
            // Pruebas 100% gratis, ilimitadas y privadas sin gastar saldo de API
            ollamaService.enviarMensaje(previousHistory, userPrompt, new OllamaService.OllamaCallback() {
                @Override
                public void onSuccess(String responseText) {
                    procesarRespuestaIA(aiBubble, responseText);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }

                @Override
                public void onError(String errorMessage) {
                    String fallback = getLocalFallback(userPrompt);
                    procesarRespuestaIA(aiBubble, fallback);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }
            });
        } else if (selectedModelIndex == 1) {
            // MOTOR 2: GOOGLE GEMINI ONLINE OFICIAL
            geminiService.enviarMensaje(previousHistory, userPrompt, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    procesarRespuestaIA(aiBubble, responseText);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }

                @Override
                public void onError(String errorMessage) {
                    String fallback = getLocalFallback(userPrompt);
                    procesarRespuestaIA(aiBubble, fallback);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }
            });
        } else {
            // MOTOR 3: MODO LOCAL SIMULADO (OFFLINE)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String localResponse = getLocalFallback(userPrompt);
                procesarRespuestaIA(aiBubble, localResponse);
                chatAdapter.notifyItemChanged(aiIndex);
                rvChatMessages.smoothScrollToPosition(aiIndex);
            }, 600);
        }
    }

    private void procesarRespuestaIA(ChatMessage bubble, String fullResponse) {
        if (fullResponse == null) {
            bubble.setText("Hubo un detalle al procesar la respuesta.");
            return;
        }

        // Buscar si la IA incluye la etiqueta oculta de web lista
        String tagPrefix = "[LOGIDESIGN_WEB_READY:";
        int tagIndex = fullResponse.indexOf(tagPrefix);

        if (tagIndex != -1) {
            int tagEnd = fullResponse.indexOf("]", tagIndex);
            if (tagEnd != -1) {
                String tagContent = fullResponse.substring(tagIndex + tagPrefix.length(), tagEnd);
                String cleanMessage = fullResponse.substring(0, tagIndex).trim();

                String[] parts = tagContent.split("\\|");
                String niche = parts.length > 0 ? parts[0].trim() : "restaurantes";
                String nombre = parts.length > 1 ? parts[1].trim() : "Mi Negocio";
                String telefono = parts.length > 2 ? parts[2].trim() : "+34 600 000 000";
                String direccion = parts.length > 3 ? parts[3].trim() : "Calle Principal";
                String servicios = parts.length > 4 ? parts[4].trim() : "";

                bubble.setText(cleanMessage);
                bubble.setWebsiteReady(true);
                bubble.setNiche(niche);
                bubble.setBusinessName(nombre);
                bubble.setPhone(telefono);
                bubble.setAddress(direccion);
                bubble.setServices(servicios);
                return;
            }
        }

        bubble.setText(fullResponse);
    }

    private int offlineStep = 0;
    private String offlineNiche = "";
    private String offlineNombre = "";
    private String offlineTelefono = "";
    private String offlineDireccion = "";
    private String offlineServicios = "";

    private String getLocalFallback(String userPrompt) {
        String lower = userPrompt.toLowerCase().trim();

        String detectedNiche = "";
        if (lower.contains("restauran") || lower.contains("estauran") || lower.contains("bar") || lower.contains("comida") || lower.contains("pizza") || lower.contains("asado")) {
            detectedNiche = "restaurantes";
        } else if (lower.contains("taller") || lower.contains("mecanic") || lower.contains("coche") || lower.contains("auto")) {
            detectedNiche = "taller";
        } else if (lower.contains("veterina") || lower.contains("mascota") || lower.contains("animal") || lower.contains("perro")) {
            detectedNiche = "veterinaria";
        } else if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construc") || lower.contains("pintor")) {
            detectedNiche = "reformas";
        } else if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho") || lower.contains("jurid")) {
            detectedNiche = "abogados";
        } else if (lower.contains("dental") || lower.contains("dentist") || lower.contains("diente") || lower.contains("clinica")) {
            detectedNiche = "dental";
        }

        // 1. Detectar si el usuario nos está enviando los DATOS DE SU NEGOCIO (tiene teléfono o varias líneas)
        boolean tieneTelefono = userPrompt.matches(".*[0-9]{8,}.*");
        boolean tieneVariasLineas = userPrompt.trim().split("\n").length >= 2;

        if (tieneTelefono || tieneVariasLineas || offlineStep >= 1) {
            // El usuario nos está enviando los datos para la web:
            extraerDatosDelTexto(userPrompt);

            // Si aún no tenía nicho asignado, usar el detectado o restaurantes
            if (offlineNiche.isEmpty()) {
                offlineNiche = !detectedNiche.isEmpty() ? detectedNiche : "restaurantes";
            }
            offlineStep = 2;

            return "🎉 ¡Excelente! He recibido y verificado los datos de tu negocio:\n\n" +
                    "📍 **Nombre:** " + offlineNombre + "\n" +
                    "📞 **Teléfono / WhatsApp:** " + offlineTelefono + "\n" +
                    "🏠 **Dirección:** " + offlineDireccion + "\n" +
                    "⭐ **Especialidades:** " + offlineServicios + "\n\n" +
                    "En un momento tu página web estará creada. Si algún dato no es correcto, puedes corregirlo ahora mismo escribiéndomelo, o pulsar abajo para explorar tu web lista:\n" +
                    "[LOGIDESIGN_WEB_READY:" + offlineNiche + "|" + offlineNombre + "|" + offlineTelefono + "|" + offlineDireccion + "|" + offlineServicios + "]";
        }

        // 2. Si el usuario nos dice el tipo de negocio o saluda:
        if (!detectedNiche.isEmpty()) {
            offlineNiche = detectedNiche;
            offlineStep = 1;
            return "¡Excelente elección! Vamos a crear la página web para tu negocio de " + offlineNiche.toUpperCase() + ". 🌟\n\n" +
                    "Por favor, facilítame los siguientes datos:\n\n" +
                    "1️⃣ Nombre de tu negocio\n" +
                    "2️⃣ Teléfono o WhatsApp\n" +
                    "3️⃣ Dirección o ciudad\n" +
                    "4️⃣ Especialidades o platos principales\n\n" +
                    "(Puedes escribirlos uno debajo del otro con la tecla Enter).";
        }

        // 3. Saludo inicial
        return "¡Hola Diovar! 👋\n\n¿De qué temática o negocio quieres crear tu página web hoy?\n\n(Por ejemplo: Restaurante, Taller Mecánico, Veterinaria, Reformas, Dental o Abogados).";
    }

    private String sanitizarTextoSeguro(String input) {
        if (input == null) return "";
        // Control de Seguridad Anti-Inyección (XSS / Scripts maliciosos):
        String clean = input.replaceAll("(?i)<script.*?>.*?</script>", "")
                            .replaceAll("(?i)javascript:", "")
                            .replaceAll("(?i)on[a-z]+\\s*=", "")
                            .trim();

        return clean.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    private boolean esDireccionTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase();
        return lower.matches(".*\\b(calle|c/|avda|av\\.|avenida|plaza|pza|paseo|poligono|polígono|crta|carretera|barrio|camino|urb|urbanizacion|urbanización|portal|nº|numero|número|piso)\\b.*");
    }

    private boolean esServicioOPlatoTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase();
        return lower.matches(".*\\b(asado|asados|brasa|horno|carne|carnes|lechazo|chuleton|pescado|tapa|tapas|menu|menú|paella|pizza|pasta|marisco|comida|postre|reparacion|reparación|mantenimiento|motor|freno|frenos|rueda|ruedas|diagnosis|reforma|reformas|obra|obras|pintura|cocina|cocinas|baño|baños|derecho|civil|penal|herencia|herencias|divorcio|divorcios|diente|dientes|sonrisa|implante|implantes|ortodoncia|vacuna|vacunas|consulta|cirugia|cirugía)\\b.*");
    }

    private void extraerDatosDelTexto(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String raw = text.trim();

        // Limpieza y partición de líneas ingresadas por el usuario
        String[] lines = raw.split("\n");
        List<String> lineasListas = new ArrayList<>();
        for (String l : lines) {
            String clean = l.trim();
            if (!clean.isEmpty()) {
                lineasListas.add(clean);
            }
        }

        if (lineasListas.size() == 1) {
            String linea = lineasListas.get(0);
            String limpio = linea.replaceFirst("^[1-4][.)\\-]?\\s*", "")
                                 .replaceFirst("(?i)(nombre|negocio|tel[eé]fono|whatsapp|direcci[oó]n|ciudad|servicios?|platos?):?\\s*", "")
                                 .trim();

            java.util.regex.Matcher pm = java.util.regex.Pattern.compile("(\\+?[0-9]{2,3}[\\s.-]?)?([6789][0-9]{8})|[0-9]{9}").matcher(limpio);
            if (pm.find()) {
                offlineTelefono = pm.group().trim();
                String antes = limpio.substring(0, pm.start()).trim();
                String despues = limpio.substring(pm.end()).trim();
                if (!antes.isEmpty()) {
                    offlineNombre = antes;
                } else if (!despues.isEmpty()) {
                    offlineNombre = despues;
                }
            } else if (esDireccionTexto(limpio)) {
                offlineDireccion = limpio;
            } else if (esServicioOPlatoTexto(limpio)) {
                offlineServicios = limpio;
            } else {
                offlineNombre = limpio;
            }
        } else if (lineasListas.size() >= 2) {
            // 1. Detectar si alguna línea es un teléfono (incluso si la escriben de primera)
            int idxPhone = -1;
            for (int i = 0; i < lineasListas.size(); i++) {
                String l = lineasListas.get(i);
                if (l.matches(".*[0-9]{8,}.*")) {
                    idxPhone = i;
                    java.util.regex.Matcher pm = java.util.regex.Pattern.compile("(\\+?[0-9]{2,3}[\\s.-]?)?([6789][0-9]{8})|[0-9]{9}").matcher(l);
                    if (pm.find()) {
                        offlineTelefono = pm.group().trim();
                    } else {
                        offlineTelefono = l.trim();
                    }
                    break;
                }
            }

            // 2. Líneas restantes que no son teléfono
            List<String> restantes = new ArrayList<>();
            for (int i = 0; i < lineasListas.size(); i++) {
                if (i != idxPhone) {
                    String limpio = lineasListas.get(i).replaceFirst("^[1-4][.)\\-]?\\s*", "")
                                                       .replaceFirst("(?i)(nombre|negocio|tel[eé]fono|whatsapp|direcci[oó]n|ciudad|servicios?|platos?):?\\s*", "")
                                                       .trim();
                    restantes.add(limpio);
                }
            }

            // 3. Buscar si alguna de las restantes es claramente una dirección
            int idxDir = -1;
            for (int i = 0; i < restantes.size(); i++) {
                if (esDireccionTexto(restantes.get(i))) {
                    idxDir = i;
                    offlineDireccion = restantes.get(i);
                    break;
                }
            }

            // 4. Buscar si alguna de las restantes es claramente servicios/plato
            int idxServ = -1;
            for (int i = 0; i < restantes.size(); i++) {
                if (i != idxDir && esServicioOPlatoTexto(restantes.get(i))) {
                    idxServ = i;
                    offlineServicios = restantes.get(i);
                    break;
                }
            }

            // 5. Las líneas restantes corresponden al Nombre (o servicios)
            for (int i = 0; i < restantes.size(); i++) {
                if (i != idxDir && i != idxServ) {
                    if (offlineNombre.isEmpty()) {
                        offlineNombre = restantes.get(i);
                    } else if (offlineServicios.isEmpty()) {
                        offlineServicios = restantes.get(i);
                    }
                }
            }

            // 6. Si no se detectó dirección por palabras clave pero hay 2 o más líneas en orden estándar:
            if (offlineDireccion.isEmpty() && restantes.size() >= 2 && idxDir == -1) {
                if (offlineNombre.isEmpty()) offlineNombre = restantes.get(0);
                offlineDireccion = restantes.get(1);
                if (restantes.size() >= 3 && offlineServicios.isEmpty()) {
                    offlineServicios = restantes.get(2);
                }
            }
        }

        // Sanitización y Blindaje de Seguridad para todos los campos:
        offlineNombre = sanitizarTextoSeguro(offlineNombre);
        offlineTelefono = sanitizarTextoSeguro(offlineTelefono);
        offlineDireccion = sanitizarTextoSeguro(offlineDireccion);
        offlineServicios = sanitizarTextoSeguro(offlineServicios);

        // Validaciones por defecto elegantes y personalizadas si el usuario NO escribió el nombre:
        if (offlineNombre.isEmpty()) {
            switch (offlineNiche.toLowerCase()) {
                case "taller":
                    offlineNombre = "Taller Mecánico Especializado";
                    break;
                case "veterinaria":
                    offlineNombre = "Clínica Veterinaria Integral";
                    break;
                case "reformas":
                    offlineNombre = "Reformas & Obras Pro";
                    break;
                case "abogados":
                    offlineNombre = "Despacho Jurídico & Legal";
                    break;
                case "dental":
                    offlineNombre = "Clínica Dental Especializada";
                    break;
                case "restaurantes":
                default:
                    offlineNombre = "Restaurante & Tradición";
                    break;
            }
        }
        if (offlineTelefono.isEmpty()) offlineTelefono = "+34 612 345 678";
        if (offlineDireccion.isEmpty()) offlineDireccion = "Calle Principal, Ciudad";
        if (offlineServicios.isEmpty()) {
            switch (offlineNiche.toLowerCase()) {
                case "taller":
                    offlineServicios = "Mantenimiento oficial y diagnosis multimarca";
                    break;
                case "veterinaria":
                    offlineServicios = "Consulta general, vacunación y cirugías";
                    break;
                case "reformas":
                    offlineServicios = "Reformas integrales de cocinas y baños";
                    break;
                case "abogados":
                    offlineServicios = "Derecho bancario, civil, penal y familiar";
                    break;
                case "dental":
                    offlineServicios = "Diseño de sonrisa y estética dental avanzada";
                    break;
                case "restaurantes":
                default:
                    offlineServicios = "Asados a la brasa y platos de la casa";
                    break;
            }
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chats) {
                startActivity(new Intent(this, Chat.class));
            } else if (itemId == R.id.nav_projects) {
                startActivity(new Intent(this, ProjectsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        LinearLayout layoutUserFooter = findViewById(R.id.layoutUserFooter);
        if (layoutUserFooter != null) {
            layoutUserFooter.setOnClickListener(v -> {
                Toast.makeText(this, "Perfil de Diovar", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void showModelSelectorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Motor")
                .setSingleChoiceItems(logiDesignModels, selectedModelIndex, (dialog, which) -> {
                    selectedModelIndex = which;
                    tvModelName.setText(logiDesignModels[selectedModelIndex]);
                    dialog.dismiss();
                    Toast.makeText(this, "Seleccionado: " + logiDesignModels[selectedModelIndex], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showProDialog() {
        new AlertDialog.Builder(this)
                .setTitle("LogiDesign Pro")
                .setMessage("Funciones avanzadas, generador completo de sitios web y exportación directa de código.")
                .setPositiveButton("Aceptar", (dialog, which) -> Toast.makeText(this, "Suscripción activada", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private int typeCharIndex = 0;
    private boolean isDeleting = false;

    private void setupNicheSloganAnimation() {
        if (tvSloganAnimated == null) return;

        // Configuración de nichos con colores temáticos y estilos de tipografía distintos
        demoNiches = new DemoNiche[]{
                new DemoNiche("RESTAURANTES", android.graphics.Color.parseColor("#E11D48"), android.graphics.Typeface.SERIF),               // Rojo gastronomía elegante Serif
                new DemoNiche("TALLER MECÁNICO", android.graphics.Color.parseColor("#EA580C"), android.graphics.Typeface.MONOSPACE),          // Naranja industrial Monospace
                new DemoNiche("VETERINARIA", android.graphics.Color.parseColor("#16A34A"), android.graphics.Typeface.DEFAULT_BOLD),          // Verde natural Sans negrita
                new DemoNiche("REFORMAS", android.graphics.Color.parseColor("#D97706"), android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL)), // Ámbar construcción Black
                new DemoNiche("ABOGADOS", android.graphics.Color.parseColor("#1E3A8A"), android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)), // Azul legal Serif cursiva
                new DemoNiche("CLÍNICA DENTAL", android.graphics.Color.parseColor("#0284C7"), android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD))  // Cian clínico moderno
        };

        sloganAnimHandler = new Handler(Looper.getMainLooper());
        currentNicheIndex = 0;
        typeCharIndex = 0;
        isDeleting = false;

        // Establecer primer nicho
        DemoNiche initial = demoNiches[currentNicheIndex];
        tvSloganAnimated.setTextColor(initial.color);
        tvSloganAnimated.setTypeface(initial.typeface);
        tvSloganAnimated.setText("|");

        sloganAnimRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;

                DemoNiche current = demoNiches[currentNicheIndex];
                String fullWord = current.name;

                if (!isDeleting) {
                    // MODO ESCRIBIR: Añadir una letra de izquierda a derecha
                    typeCharIndex++;
                    String visibleText = fullWord.substring(0, typeCharIndex) + "|";
                    tvSloganAnimated.setText(visibleText);
                    tvSloganAnimated.setTextColor(current.color);
                    tvSloganAnimated.setTypeface(current.typeface);

                    if (typeCharIndex == fullWord.length()) {
                        // Terminó de escribir la palabra completa: mostrar sin cursor y esperar 1.8 segundos
                        tvSloganAnimated.setText(fullWord);
                        isDeleting = true;
                        sloganAnimHandler.postDelayed(this, 1800);
                    } else {
                        // Velocidad de tecleo natural (entre 80ms y 110ms por letra)
                        sloganAnimHandler.postDelayed(this, 90);
                    }
                } else {
                    // MODO BORRAR: Quitar una letra de derecha a izquierda
                    typeCharIndex--;
                    if (typeCharIndex > 0) {
                        String visibleText = fullWord.substring(0, typeCharIndex) + "|";
                        tvSloganAnimated.setText(visibleText);
                        // Velocidad de borrado rápida (45ms por letra)
                        sloganAnimHandler.postDelayed(this, 45);
                    } else {
                        // Se borró por completo: pasar al siguiente nicho y empezar a teclear
                        isDeleting = false;
                        currentNicheIndex = (currentNicheIndex + 1) % demoNiches.length;
                        DemoNiche nextNiche = demoNiches[currentNicheIndex];
                        tvSloganAnimated.setTextColor(nextNiche.color);
                        tvSloganAnimated.setTypeface(nextNiche.typeface);
                        tvSloganAnimated.setText("|");
                        // Pausa de 350ms antes de comenzar a escribir la siguiente
                        sloganAnimHandler.postDelayed(this, 350);
                    }
                }
            }
        };

        // Iniciar la máquina de escribir tras 1 segundo
        sloganAnimHandler.postDelayed(sloganAnimRunnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sloganAnimHandler != null && sloganAnimRunnable != null) {
            sloganAnimHandler.removeCallbacks(sloganAnimRunnable);
        }
    }
}