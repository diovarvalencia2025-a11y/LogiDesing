package com.example.logidesignai;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.List;

public class Chat extends AppCompatActivity {

    private ImageButton btnMenuChats;
    private ImageButton btnFilterChats;
    private ImageButton btnSelectChats;
    private EditText etSearchChats;
    private LinearLayout fabNewChat;
    private RecyclerView rvHistoryChats;
    private TextView tvEmptyChatsHint;
    private HistorialDao historialDao;

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
    }

    private void cargarHistorial() {
        if (historialDao == null) return;
        List<ChatMessage> mensajes = historialDao.obtenerHistorial();
        if (mensajes == null || mensajes.isEmpty()) {
            tvEmptyChatsHint.setVisibility(View.VISIBLE);
            rvHistoryChats.setVisibility(View.GONE);
        } else {
            tvEmptyChatsHint.setVisibility(View.GONE);
            rvHistoryChats.setVisibility(View.VISIBLE);
            rvHistoryChats.setLayoutManager(new LinearLayoutManager(this));
            rvHistoryChats.setAdapter(new ChatAdapter(mensajes));
            rvHistoryChats.scrollToPosition(mensajes.size() - 1);
        }
    }

    private void setupListeners() {
        btnMenuChats.setOnClickListener(v -> finish());

        btnFilterChats.setOnClickListener(v -> 
                Toast.makeText(this, "Filtros de conversaciones", Toast.LENGTH_SHORT).show()
        );

        btnSelectChats.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Historial")
                    .setMessage("¿Deseas vaciar todo el historial?")
                    .setPositiveButton("Vaciar", (dialog, which) -> {
                        historialDao.borrarHistorial();
                        cargarHistorial();
                        Toast.makeText(this, "Historial vaciado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        fabNewChat.setOnClickListener(v -> {
            Toast.makeText(this, "Iniciando nuevo chat...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("EXTRA_NUEVO_CHAT", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}