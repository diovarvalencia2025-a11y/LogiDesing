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

public class ProjectsActivity extends AppCompatActivity {

    private ImageButton btnMenuProjects;
    private ImageButton btnFilterProjects;
    private EditText etSearchProjects;
    private LinearLayout fabNewProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_projects);

        setupWindowInsets();
        initViews();
        setupListeners();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProjects), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        btnMenuProjects = findViewById(R.id.btnMenuProjects);
        btnFilterProjects = findViewById(R.id.btnFilterProjects);
        etSearchProjects = findViewById(R.id.etSearchProjects);
        fabNewProject = findViewById(R.id.fabNewProject);
    }

    private void setupListeners() {
        btnMenuProjects.setOnClickListener(v -> finish());

        btnFilterProjects.setOnClickListener(v -> 
                Toast.makeText(this, "Filtros de proyectos", Toast.LENGTH_SHORT).show()
        );

        fabNewProject.setOnClickListener(v -> {
            Toast.makeText(this, "Creando nuevo proyecto web...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
