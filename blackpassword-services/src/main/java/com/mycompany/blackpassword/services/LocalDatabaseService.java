package com.mycompany.blackpassword.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Servicio de Persistencia Zero-Disk (100% Firebase Cloud Firestore).
 * No crea ni mantiene ninguna base de datos SQLite ni archivos locales en el equipo.
 */
public class LocalDatabaseService {

    private static final String LEGACY_DB_NAME = "blackvault_local.db";
    private static final String LEGACY_DB_CIPHER_NAME = "blackvault_local.db.enc";
    private static final String LEGACY_DB_TEMP_CIPHER = "blackvault_local.db.enc.tmp";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_SIZE = 12;
    private static final int GCM_TAG_SIZE = 128;

    // Mapa efímero en RAM (solo sesión activa) para asociar ID de semilla con el hash del usuario al eliminar
    private static final Map<String, String> seedIdToUserHash = new ConcurrentHashMap<>();
    private static volatile String lastActiveUserHash = null;

    public static void init() {
        purgeLegacyLocalFiles();
        System.out.println("[Zero-Disk] Persistencia local deshabilitada: Todos los datos residen exclusivamente en Firebase Cloud.");
    }

    public static void closeAndEncrypt() {
        seedIdToUserHash.clear();
        lastActiveUserHash = null;
        purgeLegacyLocalFiles();
    }

    private static void purgeLegacyLocalFiles() {
        secureOverwriteAndDelete(Paths.get(LEGACY_DB_NAME));
        secureOverwriteAndDelete(Paths.get(LEGACY_DB_CIPHER_NAME));
        secureOverwriteAndDelete(Paths.get(LEGACY_DB_TEMP_CIPHER));
        secureOverwriteAndDelete(Paths.get(LEGACY_DB_NAME + ".secure_delete"));
    }

