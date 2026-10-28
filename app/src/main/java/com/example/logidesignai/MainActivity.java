package com.example.logidesignai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
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
import com.example.logidesignai.data.HistorialDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private HistorialDao historialDao;
    private LogiDesignEngine engine;

    // Estado de la conversación actual (aislado por sesión)
    private long currentConversationId = -1L;
    private String currentNiche = "";
    private String currentNombre = "";
    private String currentTelefono = "";
    private String currentDireccion = "";
    private String currentServicios = "";
    private boolean isWebCreated = false;

    private final String[] logiDesignModels = {
            "Ollama Local (PC)",
            "Modo Local (Offline)",
            "Google Gemini (Presentación)"
    };
    private int selectedModelIndex = 0;

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

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;

        boolean esNuevoChat = intent.getBooleanExtra("EXTRA_NUEVO_CHAT", false);
        long convId = intent.getLongExtra("EXTRA_CONVERSACION_ID", -1L);

        if (convId > 0 && !esNuevoChat) {
            cargarConversacionEspecifica(convId);
        } else {
            iniciarNuevoChatLimpio();
        }
    }

    private void iniciarNuevoChatLimpio() {
        currentConversationId = -1L;
        currentNiche = "";
        currentNombre = "";
        currentTelefono = "";
        currentDireccion = "";
        currentServicios = "";
        isWebCreated = false;
        if (engine != null) engine.reset();

        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        centerHeroLayout.setVisibility(View.VISIBLE);
        bannerPromo.setVisibility(View.VISIBLE);
        rvChatMessages.setVisibility(View.GONE);
    }

    private void cargarConversacionEspecifica(long convId) {
        currentConversationId = convId;
        List<ChatMessage> guardados = historialDao.obtenerMensajesPorConversacion(convId);

        // Restaurar estado de la última web creada en esta conversación (si la hay)
        ConversationItem convItem = historialDao.obtenerConversacion(convId);
        currentNiche = "";
        currentNombre = "";
        currentTelefono = "";
        currentDireccion = "";
        currentServicios = "";
        isWebCreated = false;

        if (convItem != null) {
            currentNiche = convItem.getNiche() != null ? convItem.getNiche() : "";
        }

        if (guardados != null && !guardados.isEmpty()) {
            for (ChatMessage msg : guardados) {
                if (msg.isWebsiteReady()) {
                    currentNiche = msg.getNiche() != null ? msg.getNiche() : currentNiche;
                    currentNombre = msg.getBusinessName() != null ? msg.getBusinessName() : currentNombre;
                    currentTelefono = msg.getPhone() != null ? msg.getPhone() : currentTelefono;
                    currentDireccion = msg.getAddress() != null ? msg.getAddress() : currentDireccion;
                    currentServicios = msg.getServices() != null ? msg.getServices() : currentServicios;
                    isWebCreated = true;
                }
            }
        }

        syncStateToEngine();

        chatMessages.clear();
        if (guardados != null && !guardados.isEmpty()) {
            chatMessages.addAll(guardados);

            centerHeroLayout.setVisibility(View.GONE);
            bannerPromo.setVisibility(View.GONE);
            rvChatMessages.setVisibility(View.VISIBLE);
            chatAdapter.notifyDataSetChanged();
            rvChatMessages.scrollToPosition(chatMessages.size() - 1);
        } else {
            centerHeroLayout.setVisibility(View.VISIBLE);
            bannerPromo.setVisibility(View.VISIBLE);
            rvChatMessages.setVisibility(View.GONE);
            chatAdapter.notifyDataSetChanged();
        }
    }

    private void initViews() {
        geminiService = new GeminiService();
        ollamaService = new OllamaService();
        historialDao = new HistorialDao(this);
        engine = new LogiDesignEngine();

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnAvatar = findViewById(R.id.btnAvatar);
        ivLogo = findViewById(R.id.ivLogo);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvSlogan = findViewById(R.id.tvSlogan);
        tvSloganAnimated = findViewById(R.id.tvSloganAnimated);

        // Activamos la animación de texto dinámico de todos los nichos
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

        etMessagePrompt.setOnClickListener(v -> showKeyboard());
        etMessagePrompt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard();
            }
        });

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

        // Si es una conversación nueva, crear el registro en la BD
        if (currentConversationId <= 0) {
            String tituloInicial = generarTituloConversacion(text);
            currentConversationId = historialDao.crearConversacion(tituloInicial, currentNiche);
        }

        // Siempre aseguramos que el chat sea visible y ocultamos el hero central y el banner promocional
        centerHeroLayout.setVisibility(View.GONE);
        bannerPromo.setVisibility(View.GONE);
        rvChatMessages.setVisibility(View.VISIBLE);

        // Agregar mensaje del usuario
        ChatMessage userMsg = new ChatMessage(currentConversationId, text, ChatMessage.TYPE_USER);
        chatMessages.add(userMsg);
        historialDao.insertarMensaje(currentConversationId, userMsg);

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.smoothScrollToPosition(chatMessages.size() - 1);
        etMessagePrompt.setText("");

        // Respuesta con IA
        respondWithAi(text);


    }

    private String generarTituloConversacion(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("restauran") || lower.contains("bar") || lower.contains("comida")) return "Web Restaurante";
        if (lower.contains("taller") || lower.contains("mecanic") || lower.contains("coche")) return "Web Taller Mecánico";
        if (lower.contains("veterina") || lower.contains("mascota") || lower.contains("animal")) return "Web Veterinaria";
        if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construc")) return "Web Reformas";
        if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho")) return "Web Abogados";
        if (lower.contains("dental") || lower.contains("dentist") || lower.contains("diente")) return "Web Dental";

        if (prompt.length() > 30) {
            return prompt.substring(0, 30) + "...";
        }
        return prompt;
    }

    private void respondWithAi(String userPrompt) {
        ChatMessage aiBubble = new ChatMessage(currentConversationId, "LogiDesign está pensando...", ChatMessage.TYPE_AI);
        chatMessages.add(aiBubble);
        int aiIndex = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(aiIndex);
        rvChatMessages.smoothScrollToPosition(aiIndex);

        List<ChatMessage> previousHistory = new ArrayList<>();
        if (chatMessages.size() > 2) {
            previousHistory.addAll(chatMessages.subList(0, chatMessages.size() - 2));
        }

        if (selectedModelIndex == 0) {

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
            // Modo local simulado
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String localResponse = getLocalFallback(userPrompt);
                procesarRespuestaIA(aiBubble, localResponse);
                chatAdapter.notifyItemChanged(aiIndex);
                rvChatMessages.smoothScrollToPosition(aiIndex);
            }, 500);
        } else {
            // Google Gemini online
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
        }
    }

    private void procesarRespuestaIA(ChatMessage bubble, String fullResponse) {
        if (fullResponse == null) {
            bubble.setText("Hubo un detalle al procesar la respuesta.");
            historialDao.insertarMensaje(currentConversationId, bubble);
            return;
        }

        String tagPrefix = "[LOGIDESIGN_WEB_READY:";
        int tagIndex = fullResponse.indexOf(tagPrefix);

        if (tagIndex != -1) {
            int tagEnd = fullResponse.indexOf("]", tagIndex);
            if (tagEnd != -1) {
                String tagContent = fullResponse.substring(tagIndex + tagPrefix.length(), tagEnd);
                String cleanMessage = fullResponse.substring(0, tagIndex).trim();

                String[] parts = tagContent.split("\\|");
                String niche = parts.length > 0 && !parts[0].trim().isEmpty() ? parts[0].trim() : (currentNiche.isEmpty() ? "restaurantes" : currentNiche);
                String nombre = parts.length > 1 && !parts[1].trim().isEmpty() ? parts[1].trim() : (currentNombre.isEmpty() ? "Mi Negocio" : currentNombre);
                String telefono = parts.length > 2 && !parts[2].trim().isEmpty() ? parts[2].trim() : (currentTelefono.isEmpty() ? "+34 600 000 000" : currentTelefono);
                String direccion = parts.length > 3 && !parts[3].trim().isEmpty() ? parts[3].trim() : (currentDireccion.isEmpty() ? "Calle Principal" : currentDireccion);
                String servicios = parts.length > 4 ? parts[4].trim() : currentServicios;

                currentNiche = niche;
                currentNombre = nombre;
                currentTelefono = telefono;
                currentDireccion = direccion;
                currentServicios = servicios;
                isWebCreated = true;
                syncStateToEngine();

                bubble.setText(cleanMessage);
                bubble.setWebsiteReady(true);
                bubble.setNiche(niche);
                bubble.setBusinessName(nombre);
                bubble.setPhone(telefono);
                bubble.setAddress(direccion);
                bubble.setServices(servicios);

                historialDao.insertarMensaje(currentConversationId, bubble);
                historialDao.actualizarConversacion(currentConversationId, "Web: " + nombre, niche, nombre, telefono, direccion, servicios, true);
                return;
            }
        }

        bubble.setText(fullResponse);
        historialDao.insertarMensaje(currentConversationId, bubble);
    }



    private boolean esSaludoPuro(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|hey|qu[eé] tal|buenos d[ií]as|buenas tardes|buenas noches)[!.,\\s]*$");
    }

    private boolean esPeticionAjenaAWeb(String text) {
        String lower = text.toLowerCase().trim();
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

    private boolean esNombreNegocioValido(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        if (lower.isEmpty() || lower.length() > 45) return false;

        // 1. Conceptos o menciones de páginas web / creación / apps (incluyendo erratas como 'apgina', 'pagna')
        if (lower.matches(".*\\b(web|p[aá]gina|apgina|pagna|pajina|sitio|diseño|diseña|crear|hacer|haz|app|aplicaci[oó]n)\\b.*")) {
            return false;
        }

        // 2. Frases que empiezan con artículos de petición, oraciones o preposiciones ("una de...", "para un...")
        if (lower.matches("^(un|una|el|la|los|las|de|para|quiero|necesito|busca|busques|buscar|dime|hazme|puedes|podr[ií]as|ayuda|ay[uú]dame|c[oó]mo|qu[eé]|qui[eé]n|d[oó]nde|por qu[eé]|para qu[eé]|no s[eé]|me gustar[ií]a|por favor|averigua|investiga|consulta|haz)\\b.*")) {
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

    private boolean esPeticionCrearWeb(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches(".*\\b(p[aá]gina|apgina|pagna|web|sitio|diseño web|crear web|hacer web|hacer una web|crear una web|hacer una p[aá]gina|crear una p[aá]gina|quiero una web|una web|una p[aá]gina|una apgina)\\b.*");
    }

    private boolean esSaludoOGenerico(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|buenos d[ií]as|buenas tardes|buenas noches|hey|qu[eé] tal|saludos|quiero hacer una p[aá]gina|quiero una web|hacer una web|crear web|necesito una web|ok|vale|est[aá] bien|comenzar|empezar|ayuda|ay[uú]dame).*");
    }

    private String detectarNichoTexto(String lower) {
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

    private String getNombreNichoAmigable(String n) {
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

    private void syncStateToEngine() {
        if (engine == null) engine = new LogiDesignEngine();
        engine.setCurrentNiche(currentNiche);
        engine.setCurrentNombre(currentNombre);
        engine.setCurrentTelefono(currentTelefono);
        engine.setCurrentDireccion(currentDireccion);
        engine.setCurrentServicios(currentServicios);
        engine.setWebCreated(isWebCreated);
    }

    private void syncStateFromEngine() {
        if (engine == null) return;
        currentNiche = engine.getCurrentNiche();
        currentNombre = engine.getCurrentNombre();
        currentTelefono = engine.getCurrentTelefono();
        currentDireccion = engine.getCurrentDireccion();
        currentServicios = engine.getCurrentServicios();
        isWebCreated = engine.isWebCreated();
    }

    private String getLocalFallback(String userPrompt) {
        syncStateToEngine();
        String response = engine.processMessage(userPrompt);
        syncStateFromEngine();

        // Actualizar el título de la conversación si ya se detectó o refinó el nicho/nombre
        if (currentConversationId > 0 && !currentNiche.isEmpty()) {
            if (!currentNombre.isEmpty() && isWebCreated) {
                historialDao.actualizarTitulo(currentConversationId, "Web: " + currentNombre);
            } else {
                historialDao.actualizarTitulo(currentConversationId, "Web: " + engine.getNombreNichoAmigable(currentNiche));
            }
        }
        return response;
    }

    private void extraerDatosRiguroso(String text) {
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

            if ((l1.matches(".*[0-9]{8,}.*") || l0.matches(".*[0-9]{8,}.*")) && esNombreNegocioValido(l0)) {
                currentNombre = sanitizarTextoSeguro(l0);
                Matcher phoneMatcher = Pattern.compile("(\\+?[0-9]{2,3}[\\s.-]?)?([6789][0-9]{8})|[0-9]{9}").matcher(l1);
                currentTelefono = phoneMatcher.find() ? phoneMatcher.group().trim() : sanitizarTextoSeguro(l1);
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

            // 1. Teléfono (con etiqueta o por número claro de 9 dígitos)
            if (lower.matches("^(2[.)\\-]|2️⃣|tel[eé]fono|whatsapp|tlf|celular):?\\s*.*") || (clean.matches(".*[0-9]{8,}.*") && !esDireccionTexto(clean))) {
                Matcher phoneMatcher = Pattern.compile("(\\+?[0-9]{2,3}[\\s.-]?)?([6789][0-9]{8})|[0-9]{9}").matcher(clean);
                if (phoneMatcher.find() && currentTelefono.isEmpty()) {
                    currentTelefono = phoneMatcher.group().trim();
                    continue;
                }
            }

            // 2. Dirección física (con etiqueta o por nombre de calle/vía/municipio)
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
            // Si parece una lista de servicios/especialidades (tiene comas, ' y ', o palabras de oficio como ortodoncias, cirugias, asados, etc.):
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

    private String limpiarPrefijo(String s) {
        if (s == null) return "";
        return s.replaceFirst("^[1-4][.)\\-]?\\s*", "")
                .replaceFirst("^(?i)(nombre|negocio|tel[eé]fono|whatsapp|direcci[oó]n|ciudad|servicios?|especialidades?|platos?):?\\s*", "")
                .trim();
    }

    private String sanitizarTextoSeguro(String input) {
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

    private boolean esDireccionTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase().trim();
        // Prefijos y palabras de vías
        if (lower.matches(".*\\b(calle|c/|c\\.|avda|av\\.|avenida|plaza|pza|paseo|poligono|polígono|crta|carretera|barrio|camino|urb|urbanizacion|urbanización|portal|nº|numero|número|piso)\\b.*")) {
            return true;
        }
        // Vía con número y municipio (ej: "Juan xxIII 6 Basauri", "Abaroeta 10 Basauri", etc.)
        if (lower.matches(".*[0-9]{1,4}\\s+[a-záéíóúñ]+.*") || lower.matches(".*\\b(basauri|bilbao|madrid|barcelona|valencia|sevilla|zaragoza|malaga|málaga|murcia|palma|alicante|cordoba|córdoba|valladolid|vigo|gijon|gijón|vitoria|granada|oviedo|santander|burgos|pamplona|san sebastian|donostia)\\b.*")) {
            return true;
        }
        return false;
    }

    private boolean esServicioOPlatoTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase();
        return lower.matches(".*\\b(asado|asados|brasa|horno|carne|carnes|lechazo|chuleton|pescado|tapa|tapas|menu|menú|paella|pizza|pasta|marisco|postre|reparacion|reparación|mantenimiento|motor|freno|frenos|rueda|ruedas|diagnosis|reforma|reformas|obra|obras|pintura|cocina|cocinas|baño|baños|derecho|civil|penal|herencia|herencias|divorcio|divorcios|diente|dientes|sonrisa|implante|implantes|ortodoncia|vacuna|vacunas|consulta|cirugia|cirugía)\\b.*");
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

    private static final String SCRAMBLE_GLYPHS = "!@#$%^&*<>~+=-_{}[]|:;?0123456789ABCDEFXYZ";
    private final java.util.Random scrambleRandom = new java.util.Random();
    private int lockedIndex = 0;
    private int scrambleTickCount = 0;

    private void setupNicheSloganAnimation() {
        if (tvSloganAnimated == null) return;

        demoNiches = new DemoNiche[]{
                new DemoNiche("RESTAURANTES", android.graphics.Color.parseColor("#E11D48"), android.graphics.Typeface.SERIF),
                new DemoNiche("REFORMAS", android.graphics.Color.parseColor("#D97706"), android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL)),
                new DemoNiche("TALLER MECÁNICO", android.graphics.Color.parseColor("#EA580C"), android.graphics.Typeface.MONOSPACE),
                new DemoNiche("VETERINARIA", android.graphics.Color.parseColor("#16A34A"), android.graphics.Typeface.DEFAULT_BOLD),
                new DemoNiche("ABOGADOS", android.graphics.Color.parseColor("#1E3A8A"), android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)),
                new DemoNiche("CLÍNICA DENTAL", android.graphics.Color.parseColor("#0284C7"), android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD))
        };

        sloganAnimHandler = new Handler(Looper.getMainLooper());
        currentNicheIndex = 0;
        lockedIndex = 0;
        scrambleTickCount = 0;

        DemoNiche initial = demoNiches[currentNicheIndex];
        tvSloganAnimated.setTextColor(initial.color);
        tvSloganAnimated.setTypeface(initial.typeface);
        tvSloganAnimated.setText(initial.name);

        sloganAnimRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;

                DemoNiche current = demoNiches[currentNicheIndex];
                String targetWord = current.name;
                int targetLen = targetWord.length();

                if (lockedIndex < targetLen) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < targetLen; i++) {
                        if (i < lockedIndex) {
                            sb.append(targetWord.charAt(i));
                        } else if (targetWord.charAt(i) == ' ') {
                            sb.append(' ');
                        } else {
                            int randIndex = scrambleRandom.nextInt(SCRAMBLE_GLYPHS.length());
                            sb.append(SCRAMBLE_GLYPHS.charAt(randIndex));
                        }
                    }

                    tvSloganAnimated.setText(sb.toString());
                    tvSloganAnimated.setTextColor(current.color);
                    tvSloganAnimated.setTypeface(current.typeface);

                    scrambleTickCount++;
                    if (scrambleTickCount >= 2) {
                        lockedIndex++;
                        scrambleTickCount = 0;
                    }

                    sloganAnimHandler.postDelayed(this, 40);
                } else {
                    tvSloganAnimated.setText(targetWord);
                    tvSloganAnimated.setTextColor(current.color);
                    tvSloganAnimated.setTypeface(current.typeface);

                    sloganAnimHandler.postDelayed(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        currentNicheIndex = (currentNicheIndex + 1) % demoNiches.length;
                        lockedIndex = 0;
                        scrambleTickCount = 0;
                        DemoNiche nextNiche = demoNiches[currentNicheIndex];
                        tvSloganAnimated.setTextColor(nextNiche.color);
                        tvSloganAnimated.setTypeface(nextNiche.typeface);

                        sloganAnimHandler.post(sloganAnimRunnable);
                    }, 2200);
                }
            }
        };

        sloganAnimHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            currentNicheIndex = 1;
            lockedIndex = 0;
            scrambleTickCount = 0;
            DemoNiche nextNiche = demoNiches[currentNicheIndex];
            tvSloganAnimated.setTextColor(nextNiche.color);
            tvSloganAnimated.setTypeface(nextNiche.typeface);
            sloganAnimHandler.post(sloganAnimRunnable);
        }, 2200);
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sloganAnimHandler != null) {
            sloganAnimHandler.removeCallbacksAndMessages(null);
        }
    }
}