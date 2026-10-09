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

    // 10.0.2.2 es la dirección especial del emulador de Android para comunicarse con tu PC local (localhost)
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void enviarMensaje(List<ChatMessage> historial, String nuevoMensaje, OllamaCallback callback) {
        try {
            JSONObject rootJson = new JSONObject();
            rootJson.put("model", MODEL_NAME);
            rootJson.put("stream", false);

            JSONArray messagesArray = new JSONArray();

            // 1. Mensaje de Sistema
            JSONObject sysObj = new JSONObject();
            sysObj.put("role", "system");
            sysObj.put("content", "Eres LogiDesign AI, un asistente experto en creación de páginas web para negocios locales.\n" +
                    "- Sé conciso, amable y profesional.\n" +
                    "- NUNCA uses las palabras 'demo' o 'plantilla'. Para el cliente estás diseñando su página web a medida.\n" +
                    "- Si el usuario inicia la charla o saluda, pregúntale de qué negocio o temática es su web (ej: Restaurante, Taller Mecánico, Veterinaria, Reformas o Abogados).\n" +
                    "- Si el usuario menciona el negocio, felicítale y pídele en una sola lista: 1. Nombre de la empresa, 2. Teléfono o WhatsApp, 3. Dirección o ciudad, 4. Servicios principales.\n" +
                    "- Si te envía los datos, dile que su web está lista para previsualizar.");
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
