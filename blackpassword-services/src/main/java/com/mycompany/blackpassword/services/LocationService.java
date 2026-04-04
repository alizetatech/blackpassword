package com.mycompany.blackpassword.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de Geolocalización y Rastreo de IP v1.0.
 * Obtiene la ubicación pública del usuario para análisis de amenazas.
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
                return JsonParser.parseString(response.body().string()).getAsJsonObject();
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Error al obtener ubicación: " + e.getMessage());
        }
        return null;
    }

    /**
     * Devuelve una cadena formateada de la ubicación.
     */
    public static String getLocationString() {
        JsonObject loc = getCurrentLocation();
        if (loc != null && !loc.has("error")) {
            return String.format("%s, %s (%s) - IP: %s", 
                loc.get("city").getAsString(),
                loc.get("country_name").getAsString(),
                loc.get("org").getAsString(),
                loc.get("ip").getAsString()
            );
        }
        return "Ubicación Desconocida";
    }
}
