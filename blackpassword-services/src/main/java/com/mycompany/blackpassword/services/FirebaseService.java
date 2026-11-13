package com.mycompany.blackpassword.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio para gestionar la conexión con Firebase v13.0 (Cloud Functions & HTTPS Direct).
 * Soporta conexión directa a Cloud Functions mediante HTTPS TLS 1.3 con fallback
 * automático a Admin SDK para compatibilidad con paquetes anteriores.
 */
public class FirebaseService {

    private static Firestore customFirestore;
    private static boolean cloudFunctionsMode = false;
    private static String cachedProjectId = "No conectado";

    /**
     * Inicializa la conexión con Firebase / Cloud Functions a partir de la configuración descifrada.
     */
    public static void conectar(String jsonData) throws Exception {
        try {
            System.out.println("[FirebaseService] Inicializando conexión v13.0...");
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            // 1. Detectar si contiene configuración para Cloud Functions HTTPS
            String functionUrl = null;
            String apiKey = null;

            if (jsonObject.has("functionBaseUrl")) {
                functionUrl = jsonObject.get("functionBaseUrl").getAsString().trim();
            }

            if (jsonObject.has("apiKey")) {
                apiKey = jsonObject.get("apiKey").getAsString().trim();
            }

            if (jsonObject.has("project_id")) {
                cachedProjectId = jsonObject.get("project_id").getAsString().trim();
            } else if (jsonObject.has("projectId")) {
                cachedProjectId = jsonObject.get("projectId").getAsString().trim();
            }

            // Si no se especificó URL pero tenemos project_id, inferimos la URL estándar de Cloud Functions
            if ((functionUrl == null || functionUrl.isEmpty()) && !cachedProjectId.equals("No conectado")) {
                functionUrl = "https://us-central1-" + cachedProjectId + ".cloudfunctions.net/api";
            }

            if (functionUrl != null && !functionUrl.isEmpty()) {
                CloudFunctionsClient.getInstance().configure(functionUrl, apiKey != null ? apiKey : "");
                cloudFunctionsMode = true;
                System.out.println("[FirebaseService] Modo Cloud Functions HTTPS ACTIVO: " + functionUrl);
            }

            // 2. Si el JSON contiene credenciales de servicio (Service Account), inicializar también el SDK local
            if (jsonObject.has("private_key") && jsonObject.has("client_email")) {
                try {
                    if (!FirebaseApp.getApps().isEmpty()) {
                        for (FirebaseApp app : FirebaseApp.getApps()) app.delete();
                    }

                    InputStream serviceAccount = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                    String bucketName = cachedProjectId + ".firebasestorage.app";

                    FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                            .setCredentials(credentials)
                            .setProjectId(cachedProjectId)
                            .build();

                    customFirestore = firestoreOptions.getService();

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setProjectId(cachedProjectId)
                            .setStorageBucket(bucketName)
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("[FirebaseService] Conexión Admin SDK inicializada en paralelo: " + cachedProjectId);
                } catch (Exception sdkEx) {
                    System.err.println("[FirebaseService] Aviso: SDK local no inicializado (operando 100% Cloud Functions HTTPS): " + sdkEx.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[FirebaseService] ERROR CRÍTICO al conectar: " + e.getMessage());
            throw e;
        }
    }

    public static boolean isCloudFunctionsActive() {
        return cloudFunctionsMode && CloudFunctionsClient.getInstance().isConfigured();
    }

    public static CloudFunctionsClient getCloudFunctionsClient() {
        return CloudFunctionsClient.getInstance();
    }

    public static void setCustomClaims(String uid, Map<String, Object> claims) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            getAuth().setCustomUserClaims(uid, claims);
        }
    }

    public static Map<String, Object> getCustomClaims(String email) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            UserRecord user = getAuth().getUserByEmail(email);
            return user.getCustomClaims();
        }
        return Map.of();
    }

    public static Firestore getFirestore() {
        if (customFirestore != null) {
            return customFirestore;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirestoreClient.getFirestore();
        }
        return null;
    }

    public static FirebaseAuth getAuth() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseAuth.getInstance();
        }
        return null;
    }

    public static Bucket getStorageBucket() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return StorageClient.getInstance().bucket();
        }
        return null;
    }

    public static String getProjectId() {
        return cachedProjectId;
    }
}
