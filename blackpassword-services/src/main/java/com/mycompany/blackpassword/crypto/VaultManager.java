package com.mycompany.blackpassword.crypto;

import com.google.cloud.firestore.Firestore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mycompany.blackpassword.services.CloudFunctionsClient;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.SecuritySession;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * VaultManager v2.0 - Orquestador Central de Bóveda (Cloud Functions HTTPS & Firestore).
 * Opera prioritariamente a través de Cloud Functions mediante HTTPS TLS 1.3 con Cero Conocimiento.
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
     * Recupera todos los secretos de la bóveda (vía Cloud Functions HTTPS o Firestore).
     */
    public static CompletableFuture<List<Map<String, Object>>> getAllSecrets(String email) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> combined = new ArrayList<>();

            if (!SecuritySession.isIdentityValid(email)) {
                return combined;
            }

            // 1. Prioridad: Vía Cloud Functions HTTPS
            if (FirebaseService.isCloudFunctionsActive()) {
                try {
                    JsonArray items = CloudFunctionsClient.getInstance().listVaultItems(email);
                    System.out.println("[VaultManager] listVaultItems(" + email + ") recuperó " + items.size() + " elementos.");
                    for (JsonElement el : items) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();

                        String docId = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                        String enc = obj.has("payload_cifrado") ? obj.get("payload_cifrado").getAsString()
                                : (obj.has("secreto_cifrado") ? obj.get("secreto_cifrado").getAsString() : null);

                        if (enc != null) {
                            try {
                                byte[] dec = RSALogica.descifrarBytesHibrido(
                                        Base64.getDecoder().decode(enc),
                                        SecuritySession.getPrivateKey()
                                );
                                String raw = new String(dec, StandardCharsets.UTF_8);
                                String s = "DESCONOCIDO", u = "DESCONOCIDO";

                                int uIdx = raw.indexOf("|u:");
                                int pIdx = raw.indexOf("|p:", uIdx >= 0 ? uIdx : 0);
                                if (raw.startsWith("s:") && uIdx > 2 && pIdx > uIdx) {
                                    s = raw.substring(2, uIdx);
                                    u = raw.substring(uIdx + 3, pIdx);
                                } else {
                                    for (String part : raw.split("\\|")) {
                                        if (part.startsWith("s:")) s = part.substring(2);
                                        if (part.startsWith("u:")) u = part.substring(2);
                                    }
                                }

                                Map<String, Object> row = new HashMap<>();
                                row.put("id", docId);
                                row.put("service", s.toUpperCase());
                                row.put("username", u);
                                row.put("encrypted_payload", enc);
                                row.put("date", "Cloud Function (Sincronizado)");
                                combined.add(row);

                                SecureCharArray.clear(dec);
                            } catch (Exception e) {
                                Map<String, Object> errRow = new HashMap<>();
                                errRow.put("service", "⚠️ ERROR");
                                errRow.put("username", "FALLO DE DESCIFRADO");
                                combined.add(errRow);
                            }
                        }
                    }
                    System.out.println("[VaultManager] " + combined.size() + " credenciales recuperadas vía Cloud Functions HTTPS.");
                    return combined;
                } catch (Exception cfEx) {
                    System.err.println("[VaultManager] Error en Cloud Functions HTTPS listVaultItems: " + cfEx.getMessage());
                    return combined;
                }
            }

            // 2. Fallback: Conexión SDK tradicional (si existe)
            try {
                Firestore db = FirebaseService.getFirestore();
                if (db != null) {
                    String path = Objects.requireNonNull(getVaultPath(email));
                    var queryResult = db.collection(path).get().get();

                    if (queryResult.isEmpty()) {
                        String legacyPath = Objects.requireNonNull(path.toUpperCase());
                        queryResult = db.collection(Objects.requireNonNull(legacyPath)).get().get();
                    }

                    for (var doc : queryResult.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        String enc = (String) data.get("secreto_cifrado");

                        if (enc != null) {
                            try {
                                byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(enc), SecuritySession.getPrivateKey());
                                String raw = new String(dec, StandardCharsets.UTF_8);
                                String s = "DESCONOCIDO", u = "DESCONOCIDO";
                                int uIdx = raw.indexOf("|u:");
                                int pIdx = raw.indexOf("|p:", uIdx >= 0 ? uIdx : 0);
                                if (raw.startsWith("s:") && uIdx > 2 && pIdx > uIdx) {
                                    s = raw.substring(2, uIdx);
                                    u = raw.substring(uIdx + 3, pIdx);
                                } else {
                                    for (String part : raw.split("\\|")) {
                                        if (part.startsWith("s:")) s = part.substring(2);
                                        if (part.startsWith("u:")) u = part.substring(2);
                                    }
                                }

                                Map<String, Object> row = new HashMap<>();
                                row.put("id", doc.getId());
                                row.put("service", s.toUpperCase());
                                row.put("username", u);
                                row.put("encrypted_payload", enc);

                                com.google.cloud.Timestamp ts = doc.getTimestamp("fecha_actualizacion");
                                row.put("date", (ts != null) ? ts.toDate().toString().substring(0, 19) : "N/A");
                                combined.add(row);

                                SecureCharArray.clear(dec);
                            } catch (Exception e) {
                                Map<String, Object> errRow = new HashMap<>();
                                errRow.put("service", "⚠️ ERROR");
                                errRow.put("username", "FALLO DE DESCIFRADO");
                                combined.add(errRow);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[VaultManager] Error cargando desde Firebase: " + e.getMessage());
            }

            return combined;
        });
    }

    /**
     * Guarda un secreto cifrado con RSA-4096 híbrido en Cloud Functions / Firebase.
     */
    public static CompletableFuture<Boolean> saveSecret(String email, String service, String username, char[] password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!SecuritySession.isIdentityValid(email)) return false;

            byte[] encryptedData = null;
            byte[] combined = null;
            try {
                final String serviceUpper = service.trim().toUpperCase();
                byte[] sPre = "s:".getBytes(StandardCharsets.UTF_8);
                byte[] uPre = "u:".getBytes(StandardCharsets.UTF_8);
                byte[] pPre = "p:".getBytes(StandardCharsets.UTF_8);

                byte[] serviceBytes = serviceUpper.getBytes(StandardCharsets.UTF_8);
                byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
                byte[] passwordBytes = SecureCharArray.toBytes(password);

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

                SecureCharArray.clear(passwordBytes);

                encryptedData = RSALogica.cifrarBytesHibrido(combined, SecuritySession.getPublicKey());
                String b64 = Base64.getEncoder().encodeToString(encryptedData);

                // 1. Guardar vía Cloud Functions HTTPS
                if (FirebaseService.isCloudFunctionsActive()) {
                    String docId = RSALogica.hashConstantToHex(serviceUpper + "_" + username.trim().toLowerCase());
                    boolean ok = CloudFunctionsClient.getInstance().saveVaultItem(docId, b64, null, serviceUpper, email);
                    if (ok) {
                        System.out.println("[VaultManager] Credencial '" + serviceUpper + "' guardada vía Cloud Functions HTTPS.");
                    } else {
                        System.err.println("[VaultManager] Fallo al guardar credencial vía Cloud Functions HTTPS: " + serviceUpper);
                    }
                    return ok;
                }

                // 2. Fallback a SDK tradicional si no hay Cloud Functions
                Firestore db = FirebaseService.getFirestore();
                if (db != null) {
                    Map<String, Object> remoteData = new HashMap<>();
                    remoteData.put("secreto_cifrado", b64);
                    remoteData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                    var writeResultFuture = db.collection(Objects.requireNonNull(getVaultPath(email))).add(remoteData);
                    writeResultFuture.get();
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al sincronizar: " + e.getMessage());
                return false;
            } finally {
                if (password != null) SecureCharArray.clear(password);
                if (combined != null) SecureCharArray.clear(combined);
                if (encryptedData != null) SecureCharArray.clear(encryptedData);
            }
        });
    }

    /**
     * Actualiza un secreto existente en Cloud Functions / Firebase.
     */
    public static CompletableFuture<Boolean> updateSecret(String email, String firestoreDocId, String oldService, String service, String username, char[] password) {
        return CompletableFuture.supplyAsync(() -> {
            if (!SecuritySession.isIdentityValid(email)) return false;

            byte[] encryptedData = null;
            byte[] combined = null;
            try {
                final String serviceUpper = service.trim().toUpperCase();
                byte[] sPre = "s:".getBytes(StandardCharsets.UTF_8);
                byte[] uPre = "u:".getBytes(StandardCharsets.UTF_8);
                byte[] pPre = "p:".getBytes(StandardCharsets.UTF_8);

                byte[] serviceBytes = serviceUpper.getBytes(StandardCharsets.UTF_8);
                byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
                byte[] passwordBytes = SecureCharArray.toBytes(password);

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

                SecureCharArray.clear(passwordBytes);

                encryptedData = RSALogica.cifrarBytesHibrido(combined, SecuritySession.getPublicKey());
                String b64 = Base64.getEncoder().encodeToString(encryptedData);

                // 1. Vía Cloud Functions HTTPS
                if (FirebaseService.isCloudFunctionsActive()) {
                    String docId = (firestoreDocId != null && !firestoreDocId.isBlank()) 
                            ? firestoreDocId 
                            : RSALogica.hashConstantToHex(serviceUpper + "_" + username.trim().toLowerCase());
                    boolean ok = CloudFunctionsClient.getInstance().saveVaultItem(docId, b64, null, serviceUpper, email);
                    if (ok) {
                        System.out.println("[VaultManager] Credencial actualizada vía Cloud Functions HTTPS.");
                    } else {
                        System.err.println("[VaultManager] Fallo al actualizar credencial vía Cloud Functions HTTPS: " + serviceUpper);
                    }
                    return ok;
                }

                // 2. Fallback a SDK tradicional
                Firestore db = FirebaseService.getFirestore();
                if (db != null) {
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
                }
                return false;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al actualizar secreto: " + e.getMessage());
                return false;
            } finally {
                if (password != null) SecureCharArray.clear(password);
                if (combined != null) SecureCharArray.clear(combined);
                if (encryptedData != null) SecureCharArray.clear(encryptedData);
            }
        });
    }

    /**
     * Elimina un secreto vía Cloud Functions HTTPS o Firestore.
     */
    public static CompletableFuture<Boolean> deleteSecret(String email, String serviceId, String firestoreDocId) {
        if (!SecuritySession.isIdentityValid(email)) return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (FirebaseService.isCloudFunctionsActive() && firestoreDocId != null) {
                    boolean ok = CloudFunctionsClient.getInstance().deleteVaultItem(firestoreDocId, email);
                    if (ok) {
                        System.out.println("[VaultManager] Credencial eliminada vía Cloud Functions HTTPS: " + firestoreDocId);
                    } else {
                        System.err.println("[VaultManager] Fallo al eliminar credencial vía Cloud Functions HTTPS: " + firestoreDocId);
                    }
                    return ok;
                }

                if (firestoreDocId != null) {
                    Firestore db = FirebaseService.getFirestore();
                    if (db != null) {
                        var deleteFuture = db.collection(Objects.requireNonNull(getVaultPath(email))).document(firestoreDocId).delete();
                        deleteFuture.get();
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                System.err.println("[VaultManager] Error al borrar remoto: " + e.getMessage());
                return false;
            }
        });
    }
}
