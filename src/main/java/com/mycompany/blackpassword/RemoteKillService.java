package com.mycompany.blackpassword;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 * Servicio de Destrucción Remota (Kill Switch).
 * Monitorea el estado del usuario en Firebase y bloquea la app si se marca como COMPROMISED.
 */
public class RemoteKillService {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static String currentUserEmail;
    private static Runnable onKillAction;

    /**
     * Inicia el monitoreo remoto.
     * @param email Email del usuario a monitorear.
     * @param onKill Acción a ejecutar si se detecta compromiso (ej: cerrar app).
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
        }, 0, 1, TimeUnit.MINUTES); // Verificar cada minuto
    }

    public static void checkStatus() throws Exception {
        if (currentUserEmail == null) return;

        Firestore db = FirebaseService.getFirestore();
        String colName = RSALogica.hashToHex("usuarios");
        String docId = RSALogica.hashToHex(currentUserEmail);
        
        DocumentSnapshot doc = db.collection(colName).document(docId).get().get();
        
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
     * Permite al usuario marcar su propia cuenta como comprometida (Simulación de otro dispositivo).
     */
    public static void selfDestruct(String email) {
        try {
            Firestore db = FirebaseService.getFirestore();
            String colName = RSALogica.hashToHex("usuarios");
            String docId = RSALogica.hashToHex(email);
            db.collection(colName).document(docId).update("status", "COMPROMISED").get();
            AuditService.log(email, "REMOTE_KILL_TRIGGERED", "El usuario ha activado el Kill Switch manualmente.");
        } catch (Exception e) {
            System.err.println("[RemoteKill] Error al activar: " + e.getMessage());
        }
    }
}
