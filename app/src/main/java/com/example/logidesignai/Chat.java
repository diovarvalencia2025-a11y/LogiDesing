package com.example.logidesignai;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.logidesignai.data.HistorialDao;

import java.util.ArrayList;
import java.util.List;

public class Chat extends AppCompatActivity implements ConversationAdapter.OnConversationClickListener {

    private ImageButton btnMenuChats;
    private ImageButton btnFilterChats;
    private ImageButton btnSelectChats;
    private EditText etSearchChats;
    private LinearLayout fabNewChat;
    private RecyclerView rvHistoryChats;
    private TextView tvEmptyChatsHint;
    private HistorialDao historialDao;
    private ConversationAdapter conversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        setupWindowInsets();
        initViews();
        setupListeners();
        cargarHistorial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        btnMenuChats = findViewById(R.id.btnMenuChats);
        btnFilterChats = findViewById(R.id.btnFilterChats);
        btnSelectChats = findViewById(R.id.btnSelectChats);
        etSearchChats = findViewById(R.id.etSearchChats);
        fabNewChat = findViewById(R.id.fabNewChat);
        rvHistoryChats = findViewById(R.id.rvHistoryChats);
        tvEmptyChatsHint = findViewById(R.id.tvEmptyChatsHint);

        historialDao = new HistorialDao(this);
        rvHistoryChats.setLayoutManager(new LinearLayoutManager(this));
        conversationAdapter = new ConversationAdapter(new ArrayList<>(), this);
        rvHistoryChats.setAdapter(conversationAdapter);
    }

    private void cargarHistorial() {
        if (historialDao == null) return;
        List<ConversationItem> conversaciones = historialDao.obtenerConversaciones(null);
        if (conversaciones == null || conversaciones.isEmpty()) {
            tvEmptyChatsHint.setVisibility(View.VISIBLE);
            rvHistoryChats.setVisibility(View.GONE);
        } else {
            tvEmptyChatsHint.setVisibility(View.GONE);
            rvHistoryChats.setVisibility(View.VISIBLE);
            conversationAdapter.actualizarDatos(conversaciones);
        }
    }

    private void setupListeners() {
        btnMenuChats.setOnClickListener(v -> finish());

        btnFilterChats.setOnClickListener(v -> 
                Toast.makeText(this, "Filtros de conversaciones", Toast.LENGTH_SHORT).show()
        );

        btnSelectChats.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Historial de conversaciones")
                    .setMessage("Deseas vaciar todas las conversaciones?")
                    .setPositiveButton("Vaciar todo", (dialog, which) -> {
                        historialDao.borrarHistorial();
                        cargarHistorial();
                        Toast.makeText(this, "Historial vaciado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Busqueda en tiempo real
        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (conversationAdapter != null) {
                    conversationAdapter.filtrar(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Boton nuevo chat
        fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("EXTRA_NUEVO_CHAT", true);
            intent.putExtra("EXTRA_CONVERSACION_ID", -1L);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onConversationClick(ConversationItem item) {
        // Abrir conversacion seleccionada
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("EXTRA_CONVERSACION_ID", item.getId());
        intent.putExtra("EXTRA_NUEVO_CHAT", false);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConversationLongClick(ConversationItem item) {
        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setItems(new String[]{"Eliminar conversacion", "Cancelar"}, (dialog, which) -> {
                    if (which == 0) {
                        historialDao.borrarConversacion(item.getId());
                        cargarHistorial();
                        Toast.makeText(this, "Conversacion eliminada", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}