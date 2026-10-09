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

        // 3. Enviar mensaje con el botón Enviar
        btnSend.setOnClickListener(v -> sendMessage());

        // 4. Enviar mensaje con la tecla Enter/Enviar del teclado
        etMessagePrompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && !event.isShiftPressed())) {
                sendMessage();
                return true;
            }
            return false;
        });
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
                    aiBubble.setText(responseText);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }

                @Override
                public void onError(String errorMessage) {
                    String fallback = getLocalFallback(userPrompt);
                    aiBubble.setText(fallback);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }
            });
        } else if (selectedModelIndex == 1) {
            // MOTOR 2: GOOGLE GEMINI ONLINE OFICIAL
            geminiService.enviarMensaje(previousHistory, userPrompt, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    aiBubble.setText(responseText);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }

                @Override
                public void onError(String errorMessage) {
                    String fallback = getLocalFallback(userPrompt);
                    aiBubble.setText(fallback);
                    chatAdapter.notifyItemChanged(aiIndex);
                    rvChatMessages.smoothScrollToPosition(aiIndex);
                }
            });
        } else {
            // MOTOR 3: MODO LOCAL SIMULADO (OFFLINE)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String localResponse = getLocalFallback(userPrompt);
                aiBubble.setText(localResponse);
                chatAdapter.notifyItemChanged(aiIndex);
                rvChatMessages.smoothScrollToPosition(aiIndex);
            }, 600);
        }
    }

    private int offlineStep = 0;
    private String offlineNiche = "";

    private String getLocalFallback(String userPrompt) {
        String lower = userPrompt.toLowerCase().trim();

        String detectedNiche = "";
        if (lower.contains("restaurante") || lower.contains("bar") || lower.contains("comida") || lower.contains("pizza")) {
            detectedNiche = "Restaurante";
        } else if (lower.contains("taller") || lower.contains("mecánico") || lower.contains("mecanico") || lower.contains("coche")) {
            detectedNiche = "Taller Mecánico";
        } else if (lower.contains("veterinaria") || lower.contains("mascota") || lower.contains("animal")) {
            detectedNiche = "Clínica Veterinaria";
        } else if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construcci")) {
            detectedNiche = "Reformas y Construcción";
        } else if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho")) {
            detectedNiche = "Despacho de Abogados";
        }

        if (offlineStep == 0) {
            if (!detectedNiche.isEmpty()) {
                offlineNiche = detectedNiche;
                offlineStep = 1;
                return "¡Excelente! Vamos a diseñar la página web para tu " + offlineNiche + ". 🚀\n\n" +
                        "Para construirla a tu medida ahora mismo, facilítame estos datos (puedes enviármelos todos juntos):\n\n" +
                        "1️⃣ Nombre de tu negocio\n" +
                        "2️⃣ Teléfono o WhatsApp de contacto\n" +
                        "3️⃣ Dirección física o ciudad\n" +
                        "4️⃣ Servicios principales o especialidad que ofreces";
            } else {
                offlineStep = 1;
                return "¡Hola Diovar! 👋\n\n¿De qué temática o negocio quieres crear tu página web?\n\n(Por ejemplo: Restaurante, Taller Mecánico, Veterinaria, Reformas o Abogados).";
            }
        } else if (offlineStep == 1) {
            if (!detectedNiche.isEmpty()) {
                offlineNiche = detectedNiche;
                offlineStep = 2;
                return "¡Perfecto! Vamos a diseñar la página web para tu " + offlineNiche + ". 🌟\n\n" +
                        "Para estructurar tu página ahora mismo, envíame estos datos en tu próximo mensaje:\n\n" +
                        "1️⃣ Nombre de tu negocio\n" +
                        "2️⃣ Teléfono o WhatsApp de contacto\n" +
                        "3️⃣ Dirección física o ciudad\n" +
                        "4️⃣ Servicios que quieres que aparezcan";
            } else {
                offlineStep = 2;
                return "🎉 ¡Perfecto! He procesado toda la información de tu negocio:\n\n" +
                        "✅ Nombre de tu marca configurado.\n" +
                        "✅ Botón de contacto directo por WhatsApp vinculado.\n" +
                        "✅ Ubicación, mapa y catálogo de servicios integrados.\n\n" +
                        "¡Tu página web está completamente diseñada y lista para previsualizar!";
            }
        } else {
            return "¡Tu página web de " + (offlineNiche.isEmpty() ? "tu negocio" : offlineNiche) + " ya tiene todos tus datos aplicados!\n\n¿Quieres abrir la vista previa de tu web ahora?";
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
}