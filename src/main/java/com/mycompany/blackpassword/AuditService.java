package com.mycompany.blackpassword;

import com.google.cloud.firestore.Firestore;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de Auditoría de Seguridad con Cifrado de Extremo a Extremo.
 * Registra eventos críticos en Firebase Firestore de forma cifrada y anónima.
 */
public class AuditService {

    private static final String COLLECTION_LOGS = "audit_logs";

    /**
     * Registra un evento en la bitácora de seguridad de forma cifrada.
     * @param email Email del usuario que realiza la acción.
     * @param accion Descripción corta de la acción (pública para indexación).
     * @param detalles Información sensible (se cifra antes de enviar).
     */
    public static void log(String email, String accion, String detalles) {
        try {
            Firestore db = FirebaseService.getFirestore();
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", com.google.cloud.Timestamp.now());
            logEntry.put("email_hash", RSALogica.hashToHex(email));
            logEntry.put("accion", accion);
            logEntry.put("os", System.getProperty("os.name"));

            // Cifrado de los detalles si hay una sesión activa
            if (SecuritySession.isSessionActive()) {
                byte[] detallesBytes = detalles.getBytes(StandardCharsets.UTF_8);
                byte[] detallesCifrados = RSALogica.cifrarBytesHibrido(
                    detallesBytes, 
                    SecuritySession.getPublicKey()
                );
                logEntry.put("detalles_cifrados", Base64.getEncoder().encodeToString(detallesCifrados));
            } else {
                // Si no hay sesión (ej: fallo de login), ofuscamos los detalles para evitar fugas
                char[] detallesChars = detalles.toCharArray();
                try {
                    logEntry.put("detalles_ofuscados", Base64.getEncoder().encodeToString(
                        RSALogica.hashSHA256(detallesChars).getBytes(StandardCharsets.UTF_8)
                    ));
                } finally {
                    Arrays.fill(detallesChars, (char) 0);
                }
            }

            db.collection(COLLECTION_LOGS).add(logEntry);
            System.out.println("[AuditService] Evento registrado de forma segura: " + accion);
            
            procesarNotificacionVisual(accion, detalles);
            
        } catch (Exception e) {
            System.err.println("[AuditService] Error al registrar log: " + e.getMessage());
        }
    }
    
    private static void procesarNotificacionVisual(String accion, String detalles) {
        java.awt.Window activeWindow = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow == null) return;

        if (accion.contains("SUCCESS") || accion.contains("CREATE") || accion.contains("UPDATE")) {
            ToastNotification.show(activeWindow, detalles, ToastNotification.Type.SUCCESS);
        } else if (accion.contains("FAILED") || accion.contains("ERROR")) {
            ToastNotification.show(activeWindow, "Operación fallida o denegada", ToastNotification.Type.ERROR);
        } else if (accion.contains("COPY")) {
            ToastNotification.show(activeWindow, "Copiado al portapapeles", ToastNotification.Type.INFO);
        }
    }
    
    public static void logSystem(String accion, String detalles) {
        log("SYSTEM", accion, detalles);
    }
}
