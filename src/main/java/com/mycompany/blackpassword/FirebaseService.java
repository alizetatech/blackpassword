package com.mycompany.blackpassword;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Servicio para gestionar la conexión con Firebase v9.4-FINAL.
 * Soporta Admin SDK, Auth, Firestore y Cloud Storage con Bucket explícito.
 */
public class FirebaseService {

    /**
     * Inicializa Firebase usando el contenido JSON de una cuenta de servicio.
     */
    public static void conectar(String jsonData) throws Exception {
        try {
            System.out.println("[FirebaseService] Iniciando conexión v9.4...");
            
            if (!FirebaseApp.getApps().isEmpty()) {
                for (FirebaseApp app : FirebaseApp.getApps()) app.delete();
                System.out.println("[FirebaseService] Instancias previas purgadas.");
            }

            InputStream serviceAccount = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
            
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonData).getAsJsonObject();
            String projectId = jsonObject.get("project_id").getAsString();
            String bucketName = "blackpassword.firebasestorage.app"; // Coordenadas exactas

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(projectId)
                .setStorageBucket(bucketName) // Grabamos a fuego el nombre correcto
                .build();

            FirebaseApp.initializeApp(options);
            System.out.println("[FirebaseService] Conexión exitosa con el proyecto: " + getProjectId());
            System.out.println("[FirebaseService] Bóveda conectada explícitamente: " + getStorageBucket().getName());
            
        } catch (Exception e) {
            System.err.println("[FirebaseService] ERROR DE CONEXIÓN: " + e.getMessage());
            throw e;
        }
    }
    
    public static Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }
    
    /**
     * Obtiene el bucket de almacenamiento por defecto asociado con la app.
     */
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
