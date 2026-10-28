package com.example.logidesignai;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AssetUtils {


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
