package com.mycompany.blackpassword.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Cliente HTTPS para interactuar con el backend de Firebase Cloud Functions v2.0.
 * Proporciona acceso Zero-Knowledge y autenticación mediante Firebase Auth REST API.
 */
public class CloudFunctionsClient {

    private static CloudFunctionsClient instance;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private String functionBaseUrl;
    private String firebaseApiKey;
    private String idToken;
    private String refreshToken;
    private String userUid;

    private CloudFunctionsClient() {
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized CloudFunctionsClient getInstance() {
        if (instance == null) {
            instance = new CloudFunctionsClient();
        }
        return instance;
    }

    public void configure(String functionBaseUrl, String firebaseApiKey) {
        if (functionBaseUrl != null) {
            this.functionBaseUrl = functionBaseUrl.endsWith("/") 
                    ? functionBaseUrl.substring(0, functionBaseUrl.length() - 1) 
                    : functionBaseUrl;
        }
        this.firebaseApiKey = firebaseApiKey;
    }

    public boolean isConfigured() {
        return functionBaseUrl != null && !functionBaseUrl.isBlank() 
                && firebaseApiKey != null && !firebaseApiKey.isBlank();
    }

    // -------------------------------------------------------------
    // AUTENTICACIÓN DIRECTA VÍA FIREBASE AUTH REST API
    // -------------------------------------------------------------

    public boolean signIn(String email, String password) throws IOException {
        String authUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;

        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("email", email);
        reqBody.addProperty("password", password);
        reqBody.addProperty("returnSecureToken", true);

        RequestBody body = RequestBody.create(reqBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(authUrl).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return false;
            }
            JsonObject respJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            this.idToken = respJson.get("idToken").getAsString();
            this.refreshToken = respJson.get("refreshToken").getAsString();
            this.userUid = respJson.get("localId").getAsString();
            return true;
        }
    }

