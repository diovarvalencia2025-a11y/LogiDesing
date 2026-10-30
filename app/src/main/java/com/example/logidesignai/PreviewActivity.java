package com.example.logidesignai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class PreviewActivity extends AppCompatActivity {

    public static final String EXTRA_NICHE = "EXTRA_NICHE";
    public static final String EXTRA_NOMBRE = "EXTRA_NOMBRE";
    public static final String EXTRA_TELEFONO = "EXTRA_TELEFONO";
    public static final String EXTRA_DIRECCION = "EXTRA_DIRECCION";
    public static final String EXTRA_SERVICIOS = "EXTRA_SERVICIOS";

    private TextView tvPreviewTitle;
    private TextView tvPreviewSubtitle;
    private ImageButton btnBackPreview;
    private ImageButton btnChangeLogo;
    private ImageButton btnChangeColor;
    private ImageButton btnCopyCode;

    private TextView tabPreview;
    private TextView tabCode;
    private WebView webViewPreview;
    private ScrollView scrollCodePreview;
    private TextView tvCodePreview;

    private String generatedHtml = "";
    private String customBaseUrl = "";
    private ActivityResultLauncher<String> pickImageLauncher;

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

        // Registrar selector de imagen para carga de logo
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                aplicarLogoDesdeUri(uri);
            }
        });

        initViews();
        loadAndCustomizeTemplate();
        setupListeners();
    }

    private void initViews() {
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        tvPreviewSubtitle = findViewById(R.id.tvPreviewSubtitle);
        btnBackPreview = findViewById(R.id.btnBackPreview);
        btnChangeLogo = findViewById(R.id.btnChangeLogo);
        btnChangeColor = findViewById(R.id.btnChangeColor);
        btnCopyCode = findViewById(R.id.btnCopyCode);

        tabPreview = findViewById(R.id.tabPreview);
        tabCode = findViewById(R.id.tabCode);
        webViewPreview = findViewById(R.id.webViewPreview);
        scrollCodePreview = findViewById(R.id.scrollCodePreview);
        tvCodePreview = findViewById(R.id.tvCodePreview);

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
        tvPreviewSubtitle.setText("Web Oficial | " + niche.toUpperCase());

        String folderName = mapearCarpetaNicho(niche);
        String templatePath = "templates/" + folderName + "/index.html";
        customBaseUrl = "file:///android_asset/templates/" + folderName + "/";

        String rawHtml = AssetUtils.leerAsset(this, templatePath);
        if (rawHtml.isEmpty()) {
            rawHtml = AssetUtils.leerAsset(this, "templates/restaurantes/index.html");
            customBaseUrl = "file:///android_asset/templates/restaurantes/";
        }

        String cleanPhone = telefono.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 9 && !cleanPhone.startsWith("34")) {
            cleanPhone = "34" + cleanPhone;
        }
        String whatsappLink = "https://wa.me/" + cleanPhone;

        String htmlServicios = generarServiciosHtml(servicios, folderName);

        generatedHtml = rawHtml
                .replace("{{NOMBRE_NEGOCIO}}", nombre)
                .replace("{{TELEFONO}}", telefono)
                .replace("{{ENLACE_WHATSAPP}}", whatsappLink)
                .replace("{{DIRECCION}}", direccion)
                .replace("{{SERVICIOS}}", htmlServicios);

        webViewPreview.loadDataWithBaseURL(customBaseUrl, generatedHtml, "text/html", "UTF-8", null);
        tvCodePreview.setText(generatedHtml);
    }

    private String mapearCarpetaNicho(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("burdina") || lower.contains("asador")) {
            return "burdina";
        } else if (lower.contains("restauran") || lower.contains("bar") || lower.contains("comida")) {
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

    private String generarServiciosHtml(String servicios, String folder) {
        if (servicios != null && servicios.startsWith("<div")) {
            return servicios;
        }

        if (folder == null) folder = "restaurantes";

        switch (folder.toLowerCase()) {
            case "taller": {
                String t1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Mantenimiento Oficial & Revisiones";
                return "<!-- SERVICIO 1: MOTOR Y MANTENIMIENTO -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1511919884226-fd3cad34687c?auto=format&fit=crop&w=600&q=80\" alt=\"" + t1 + "\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">" + t1 + "</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Cambio de aceite sintetico, filtros de aceite, aire, habitaculo y combustible, revision de 30 puntos de seguridad y sellado del libro.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Pedir cita taller\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 2: DIAGNOSIS ELECTRONICA -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1552519507-da3b142c6e3d?auto=format&fit=crop&w=600&q=80\" alt=\"Diagnosis Electronica Multimarca\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><rect width=\"20\" height=\"14\" x=\"2\" y=\"3\" rx=\"2\"/><line x1=\"8\" x2=\"16\" y1=\"21\" y2=\"21\"/><line x1=\"12\" x2=\"12\" y1=\"17\" y2=\"21\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Diagnosis Electronica Multimarca</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Lectura y borrado de averias OBD, testigos encendidos de motor/ABS/Airbag, comprobacion de sensores e inyeccion electronica.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar diagnosis\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 3: FRENOS Y NEUMATICOS -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=600&q=80\" alt=\"Frenos, Suspension y Neumaticos\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><circle cx=\"12\" cy=\"12\" r=\"4\"/><line x1=\"4.93\" x2=\"9.17\" y1=\"4.93\" y2=\"9.17\"/><line x1=\"14.83\" x2=\"19.07\" y1=\"14.83\" y2=\"19.07\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Frenos, Suspension y Neumaticos</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Sustitucion de pastillas y discos de freno, amortiguadores, neumaticos de primeras marcas y alineacion computerizada 3D.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Revisar frenos y ruedas\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>";
            }

            case "veterinaria": {
                String v1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Consulta General & Vacunacion";
                return "<!-- SERVICIO 1: CONSULTA Y PREVENCION -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?auto=format&fit=crop&w=600&q=80\" alt=\"" + v1 + "\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z\"/><path d=\"m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">" + v1 + "</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Revisiones periodicas de salud, cartilla oficial de vacunacion, implantacion de microchip, desparasitacion y chequeos geriatricos.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-emerald-700 hover:text-emerald-800 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Pedir cita veterinaria\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 2: DIAGNOSTICO Y RADIOGRAFIA -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1576201836106-db1758fd1c97?auto=format&fit=crop&w=600&q=80\" alt=\"Diagnostico por Imagen & Laboratorio\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><rect width=\"18\" height=\"18\" x=\"3\" y=\"3\" rx=\"2\"/><circle cx=\"9\" cy=\"9\" r=\"2\"/><path d=\"m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Diagnostico por Imagen &amp; Laboratorio</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Ecografia abdominal de alta resolucion, radiografia digital directa y analisis de sangre/orina en el mismo dia.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-emerald-700 hover:text-emerald-800 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar pruebas diagnosticas\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 3: CIRUGIA Y QUIROFANO -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=600&q=80\" alt=\"Cirugia General & Hospitalizacion\" class=\"w-full h-44 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <div class=\"w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center mb-3 -mt-11 relative z-10 shadow-md\">\n" +
                        "        <svg width=\"20\" height=\"20\" class=\"w-5 h-5\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z\"/></svg>\n" +
                        "      </div>\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Cirugia General &amp; Hospitalizacion</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Quirofano monitorizado con anestesia inhalatoria, esterilizaciones seguras y zona de hospitalizacion con vigilancia constante.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#cita\" class=\"text-emerald-700 hover:text-emerald-800 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar cirugias\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>";
            }

            case "reformas": {
                String r1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Reformas de Cocinas a Medida";
                return "<!-- SERVICIO 1: COCINAS -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1556911220-e15b29be8c8f?auto=format&fit=crop&w=600&q=80\" alt=\"" + r1 + "\" class=\"w-full h-48 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">" + r1 + "</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Diseno ergonomico, islas centrales, encimeras de porcelanico o cuarzo, fontaneria renovada e iluminacion LED bajo muebles.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#presupuesto\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar reforma de cocina\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 2: BANOS -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1620626011761-996317b8d101?auto=format&fit=crop&w=600&q=80\" alt=\"Banos y Cambio de Banera\" class=\"w-full h-48 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Banos &amp; Cambio de Banera</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Platos de ducha de resina antideslizantes, mamparas a medida, griferia empotrada y alicatados modernos rectificados.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#presupuesto\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar reforma de bano\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- SERVICIO 3: PISOS INTEGRALES -->\n" +
                        "<div class=\"bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between\">\n" +
                        "  <div>\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=600&q=80\" alt=\"Reformas Integrales de Viviendas\" class=\"w-full h-48 object-cover\">\n" +
                        "    <div class=\"p-6\">\n" +
                        "      <h3 class=\"font-extrabold text-slate-900 text-lg mb-2\">Reformas Integrales de Viviendas</h3>\n" +
                        "      <p class=\"text-slate-600 text-xs sm:text-sm leading-relaxed\">\n" +
                        "        Redistribucion de espacios abiertos, demolicion de tabiques, aislamiento acustico/termico, suelos laminados y carpinteria.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"px-6 pb-6 pt-2 border-t border-slate-100\">\n" +
                        "    <a href=\"#presupuesto\" class=\"text-orange-600 hover:text-orange-700 font-bold text-xs flex items-center gap-1\">\n" +
                        "      Consultar reforma integral\n" +
                        "    </a>\n" +
                        "  </div>\n" +
                        "</div>";
            }

            case "abogados": {
                String a1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Derecho Bancario y Financiero";
                return "<!-- Tarjeta 1: Especialidad Principal -->\n" +
                        "<div class=\"bg-white rounded-xl overflow-hidden border border-[#e8e4dc] shadow-sm hover:shadow-md transition flex flex-col\">\n" +
                        "  <div class=\"relative h-44 overflow-hidden bg-slate-100\">\n" +
                        "    <img src=\"assets/card-bancario.jpg\" alt=\"" + a1 + "\" class=\"w-full h-full object-cover transition-transform duration-500 hover:scale-105\">\n" +
                        "    <div class=\"absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent\"></div>\n" +
                        "    <div class=\"absolute bottom-3 left-3 bg-white/90 backdrop-blur-sm p-1.5 rounded-md text-amber-700 shadow\">\n" +
                        "      <svg class=\"w-5 h-5\" fill=\"none\" stroke=\"currentColor\" viewBox=\"0 0 24 24\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4\" /></svg>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"p-6 flex-1 flex flex-col justify-between\">\n" +
                        "    <div>\n" +
                        "      <h3 class=\"font-serif text-xl font-bold text-slate-900 mb-3 leading-snug\">" + a1 + "</h3>\n" +
                        "      <ul class=\"space-y-2 text-xs sm:text-sm text-slate-600\">\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Clausulas abusivas e hipotecas</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Reclamacion de gastos hipotecarios</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Defensa legal especializada</span></li>\n" +
                        "      </ul>\n" +
                        "    </div>\n" +
                        "    <div class=\"mt-6 pt-4 border-t border-slate-100\">\n" +
                        "      <a href=\"#contacto\" class=\"text-xs font-semibold text-amber-700 hover:text-amber-800 flex items-center gap-1\">\n" +
                        "        <span>Consultar este servicio</span>\n" +
                        "      </a>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- Tarjeta 2: Civil e Inmobiliario -->\n" +
                        "<div class=\"bg-white rounded-xl overflow-hidden border border-[#e8e4dc] shadow-sm hover:shadow-md transition flex flex-col\">\n" +
                        "  <div class=\"relative h-44 overflow-hidden bg-slate-100\">\n" +
                        "    <img src=\"assets/card-civil.jpg\" alt=\"Derecho Civil e Inmobiliario\" class=\"w-full h-full object-cover transition-transform duration-500 hover:scale-105\">\n" +
                        "    <div class=\"absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent\"></div>\n" +
                        "    <div class=\"absolute bottom-3 left-3 bg-white/90 backdrop-blur-sm p-1.5 rounded-md text-amber-700 shadow\">\n" +
                        "      <svg class=\"w-5 h-5\" fill=\"none\" stroke=\"currentColor\" viewBox=\"0 0 24 24\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6\" /></svg>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"p-6 flex-1 flex flex-col justify-between\">\n" +
                        "    <div>\n" +
                        "      <h3 class=\"font-serif text-xl font-bold text-slate-900 mb-3 leading-snug\">Derecho Civil e Inmobiliario</h3>\n" +
                        "      <ul class=\"space-y-2 text-xs sm:text-sm text-slate-600\">\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Contratos de compraventa y alquiler</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Reclamacion de cantidades impagadas</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Propiedad horizontal y desahucios</span></li>\n" +
                        "      </ul>\n" +
                        "    </div>\n" +
                        "    <div class=\"mt-6 pt-4 border-t border-slate-100\">\n" +
                        "      <a href=\"#contacto\" class=\"text-xs font-semibold text-amber-700 hover:text-amber-800 flex items-center gap-1\">\n" +
                        "        <span>Consultar este servicio</span>\n" +
                        "      </a>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- Tarjeta 3: Penal y Delitos -->\n" +
                        "<div class=\"bg-white rounded-xl overflow-hidden border border-[#e8e4dc] shadow-sm hover:shadow-md transition flex flex-col\">\n" +
                        "  <div class=\"relative h-44 overflow-hidden bg-slate-100\">\n" +
                        "    <img src=\"assets/card-penal.jpg\" alt=\"Derecho Penal\" class=\"w-full h-full object-cover transition-transform duration-500 hover:scale-105\">\n" +
                        "    <div class=\"absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent\"></div>\n" +
                        "    <div class=\"absolute bottom-3 left-3 bg-white/90 backdrop-blur-sm p-1.5 rounded-md text-amber-700 shadow\">\n" +
                        "      <svg class=\"w-5 h-5\" fill=\"none\" stroke=\"currentColor\" viewBox=\"0 0 24 24\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z\" /></svg>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"p-6 flex-1 flex flex-col justify-between\">\n" +
                        "    <div>\n" +
                        "      <h3 class=\"font-serif text-xl font-bold text-slate-900 mb-3 leading-snug\">Derecho Penal</h3>\n" +
                        "      <ul class=\"space-y-2 text-xs sm:text-sm text-slate-600\">\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Defensa y acusacion particular</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Asistencia rapida en comisaria y juzgado</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Delitos de trafico y alcoholemias</span></li>\n" +
                        "      </ul>\n" +
                        "    </div>\n" +
                        "    <div class=\"mt-6 pt-4 border-t border-slate-100\">\n" +
                        "      <a href=\"#contacto\" class=\"text-xs font-semibold text-amber-700 hover:text-amber-800 flex items-center gap-1\">\n" +
                        "        <span>Consultar este servicio</span>\n" +
                        "      </a>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "</div>\n\n" +
                        "<!-- Tarjeta 4: Familia y Sucesiones -->\n" +
                        "<div class=\"bg-white rounded-xl overflow-hidden border border-[#e8e4dc] shadow-sm hover:shadow-md transition flex flex-col\">\n" +
                        "  <div class=\"relative h-44 overflow-hidden bg-slate-100\">\n" +
                        "    <img src=\"assets/card-familia.jpg\" alt=\"Familia y Herencias\" class=\"w-full h-full object-cover transition-transform duration-500 hover:scale-105\">\n" +
                        "    <div class=\"absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent\"></div>\n" +
                        "    <div class=\"absolute bottom-3 left-3 bg-white/90 backdrop-blur-sm p-1.5 rounded-md text-amber-700 shadow\">\n" +
                        "      <svg class=\"w-5 h-5\" fill=\"none\" stroke=\"currentColor\" viewBox=\"0 0 24 24\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z\" /></svg>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"p-6 flex-1 flex flex-col justify-between\">\n" +
                        "    <div>\n" +
                        "      <h3 class=\"font-serif text-xl font-bold text-slate-900 mb-3 leading-snug\">Familia &amp; Herencias</h3>\n" +
                        "      <ul class=\"space-y-2 text-xs sm:text-sm text-slate-600\">\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Divorcios de mutuo acuerdo y contenciosos</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Custodias y pensiones alimenticias</span></li>\n" +
                        "        <li class=\"flex items-start gap-2\"><span class=\"text-amber-600 font-bold\">-</span><span>Herencias y testamentos</span></li>\n" +
                        "      </ul>\n" +
                        "    </div>\n" +
                        "    <div class=\"mt-6 pt-4 border-t border-slate-100\">\n" +
                        "      <a href=\"#contacto\" class=\"text-xs font-semibold text-amber-700 hover:text-amber-800 flex items-center gap-1\">\n" +
                        "        <span>Consultar este servicio</span>\n" +
                        "      </a>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "</div>";
            }

            case "dental": {
                String d1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Dental Veneers";
                return "<!-- Service 1 (Especialidad / Usuario) -->\n" +
                        "<div class=\"flex-1 min-w-[calc(50%-4px)] md:min-w-0 rounded-xl md:rounded-2xl p-3 md:p-5 flex flex-col justify-between bg-white/95 backdrop-blur-md shadow-sm\">\n" +
                        "  <h3 class=\"text-xl md:text-3xl font-bold leading-[1.05] text-black\">" + d1 + "</h3>\n" +
                        "  <span class=\"self-end w-8 h-8 md:w-10 md:h-10 rounded-full border border-black text-black flex items-center justify-center text-xs md:text-sm font-semibold\">01</span>\n" +
                        "</div>\n\n" +
                        "<!-- Service 2 -->\n" +
                        "<div class=\"flex-1 min-w-[calc(50%-4px)] md:min-w-0 rounded-xl md:rounded-2xl p-3 md:p-5 flex flex-col justify-between bg-white/70 backdrop-blur-sm shadow-sm\">\n" +
                        "  <h3 class=\"text-xl md:text-3xl font-bold leading-[1.05] text-black\">Implantes<br>&amp; Coronas</h3>\n" +
                        "  <span class=\"self-end w-8 h-8 md:w-10 md:h-10 rounded-full border border-black text-black flex items-center justify-center text-xs md:text-sm font-semibold\">02</span>\n" +
                        "</div>\n\n" +
                        "<!-- Service 3 -->\n" +
                        "<div class=\"flex-1 min-w-[calc(50%-4px)] md:min-w-0 rounded-xl md:rounded-2xl p-3 md:p-5 flex flex-col justify-between bg-white/70 backdrop-blur-sm shadow-sm\">\n" +
                        "  <h3 class=\"text-xl md:text-3xl font-bold leading-[1.05] text-black\">Blanqueamiento<br>Laser</h3>\n" +
                        "  <span class=\"self-end w-8 h-8 md:w-10 md:h-10 rounded-full border border-black text-black flex items-center justify-center text-xs md:text-sm font-semibold\">03</span>\n" +
                        "</div>\n\n" +
                        "<!-- Service 4 -->\n" +
                        "<div class=\"flex-1 min-w-[calc(50%-4px)] md:min-w-0 rounded-xl md:rounded-2xl p-3 md:p-5 flex flex-col justify-between bg-white/70 backdrop-blur-sm shadow-sm\">\n" +
                        "  <h3 class=\"text-xl md:text-3xl font-bold leading-[1.05] text-black\">Ortodoncia<br>Invisible</h3>\n" +
                        "  <span class=\"self-end w-8 h-8 md:w-10 md:h-10 rounded-full border border-black text-black flex items-center justify-center text-xs md:text-sm font-semibold\">04</span>\n" +
                        "</div>";
            }

            case "restaurantes":
            default: {
                String res1 = (servicios != null && !servicios.trim().isEmpty()) ? servicios.trim() : "Chuleton Tomahawk Madurado a la Brasa";
                return "<!-- BENTO HERO CARD: LA OBRA MAESTRA DEL CHEF / SERVICIO DEL CLIENTE -->\n" +
                        "<div class=\"md:col-span-2 bento-card rounded-3xl overflow-hidden flex flex-col justify-between group dish-item\" data-category=\"carnes\" data-title=\"" + res1 + "\" data-desc=\"Corte noble seleccionado a mano con maduracion artesanal de 45 dias en camara propia. Sellado a fuego vivo sobre carbon de encina y lena de roble con mantequilla trufada y flor de sal del Himalaya.\" data-price=\"42,00 EUR\" data-pair=\"Ribera del Duero Gran Reserva 2018\" data-img=\"https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1200&q=85\">\n" +
                        "  <div class=\"overflow-hidden h-64 sm:h-72 relative\">\n" +
                        "    <img src=\"https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1200&q=85\" alt=\"" + res1 + "\" class=\"w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 ease-out\">\n" +
                        "    <div class=\"absolute inset-0 bg-gradient-to-t from-[#110e0c] via-transparent to-black/30\"></div>\n" +
                        "    <div class=\"absolute top-4 left-4 flex gap-2\">\n" +
                        "      <span class=\"bg-[#070605]/85 backdrop-blur-md border border-[#dfbe82]/50 px-3 py-1 rounded-full text-[10px] text-[#dfbe82] font-bold uppercase tracking-widest\">Obra Maestra del Chef</span>\n" +
                        "      <span class=\"bg-black/60 backdrop-blur-md px-3 py-1 rounded-full text-[10px] text-stone-300 font-medium uppercase tracking-wider hidden sm:inline-block\">Maduracion 45 Dias</span>\n" +
                        "    </div>\n" +
                        "    <div class=\"absolute bottom-4 right-4 bg-[#070605]/90 backdrop-blur-md px-4 py-1.5 rounded-full border border-[#dfbe82]/40\">\n" +
                        "      <span class=\"font-cinzel text-[#dfbe82] font-bold text-lg\">42,00 EUR</span>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "  <div class=\"p-6 sm:p-8 flex flex-col justify-between flex-1\">\n" +
                        "    <div>\n" +
                        "      <h3 class=\"font-serif-luxury font-bold text-2xl sm:text-3xl text-white mb-2 leading-tight\">" + res1 + "</h3>\n" +
                        "      <p class=\"text-stone-300 text-xs sm:text-sm leading-relaxed font-light mb-4\">\n" +
                        "        Corte noble seleccionado a mano con maduracion artesanal de 45 dias en camara propia. Sellado a fuego vivo sobre carbon de encina y lena de roble con mantequilla trufada y flor de sal del Himalaya.\n" +
                        "      </p>\n" +
                        "    </div>\n" +
                        "    <div class=\"pt-4 border-t border-stone-800/80 flex flex-wrap items-center justify-between gap-3\">\n" +
                        "      <button onclick=\"abrirModalGourmet(this.closest('.dish-item'))\" class=\"text-xs text-[#dfbe82] hover:text-white font-semibold tracking-wider uppercase flex items-center gap-1.5 transition\">\n" +
                        "        <span>Ver Ficha de Cata &amp; Maridaje</span>\n" +
                        "        <span>+</span>\n" +
                        "      </button>\n" +
                        "      <a href=\"#reserva\" class=\"btn-luxury-outline px-6 py-2 rounded-full text-[11px] uppercase tracking-wider font-semibold\">\n" +
                        "        Reservar esta Creacion\n" +
                        "      </a>\n" +
                        "    </div>\n" +
                        "  </div>\n" +
                        "</div>";
            }
        }
    }

    private void aplicarLogoDesdeUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) {
                Toast.makeText(this, "No se pudo cargar la imagen del logo", Toast.LENGTH_SHORT).show();
                return;
            }

            int maxW = 400;
            if (bitmap.getWidth() > maxW) {
                int newH = (int) (bitmap.getHeight() * ((float) maxW / bitmap.getWidth()));
                bitmap = Bitmap.createScaledBitmap(bitmap, maxW, newH, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos);
            byte[] bytes = baos.toByteArray();
            String base64Logo = "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);

            if (generatedHtml.contains("{{LOGO_URL}}")) {
                generatedHtml = generatedHtml.replace("{{LOGO_URL}}", base64Logo);
            } else {
                generatedHtml = generatedHtml.replaceFirst("(?i)src=[\"'][^\"']*logo[^\"']*[\"']", "src=\"" + base64Logo + "\"");
            }

            webViewPreview.loadDataWithBaseURL(customBaseUrl, generatedHtml, "text/html", "UTF-8", null);
            tvCodePreview.setText(generatedHtml);

            Toast.makeText(this, "Logotipo aplicado con exito", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar logo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showColorPaletteDialog() {
        final String[] coloresNombres = {
                "Azul Corporativo (#0284C7)",
                "Naranja Gastro/Taller (#EA580C)",
                "Verde Esmeralda (#059669)",
                "Violeta Moderno (#7C3AED)",
                "Dorado / Ambar (#D97706)",
                "Rojo Intenso (#DC2626)"
        };

        final String[] coloresHex = {
                "#0284C7",
                "#EA580C",
                "#059669",
                "#7C3AED",
                "#D97706",
                "#DC2626"
        };

        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Color de Marca")
                .setItems(coloresNombres, (dialog, which) -> {
                    String hex = coloresHex[which];
                    aplicarColorPrimario(hex, coloresNombres[which]);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aplicarColorPrimario(String hex, String nombreColor) {
        String cssInjection = "<style id=\"custom-brand-style\">" +
                ":root { --brand-primary: " + hex + " !important; } " +
                ".bg-orange-600, .bg-amber-600, .bg-emerald-700, .bg-amber-700 { background-color: " + hex + " !important; } " +
                ".text-orange-600, .text-amber-600, .text-emerald-700, .text-amber-700 { color: " + hex + " !important; } " +
                ".border-orange-600, .border-amber-600 { border-color: " + hex + " !important; } " +
                "button.btn-luxury-outline, a.btn-luxury-outline { border-color: " + hex + " !important; color: " + hex + " !important; } " +
                "</style>";

        if (generatedHtml.contains("<style id=\"custom-brand-style\">")) {
            generatedHtml = generatedHtml.replaceAll("<style id=\"custom-brand-style\">[\\s\\S]*?</style>", cssInjection);
        } else if (generatedHtml.contains("</head>")) {
            generatedHtml = generatedHtml.replace("</head>", cssInjection + "\n</head>");
        } else {
            generatedHtml = cssInjection + "\n" + generatedHtml;
        }

        webViewPreview.loadDataWithBaseURL(customBaseUrl, generatedHtml, "text/html", "UTF-8", null);
        tvCodePreview.setText(generatedHtml);

        Toast.makeText(this, "Color aplicado: " + nombreColor, Toast.LENGTH_SHORT).show();
    }

    private void setupListeners() {
        btnBackPreview.setOnClickListener(v -> finish());

        if (btnChangeLogo != null) {
            btnChangeLogo.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        if (btnChangeColor != null) {
            btnChangeColor.setOnClickListener(v -> showColorPaletteDialog());
        }

        // Alternar a Vista Previa
        tabPreview.setOnClickListener(v -> {
            tabPreview.setBackgroundColor(0xFF334155);
            tabPreview.setTextColor(0xFFFFFFFF);
            tabCode.setBackgroundColor(0xFF0F172A);
            tabCode.setTextColor(0xFF94A3B8);

            webViewPreview.setVisibility(View.VISIBLE);
            scrollCodePreview.setVisibility(View.GONE);
        });

        // Alternar a Codigo HTML
        tabCode.setOnClickListener(v -> {
            tabCode.setBackgroundColor(0xFF334155);
            tabCode.setTextColor(0xFFFFFFFF);
            tabPreview.setBackgroundColor(0xFF0F172A);
            tabPreview.setTextColor(0xFF94A3B8);

            webViewPreview.setVisibility(View.GONE);
            scrollCodePreview.setVisibility(View.VISIBLE);
        });

        // Copiar Codigo al Portapapeles
        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Codigo HTML LogiDesign AI", generatedHtml);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Codigo HTML copiado al portapapeles!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
