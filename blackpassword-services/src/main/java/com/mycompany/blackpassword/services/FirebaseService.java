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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio para gestionar la conexión con Firebase v12.5-ESTABLE.
 * Restaura el blindaje 'dns:///' para Firestore.
 */
public class FirebaseService {

    private static Firestore customFirestore;

    /**
     * Inicializa Firebase usando el contenido JSON de una cuenta de servicio.
     */
    public static void conectar(String jsonData) throws Exception {
        try {
            System.out.println("[FirebaseService] Iniciando conexión v12.5-ESTABLE...");
            
            if (!FirebaseApp.getApps().isEmpty()) {
                for (FirebaseApp app : FirebaseApp.getApps()) app.delete();
            }

            InputStream serviceAccount = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonData).getAsJsonObject();
            String projectId = jsonObject.get("project_id").getAsString();
            String bucketName = projectId + ".firebasestorage.app"; 

            // --- CONFIGURACIÓN MAESTRA DE FIRESTORE (DELEGAR A gRPC GLOBAL) ---
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
            
            customFirestore = firestoreOptions.getService();
            // ------------------------------------------------------------------

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .setStorageBucket(bucketName)
                .build();

            FirebaseApp.initializeApp(options);
            System.out.println("[FirebaseService] Conexión v12.5 establecida: " + getProjectId());
            
        } catch (Exception e) {
            System.err.println("[FirebaseService] ERROR CRÍTICO: " + e.getMessage());
            throw e;
        }
    }
    
    public static void setCustomClaims(String uid, Map<String, Object> claims) throws Exception {
        getAuth().setCustomUserClaims(uid, claims);
    }

    public static Map<String, Object> getCustomClaims(String email) throws Exception {
        UserRecord user = getAuth().getUserByEmail(email);
        return user.getCustomClaims();
    }

    public static Firestore getFirestore() {
        return (customFirestore != null) ? customFirestore : FirestoreClient.getFirestore();
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }
    
    public static Bucket getStorageBucket() {
        return StorageClient.getInstance().bucket();
    }
    
    public static String getProjectId() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance().getOptions().getProjectId();
        }
        return "No conectado";
    }
}
