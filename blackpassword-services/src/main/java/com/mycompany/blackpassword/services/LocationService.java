package com.mycompany.blackpassword.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de Geolocalización y Rastreo de IP v1.1.
 * Obtiene la ubicación pública del usuario para análisis de amenazas con manejo seguro de JSON nulo.
 */
public class LocationService {

    private static final String API_URL = "https://ipapi.co/json/";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    /**
     * Obtiene los detalles de ubicación de la IP actual.
     * @return JsonObject con ip, city, region, country_name, etc.
     */
    public static JsonObject getCurrentLocation() {
        Request request = new Request.Builder().url(API_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonElement parsed = JsonParser.parseString(response.body().string());
                if (parsed != null && parsed.isJsonObject()) {
                    return parsed.getAsJsonObject();
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Error al obtener ubicación: " + e.getMessage());
        }
        return null;
    }

    private static String getSafeString(JsonObject obj, String key, String defaultVal) {
        if (obj == null || !obj.has(key)) return defaultVal;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) return defaultVal;
        return el.getAsString();
    }

    /**
     * Devuelve una cadena formateada de la ubicación.
     */
    public static String getLocationString() {
        JsonObject loc = getCurrentLocation();
        if (loc != null && !loc.has("error")) {
            return String.format("%s, %s (%s) - IP: %s", 
                getSafeString(loc, "city", "Ciudad N/D"),
                getSafeString(loc, "country_name", "País N/D"),
                getSafeString(loc, "org", "ISP N/D"),
                getSafeString(loc, "ip", "IP N/D")
            );
        }
        return "Ubicación Desconocida";
    }
}