    public boolean signUp(String email, String password) throws IOException {
        String authUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + firebaseApiKey;

        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("email", email);
        reqBody.addProperty("password", password);
        reqBody.addProperty("returnSecureToken", true);

        RequestBody body = RequestBody.create(reqBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(authUrl).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return false;
            }
            JsonObject respJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            this.idToken = respJson.get("idToken").getAsString();
            this.refreshToken = respJson.get("refreshToken").getAsString();
            this.userUid = respJson.get("localId").getAsString();
            return true;
        }
    }

    // -------------------------------------------------------------
    // PERFIL Y AUTENTICACIÓN AVANZADA EN CLOUD FUNCTIONS
    // -------------------------------------------------------------

    public JsonObject getProfile(String email) throws IOException {
        String path = "/auth/profile" + (email != null && !email.isBlank() 
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) 
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return JsonParser.parseString(response.body().string()).getAsJsonObject();
            }
        }
        return null;
    }

    public boolean updateProfile(JsonObject updates, String docPath) throws IOException {
        JsonObject payload = new JsonObject();
        payload.add("updates", updates);
        if (docPath != null) {
            payload.addProperty("docPath", docPath);
        }
        return executePost("/auth/profile/update", payload);
    }

    public boolean registerProfile(JsonObject profileData) throws IOException {
        return executePost("/auth/register-profile", profileData);
    }

    public String checkAccountStatus() throws IOException {
        Request request = buildAuthenticatedRequest("/auth/status", "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("status")) {
                    return resp.get("status").getAsString();
                }
            }
        }
        return "UNKNOWN";
    }

    public boolean triggerKillSwitch() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "COMPROMISED");
        return executePost("/auth/kill-switch", payload);
    }

    // -------------------------------------------------------------
    // OPERACIONES DE BÓVEDA EN CLOUD FUNCTIONS
    // -------------------------------------------------------------

    public boolean saveVaultItem(String id, String payloadCifrado, String metadataCifrada, String tag, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", id);
        payload.addProperty("payload_cifrado", payloadCifrado);
        if (metadataCifrada != null) payload.addProperty("metadata_cifrada", metadataCifrada);
        if (tag != null) payload.addProperty("tag", tag);
        if (email != null) payload.addProperty("email", email);

        return executePost("/vault/save", payload);
    }

    public boolean saveVaultItem(String id, String payloadCifrado, String metadataCifrada, String tag) throws IOException {
        return saveVaultItem(id, payloadCifrado, metadataCifrada, tag, null);
    }

    public JsonArray listVaultItems(String email) throws IOException {
        String path = "/vault/list" + (email != null && !email.isBlank() 
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) 
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    public JsonArray listVaultItems() throws IOException {
        return listVaultItems(null);
    }

    public boolean deleteVaultItem(String id, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", id);
        if (email != null) payload.addProperty("email", email);
        return executePost("/vault/delete", payload);
    }

    public boolean deleteVaultItem(String id) throws IOException {
        return deleteVaultItem(id, null);
    }

    // -------------------------------------------------------------
    // OPERACIONES DE WIREGUARD VPN
    // -------------------------------------------------------------

    public boolean saveWireguardProfile(String id, String profileName, String payloadCifrado, String address, String endpoint, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", id);
        payload.addProperty("nombre_perfil", profileName);
        payload.addProperty("payload_cifrado", payloadCifrado);
        payload.addProperty("address", address);
        payload.addProperty("endpoint", endpoint);
        if (email != null) payload.addProperty("email", email);
        return executePost("/wireguard/save", payload);
    }

    public boolean saveWireguardProfile(String id, String profileName, String payloadCifrado, String address, String endpoint) throws IOException {
        return saveWireguardProfile(id, profileName, payloadCifrado, address, endpoint, null);
    }

    public JsonArray listWireguardProfiles(String email) throws IOException {
        String path = "/wireguard/list" + (email != null && !email.isBlank()
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    public JsonArray listWireguardProfiles() throws IOException {
        return listWireguardProfiles(null);
    }

    public boolean deleteWireguardProfile(String id, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", id);
        if (email != null) payload.addProperty("email", email);
        return executePost("/wireguard/delete", payload);
    }

    public boolean deleteWireguardProfile(String id) throws IOException {
        return deleteWireguardProfile(id, null);
    }

    // -------------------------------------------------------------
    // OPERACIONES DE ARCHIVOS
    // -------------------------------------------------------------

    public JsonArray listFiles(String email) throws IOException {
        String path = "/files/list" + (email != null && !email.isBlank()
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    public boolean saveFile(String id, String metadataCifrada, String contenidoCifrado, String email) throws IOException {
        JsonObject payload = new JsonObject();
        if (id != null) payload.addProperty("id", id);
        payload.addProperty("metadata_cifrada", metadataCifrada);
        if (contenidoCifrado != null) payload.addProperty("contenido_cifrado", contenidoCifrado);
        if (email != null) payload.addProperty("email", email);
        return executePost("/files/save", payload);
    }

    public JsonObject getFile(String id, String email) throws IOException {
        String path = "/files/get?id=" + java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8)
                + (email != null ? "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return JsonParser.parseString(response.body().string()).getAsJsonObject();
            }
        }
        return null;
    }

    public boolean deleteFile(String id, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", id);
        if (email != null) payload.addProperty("email", email);
        return executePost("/files/delete", payload);
    }

    // -------------------------------------------------------------
    // OPERACIONES DE IMÁGENES
    // -------------------------------------------------------------

    public JsonArray listImages(String email) throws IOException {
        String path = "/images/list" + (email != null && !email.isBlank()
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    public boolean saveImage(String docId, String metadataCifrada, String imageBytesBase64, String storagePath, String email) throws IOException {
        JsonObject payload = new JsonObject();
        if (docId != null) payload.addProperty("docId", docId);
        payload.addProperty("metadata_cifrada", metadataCifrada);
        if (imageBytesBase64 != null) payload.addProperty("image_bytes_base64", imageBytesBase64);
        if (storagePath != null) payload.addProperty("storage_path", storagePath);
        if (email != null) payload.addProperty("email", email);
        return executePost("/images/save", payload);
    }

    public byte[] getImageContent(String storagePath) throws IOException {
        String path = "/images/content?storage_path=" + java.net.URLEncoder.encode(storagePath, java.nio.charset.StandardCharsets.UTF_8);
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().bytes();
            }
        }
        return null;
    }

    public boolean deleteImage(String docId, String storagePath, String email) throws IOException {
        JsonObject payload = new JsonObject();
        if (docId != null) payload.addProperty("docId", docId);
        if (storagePath != null) payload.addProperty("storage_path", storagePath);
        if (email != null) payload.addProperty("email", email);
        return executePost("/images/delete", payload);
    }

    // -------------------------------------------------------------
    // OPERACIONES DE BILLETERAS CRIPTO
    // -------------------------------------------------------------

    public JsonObject getWallet(String walletType, String network, String email) throws IOException {
        String path = "/wallets/get?wallet_type=" + walletType + "&network=" + network
                + (email != null ? "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return JsonParser.parseString(response.body().string()).getAsJsonObject();
            }
        }
        return null;
    }

    public boolean saveWallet(String walletType, String network, String payloadCifrado, String email) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("wallet_type", walletType);
        payload.addProperty("network", network);
        payload.addProperty("payload_cifrado", payloadCifrado);
        if (email != null) payload.addProperty("email", email);
        return executePost("/wallets/save", payload);
    }

    public JsonArray listWallets() throws IOException {
        Request request = buildAuthenticatedRequest("/wallets/list", "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    // -------------------------------------------------------------
    // AUDITORÍA Y REPORTES
    // -------------------------------------------------------------

    public JsonArray listAuditLogs(String email) throws IOException {
        String path = "/audit/list" + (email != null && !email.isBlank()
                ? "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                : "");
        Request request = buildAuthenticatedRequest(path, "GET", null);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject resp = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (resp.has("items")) return resp.getAsJsonArray("items");
            }
        }
        return new JsonArray();
    }

    // -------------------------------------------------------------
    // AUDITORÍA
    // -------------------------------------------------------------

    public boolean logAudit(String accion, String detallesCifrados) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("accion", accion);
            payload.addProperty("detalles_cifrados", detallesCifrados != null ? detallesCifrados : "");
            return executePost("/audit/log", payload);
        } catch (Exception ignored) {
            return false;
        }
    }

    // -------------------------------------------------------------
    // HELPERS HTTP
    // -------------------------------------------------------------

    private boolean executePost(String path, JsonObject jsonPayload) throws IOException {
        RequestBody body = RequestBody.create(jsonPayload.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = buildAuthenticatedRequest(path, "POST", body);

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    private Request buildAuthenticatedRequest(String path, String method, RequestBody body) {
        String url = functionBaseUrl + path;
        Request.Builder builder = new Request.Builder().url(url);

        if (idToken != null) {
            builder.addHeader("Authorization", "Bearer " + idToken);
        }

        if ("POST".equalsIgnoreCase(method)) {
            builder.post(body != null ? body : RequestBody.create(new byte[0], null));
        } else {
            builder.get();
        }

        return builder.build();
    }

    public String getUserUid() {
        return userUid;
    }

    public boolean isAuthenticated() {
        return idToken != null && !idToken.isBlank();
    }

    public void logout() {
        this.idToken = null;
        this.refreshToken = null;
        this.userUid = null;
    }
}
