package com.example.logidesignai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

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

    // Coleccion de nichos con nombre, color y estilo de fuente (Typeface)
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
    private android.widget.HorizontalScrollView scrollQuickChips;
    private LinearLayout layoutQuickChips;
    private EditText etMessagePrompt;
    private View layoutAttachmentPreview;
    private ImageView ivAttachmentThumbnail;
    private ImageButton btnRemoveAttachment;
    private ActivityResultLauncher<String> pickAttachmentLauncher;
    private ActivityResultLauncher<Void> takePictureLauncher;
    private Uri selectedAttachmentUri = null;
    private String selectedAttachmentBase64 = null;
    private String selectedAttachmentMimeType = null;
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

    // Estado de la conversacion actual (aislado por sesion)
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
            "Google Gemini (Presentacion)"
    };
    private int selectedModelIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
                setContentView(R.layout.activity_main);

        // Registro de selector para adjuntar imagenes de negocio
        pickAttachmentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                mostrarAdjuntoSeleccionado(uri);
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                mostrarBitmapSeleccionado(bitmap);
            }
        });

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
        actualizarQuickChips();

        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        centerHeroLayout.setVisibility(View.VISIBLE);
        bannerPromo.setVisibility(View.VISIBLE);
        rvChatMessages.setVisibility(View.GONE);
    }

    private void cargarConversacionEspecifica(long convId) {
        currentConversationId = convId;
        List<ChatMessage> guardados = historialDao.obtenerMensajesPorConversacion(convId);

        // Restaurar estado de la ultima web creada en esta conversacion (si la hay)
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
        actualizarQuickChips();

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

        // Activamos la animacion de texto dinamico de todos los nichos
        setupNicheSloganAnimation();

        bannerPromo = findViewById(R.id.bannerPromo);
        tvBannerAction = findViewById(R.id.tvBannerAction);
        etMessagePrompt = findViewById(R.id.etMessagePrompt);
                btnAddAttachment = findViewById(R.id.btnAddAttachment);
        layoutAttachmentPreview = findViewById(R.id.layoutAttachmentPreview);
        ivAttachmentThumbnail = findViewById(R.id.ivAttachmentThumbnail);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);
        pillModelSelector = findViewById(R.id.pillModelSelector);
        tvModelName = findViewById(R.id.tvModelName);
        tvModelName.setText(logiDesignModels[selectedModelIndex]);




        scrollQuickChips = findViewById(R.id.scrollQuickChips);
        layoutQuickChips = findViewById(R.id.layoutQuickChips);
        actualizarQuickChips();

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
            tvGreeting.setText("Trasnochando, Diovar?");
        } else if (hour >= 6 && hour < 12) {
            tvGreeting.setText("Que web crearemos hoy, Diovar?");
        } else if (hour >= 12 && hour < 19) {
            tvGreeting.setText("Buenas tardes, Diovar");
        } else {
            tvGreeting.setText("Que disenamos hoy, Diovar?");
        }
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnAvatar.setOnClickListener(v -> Toast.makeText(this, "LogiDesign Studio", Toast.LENGTH_SHORT).show());

        bannerPromo.setOnClickListener(v -> showProDialog());
        tvBannerAction.setOnClickListener(v -> showProDialog());

                btnAddAttachment.setOnClickListener(v -> showAttachmentOptionsDialog());
        if (btnRemoveAttachment != null) {
            btnRemoveAttachment.setOnClickListener(v -> limpiarAdjunto());
        }

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

                etMessagePrompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void showAttachmentOptionsDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_attachment_bottom_sheet, null);
        bottomSheet.setContentView(sheetView);

        if (bottomSheet.getWindow() != null) {
            View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        }

        // 1. Fotos y galeria
        sheetView.findViewById(R.id.sheetItemPhotos).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickAttachmentLauncher.launch("image/*");
        });

        // 2. Archivos y documentos
        sheetView.findViewById(R.id.sheetItemFiles).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickAttachmentLauncher.launch("*/*");
        });

        // 3. Camara
        sheetView.findViewById(R.id.sheetItemCamera).setOnClickListener(v -> {
            bottomSheet.dismiss();
            try {
                takePictureLauncher.launch(null);
            } catch (Exception e) {
                pickAttachmentLauncher.launch("image/*");
            }
        });

        // 4. Buscar en la web
        sheetView.findViewById(R.id.sheetItemWebSearch).setOnClickListener(v -> {
            bottomSheet.dismiss();
            etMessagePrompt.setText("Buscar informacion sobre ");
            etMessagePrompt.setSelection(etMessagePrompt.getText().length());
            showKeyboard();
        });

        // 5. Plantillas de diseno
        sheetView.findViewById(R.id.sheetItemTemplates).setOnClickListener(v -> {
            bottomSheet.dismiss();
            etMessagePrompt.setText("Quiero ver opciones de diseno para ");
            etMessagePrompt.setSelection(etMessagePrompt.getText().length());
            showKeyboard();
        });

        bottomSheet.show();
    }

    private void mostrarBitmapSeleccionado(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            if (ivAttachmentThumbnail != null) {
                ivAttachmentThumbnail.setImageBitmap(bitmap);
            }
            if (layoutAttachmentPreview != null) {
                layoutAttachmentPreview.setVisibility(View.VISIBLE);
            }

            int maxW = 800;
            if (bitmap.getWidth() > maxW) {
                int newH = (int) (bitmap.getHeight() * ((float) maxW / bitmap.getWidth()));
                bitmap = Bitmap.createScaledBitmap(bitmap, maxW, newH, true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            selectedAttachmentBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            selectedAttachmentMimeType = "image/jpeg";
            selectedAttachmentUri = null;

            btnSend.setVisibility(View.VISIBLE);
            btnVoiceMode.setVisibility(View.GONE);
            btnMic.setVisibility(View.GONE);

            Toast.makeText(this, "Foto adjuntada correctamente", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo procesar la foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarAdjuntoSeleccionado(Uri uri) {
        selectedAttachmentUri = uri;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap != null) {
                if (ivAttachmentThumbnail != null) {
                    ivAttachmentThumbnail.setImageBitmap(bitmap);
                }
                if (layoutAttachmentPreview != null) {
                    layoutAttachmentPreview.setVisibility(View.VISIBLE);
                }

                // Escalar y convertir a Base64 para Gemini multimodal
                int maxW = 800;
                if (bitmap.getWidth() > maxW) {
                    int newH = (int) (bitmap.getHeight() * ((float) maxW / bitmap.getWidth()));
                    bitmap = Bitmap.createScaledBitmap(bitmap, maxW, newH, true);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                selectedAttachmentBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                selectedAttachmentMimeType = "image/jpeg";

                btnSend.setVisibility(View.VISIBLE);
                btnVoiceMode.setVisibility(View.GONE);
                btnMic.setVisibility(View.GONE);

                Toast.makeText(this, "Imagen adjuntada correctamente", Toast.LENGTH_SHORT).show();
            } else {
                if (ivAttachmentThumbnail != null) {
                    ivAttachmentThumbnail.setImageResource(android.R.drawable.ic_menu_save);
                }
                if (layoutAttachmentPreview != null) {
                    layoutAttachmentPreview.setVisibility(View.VISIBLE);
                }
                selectedAttachmentMimeType = getContentResolver().getType(uri);
                if (selectedAttachmentMimeType == null) {
                    selectedAttachmentMimeType = "application/octet-stream";
                }

                btnSend.setVisibility(View.VISIBLE);
                btnVoiceMode.setVisibility(View.GONE);
                btnMic.setVisibility(View.GONE);

                Toast.makeText(this, "Archivo adjuntado correctamente", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo cargar el archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarAdjunto() {
        selectedAttachmentUri = null;
        selectedAttachmentBase64 = null;
        selectedAttachmentMimeType = null;
        if (layoutAttachmentPreview != null) {
            layoutAttachmentPreview.setVisibility(View.GONE);
        }
        if (ivAttachmentThumbnail != null) {
            ivAttachmentThumbnail.setImageDrawable(null);
        }
        boolean hasText = etMessagePrompt.getText().toString().trim().length() > 0;
        btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnVoiceMode.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnMic.setVisibility(hasText ? View.GONE : View.VISIBLE);
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
        if (text.isEmpty() && selectedAttachmentUri != null) {
            text = "Adjunto esta imagen de mi negocio para el diseno web.";
        }
        if (text.isEmpty()) return;

        // Si es una conversacion nueva, crear el registro en la BD
        if (currentConversationId <= 0) {
            String tituloInicial = generarTituloConversacion(text);
            currentConversationId = historialDao.crearConversacion(tituloInicial, currentNiche);
        }

        centerHeroLayout.setVisibility(View.GONE);
        bannerPromo.setVisibility(View.GONE);
        rvChatMessages.setVisibility(View.VISIBLE);

        String imgBase64 = selectedAttachmentBase64;
        String mime = selectedAttachmentMimeType;
        String uriStr = selectedAttachmentUri != null ? selectedAttachmentUri.toString() : null;

        limpiarAdjunto();

        ChatMessage userMsg = new ChatMessage(currentConversationId, text, ChatMessage.TYPE_USER);
        if (uriStr != null) {
            userMsg.setImageUri(uriStr);
        }
        chatMessages.add(userMsg);
        historialDao.insertarMensaje(currentConversationId, userMsg);

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.smoothScrollToPosition(chatMessages.size() - 1);
        etMessagePrompt.setText("");

        respondWithAi(text, imgBase64, mime);
    }

    private void respondWithAi(String userPrompt) {
        respondWithAi(userPrompt, null, null);
    }

    private String generarTituloConversacion(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("restauran") || lower.contains("bar") || lower.contains("comida")) return "Web Restaurante";
        if (lower.contains("taller") || lower.contains("mecanic") || lower.contains("coche")) return "Web Taller Mecanico";
        if (lower.contains("veterina") || lower.contains("mascota") || lower.contains("animal")) return "Web Veterinaria";
        if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construc")) return "Web Reformas";
        if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho")) return "Web Abogados";
        if (lower.contains("dental") || lower.contains("dentist") || lower.contains("diente")) return "Web Dental";

        if (prompt.length() > 30) {
            return prompt.substring(0, 30) + "...";
        }
        return prompt;
    }

    private void respondWithAi(String userPrompt, String imageBase64, String mimeType) {
        ChatMessage aiBubble = new ChatMessage(currentConversationId, "LogiDesign esta pensando...", ChatMessage.TYPE_AI);
        chatMessages.add(aiBubble);
        int aiIndex = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(aiIndex);
        rvChatMessages.smoothScrollToPosition(aiIndex);

        List<ChatMessage> previousHistory = new ArrayList<>();
        if (chatMessages.size() > 2) {
            previousHistory.addAll(chatMessages.subList(0, chatMessages.size() - 2));
        }

        if (selectedModelIndex == 0) {
            String promptOllama = (imageBase64 != null)
                    ? "(El usuario adjunto una imagen de su negocio) " + userPrompt
                    : userPrompt;
            ollamaService.enviarMensaje(MainActivity.this, previousHistory, promptOllama, new OllamaService.OllamaCallback() {
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
            // Google Gemini online con soporte multimodal
            geminiService.enviarMensaje(previousHistory, userPrompt, imageBase64, mimeType, new GeminiService.GeminiCallback() {
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
        actualizarQuickChips();

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
        actualizarQuickChips();
    }



    private boolean esSaludoPuro(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|hey|qu[ee] tal|buenos d[ii]as|buenas tardes|buenas noches)[!.,\\s]*$");
    }

    private boolean esPeticionAjenaAWeb(String text) {
        String lower = text.toLowerCase().trim();
        // 1. Peticiones de busqueda, investigacion o consulta general
        if (lower.matches(".*\\b(busca|busques|buscar|buscame|informaci[oo]n|informacionn|investiga|investigar|averigua|averiguar|googlear|consulta|consultar|explica|cu[ee]ntame)\\b.*")) {
            return true;
        }
        // 2. Chistes, clima, recetas, tareas escolares, ocio
        if (lower.matches(".*\\b(chiste|chistes|broma|clima|tiempo|temperatura|receta|recetas|cocinar|tarea|tareas|matem[aa]ticas|ecuaci[oo]n|suma|resta|multiplica|divide|canci[oo]n|pel[ii]cula|noticia|noticias|f[uu]tbol|juego|adivinanza|poema|historia|cuento|traducci[oo]n|traduce)\\b.*")) {
            return true;
        }
        // 3. Preguntas conversacionales ajenas a webs ("quien es", "que es", "como se hace", etc.)
        if (lower.matches("^(qui[ee]n|qui[ee]nes|qu[ee] es|c[oo]mo se hace|cu[aa]ndo|d[oo]nde est[aa]|por qu[ee]|cu[aa]l es el|cu[aa]l es la|dime la hora|dime el tiempo).*") 
                && !lower.contains("web") && !lower.contains("pagina") && !lower.contains("sitio")) {
            return true;
        }
        return false;
    }

    private boolean esNombreNegocioValido(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        if (lower.isEmpty() || lower.length() > 45) return false;

        // 1. Conceptos o menciones de paginas web / creacion / apps (incluyendo erratas como 'apgina', 'pagna')
        if (lower.matches(".*\\b(web|p[aa]gina|apgina|pagna|pajina|sitio|diseno|disena|crear|hacer|haz|app|aplicaci[oo]n)\\b.*")) {
            return false;
        }

        // 2. Frases que empiezan con articulos de peticion, oraciones o preposiciones ("una de...", "para un...")
        if (lower.matches("^(un|una|el|la|los|las|de|para|quiero|necesito|busca|busques|buscar|dime|hazme|puedes|podr[ii]as|ayuda|ay[uu]dame|c[oo]mo|qu[ee]|qui[ee]n|d[oo]nde|por qu[ee]|para qu[ee]|no s[ee]|me gustar[ii]a|por favor|averigua|investiga|consulta|haz)\\b.*")) {
            return false;
        }

        // 3. Nombres de nichos genericos solos ("dentista", "restaurante", "abogados", etc.)
        if (lower.matches("^(dentista|dentistas|dental|odontolog[ii]a|restaurante|restaurantes|bar|bares|cafeter[ii]a|taller|talleres|mec[aa]nico|reformas|construcci[oo]n|abogado|abogados|legal|veterinaria|veterinario|cl[ii]nica)$")) {
            return false;
        }

        // 4. Frases con estructuras compuestas o comas indicando lista de servicios ("ortodoncias, cirugias")
        if (lower.contains(",") || lower.matches(".*\\b(que busques|que hagas|que me digas|una informaci[oo]n|porque|para que|no entiendo|estoy probando|de dentista|de restaurante|de taller|de abogado|de reformas|de veterinaria)\\b.*")) {
            return false;
        }

        // 5. Saludo o texto generico
        if (esSaludoOGenerico(lower)) return false;

        return true;
    }

    private boolean esPeticionCrearWeb(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches(".*\\b(p[aa]gina|apgina|pagna|web|sitio|diseno web|crear web|hacer web|hacer una web|crear una web|hacer una p[aa]gina|crear una p[aa]gina|quiero una web|una web|una p[aa]gina|una apgina)\\b.*");
    }

    private boolean esSaludoOGenerico(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches("^(hola|buenas|buenos d[ii]as|buenas tardes|buenas noches|hey|qu[ee] tal|saludos|quiero hacer una p[aa]gina|quiero una web|hacer una web|crear web|necesito una web|ok|vale|est[aa] bien|comenzar|empezar|ayuda|ay[uu]dame).*");
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
            case "taller": return "Taller Mecanico & Automocion";
            case "veterinaria": return "Clinica Veterinaria & Cuidado Animal";
            case "reformas": return "Reformas Integrales & Construccion";
            case "abogados": return "Despacho de Abogados & Asesoria Legal";
            case "dental": return "Clinica Dental & Odontologia";
            case "restaurantes":
            default: return "Restaurante & Gastronomia";
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
        actualizarQuickChips();
        String response = engine.processMessage(userPrompt);
        syncStateFromEngine();

        // Actualizar el titulo de la conversacion si ya se detecto o refino el nicho/nombre
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

        // CASO 1: Exactamente 4 lineas limpias (Nombre, Telefono, Direccion, Servicios)
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

        // CASO 2: Procesamiento secuencial inteligente de cada linea (con o sin etiquetas)
        for (String l : limpias) {
            String clean = limpiarPrefijo(l);
            if (clean.isEmpty() || esSaludoOGenerico(clean)) continue;
            String lower = clean.toLowerCase();

            // 1. Telefono (con etiqueta o por numero claro de 9 digitos)
            if (lower.matches("^(2[.)\\-]|2  |tel[ee]fono|whatsapp|tlf|celular):?\\s*.*") || (clean.matches(".*[0-9]{8,}.*") && !esDireccionTexto(clean))) {
                Matcher phoneMatcher = Pattern.compile("(\\+?[0-9]{2,3}[\\s.-]?)?([6789][0-9]{8})|[0-9]{9}").matcher(clean);
                if (phoneMatcher.find() && currentTelefono.isEmpty()) {
                    currentTelefono = phoneMatcher.group().trim();
                    continue;
                }
            }

            // 2. Direccion fisica (con etiqueta o por nombre de calle/via/municipio)
            if (lower.matches("^(3[.)\\-]|3  |direcci[oo]n|ciudad|ubicaci[oo]n):?\\s*.*") || esDireccionTexto(clean)) {
                String val = clean.replaceFirst("^(?i)(3[.)\\-]|3  |direcci[oo]n|ciudad|ubicaci[oo]n):?\\s*", "").trim();
                if (!val.isEmpty() && currentDireccion.isEmpty()) {
                    currentDireccion = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 3. Etiqueta explicita de Nombre
            if (lower.matches("^(1[.)\\-]|1  |nombre|negocio|empresa|marca):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(1[.)\\-]|1  |nombre|negocio|empresa|marca):?\\s*", "").trim();
                if (!val.isEmpty() && esNombreNegocioValido(val)) {
                    currentNombre = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 4. Etiqueta explicita de Servicios / Especialidades
            if (lower.matches("^(4[.)\\-]|4  |servicios?|especialidades?|platos?):?\\s*.*")) {
                String val = clean.replaceFirst("^(?i)(4[.)\\-]|4  |servicios?|especialidades?|platos?):?\\s*", "").trim();
                if (!val.isEmpty()) {
                    currentServicios = sanitizarTextoSeguro(val);
                    continue;
                }
            }

            // 5. Linea sin etiquetas:
            // Si parece una lista de servicios/especialidades (tiene comas, ' y ', o palabras de oficio como ortodoncias, cirugias, asados, etc.):
            boolean pareceServicios = clean.contains(",") || clean.contains(" y ") || lower.matches(".*\\b(ortodoncia|ortodoncias|cirug[ii]a|cirugias|cirugias|implante|implantes|limpieza|blanqueamiento|pr[oo]tesis|revisi[oo]n|asado|asados|men[uu]|tapa|tapas|pizza|pizzas|reforma|reformas|fontaner[ii]a|electricidad|pintura|chapa|freno|frenos|aceite|neum[aa]tico|neumaticos|juicio|divorcio|herencia|laboral|penal|vacuna|vacunas|desparasitaci[oo]n|ecograf[ii]a)\\b.*");

            if (pareceServicios && currentServicios.isEmpty()) {
                currentServicios = sanitizarTextoSeguro(clean);
            }
            // Si falta el nombre y la linea es un nombre de negocio valido:
            else if (currentNombre.isEmpty() && esNombreNegocioValido(clean)) {
                currentNombre = sanitizarTextoSeguro(clean);
            }
            // Si falta la direccion y parece direccion:
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
                .replaceFirst("^(?i)(nombre|negocio|tel[ee]fono|whatsapp|direcci[oo]n|ciudad|servicios?|especialidades?|platos?):?\\s*", "")
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
        // Prefijos y palabras de vias
        if (lower.matches(".*\\b(calle|c/|c\\.|avda|av\\.|avenida|plaza|pza|paseo|poligono|poligono|crta|carretera|barrio|camino|urb|urbanizacion|urbanizacion|portal|no|numero|numero|piso)\\b.*")) {
            return true;
        }
        // Via con numero y municipio (ej: "Juan xxIII 6 Basauri", "Abaroeta 10 Basauri", etc.)
        if (lower.matches(".*[0-9]{1,4}\\s+[a-zaeioun]+.*") || lower.matches(".*\\b(basauri|bilbao|madrid|barcelona|valencia|sevilla|zaragoza|malaga|malaga|murcia|palma|alicante|cordoba|cordoba|valladolid|vigo|gijon|gijon|vitoria|granada|oviedo|santander|burgos|pamplona|san sebastian|donostia)\\b.*")) {
            return true;
        }
        return false;
    }

    private boolean esServicioOPlatoTexto(String t) {
        if (t == null) return false;
        String lower = t.toLowerCase();
        return lower.matches(".*\\b(asado|asados|brasa|horno|carne|carnes|lechazo|chuleton|pescado|tapa|tapas|menu|menu|paella|pizza|pasta|marisco|postre|reparacion|reparacion|mantenimiento|motor|freno|frenos|rueda|ruedas|diagnosis|reforma|reformas|obra|obras|pintura|cocina|cocinas|bano|banos|derecho|civil|penal|herencia|herencias|divorcio|divorcios|diente|dientes|sonrisa|implante|implantes|ortodoncia|vacuna|vacunas|consulta|cirugia|cirugia)\\b.*");
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

    private void actualizarQuickChips() {
        if (layoutQuickChips == null || scrollQuickChips == null) return;
        layoutQuickChips.removeAllViews();

        List<String> chips = new ArrayList<>();

        if (currentNiche == null || currentNiche.isEmpty()) {
            chips.add("Restaurantes");
            chips.add("Taller");
            chips.add("Veterinaria");
            chips.add("Reformas");
            chips.add("Abogados");
            chips.add("Dental");
        } else if (!currentTelefono.isEmpty() && (currentDireccion.isEmpty() || currentServicios.isEmpty())) {
            chips.add("Omitir este paso");
        }

        if (chips.isEmpty()) {
            scrollQuickChips.setVisibility(View.GONE);
        } else {
            scrollQuickChips.setVisibility(View.VISIBLE);
            for (String texto : chips) {
                TextView chip = new TextView(this);
                chip.setText(texto);
                chip.setTextSize(13);
                chip.setTextColor(getResources().getColor(R.color.pill_text));
                chip.setBackgroundResource(R.drawable.bg_pill_button);
                chip.setPadding(32, 14, 32, 14);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMarginEnd(16);
                chip.setLayoutParams(lp);

                chip.setOnClickListener(v -> {
                    String clean = texto.equalsIgnoreCase("Omitir este paso") ? "omitir" : texto;
                    etMessagePrompt.setText(clean);
                    sendMessage();
                });

                layoutQuickChips.addView(chip);
            }
        }
    }

    private void showConfigurarOllamaDialog() {
        String currentHost = OllamaService.getOllamaHost(this);
        final EditText input = new EditText(this);
        input.setText(currentHost);
        input.setHint("10.0.2.2:11434 o 192.168.1.X:11434");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Servidor Ollama")
                .setMessage("Ingresa la IP y puerto de tu PC:\n- Emulador: 10.0.2.2:11434\n- Movil fisico: Tu IP local (ej. 192.168.1.50:11434)")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoHost = input.getText().toString().trim();
                    OllamaService.setOllamaHost(this, nuevoHost);
                    Toast.makeText(this, "Servidor guardado: " + OllamaService.getOllamaHost(this), Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Probar", (dialog, which) -> {
                    String testHost = input.getText().toString().trim();
                    Toast.makeText(this, "Probando conexion con Ollama...", Toast.LENGTH_SHORT).show();
                    ollamaService.probarConexion(this, testHost, new OllamaService.OllamaCallback() {
                        @Override
                        public void onSuccess(String responseText) {
                            Toast.makeText(MainActivity.this, "Conexion exitosa con Ollama!", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Fallo: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showModelSelectorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Motor")
                .setSingleChoiceItems(logiDesignModels, selectedModelIndex, (dialog, which) -> {
                    selectedModelIndex = which;
                    tvModelName.setText(logiDesignModels[selectedModelIndex]);
                    dialog.dismiss();
                    if (selectedModelIndex == 0) {
                        showConfigurarOllamaDialog();
                    } else {
                        Toast.makeText(this, "Seleccionado: " + logiDesignModels[selectedModelIndex], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Configurar IP", (dialog, which) -> showConfigurarOllamaDialog())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showProDialog() {
        new AlertDialog.Builder(this)
                .setTitle("LogiDesign Pro")
                .setMessage("Funciones avanzadas, generador completo de sitios web y exportacion directa de codigo.")
                .setPositiveButton("Aceptar", (dialog, which) -> Toast.makeText(this, "Suscripcion activada", Toast.LENGTH_SHORT).show())
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
                new DemoNiche("TALLER MECANICO", android.graphics.Color.parseColor("#EA580C"), android.graphics.Typeface.MONOSPACE),
                new DemoNiche("VETERINARIA", android.graphics.Color.parseColor("#16A34A"), android.graphics.Typeface.DEFAULT_BOLD),
                new DemoNiche("ABOGADOS", android.graphics.Color.parseColor("#1E3A8A"), android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)),
                new DemoNiche("CLINICA DENTAL", android.graphics.Color.parseColor("#0284C7"), android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD))
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