    private static void secureOverwriteAndDelete(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            long size = Files.size(file);
            byte[] zeros = new byte[8192];
            try (java.io.OutputStream os = new java.io.FileOutputStream(file.toFile())) {
                long written = 0;
                while (written < size) {
                    long toWrite = Math.min(zeros.length, size - written);
                    os.write(zeros, 0, (int) toWrite);
                    written += toWrite;
                }
                os.flush();
            }
            Files.deleteIfExists(file);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(file);
            } catch (Exception ignored) {}
        }
    }

    // --- MÉTODOS DE BÓVEDA (DELEGADOS 100% A FIREBASE VÍA VAULTMANAGER) ---

    public static boolean savePassword(String email, String service, String username, String password) {
        // No se guarda nada localmente
        return true;
    }

    public static List<Map<String, Object>> getPasswords(String email) {
        // Sin base de datos local: retorna lista vacía si Firebase no está disponible
        return Collections.emptyList();
    }

    public static void syncSecret(String id, String email, String dataCifradaB64) {
        // Zero-Disk: No se sincroniza ningún secreto en disco local
    }

    public static void deleteLocalSecret(String id) {
        // Zero-Disk: No existe tabla local que eliminar
    }

    public static void clearCache(String email) {
        seedIdToUserHash.clear();
        purgeLegacyLocalFiles();
    }

    // =====================================================
    // --- BÓVEDA DE SEMILLAS DE BILLETERAS BITCOIN (CLOUD) ---
    // =====================================================

    public static class BtcSeedEntry {
        public final String id;
        public final String userHash;
        public final String codigoWallet;
        public final String red;
        public final String direccion;
        public final String semillaCifrada;
        public final String saltB64;
        public final long timestamp;

        public BtcSeedEntry(String id, String userHash, String codigoWallet, String red, String direccion, String semillaCifrada, String saltB64, long timestamp) {
            this.id = id;
            this.userHash = userHash;
            this.codigoWallet = codigoWallet;
            this.red = red;
            this.direccion = direccion;
            this.semillaCifrada = semillaCifrada;
            this.saltB64 = saltB64;
            this.timestamp = timestamp;
        }

        public String getFormattedDate() {
            if (timestamp <= 0) return "--";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new java.util.Date(timestamp));
        }
    }

    public static boolean guardarSemillaWallet(String userEmail, String codigoWallet, String red, String direccion, String fraseSemilla, char[] pin) {
        if (userEmail == null || codigoWallet == null || fraseSemilla == null || pin == null) {
            return false;
        }
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);

            byte[] iv = new byte[GCM_IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));

            byte[] seedBytes = fraseSemilla.getBytes(StandardCharsets.UTF_8);
            byte[] enc = cipher.doFinal(seedBytes);
            java.util.Arrays.fill(seedBytes, (byte) 0);

            ByteBuffer bb = ByteBuffer.allocate(iv.length + enc.length);
            bb.put(iv);
            bb.put(enc);
            byte[] encryptedPayload = bb.array();

            // Capa Híbrida Asimétrica RSA-4096 (Estándar Bóveda de Contraseñas)
            byte[] payloadToStore = encryptedPayload;
            PublicKey pub = SecuritySession.getPublicKey();
            if (pub != null) {
                try {
                    payloadToStore = RSALogica.cifrarBytesHibrido(encryptedPayload, pub);
                } catch (Exception ex) {
                    System.err.println("[CloudSeeds] Nota: Error aplicando envoltura RSA a semilla BTC: " + ex.getMessage());
                }
            }

            String encB64 = Base64.getEncoder().encodeToString(payloadToStore);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            String userHash = RSALogica.hashConstantToHex(userEmail.trim().toLowerCase());
            lastActiveUserHash = userHash;
            long now = System.currentTimeMillis();
            String id = "SEED_" + userHash.substring(0, Math.min(8, userHash.length())) + "_" + now;

            Firestore db = FirebaseService.getFirestore();
            if (db == null) {
                return false;
            }

            String colUsuarios = RSALogica.hashConstantToHex("usuarios");
            String colSemillas = RSALogica.hashConstantToHex("semillas_btc");
            Map<String, Object> cloudData = new HashMap<>();
            cloudData.put("id", id);
            cloudData.put("codigo_wallet", codigoWallet.trim().toUpperCase());
            cloudData.put("red", red.toUpperCase());
            cloudData.put("direccion", direccion != null ? direccion : "");
            cloudData.put("semilla_cifrada", encB64);
            cloudData.put("salt_b64", saltB64);
            cloudData.put("ts", now);
            cloudData.put("fecha_registro", com.google.cloud.Timestamp.now());

            db.collection(colUsuarios + "/" + userHash + "/" + colSemillas).document(id).set(cloudData).get();
            seedIdToUserHash.put(id, userHash);
            System.out.println("[Firestore] Semilla BTC guardada exclusivamente en Firebase Cloud: " + codigoWallet);
            return true;
        } catch (Exception e) {
            System.err.println("[Firestore] Error al guardar semilla BTC en Firebase: " + e.getMessage());
            return false;
        }
    }

    public static List<BtcSeedEntry> obtenerSemillasWallet(String userEmail) {
        List<BtcSeedEntry> list = new ArrayList<>();
        if (userEmail == null || userEmail.isEmpty()) return list;
        String userHash = RSALogica.hashConstantToHex(userEmail.trim().toLowerCase());
        lastActiveUserHash = userHash;

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String colUsuarios = RSALogica.hashConstantToHex("usuarios");
                String colSemillas = RSALogica.hashConstantToHex("semillas_btc");
                var querySnapshot = db.collection(colUsuarios + "/" + userHash + "/" + colSemillas).get().get();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (var doc : querySnapshot.getDocuments()) {
                        String docId = doc.getString("id");
                        if (docId == null) docId = doc.getId();
                        String cod = doc.getString("codigo_wallet");
                        String r = doc.getString("red");
                        String dir = doc.getString("direccion");
                        String semEnc = doc.getString("semilla_cifrada");
                        String saltB = doc.getString("salt_b64");
                        Long ts = doc.getLong("ts");
                        if (ts == null) ts = System.currentTimeMillis();

                        seedIdToUserHash.put(docId, userHash);
                        list.add(new BtcSeedEntry(
                            docId,
                            userHash,
                            cod != null ? cod : "",
                            r != null ? r : "TESTNET",
                            dir != null ? dir : "",
                            semEnc != null ? semEnc : "",
                            saltB != null ? saltB : "",
                            ts
                        ));
                    }
                    list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                }
            }
        } catch (Throwable t) {
            System.err.println("[Firestore] Error al obtener semillas BTC desde Firebase: " + t.getMessage());
        }
        return list;
    }

    public static String descifrarSemillaWallet(BtcSeedEntry entry, char[] pin) throws Exception {
        if (entry == null || pin == null) {
            throw new IllegalArgumentException("Parametros invalidos para descifrar semilla.");
        }
        byte[] salt = Base64.getDecoder().decode(entry.saltB64);
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);

        byte[] rawData = Base64.getDecoder().decode(entry.semillaCifrada);

        PrivateKey priv = SecuritySession.getPrivateKey();
        if (priv != null) {
            try {
                rawData = RSALogica.descifrarBytesHibrido(rawData, priv);
            } catch (Exception ignored) {
                // Retrocompatibilidad con semillas previas sin capa RSA
            }
        }

        if (rawData.length < GCM_IV_SIZE) {
            throw new IllegalArgumentException("Datos corruptos de semilla.");
        }

        byte[] iv = java.util.Arrays.copyOfRange(rawData, 0, GCM_IV_SIZE);
        byte[] enc = java.util.Arrays.copyOfRange(rawData, GCM_IV_SIZE, rawData.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
        byte[] decrypted = cipher.doFinal(enc);

        String frase = new String(decrypted, StandardCharsets.UTF_8);
        java.util.Arrays.fill(decrypted, (byte) 0);
        return frase;
    }

    public static boolean eliminarSemillaWallet(String id) {
        if (id == null) return false;
        try {
            String uHash = seedIdToUserHash.getOrDefault(id, lastActiveUserHash);
            if (uHash == null) return false;

            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String colUsuarios = RSALogica.hashConstantToHex("usuarios");
                String colSemillas = RSALogica.hashConstantToHex("semillas_btc");
                db.collection(colUsuarios + "/" + uHash + "/" + colSemillas).document(id).delete().get();
                seedIdToUserHash.remove(id);
                System.out.println("[Firestore] Semilla BTC eliminada de Firebase Firestore: " + id);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[Firestore] Error al eliminar semilla BTC: " + e.getMessage());
            return false;
        }
    }

    // --- BÓVEDA DE BILLETERAS BITCOIN ACTIVAS (100% FIREBASE) ---

    public static boolean guardarBtcWalletActiva(String userEmail, boolean testnet, String payloadCifradoB64) {
        if (userEmail == null || payloadCifradoB64 == null) return false;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = BitcoinWalletService.getBtcFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> cloudData = new HashMap<>();
                cloudData.put("payload_cifrado", payloadCifradoB64);
                cloudData.put("red", testnet ? "TESTNET" : "MAINNET");
                cloudData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(cloudData, SetOptions.merge()).get();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error guardando wallet BTC activa en Firebase: " + e.getMessage());
        }
        return false;
    }

    public static String obtenerBtcWalletActiva(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return null;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = BitcoinWalletService.getBtcFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("payload_cifrado")) {
                    String payload = docSnap.getString("payload_cifrado");
                    return (payload != null && !payload.isEmpty()) ? payload : null;
                }
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error obteniendo wallet BTC activa desde Firebase: " + e.getMessage());
        }
        return null;
    }

    public static boolean existeBtcWalletActiva(String userEmail, boolean testnet) {
        String payload = obtenerBtcWalletActiva(userEmail, testnet);
        return payload != null && !payload.isEmpty();
    }

    public static boolean eliminarBtcWalletActiva(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return false;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = BitcoinWalletService.getBtcFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                db.collection(path).document(docId).delete().get();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error eliminando wallet BTC activa en Firebase: " + e.getMessage());
        }
        return false;
    }

    public static String obtenerBtcAddressBook(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return "[]";
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = BitcoinWalletService.getBtcFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("address_book_json")) {
                    String json = docSnap.getString("address_book_json");
                    return (json != null && !json.isEmpty()) ? json : "[]";
                }
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error obteniendo libreta BTC desde Firebase: " + e.getMessage());
        }
        return "[]";
    }

    public static void guardarBtcAddressBook(String userEmail, boolean testnet, String addressBookJson) {
        if (userEmail == null || userEmail.isEmpty()) return;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = BitcoinWalletService.getBtcFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("address_book_json", addressBookJson != null ? addressBookJson : "[]");
                updateMap.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(updateMap, SetOptions.merge()).get();
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error guardando libreta BTC en Firebase: " + e.getMessage());
        }
    }

    // =======================================================
    // --- BÓVEDA DE SEMILLAS DOGECOIN (BIP-44, 100% CLOUD) ---
    // =======================================================

    public static class DogeSeedEntry {
        public final String id;
        public final String userHash;
        public final String codigoWallet;
        public final String red;
        public final String direccion;
        public final String semillaCifrada;
        public final String saltB64;
        public final long timestamp;

        public DogeSeedEntry(String id, String userHash, String codigoWallet, String red, String direccion, String semillaCifrada, String saltB64, long timestamp) {
            this.id = id;
            this.userHash = userHash;
            this.codigoWallet = codigoWallet;
            this.red = red;
            this.direccion = direccion;
            this.semillaCifrada = semillaCifrada;
            this.saltB64 = saltB64;
            this.timestamp = timestamp;
        }

        public String getFormattedDate() {
            if (timestamp <= 0) return "--";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new java.util.Date(timestamp));
        }
    }

    public static boolean guardarSemillaDogeWallet(String userEmail, String codigoWallet, String red, String direccion, String fraseSemilla, char[] pin) {
        if (userEmail == null || codigoWallet == null || fraseSemilla == null || pin == null) {
            return false;
        }
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);

            byte[] iv = new byte[GCM_IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));

            byte[] seedBytes = fraseSemilla.getBytes(StandardCharsets.UTF_8);
            byte[] enc = cipher.doFinal(seedBytes);
            java.util.Arrays.fill(seedBytes, (byte) 0);

            ByteBuffer bb = ByteBuffer.allocate(iv.length + enc.length);
            bb.put(iv);
            bb.put(enc);
            byte[] encryptedPayload = bb.array();

            // Capa Híbrida Asimétrica RSA-4096 (Estándar Bóveda de Contraseñas)
            byte[] payloadToStore = encryptedPayload;
            PublicKey pub = SecuritySession.getPublicKey();
            if (pub != null) {
                try {
                    payloadToStore = RSALogica.cifrarBytesHibrido(encryptedPayload, pub);
                } catch (Exception ex) {
                    System.err.println("[CloudSeeds] Nota: Error aplicando envoltura RSA a semilla Doge: " + ex.getMessage());
                }
            }

            String encB64 = Base64.getEncoder().encodeToString(payloadToStore);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            String userHash = RSALogica.hashConstantToHex(userEmail.trim().toLowerCase());
            lastActiveUserHash = userHash;
            long now = System.currentTimeMillis();
            String id = "DOGE_SEED_" + userHash.substring(0, Math.min(8, userHash.length())) + "_" + now;

            Firestore db = FirebaseService.getFirestore();
            if (db == null) {
                return false;
            }

            String colUsuarios = RSALogica.hashConstantToHex("usuarios");
            String colSemillas = RSALogica.hashConstantToHex("semillas_doge");
            Map<String, Object> cloudData = new HashMap<>();
            cloudData.put("id", id);
            cloudData.put("codigo_wallet", codigoWallet.trim().toUpperCase());
            cloudData.put("red", red.toUpperCase());
            cloudData.put("direccion", direccion != null ? direccion : "");
            cloudData.put("semilla_cifrada", encB64);
            cloudData.put("salt_b64", saltB64);
            cloudData.put("ts", now);
            cloudData.put("fecha_registro", com.google.cloud.Timestamp.now());

            db.collection(colUsuarios + "/" + userHash + "/" + colSemillas).document(id).set(cloudData).get();
            seedIdToUserHash.put(id, userHash);
            System.out.println("[Firestore] Semilla Dogecoin guardada exclusivamente en Firebase Cloud: " + codigoWallet);
            return true;
        } catch (Exception e) {
            System.err.println("[Firestore] Error al guardar semilla Doge en Firebase: " + e.getMessage());
            return false;
        }
    }

    public static List<DogeSeedEntry> obtenerSemillasDogeWallet(String userEmail) {
        List<DogeSeedEntry> list = new ArrayList<>();
        if (userEmail == null || userEmail.isEmpty()) return list;
        String userHash = RSALogica.hashConstantToHex(userEmail.trim().toLowerCase());
        lastActiveUserHash = userHash;

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String colUsuarios = RSALogica.hashConstantToHex("usuarios");
                String colSemillas = RSALogica.hashConstantToHex("semillas_doge");
                var querySnapshot = db.collection(colUsuarios + "/" + userHash + "/" + colSemillas).get().get();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (var doc : querySnapshot.getDocuments()) {
                        String docId = doc.getString("id");
                        if (docId == null) docId = doc.getId();
                        String cod = doc.getString("codigo_wallet");
                        String r = doc.getString("red");
                        String dir = doc.getString("direccion");
                        String semEnc = doc.getString("semilla_cifrada");
                        String saltB = doc.getString("salt_b64");
                        Long ts = doc.getLong("ts");
                        if (ts == null) ts = System.currentTimeMillis();

                        seedIdToUserHash.put(docId, userHash);
                        list.add(new DogeSeedEntry(
                            docId,
                            userHash,
                            cod != null ? cod : "",
                            r != null ? r : "MAINNET",
                            dir != null ? dir : "",
                            semEnc != null ? semEnc : "",
                            saltB != null ? saltB : "",
                            ts
                        ));
                    }
                    list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                }
            }
        } catch (Throwable t) {
            System.err.println("[Firestore] Error al obtener semillas Doge desde Firebase: " + t.getMessage());
        }
        return list;
    }

    public static String descifrarSemillaDogeWallet(DogeSeedEntry entry, char[] pin) throws Exception {
        if (entry == null || pin == null) {
            throw new IllegalArgumentException("Parametros invalidos para descifrar semilla Doge.");
        }
        byte[] salt = Base64.getDecoder().decode(entry.saltB64);
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);

        byte[] rawData = Base64.getDecoder().decode(entry.semillaCifrada);

        PrivateKey priv = SecuritySession.getPrivateKey();
        if (priv != null) {
            try {
                rawData = RSALogica.descifrarBytesHibrido(rawData, priv);
            } catch (Exception ignored) {
                // Retrocompatibilidad con semillas previas sin capa RSA
            }
        }

        if (rawData.length < GCM_IV_SIZE) {
            throw new IllegalArgumentException("Datos corruptos de semilla Doge.");
        }

        byte[] iv = java.util.Arrays.copyOfRange(rawData, 0, GCM_IV_SIZE);
        byte[] enc = java.util.Arrays.copyOfRange(rawData, GCM_IV_SIZE, rawData.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
        byte[] decrypted = cipher.doFinal(enc);

        String frase = new String(decrypted, StandardCharsets.UTF_8);
        java.util.Arrays.fill(decrypted, (byte) 0);
        return frase;
    }

    public static boolean eliminarSemillaDogeWallet(String id) {
        if (id == null) return false;
        try {
            String uHash = seedIdToUserHash.getOrDefault(id, lastActiveUserHash);
            if (uHash == null) return false;

            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String colUsuarios = RSALogica.hashConstantToHex("usuarios");
                String colSemillas = RSALogica.hashConstantToHex("semillas_doge");
                db.collection(colUsuarios + "/" + uHash + "/" + colSemillas).document(id).delete().get();
                seedIdToUserHash.remove(id);
                System.out.println("[Firestore] Semilla Doge eliminada en Firebase Firestore: " + id);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[Firestore] Error al eliminar semilla Doge: " + e.getMessage());
            return false;
        }
    }

    // --- BÓVEDA DE BILLETERAS DOGECOIN ACTIVAS (100% FIREBASE) ---

    public static boolean guardarDogeWalletActiva(String userEmail, boolean testnet, String payloadCifradoB64) {
        if (userEmail == null || payloadCifradoB64 == null) return false;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = DogecoinWalletService.getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> cloudData = new HashMap<>();
                cloudData.put("payload_cifrado", payloadCifradoB64);
                cloudData.put("red", testnet ? "TESTNET" : "MAINNET");
                cloudData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(cloudData, SetOptions.merge()).get();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error guardando wallet Doge activa en Firebase: " + e.getMessage());
        }
        return false;
    }

    public static String obtenerDogeWalletActiva(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return null;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = DogecoinWalletService.getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("payload_cifrado")) {
                    String payload = docSnap.getString("payload_cifrado");
                    return (payload != null && !payload.isEmpty()) ? payload : null;
                }
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error obteniendo wallet Doge activa desde Firebase: " + e.getMessage());
        }
        return null;
    }

    public static boolean existeDogeWalletActiva(String userEmail, boolean testnet) {
        String payload = obtenerDogeWalletActiva(userEmail, testnet);
        return payload != null && !payload.isEmpty();
    }

    public static boolean eliminarDogeWalletActiva(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return false;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = DogecoinWalletService.getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                db.collection(path).document(docId).delete().get();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error eliminando wallet Doge activa en Firebase: " + e.getMessage());
        }
        return false;
    }

    public static String obtenerDogeAddressBook(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return "[]";
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = DogecoinWalletService.getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("address_book_json")) {
                    String json = docSnap.getString("address_book_json");
                    return (json != null && !json.isEmpty()) ? json : "[]";
                }
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error obteniendo libreta Doge desde Firebase: " + e.getMessage());
        }
        return "[]";
    }

    public static void guardarDogeAddressBook(String userEmail, boolean testnet, String addressBookJson) {
        if (userEmail == null || userEmail.isEmpty()) return;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = DogecoinWalletService.getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("address_book_json", addressBookJson != null ? addressBookJson : "[]");
                updateMap.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(updateMap, SetOptions.merge()).get();
            }
        } catch (Exception e) {
            System.err.println("[Firestore] Error guardando libreta Doge en Firebase: " + e.getMessage());
        }
    }
}
