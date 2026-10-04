package com.mycompany.blackpassword.crypto;

import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.SecuritySession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * VaultManager v1.1 - Orquestador Central de Secretos (Reparado).
 * Maneja la sincronización entre Firebase y la DB Local con Blindaje Nivel 11.
 */
public class VaultManager {

    private static String getVaultPath(String email) throws Exception {
        char[] emailChars = email.toCharArray();
        String path = getHashedUserPath(emailChars);
        SecureCharArray.clear(emailChars);
        return path;
    }

    public static String getHashedUserPath(char[] email) throws Exception {
        char[] emailHashChars = RSALogica.hashToHex(email, null);
        String emailHash = new String(emailHashChars);
        String colUsuarios = RSALogica.hashConstantToHex("usuarios");
        String colVaults = RSALogica.hashConstantToHex("boveda");
        String finalPath = colUsuarios + "/" + emailHash + "/" + colVaults;
        SecureCharArray.clear(emailHashChars);
        return finalPath;
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
                String path = java.util.Objects.requireNonNull(getVaultPath(email));
                var queryResult = db.collection(path).get().get();
                
                // --- LÓGICA DE RECUPERACIÓN LEGACY (Si la ruta moderna está vacía) ---
                if (queryResult.isEmpty()) {
                    System.out.println("[VaultManager] Ruta moderna vacía. Probando persistencia Legacy...");
                    String legacyPath = Objects.requireNonNull(path.toUpperCase());
                    queryResult = db.collection(Objects.requireNonNull(legacyPath)).get().get();
                }

                for (var doc : queryResult.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    String enc = (String) data.get("secreto_cifrado");
                    
                    if (enc != null) {
                        try {
                            byte[] dec = RSALogica.descifrarBytesHibrido(java.util.Base64.getDecoder().decode(enc), SecuritySession.getPrivateKey());
                            String raw = new String(dec, java.nio.charset.StandardCharsets.UTF_8);
                            String s = "DESCONOCIDO", u = "DESCONOCIDO";
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
                            LocalDatabaseService.syncSecret(s.toUpperCase(), email, enc);
                            
                            com.mycompany.blackpassword.crypto.SecureCharArray.clear(dec);
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
     * Guarda un secreto de forma atómica en local y remoto con Inmunidad de Heap.
     */
    public static CompletableFuture<Boolean> saveSecret(String email, String service, String username, char[] password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!SecuritySession.isIdentityValid(email)) return false;

            byte[] encryptedData = null;
            byte[] combined = null;
            try {
                final String serviceUpper = service.trim().toUpperCase();
                byte[] sPre = "s:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] uPre = "u:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] pPre = "p:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                
                byte[] serviceBytes = serviceUpper.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] usernameBytes = username.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] passwordBytes = com.mycompany.blackpassword.crypto.SecureCharArray.toBytes(password);
                
                combined = new byte[sPre.length + serviceBytes.length + 1 + uPre.length + usernameBytes.length + 1 + pPre.length + passwordBytes.length];
                int offset = 0;
                
                System.arraycopy(sPre, 0, combined, offset, sPre.length);
                offset += sPre.length;
                System.arraycopy(serviceBytes, 0, combined, offset, serviceBytes.length);
                offset += serviceBytes.length;
                combined[offset++] = '|';
                
                System.arraycopy(uPre, 0, combined, offset, uPre.length);
                offset += uPre.length;
                System.arraycopy(usernameBytes, 0, combined, offset, usernameBytes.length);
                offset += usernameBytes.length;
                combined[offset++] = '|';
                
                System.arraycopy(pPre, 0, combined, offset, pPre.length);
                offset += pPre.length;
                System.arraycopy(passwordBytes, 0, combined, offset, passwordBytes.length);

                com.mycompany.blackpassword.crypto.SecureCharArray.clear(passwordBytes);

                encryptedData = RSALogica.cifrarBytesHibrido(combined, SecuritySession.getPublicKey());
                String b64 = java.util.Base64.getEncoder().encodeToString(encryptedData);

                LocalDatabaseService.syncSecret(serviceUpper, email, b64);

                Firestore db = FirebaseService.getFirestore();
                Map<String, Object> remoteData = new HashMap<>();
                remoteData.put("secreto_cifrado", b64);
                remoteData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                var writeResultFuture = db.collection(Objects.requireNonNull(getVaultPath(email))).add(remoteData);
                writeResultFuture.get();
                return true;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al sincronizar: " + e.getMessage());
                return false;
            } finally {
                if (password != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(password);
                if (combined != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(combined);
                if (encryptedData != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(encryptedData);
            }
        });
    }

    /**
     * Actualiza un secreto de forma atómica en local y remoto con Inmunidad de Heap.
     */
    public static CompletableFuture<Boolean> updateSecret(String email, String firestoreDocId, String oldService, String service, String username, char[] password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!SecuritySession.isIdentityValid(email)) return false;

            byte[] encryptedData = null;
            byte[] combined = null;
            try {
                final String serviceUpper = service.trim().toUpperCase();
                byte[] sPre = "s:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] uPre = "u:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] pPre = "p:".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                
                byte[] serviceBytes = serviceUpper.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] usernameBytes = username.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] passwordBytes = com.mycompany.blackpassword.crypto.SecureCharArray.toBytes(password);
                
                combined = new byte[sPre.length + serviceBytes.length + 1 + uPre.length + usernameBytes.length + 1 + pPre.length + passwordBytes.length];
                int offset = 0;
                
                System.arraycopy(sPre, 0, combined, offset, sPre.length);
                offset += sPre.length;
                System.arraycopy(serviceBytes, 0, combined, offset, serviceBytes.length);
                offset += serviceBytes.length;
                combined[offset++] = '|';
                
                System.arraycopy(uPre, 0, combined, offset, uPre.length);
                offset += uPre.length;
                System.arraycopy(usernameBytes, 0, combined, offset, usernameBytes.length);
                offset += usernameBytes.length;
                combined[offset++] = '|';
                
                System.arraycopy(pPre, 0, combined, offset, pPre.length);
                offset += pPre.length;
                System.arraycopy(passwordBytes, 0, combined, offset, passwordBytes.length);

                com.mycompany.blackpassword.crypto.SecureCharArray.clear(passwordBytes);

                encryptedData = RSALogica.cifrarBytesHibrido(combined, SecuritySession.getPublicKey());
                String b64 = java.util.Base64.getEncoder().encodeToString(encryptedData);

                if (oldService != null && !oldService.trim().equalsIgnoreCase(serviceUpper)) {
                    LocalDatabaseService.deleteLocalSecret(oldService.trim().toUpperCase());
                }
                LocalDatabaseService.syncSecret(serviceUpper, email, b64);

                Firestore db = FirebaseService.getFirestore();
                Map<String, Object> remoteData = new HashMap<>();
                remoteData.put("secreto_cifrado", b64);
                remoteData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                if (firestoreDocId != null && !firestoreDocId.trim().isEmpty()) {
                    var writeResultFuture = db.collection(Objects.requireNonNull(getVaultPath(email))).document(firestoreDocId).set(remoteData);
                    writeResultFuture.get();
                } else {
                    var writeResultFuture = db.collection(Objects.requireNonNull(getVaultPath(email))).add(remoteData);
                    writeResultFuture.get();
                }
                return true;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al actualizar secreto: " + e.getMessage());
                return false;
            } finally {
                if (password != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(password);
                if (combined != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(combined);
                if (encryptedData != null) com.mycompany.blackpassword.crypto.SecureCharArray.clear(encryptedData);
            }
        });
    }

    /**
     * Elimina un secreto de ambas fuentes.
     */
    public static void deleteSecret(String email, String serviceId, String firestoreDocId) {
        if (!SecuritySession.isIdentityValid(email)) return;
        
        LocalDatabaseService.deleteLocalSecret(serviceId.toUpperCase());
        
        if (firestoreDocId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    var deleteFuture = FirebaseService.getFirestore().collection(Objects.requireNonNull(getVaultPath(email))).document(firestoreDocId).delete();
                    deleteFuture.get();
                } catch (Exception e) {
                    System.err.println("[VaultManager] Error al borrar remoto: " + e.getMessage());
                }
            });
        }
    }
}
