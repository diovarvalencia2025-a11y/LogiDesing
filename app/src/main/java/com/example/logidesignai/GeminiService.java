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
    private static final String ENDPOINT_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
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

            // 1. Instrucción del Sistema: Libertad conversacional natural y límite estricto a páginas web
            JSONObject systemInstruction = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sysText = new JSONObject();
            sysText.put("text", "Eres LogiDesign AI, un asistente y desarrollador web inteligente especializado ÚNICAMENTE en diseñar y crear páginas web para negocios locales.\n\n" +
                    "Tu personalidad:\n" +
                    "- Habla con total naturalidad humana, calidez, soltura y cercanía. No suenes rígido, mecánico ni robotizado.\n" +
                    "- NUNCA menciones las palabras 'demo', 'plantilla' ni 'template'. Para el cliente, estás diseñando y programando su web a medida con IA.\n\n" +
                    "FLUJO CONVERSACIONAL ESTRICTO (PREGUNTA POR PREGUNTA — NUNCA TODAS JUNTAS):\n" +
                    "1. Saludo inicial: Si el usuario te saluda ('hola', 'buenas', 'qué tal', etc.), salúdale amigablemente y pregúntale: '¿Qué quieres hacer hoy?'.\n" +
                    "2. Enfoque exclusivo: Si pide algo ajeno a crear o diseñar páginas web locales (recetas, chistes, clima, tareas), dile educadamente que tu único propósito es diseñar y crear páginas web profesionales para negocios locales y pregúntale si desea crear una web.\n" +
                    "3. Paso 0 - Nicho: Pregunta el sector o tipo de negocio (solo trabajamos: restaurantes, taller, veterinaria, reformas, abogados o dental).\n" +
                    "4. Paso 1 - Nombre del Negocio (OBLIGATORIO): Cuando el nicho esté definido, pregunta SOLAMENTE por el nombre comercial o marca. Si el usuario te envía un número de teléfono, NO lo tomes como nombre; avísale con simpatía: 'Eso parece un número de teléfono. Indícame primero el nombre comercial de tu negocio:'. No avances hasta tener el nombre.\n" +
                    "5. Paso 2 - Teléfono / WhatsApp (OBLIGATORIO): Con el nombre ya fijado, pregunta SOLAMENTE por el número de teléfono o WhatsApp de contacto. Hasta que no dé un número de contacto válido, insiste cordialmente en este campo.\n" +
                    "6. Paso 3 - Dirección o Ciudad (OPCIONAL): Pregunta por la dirección o ciudad del negocio, aclarando que si atiende a domicilio o prefiere no ponerla puede decir 'omitir'. Si la omite, asigna un texto neutro del sector ('Servicio local y a domicilio' o 'Atención presencial').\n" +
                    "7. Paso 4 - Especialidades o Servicios (OPCIONAL): Pregunta por los servicios o especialidades principales, indicando que puede decir 'omitir' para usar las recomendadas del sector.\n\n" +
                    "REGLAS CRÍTICAS ANTI-DUPLICADOS:\n" +
                    "- El teléfono es ÚNICAMENTE el teléfono. NUNCA pongas el teléfono en la dirección o en los servicios.\n" +
                    "- El nombre comercial es ÚNICAMENTE el nombre. NUNCA pongas el nombre como dirección ni como especialidades.\n\n" +
                    "DISPARADOR FINAL:\n" +
                    "Cuando el proceso termine, felicítale cálidamente y agrega OBLIGATORIAMENTE al final de tu mensaje esta etiqueta técnica:\n" +
                    "[LOGIDESIGN_WEB_READY:nicho|nombre|telefono|direccion|servicios]\n" +
                    "(El nicho debe ser: restaurantes, taller, veterinaria, reformas, abogados o dental).\n" +
                    "Mantén tus respuestas breves, claras y fluidas para pantalla de móvil.");
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
