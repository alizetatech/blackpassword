package com.mycompany.blackpassword.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.Timestamp;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.crypto.SecureCharArray;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Servicio de Vigilancia y Detección de Intrusos v8.0.
 * Implementa la 'Opción Nuclear' inmediata para violaciones de RSA-Binding.
 */
public class IntrusionService {

    private static final Map<String, Integer> failureMap = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Registra un fallo de acceso. 
     * Si es una violación de RSA-Binding o se alcanza el límite, ejecuta purga inmediata.
     */
    public static void reportFailure(String email, String type) {
        int attempts = failureMap.getOrDefault(email, 0) + 1;
        failureMap.put(email, attempts);

        AuditService.log(email, type + "_FAILURE", "Intento fallido #" + attempts + " de tipo: " + type);

        // Violaciones críticas o exceso de intentos
        if ("RSA_BINDING_VIOLATION".equals(type) || attempts >= MAX_ATTEMPTS) {
            System.err.println("[IntrusionService] 🚨 AMENAZA CRÍTICA DETECTADA (" + type + "). Ejecutando purga de seguridad...");
            
            // OPCIÓN NUCLEAR: Borrar rastro local
            LocalDatabaseService.clearCache(email);
            
            generateIntrusionAlert(email, type, attempts);
            failureMap.put(email, 0); 
        }
    }

    public static void reportSuccess(String email) {
        failureMap.remove(email);
    }

    private static void generateIntrusionAlert(String email, String type, int attempts) {
        try {
            Firestore db = FirebaseService.getFirestore();
            Map<String, Object> alert = new HashMap<>();
            alert.put("timestamp", Timestamp.now());
            
            char[] emailChars = email.toCharArray();
            char[] emailHashChars = RSALogica.hashToHex(emailChars, null);
            alert.put("email_hash", new String(emailHashChars));
            SecureCharArray.clear(emailChars);
            SecureCharArray.clear(emailHashChars);
            
            alert.put("tipo", "CRITICAL_INTRUSION");
            alert.put("origen", type);
            alert.put("detalles", "Ataque detectado: " + type + ". Intentos: " + attempts + ". Caché local purgada.");
            alert.put("leida", false);

            db.collection("intrusion_alerts").add(alert);
            System.err.println("[IntrusionService] ¡ALERTA CRÍTICA GENERADA EN LA NUBE!");
        } catch (Exception e) {
            System.err.println("[IntrusionService] Error al generar alerta: " + e.getMessage());
        }
    }

    public static List<Map<String, Object>> getPendingAlerts(String email) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        try {
            Firestore db = FirebaseService.getFirestore();
            char[] emailChars = email.toCharArray();
            char[] emailHashChars = RSALogica.hashToHex(emailChars, null);
            String emailHash = new String(emailHashChars);
            SecureCharArray.clear(emailChars);
            SecureCharArray.clear(emailHashChars);
            
            var query = db.collection("intrusion_alerts")
                    .whereEqualTo("email_hash", emailHash)
                    .whereEqualTo("leida", false)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().get();

            for (QueryDocumentSnapshot doc : query.getDocuments()) {
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                alerts.add(data);
            }
        } catch (Exception e) {
            System.err.println("[IntrusionService] Error al consultar alertas: " + e.getMessage());
        }
        return alerts;
    }

    public static void markAsRead(String alertId) {
        try {
            java.util.Objects.requireNonNull(alertId);
            FirebaseService.getFirestore().collection("intrusion_alerts").document(alertId).update("leida", true);
        } catch (Exception e) {
            System.err.println("[IntrusionService] Error al marcar alerta como leída: " + e.getMessage());
        }
    }
}
