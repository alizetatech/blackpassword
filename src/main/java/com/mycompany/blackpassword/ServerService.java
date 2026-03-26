package com.mycompany.blackpassword;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de enlace con la Fortaleza Digital (EncryptedCompanyServer).
 * Adaptado para despliegue en GlassFish (Puerto 8080 / Context: fortaleza).
 */
public class ServerService {

    private static ServerService instance;
    private final OkHttpClient client;
    private final Gson gson;
    private String jwtToken;
    private String baseUrl = "http://localhost:8080/fortaleza/api/v1";

    private ServerService() {
        this.gson = new Gson();
        this.client = createUnsafeOkHttpClient();
    }

    public static synchronized ServerService getInstance() {
        if (instance == null) {
            instance = new ServerService();
        }
        return instance;
    }

    public boolean login(String username, String password) throws IOException {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty("username", username);
        loginJson.addProperty("password", password);

        RequestBody body = RequestBody.create(
                loginJson.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject respJson = gson.fromJson(response.body().string(), JsonObject.class);
                this.jwtToken = respJson.get("token").getAsString();
                return true;
            }
            return false;
        }
    }

    public boolean register(String username, String password) throws IOException {
        JsonObject regJson = new JsonObject();
        regJson.addProperty("username", username);
        regJson.addProperty("password", password);

        RequestBody body = RequestBody.create(
                regJson.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean storeSecret(String identifier, String content, String description) throws IOException {
        if (jwtToken == null) return false;

        JsonObject secretJson = new JsonObject();
        secretJson.addProperty("identifier", identifier);
        secretJson.addProperty("secretContent", content);
        secretJson.addProperty("description", description);

        RequestBody body = RequestBody.create(
                secretJson.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/vault/store")
                .addHeader("Authorization", "Bearer " + jwtToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public JsonArray getAllSecrets() throws IOException {
        if (jwtToken == null) throw new IOException("No hay sesión activa en la Fortaleza");

        Request request = new Request.Builder()
                .url(baseUrl + "/vault/list")
                .addHeader("Authorization", "Bearer " + jwtToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().string(), JsonArray.class);
            }
            throw new IOException("Error al recuperar datos: " + response.code());
        }
    }

    private OkHttpClient createUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isConnected() { return jwtToken != null; }
    public void logout() { this.jwtToken = null; }
    public void setBaseUrl(String url) { this.baseUrl = url; }
}
