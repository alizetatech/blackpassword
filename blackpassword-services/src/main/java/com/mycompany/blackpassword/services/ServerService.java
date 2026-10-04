package com.mycompany.blackpassword.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de enlace con la Fortaleza Digital (EncryptedCompanyServer).
 * Adaptado para despliegue en GlassFish (Puerto 8080 / Context: fortaleza).
 * 
 * SEGURIDAD: Los métodos que aceptan passwords usan char[] para evitar
 * que las contraseñas permanezcan en String Pool.
 */
public class ServerService {

    private static ServerService instance;
    private final OkHttpClient client;
    private final Gson gson;
    private String jwtToken;
    private String baseUrl = "http://localhost:8080/fortaleza/api/v1";

    private ServerService() {
        this.gson = new Gson();
        this.client = createSecureOkHttpClient();
    }

    public static synchronized ServerService getInstance() {
        if (instance == null) {
            instance = new ServerService();
        }
        return instance;
    }

    /**
     * Login seguro usando char[] para la contraseña.
     * La contraseña se limpia después de ser usada.
     */
    public boolean login(String username, char[] password) throws IOException {
        byte[] passwordBytes = toBytes(password);
        Arrays.fill(password, (char) 0);
        
        try {
            return loginInternal(username, passwordBytes);
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    @Deprecated
    public boolean login(String username, String password) throws IOException {
        return login(username, password.toCharArray());
    }

    private boolean loginInternal(String username, byte[] passwordHash) throws IOException {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty("username", username);
        loginJson.addProperty("password_hash", Base64.getEncoder().encodeToString(passwordHash));

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

    /**
     * Registro seguro usando char[] para la contraseña.
     * La contraseña se limpia después de ser usada.
     */
    public boolean register(String username, char[] password) throws IOException {
        byte[] passwordBytes = toBytes(password);
        Arrays.fill(password, (char) 0);
        
        try {
            return registerInternal(username, passwordBytes);
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    @Deprecated
    public boolean register(String username, String password) throws IOException {
        return register(username, password.toCharArray());
    }

    private boolean registerInternal(String username, byte[] passwordHash) throws IOException {
        JsonObject regJson = new JsonObject();
        regJson.addProperty("username", username);
        regJson.addProperty("password_hash", Base64.getEncoder().encodeToString(passwordHash));

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

    private byte[] toBytes(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] result = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
        Arrays.fill(bb.array(), (byte) 0);
        return result;
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

    private OkHttpClient createSecureOkHttpClient() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((java.security.KeyStore) null);
            
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
            
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create secure HTTP client", e);
        }
    }

    public boolean isConnected() { return jwtToken != null; }
    public void logout() { this.jwtToken = null; }
    public void setBaseUrl(String url) { this.baseUrl = url; }
}
