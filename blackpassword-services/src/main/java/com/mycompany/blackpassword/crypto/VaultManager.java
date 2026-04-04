package com.mycompany.blackpassword.crypto;

import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.SecuritySession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * VaultManager v1.0 - Orquestador Central de Secretos.
 * Maneja la sincronización entre Firebase y la DB Local, validando identidad en cada paso.
 */
public class VaultManager {

    private static String getVaultPath(String email) throws Exception {
        return RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(email) + "/" + RSALogica.hashToHex("boveda");
    }

    /**
     * Recupera todos los secretos combinando la nube y la caché local.
     */
    public static CompletableFuture<List<Map<String, Object>>> getAllSecrets(String email) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> combined = new ArrayList<>();
            
            if (!SecuritySession.isIdentityValid(email)) {
                return combined;
            }

            try {
                Firestore db = FirebaseService.getFirestore();
                String path = getVaultPath(email);
                var query = db.collection(path).get().get();
                
                for (var doc : query.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    String enc = (String) data.get("secreto_cifrado");
                    
                    if (enc != null) {
                        try {
                            byte[] dec = RSALogica.descifrarBytesHibrido(java.util.Base64.getDecoder().decode(enc), SecuritySession.getPrivateKey());
                            String raw = new String(dec, java.nio.charset.StandardCharsets.UTF_8);
                            String s = "Desconocido", u = "Desconocido";
                            for (String part : raw.split("\\|")) {
                                if (part.startsWith("s:")) s = part.substring(2);
                                if (part.startsWith("u:")) u = part.substring(2);
                            }
                            
                            Map<String, Object> row = new HashMap<>();
                            row.put("id", doc.getId());
                            row.put("service", s.toUpperCase());
                            row.put("username", u);
                            row.put("encrypted_payload", enc);
                            
                            com.google.cloud.Timestamp ts = doc.getTimestamp("fecha_actualizacion");
                            row.put("date", (ts != null) ? ts.toDate().toString().substring(0, 19) : "N/A");
                            
                            combined.add(row);
                            
                            // Sincronizar con local (usamos el formato que espera LocalDatabaseService)
                            LocalDatabaseService.syncSecret(s, email, enc);
                        } catch (Exception e) {
                            Map<String, Object> errRow = new HashMap<>();
                            errRow.put("service", "⚠️ ERROR");
                            errRow.put("username", "FALLO DE DESCIFRADO");
                            combined.add(errRow);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[VaultManager] Error cargando desde Firebase, usando local: " + e.getMessage());
                return LocalDatabaseService.getPasswords(email);
            }

            return combined;
        });
    }

    /**
     * Guarda un secreto de forma atómica en local y remoto.
     */
    public static CompletableFuture<Boolean> saveSecret(String email, String service, String username, char[] password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!SecuritySession.isIdentityValid(email)) return false;

            try {
                String passStr = new String(password); // Temporal para el payload
                String raw = "s:" + service + "|u:" + username + "|p:" + passStr;
                byte[] enc = RSALogica.cifrarBytesHibrido(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                String b64 = java.util.Base64.getEncoder().encodeToString(enc);

                // 1. Guardar Local
                LocalDatabaseService.syncSecret(service, email, b64);

                // 2. Guardar Remoto
                Firestore db = FirebaseService.getFirestore();
                Map<String, Object> remoteData = new HashMap<>();
                remoteData.put("secreto_cifrado", b64);
                remoteData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                db.collection(getVaultPath(email)).add(remoteData).get();
                return true;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al sincronizar: " + e.getMessage());
                return false;
            } finally {
                java.util.Arrays.fill(password, (char) 0);
            }
        });
    }

    /**
     * Elimina un secreto de ambas fuentes.
     */
    public static void deleteSecret(String email, String serviceId, String firestoreDocId) {
        if (!SecuritySession.isIdentityValid(email)) return;
        
        LocalDatabaseService.deleteLocalSecret(serviceId);
        
        if (firestoreDocId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    FirebaseService.getFirestore().collection(getVaultPath(email)).document(firestoreDocId).delete().get();
                } catch (Exception e) {
                    System.err.println("[VaultManager] Error al borrar remoto: " + e.getMessage());
                }
            });
        }
    }
}
