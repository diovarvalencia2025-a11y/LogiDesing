package com.example.logidesignai;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AssetUtils {

    /**
     * Lee el contenido completo de un archivo de texto alojado en app/src/main/assets/
     * @param context Contexto de la aplicación o actividad
     * @param filePath Ruta relativa dentro de assets (ej: "templates/restaurantes/index.html")
     * @return El contenido como String en formato UTF-8
     */
    public static String leerAsset(Context context, String filePath) {
        try {
            InputStream is = context.getAssets().open(filePath);
            int size = is.available();
            byte[] buffer = new byte[size];
            int readBytes = is.read(buffer);
            is.close();
            if (readBytes > 0) {
                return new String(buffer, StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
