package com.mycompany.blackpassword.services;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.UserRecord;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 * Servicio de Destrucción Remota (Kill Switch) v2.0.
 * Monitorea el estado del usuario y bloquea la app mediante Custom Claims y Firestore.
 */
public class RemoteKillService {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static String currentUserEmail;
    private static Runnable onKillAction;

    /**
     * Inicia el monitoreo remoto.
     */
    public static void startMonitoring(String email, Runnable onKill) {
        currentUserEmail = email;
        onKillAction = onKill;
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkStatus();
            } catch (Exception e) {
                System.err.println("[RemoteKill] Error en monitoreo: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static void checkStatus() throws Exception {
        if (currentUserEmail == null) return;

        Firestore db = FirebaseService.getFirestore();
        String colName = RSALogica.hashConstantToHex("usuarios");
        String docId = RSALogica.hashConstantToHex(currentUserEmail);
        
        DocumentSnapshot doc = db.collection(java.util.Objects.requireNonNull(colName))
                .document(java.util.Objects.requireNonNull(docId)).get().get();
        
        if (doc.exists()) {
            String status = doc.getString("status");
            if ("COMPROMISED".equals(status)) {
                System.err.println("[RemoteKill] ¡ESTADO COMPROMETIDO DETECTADO! Iniciando purga...");
                triggerKill();
            }
        }
    }

    private static void triggerKill() {
        scheduler.shutdownNow();
        java.awt.EventQueue.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                "🚨 ALERTA DE SEGURIDAD CRÍTICA\n" +
                "Esta cuenta ha sido marcada como COMPROMETIDA de forma remota.\n" +
                "La aplicación se cerrará y los datos locales serán purgados.", 
                "DESTRUCCIÓN REMOTA ACTIVADA", JOptionPane.ERROR_MESSAGE);
            
            if (onKillAction != null) onKillAction.run();
            System.exit(0);
        });
    }

    /**
     * Activa el protocolo de pánico global para una cuenta.
     */
    public static void selfDestruct(String email) {
        try {
            // 1. Actualizar Firestore para monitoreo activo
            Firestore db = FirebaseService.getFirestore();
            String colName = RSALogica.hashConstantToHex("usuarios");
            String docId = RSALogica.hashConstantToHex(email);
            db.collection(java.util.Objects.requireNonNull(colName))
                .document(java.util.Objects.requireNonNull(docId)).update("status", "COMPROMISED").get();

            // 2. Actualizar Custom Claims para bloqueo inmediato por Reglas
            UserRecord user = FirebaseService.getAuth().getUserByEmail(email);
            Map<String, Object> claims = new HashMap<>(user.getCustomClaims());
            claims.put("account_status", "COMPROMISED");
            FirebaseService.setCustomClaims(user.getUid(), claims);

            AuditService.log(email, "REMOTE_KILL_TRIGGERED", "El usuario ha activado el Kill Switch manualmente. Cuenta bloqueada en Auth y DB.");
        } catch (Exception e) {
            System.err.println("[RemoteKill] Error al activar: " + e.getMessage());
        }
    }
}
