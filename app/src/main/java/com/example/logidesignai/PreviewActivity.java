package com.example.logidesignai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PreviewActivity extends AppCompatActivity {

    public static final String EXTRA_NICHE = "EXTRA_NICHE";
    public static final String EXTRA_NOMBRE = "EXTRA_NOMBRE";
    public static final String EXTRA_TELEFONO = "EXTRA_TELEFONO";
    public static final String EXTRA_DIRECCION = "EXTRA_DIRECCION";
    public static final String EXTRA_SERVICIOS = "EXTRA_SERVICIOS";

    private TextView tvPreviewTitle;
    private TextView tvPreviewSubtitle;
    private ImageButton btnBackPreview;
    private ImageButton btnCopyCode;

    private TextView tabPreview;
    private TextView tabCode;
    private WebView webViewPreview;
    private ScrollView scrollCodePreview;
    private TextView tvCodePreview;

    private String generatedHtml = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preview);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPreview), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadAndCustomizeTemplate();
        setupListeners();
    }

    private void initViews() {
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        tvPreviewSubtitle = findViewById(R.id.tvPreviewSubtitle);
        btnBackPreview = findViewById(R.id.btnBackPreview);
        btnCopyCode = findViewById(R.id.btnCopyCode);

        tabPreview = findViewById(R.id.tabPreview);
        tabCode = findViewById(R.id.tabCode);
        webViewPreview = findViewById(R.id.webViewPreview);
        scrollCodePreview = findViewById(R.id.scrollCodePreview);
        tvCodePreview = findViewById(R.id.tvCodePreview);

        // Configuración de WebView para soportar HTML5, CSS y Tailwind
        WebSettings settings = webViewPreview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webViewPreview.setWebViewClient(new WebViewClient());
    }

    private void loadAndCustomizeTemplate() {
        String niche = getIntent().getStringExtra(EXTRA_NICHE);
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
        String telefono = getIntent().getStringExtra(EXTRA_TELEFONO);
        String direccion = getIntent().getStringExtra(EXTRA_DIRECCION);
        String servicios = getIntent().getStringExtra(EXTRA_SERVICIOS);

        if (niche == null || niche.isEmpty()) niche = "restaurantes";
        if (nombre == null || nombre.isEmpty()) nombre = "Mi Negocio Digital";
        if (telefono == null || telefono.isEmpty()) telefono = "+34 600 000 000";
        if (direccion == null || direccion.isEmpty()) direccion = "Calle Principal, Ciudad";

        tvPreviewTitle.setText(nombre);
        tvPreviewSubtitle.setText("Web Oficial • " + niche.toUpperCase());

        // Mapear nombre de carpeta en assets/templates/
        String folderName = mapearCarpetaNicho(niche);
        String templatePath = "templates/" + folderName + "/index.html";
        String baseUrl = "file:///android_asset/templates/" + folderName + "/";

        // 1. Leer el HTML original desde assets
        String rawHtml = AssetUtils.leerAsset(this, templatePath);
        if (rawHtml.isEmpty()) {
            // Fallback en caso de que no encuentre esa plantilla exacta
            rawHtml = AssetUtils.leerAsset(this, "templates/restaurantes/index.html");
            baseUrl = "file:///android_asset/templates/restaurantes/";
        }

        // 2. Limpieza de teléfono para el enlace de WhatsApp
        String cleanPhone = telefono.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 9 && !cleanPhone.startsWith("34")) {
            cleanPhone = "34" + cleanPhone;
        }
        String whatsappLink = "https://wa.me/" + cleanPhone;

        // 3. Generar bloque de servicios HTML por defecto si no vienen completos
        if (servicios == null || servicios.trim().isEmpty()) {
            servicios = generarServiciosDefault(folderName);
        }

        // 4. Reemplazar todas las variables dinámicas
        generatedHtml = rawHtml
                .replace("{{NOMBRE_NEGOCIO}}", nombre)
                .replace("{{TELEFONO}}", telefono)
                .replace("{{ENLACE_WHATSAPP}}", whatsappLink)
                .replace("{{DIRECCION}}", direccion)
                .replace("{{SERVICIOS}}", servicios);

        // 5. Cargar en el WebView
        webViewPreview.loadDataWithBaseURL(baseUrl, generatedHtml, "text/html", "UTF-8", null);

        // 6. Colocar en el visor de código
        tvCodePreview.setText(generatedHtml);
    }

    private String mapearCarpetaNicho(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("restauran") || lower.contains("bar") || lower.contains("comida")) {
            return "restaurantes";
        } else if (lower.contains("dental") || lower.contains("dentist") || lower.contains("diente")) {
            return "dental";
        } else if (lower.contains("taller") || lower.contains("mecanic") || lower.contains("coche")) {
            return "taller";
        } else if (lower.contains("veterina") || lower.contains("mascota") || lower.contains("animal")) {
            return "veterinaria";
        } else if (lower.contains("reforma") || lower.contains("obra") || lower.contains("construc")) {
            return "reformas";
        } else if (lower.contains("abogado") || lower.contains("legal") || lower.contains("despacho")) {
            return "abogados";
        }
        return "restaurantes";
    }

    private String generarServiciosDefault(String folder) {
        return "<div class=\"p-6 bg-white rounded-2xl shadow-sm border border-slate-200\">\n" +
                "  <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Servicio Premium 1</h3>\n" +
                "  <p class=\"text-slate-600 text-sm leading-relaxed\">Atención personalizada y máxima calidad garantizada para nuestros clientes.</p>\n" +
                "</div>\n" +
                "<div class=\"p-6 bg-white rounded-2xl shadow-sm border border-slate-200\">\n" +
                "  <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Servicio Premium 2</h3>\n" +
                "  <p class=\"text-slate-600 text-sm leading-relaxed\">Soluciones adaptadas a tus necesidades con el mejor asesoramiento profesional.</p>\n" +
                "</div>\n" +
                "<div class=\"p-6 bg-white rounded-2xl shadow-sm border border-slate-200\">\n" +
                "  <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Servicio Premium 3</h3>\n" +
                "  <p class=\"text-slate-600 text-sm leading-relaxed\">Presupuesto transparente y entrega en tiempo récord con garantía oficial.</p>\n" +
                "</div>";
    }

    private void setupListeners() {
        btnBackPreview.setOnClickListener(v -> finish());

        // Alternar a Vista Previa
        tabPreview.setOnClickListener(v -> {
            tabPreview.setBackgroundColor(0xFF334155);
            tabPreview.setTextColor(0xFFFFFFFF);
            tabCode.setBackgroundColor(0xFF0F172A);
            tabCode.setTextColor(0xFF94A3B8);

            webViewPreview.setVisibility(View.VISIBLE);
            scrollCodePreview.setVisibility(View.GONE);
        });

        // Alternar a Código HTML
        tabCode.setOnClickListener(v -> {
            tabCode.setBackgroundColor(0xFF334155);
            tabCode.setTextColor(0xFFFFFFFF);
            tabPreview.setBackgroundColor(0xFF0F172A);
            tabPreview.setTextColor(0xFF94A3B8);

            webViewPreview.setVisibility(View.GONE);
            scrollCodePreview.setVisibility(View.VISIBLE);
        });

        // Copiar Código al Portapapeles
        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Código HTML LogiDesign AI", generatedHtml);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "¡Código HTML copiado al portapapeles!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
