package com.example.logidesignai;

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

    private static final String OLLAMA_URL = "http://10.0.2.2:11434/api/chat";
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

    public void enviarMensaje(List<ChatMessage> historial, String nuevoMensaje, OllamaCallback callback) {
        try {
            JSONObject rootJson = new JSONObject();
            rootJson.put("model", MODEL_NAME);
            rootJson.put("stream", false);

            JSONArray messagesArray = new JSONArray();

            // 1. Mensaje de Sistema: IA conversacional pero estrictamente limitada a los parámetros de creación web
            JSONObject sysObj = new JSONObject();
            sysObj.put("role", "system");
            sysObj.put("content", "Eres LogiDesign AI, un asistente y desarrollador web inteligente y cercano, especializado ÚNICAMENTE en crear páginas web para negocios locales.\n\n" +
                    "COMPORTAMIENTO Y PARÁMETROS ESTRICTOS:\n" +
                    "1. Tono y personalidad: Habla con naturalidad, calidez y empatía con Diovar. Sé un asistente inteligente y proactivo, nunca suenes robótico ni rígido.\n" +
                    "2. Enfoque y límites estrictos: No te salgas bajo ninguna circunstancia de la creación de páginas web para negocios locales. Si te piden chistes, tareas escolares, recetas de comida, noticias, clima o cualquier tema ajeno, explica educadamente que tu único propósito es diseñar páginas web profesionales para negocios locales y pregúntale si desea crear una web.\n" +
                    "3. Nichos de la plataforma: Solo trabajas con estos 6 nichos locales:\n" +
                    "   - restaurantes, taller, veterinaria, reformas, abogados o dental.\n\n" +
                    "FLUJO PASO A PASO (PREGUNTA POR PREGUNTA — NUNCA TODAS JUNTAS):\n" +
                    "- Saludo inicial: Si Diovar solo saluda ('hola', 'buenas'), salúdale cordialmente y pregúntale: '¿Qué quieres hacer hoy?'\n" +
                    "- Paso 0: Al indicar que quiere una web, pregúntale de qué sector o nicho se trata.\n" +
                    "- Paso 1 (Nombre - OBLIGATORIO): Con el nicho elegido, pregunta SOLAMENTE por el nombre comercial o marca. Si envía un número telefónico, adviértele que es un número y vuelve a pedir el nombre comercial. No avances sin el nombre.\n" +
                    "- Paso 2 (Teléfono/WhatsApp - OBLIGATORIO): Con el nombre registrado, pregunta SOLAMENTE por el teléfono o WhatsApp de contacto. Insiste cordialmente hasta obtener un número válido.\n" +
                    "- Paso 3 (Dirección - OPCIONAL): Pregunta por la dirección física o ciudad, indicando que si es a domicilio o no desea ponerla puede decir 'omitir'. Si se omite, usa un texto neutro ('Servicio local y a domicilio').\n" +
                    "- Paso 4 (Especialidades - OPCIONAL): Pregunta por los servicios o especialidades principales, indicando que puede decir 'omitir' para usar las recomendadas del nicho.\n\n" +
                    "REGLAS CRÍTICAS ANTI-DUPLICADOS:\n" +
                    "- El teléfono es ÚNICAMENTE el teléfono. NUNCA coloques el teléfono en la dirección o en los servicios.\n" +
                    "- El nombre comercial es ÚNICAMENTE el nombre. NUNCA pongas el nombre en la dirección ni en las especialidades.\n\n" +
                    "DISPARADOR FINAL:\n" +
                    "Cuando tengas el proceso completo, felicítale con un resumen claro y añade OBLIGATORIAMENTE al final de tu respuesta esta etiqueta técnica:\n" +
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
            Request request = new Request.Builder()
                    .url(OLLAMA_URL)
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
                        mainHandler.post(() -> callback.onError("Ollama respondió código: " + response.code()));
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
            mainHandler.post(() -> callback.onError("Error al armar la petición: " + e.getMessage()));
        }
    }


}
