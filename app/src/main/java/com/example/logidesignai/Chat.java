package com.example.logidesignai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Chat extends AppCompatActivity {

    private ImageButton btnMenuChats;
    private ImageButton btnFilterChats;
    private ImageButton btnSelectChats;
    private EditText etSearchChats;
    private LinearLayout fabNewChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        setupWindowInsets();
        initViews();
        setupListeners();
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
    }

    private void setupListeners() {
        btnMenuChats.setOnClickListener(v -> finish());

        btnFilterChats.setOnClickListener(v -> 
                Toast.makeText(this, "Filtros de conversaciones", Toast.LENGTH_SHORT).show()
        );

        btnSelectChats.setOnClickListener(v -> 
                Toast.makeText(this, "Seleccionar conversaciones", Toast.LENGTH_SHORT).show()
        );

        fabNewChat.setOnClickListener(v -> {
            Toast.makeText(this, "Iniciando nuevo chat...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}