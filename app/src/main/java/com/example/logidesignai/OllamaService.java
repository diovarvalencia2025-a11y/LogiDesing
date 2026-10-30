package com.example.logidesignai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OllamaService {

    public static final String DEFAULT_HOST = "10.0.2.2:11434";
    private static final String PREFS_NAME = "LogiDesignPrefs";
    private static final String PREF_OLLAMA_HOST = "ollama_host";
    private static final String MODEL_NAME = "qwen2.5-coder:7b";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface OllamaCallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public OllamaService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static String getOllamaHost(Context context) {
        if (context == null) return DEFAULT_HOST;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(PREF_OLLAMA_HOST, DEFAULT_HOST);
    }

    public static void setOllamaHost(Context context, String host) {
        if (context == null) return;
        String cleanHost = host != null ? host.trim() : "";
        if (cleanHost.startsWith("http://")) cleanHost = cleanHost.substring(7);
        if (cleanHost.startsWith("https://")) cleanHost = cleanHost.substring(8);
        if (cleanHost.endsWith("/")) cleanHost = cleanHost.substring(0, cleanHost.length() - 1);
        if (cleanHost.isEmpty()) cleanHost = DEFAULT_HOST;

        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(PREF_OLLAMA_HOST, cleanHost).apply();
    }

    public static String getChatUrl(Context context) {
        return "http://" + getOllamaHost(context) + "/api/chat";
    }

    public void probarConexion(Context context, String testHost, OllamaCallback callback) {
        String host = testHost != null && !testHost.trim().isEmpty() ? testHost.trim() : getOllamaHost(context);
        if (host.startsWith("http://")) host = host.substring(7);
        if (host.startsWith("https://")) host = host.substring(8);
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);

        String testUrl = "http://" + host + "/api/tags";
        Request request = new Request.Builder()
                .url(testUrl)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("No se pudo conectar con Ollama: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean ok = response.isSuccessful();
                int code = response.code();
                response.close();
                mainHandler.post(() -> {
                    if (ok) {
                        callback.onSuccess("Conexion exitosa con Ollama");
                    } else {
                        callback.onError("El servidor respondio con codigo: " + code);
                    }
                });
            }
        });
    }

    public void enviarMensaje(List<ChatMessage> historial, String nuevoMensaje, OllamaCallback callback) {
        enviarMensaje(null, historial, nuevoMensaje, callback);
    }

    public void enviarMensaje(Context context, List<ChatMessage> historial, String nuevoMensaje, OllamaCallback callback) {
        try {
            JSONObject rootJson = new JSONObject();
            rootJson.put("model", MODEL_NAME);
            rootJson.put("stream", false);

            JSONArray messagesArray = new JSONArray();

            // 1. Mensaje de Sistema: IA conversacional pero estrictamente limitada a los parametros de creacion web
            JSONObject sysObj = new JSONObject();
            sysObj.put("role", "system");
            sysObj.put("content", "Eres LogiDesign AI, un asistente y desarrollador web inteligente y cercano, especializado UNICAMENTE en crear paginas web para negocios locales.\n\n" +
                    "COMPORTAMIENTO Y PARAMETROS ESTRICTOS:\n" +
                    "1. Tono y personalidad: Habla con naturalidad, calidez y empatia con Diovar. Se un asistente inteligente y proactivo, nunca suenes robotico ni rigido.\n" +
                    "2. Enfoque y limites estrictos: No te salgas bajo ninguna circunstancia de la creacion de paginas web para negocios locales. Si te piden chistes, tareas escolares, recetas de comida, noticias, clima o cualquier tema ajeno, explica educadamente que tu unico proposito es disenar paginas web profesionales para negocios locales y preguntale si desea crear una web.\n" +
                    "3. Nichos de la plataforma: Solo trabajas con estos 6 nichos locales:\n" +
                    "   - restaurantes, taller, veterinaria, reformas, abogados o dental.\n\n" +
                    "FLUJO PASO A PASO (PREGUNTA POR PREGUNTA - NUNCA TODAS JUNTAS):\n" +
                    "- Saludo inicial: Si Diovar solo saluda ('hola', 'buenas'), saludale cordialmente y preguntale: 'Que quieres hacer hoy?'\n" +
                    "- Paso 0: Al indicar que quiere una web, preguntale de que sector o nicho se trata.\n" +
                    "- Paso 1 (Nombre - OBLIGATORIO): Con el nicho elegido, pregunta SOLAMENTE por el nombre comercial o marca. Si envia un numero telefonico, adviertele que es un numero y vuelve a pedir el nombre comercial. No avances sin el nombre.\n" +
                    "- Paso 2 (Telefono/WhatsApp - OBLIGATORIO): Con el nombre registrado, pregunta SOLAMENTE por el telefono o WhatsApp de contacto. Insiste cordialmente hasta obtener un numero valido.\n" +
                    "- Paso 3 (Direccion - OPCIONAL): Pregunta por la direccion fisica o ciudad, indicando que si es a domicilio o no desea ponerla puede decir 'omitir'. Si se omite, usa un texto neutro ('Servicio local y a domicilio').\n" +
                    "- Paso 4 (Especialidades - OPCIONAL): Pregunta por los servicios o especialidades principales, indicando que puede decir 'omitir' para usar las recomendadas del nicho.\n\n" +
                    "REGLAS CRITICAS ANTI-DUPLICADOS:\n" +
                    "- El telefono es UNICAMENTE el telefono. NUNCA coloques el telefono en la direccion o en los servicios.\n" +
                    "- El nombre comercial es UNICAMENTE el nombre. NUNCA pongas el nombre en la direccion ni en las especialidades.\n\n" +
                    "DISPARADOR FINAL:\n" +
                    "Cuando tengas el proceso completo, felicita con un resumen claro y anade OBLIGATORIAMENTE al final de tu respuesta esta etiqueta tecnica:\n" +
                    "[LOGIDESIGN_WEB_READY:nicho|nombre|telefono|direccion|servicios]\n" +
                    "(nicho debe ser uno de los 6: restaurantes, taller, veterinaria, reformas, abogados o dental).\n" +
                    "Prohibido usar las palabras 'demo' o 'plantilla'. Para el cliente la web se crea y programa con IA.");
            messagesArray.put(sysObj);

            // 2. Historial de mensajes previos
            for (ChatMessage msg : historial) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
                msgObj.put("content", msg.getText());
                messagesArray.put(msgObj);
            }

            // 3. Nuevo mensaje del usuario
            JSONObject newMsgObj = new JSONObject();
            newMsgObj.put("role", "user");
            newMsgObj.put("content", nuevoMensaje);
            messagesArray.put(newMsgObj);

            rootJson.put("messages", messagesArray);

            RequestBody body = RequestBody.create(rootJson.toString(), JSON_MEDIA_TYPE);
            String targetUrl = getChatUrl(context);
            Request request = new Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Error conectando con Ollama en tu PC: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("Ollama respondio codigo: " + response.code()));
                        return;
                    }

                    try {
                        String bodyStr = response.body().string();
                        JSONObject resJson = new JSONObject(bodyStr);
                        JSONObject messageObj = resJson.getJSONObject("message");
                        String contenido = messageObj.getString("content").trim();
                        mainHandler.post(() -> callback.onSuccess(contenido));
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Error procesando respuesta de Ollama: " + ex.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Error al armar la peticion: " + e.getMessage()));
        }
    }
}
