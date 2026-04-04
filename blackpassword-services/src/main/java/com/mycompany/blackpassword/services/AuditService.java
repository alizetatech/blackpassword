package com.mycompany.blackpassword.services;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Servicio de Auditoría de Seguridad v4.8 - Edición Final Estable.
 * Gestiona el registro de eventos con cifrado y notificaciones instantáneas (Decoupled).
 */
public class AuditService {

    private static final String COLLECTION_LOGS = "audit_logs";
    private static BiConsumer<String, String> notificationListener;

    /**
     * Registra un listener para las notificaciones visuales.
     */
    public static void setNotificationListener(BiConsumer<String, String> listener) {
        notificationListener = listener;
    }

    /**
     * Registra un evento. Muestra notificación instantánea y guarda en la nube en segundo plano.
     */
    public static void log(String email, String accion, String detalles) {
        final String fEmail = (email == null) ? "UNKNOWN" : email;
        final String fAccion = (accion == null) ? "UNKNOWN_ACTION" : accion;
        final String fDetalles = (detalles == null) ? "No details" : detalles;

        // 1. NOTIFICACIÓN VISUAL INSTANTÁNEA (Vía Listener)
        if (notificationListener != null && debeNotificar(fAccion)) {
            notificationListener.accept(fAccion, fDetalles);
        }

        // 2. PERSISTENCIA EN LA NUBE (Hilo de Fondo)
        new Thread(() -> {
            try {
                if (FirebaseApp.getApps().isEmpty()) {
                    System.err.println("[AuditService] Firebase no inicializado. Log omitido: " + fAccion);
                    return;
                }

                Firestore db = FirebaseService.getFirestore();
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("timestamp", Timestamp.now());
                logEntry.put("email_hash", RSALogica.hashToHex(fEmail));
                logEntry.put("accion", fAccion);
                logEntry.put("os", System.getProperty("os.name"));

                if (SecuritySession.isSessionActive() && SecuritySession.getPublicKey() != null) {
                    byte[] enc = RSALogica.cifrarBytesHibrido(fDetalles.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                    logEntry.put("detalles_cifrados", Base64.getEncoder().encodeToString(enc));
                } else {
                    char[] chars = fDetalles.toCharArray();
                    try {
                        logEntry.put("detalles_ofuscados", RSALogica.hashSHA256(chars, RSALogica.getUniversalSalt()));
                    } finally { Arrays.fill(chars, (char) 0); }
                }

                db.collection(COLLECTION_LOGS).add(logEntry).get();
                System.out.println("[AuditService] Log registrado en la nube: " + fAccion);

            } catch (Exception e) {
                System.err.println("[AuditService] Error al persistir log: " + e.getMessage());
            }
        }).start();
    }

    private static boolean debeNotificar(String accion) {
        return accion.contains("SUCCESS") || accion.contains("UPDATE") || 
               accion.contains("DELETE") || accion.contains("COPY") || 
               accion.contains("ERROR") || accion.contains("VIOLATION") ||
               accion.contains("LOGIN") || accion.contains("KILL") ||
               accion.contains("VAULT");
    }

    public static void logSystem(String accion, String detalles) {
        log("SYSTEM", accion, detalles);
    }
}
