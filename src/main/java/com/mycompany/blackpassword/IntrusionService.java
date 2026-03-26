package com.mycompany.blackpassword;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Servicio de Vigilancia y Detección de Intrusos v7.0.
 * Implementa la 'Opción Nuclear': Borrado de caché local ante ataques.
 */
public class IntrusionService {

    private static final Map<String, Integer> failureMap = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Registra un fallo de acceso. Si se alcanza el límite, genera una alerta y borra la caché local.
     */
    public static void reportFailure(String email, String type) {
        int attempts = failureMap.getOrDefault(email, 0) + 1;
        failureMap.put(email, attempts);

        AuditService.log(email, type + "_FAILURE", "Intento fallido #" + attempts);

        if (attempts >= MAX_ATTEMPTS) {
            System.err.println("[IntrusionService] ¡LÍMITE DE FALLOS ALCANZADO! Ejecutando purga de seguridad...");
            
            // OPCIÓN NUCLEAR: Borrar rastro local
            LocalDatabaseService.clearCache(email);
            
            generateIntrusionAlert(email, type);
            failureMap.put(email, 0); 
        }
    }

    public static void reportSuccess(String email) {
        failureMap.remove(email);
    }

    private static void generateIntrusionAlert(String email, String type) {
        try {
            Firestore db = FirebaseService.getFirestore();
            Map<String, Object> alert = new HashMap<>();
            alert.put("timestamp", com.google.cloud.Timestamp.now());
            alert.put("email_hash", RSALogica.hashToHex(email));
            alert.put("tipo", "INTRUSION_DETECTED");
            alert.put("origen", type);
            alert.put("detalles", "Múltiples intentos fallidos detectados. Caché local purgada por seguridad.");
            alert.put("leida", false);

            db.collection("intrusion_alerts").add(alert);
            System.err.println("[IntrusionService] ¡ALERTA GENERADA PARA: " + email + "!");
        } catch (Exception e) {
            System.err.println("[IntrusionService] Error al generar alerta: " + e.getMessage());
        }
    }

    public static List<Map<String, Object>> getPendingAlerts(String email) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        try {
            Firestore db = FirebaseService.getFirestore();
            String emailHash = RSALogica.hashToHex(email);
            
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
            FirebaseService.getFirestore().collection("intrusion_alerts").document(alertId).update("leida", true);
        } catch (Exception e) {}
    }
}
