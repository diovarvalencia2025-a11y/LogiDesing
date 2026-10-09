package com.example.logidesignai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {


    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String ENDPOINT_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public GeminiService() {
        this.httpClient = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void enviarMensaje(List<ChatMessage> historial, String nuevoMensaje, GeminiCallback callback) {
        try {
            JSONObject rootJson = new JSONObject();

            // 1. Instrucción del Sistema Oficial para LogiDesign AI
            JSONObject systemInstruction = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sysText = new JSONObject();
            sysText.put("text", "Eres LogiDesign AI, un diseñador y desarrollador web inteligente de clase mundial.\n" +
                    "Tu misión es guiar al usuario mediante una conversación amigable para crear su página web personalizada:\n" +
                    "- Habla de forma cálida, cercana, moderna y profesional.\n" +
                    "- NUNCA menciones las palabras 'demo', 'plantilla' ni 'template'. Para el cliente, estás diseñando y programando su web a medida con IA.\n" +
                    "- Paso 1: Pregúntale de qué temática o negocio es la web (ej: Restaurante, Dental, Taller Mecánico, Veterinaria, Reformas o Abogados).\n" +
                    "- Paso 2: Apenas el usuario diga el negocio, felicítale y pídele amablemente en un solo mensaje estos datos: 1. Nombre de la marca o negocio, 2. Teléfono o WhatsApp, 3. Dirección física o ciudad, 4. Servicios o especialidades principales.\n" +
                    "- Paso 3: Cuando el usuario te entregue esos datos (o la mayoría de ellos), felicítale entusiastamente diciéndole que su página web ha sido diseñada con éxito y está lista para ver y copiar el código.\n" +
                    "IMPORTANTE: Al final de tu mensaje en el Paso 3, agrega EXACTAMENTE esta etiqueta oculta con los datos extraídos para que la app abra la web:\n" +
                    "[LOGIDESIGN_WEB_READY:nicho|nombre|telefono|direccion|servicios]\n" +
                    "(Por ejemplo: [LOGIDESIGN_WEB_READY:restaurantes|Trattoria Don Luigi|+34 612 345 678|Calle Mayor 12, Madrid|Pizzas al horno y pastas caseras])\n" +
                    "- Mantén tus mensajes claros, concisos y fáciles de leer en pantalla de móvil.");
            sysParts.put(sysText);
            systemInstruction.put("parts", sysParts);
            rootJson.put("systemInstruction", systemInstruction);

            // 2. Historial de conversación
            JSONArray contentsArray = new JSONArray();

            for (ChatMessage msg : historial) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "model");
                JSONArray parts = new JSONArray();
                JSONObject partText = new JSONObject();
                partText.put("text", msg.getText());
                parts.put(partText);
                msgObj.put("parts", parts);
                contentsArray.put(msgObj);
            }

            // Añadir el nuevo mensaje actual
            JSONObject currentMsgObj = new JSONObject();
            currentMsgObj.put("role", "user");
            JSONArray curParts = new JSONArray();
            JSONObject curPartText = new JSONObject();
            curPartText.put("text", nuevoMensaje);
            curParts.put(curPartText);
            currentMsgObj.put("parts", curParts);
            contentsArray.put(currentMsgObj);

            rootJson.put("contents", contentsArray);

            RequestBody body = RequestBody.create(rootJson.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(ENDPOINT_URL)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Error de conexión: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("Respuesta del servidor: " + response.code()));
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject resJson = new JSONObject(responseBody);
                        JSONArray candidates = resJson.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < parts.length(); i++) {
                                JSONObject p = parts.getJSONObject(i);
                                if (p.has("text")) {
                                    sb.append(p.getString("text"));
                                }
                            }
                            String respuestaTexto = sb.toString().trim();
                            mainHandler.post(() -> callback.onSuccess(respuestaTexto));
                        } else {
                            mainHandler.post(() -> callback.onError("No se pudo obtener la respuesta de la IA."));
                        }
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Error al procesar respuesta: " + ex.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Error al preparar la solicitud: " + e.getMessage()));
        }
    }
}
