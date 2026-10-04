package com.mycompany.blackpassword.services;

import java.nio.CharBuffer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Servicio de Base de Datos Local v10.0 - HSM-BACKED ENCRYPTION.
 * El archivo SQLite se cifra con AES-256-GCM usando llaves protegidas por el hardware del SO.
 */
public class LocalDatabaseService {

    private static final String DB_NAME = "blackvault_local.db";
    private static final String DB_CIPHER_NAME = "blackvault_local.db.enc";
    private static final String DB_TEMP_CIPHER = "blackvault_local.db.enc.tmp";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_SIZE = 12;
    private static final int GCM_TAG_SIZE = 128;
    private static Connection connection;
    private static final Gson gson = new Gson();
    private static boolean isDbDecrypted = false;

    public static void init() {
        try {
            decryptDatabaseIfNeeded();
            isDbDecrypted = true;
            
            String url = "jdbc:sqlite:" + DB_NAME;
            connection = DriverManager.getConnection(url);
            createTables();
            System.out.println("[v10.0] Base de datos local blindada por HSM.");
        } catch (Exception e) {
            System.err.println("[LocalDB] Error crítico: " + e.getMessage());
            secureDeleteTempFile();
        }
    }
    
    private static void secureDeleteTempFile() {
        try {
            Path dbFile = Paths.get(DB_NAME);
            if (Files.exists(dbFile)) {
                Path secureDelete = Paths.get(DB_NAME + ".secure_delete");
                Files.move(dbFile, secureDelete);
                Files.deleteIfExists(secureDelete);
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Advertencia: No se pudo eliminar archivo temporal: " + e.getMessage());
        }
    }

    private static void decryptDatabaseIfNeeded() throws Exception {
        Path cipherFile = Paths.get(DB_CIPHER_NAME);
        Path dbFile = Paths.get(DB_NAME);
        
        if (Files.exists(cipherFile) && !Files.exists(dbFile)) {
            System.out.println("[LocalDB] Descifrando base de datos desde HSM...");
            char[] password = SecuritySession.unsealMasterPassword();
            
            byte[] cipherData = Files.readAllBytes(cipherFile);
            if (cipherData.length < GCM_IV_SIZE + GCM_TAG_SIZE / 8) {
                java.util.Arrays.fill(password, (char) 0);
                throw new Exception("Archivo cifrado corrupto o inválido.");
            }
            
            final byte[] iv = new byte[GCM_IV_SIZE];
            final byte[] encryptedData = new byte[cipherData.length - GCM_IV_SIZE];
            System.arraycopy(cipherData, 0, iv, 0, GCM_IV_SIZE);
            System.arraycopy(cipherData, GCM_IV_SIZE, encryptedData, 0, encryptedData.length);
            
            try {
                SecretKeySpec key = RSALogica.derivarLlavePBKDF2(password, "Blackpassword_DB_Key_2026".getBytes(StandardCharsets.UTF_8), 600000);
                Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
                byte[] decrypted = cipher.doFinal(encryptedData);
                Files.write(Paths.get(DB_NAME), decrypted);
                java.util.Arrays.fill(decrypted, (byte) 0);
            } finally {
                java.util.Arrays.fill(password, (char) 0);
            }
            
            System.out.println("[LocalDB] Base de datos descifrada exitosamente.");
        } else if (!Files.exists(dbFile)) {
            Files.createFile(dbFile);
        }
    }

    public static void closeAndEncrypt() {
        if (!isDbDecrypted) {
            return;
        }
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
            
            Path dbFile = Paths.get(DB_NAME);
            if (Files.exists(dbFile)) {
                char[] password = SecuritySession.unsealMasterPassword();
                if (password != null) {
                    Path tempCipherFile = Paths.get(DB_TEMP_CIPHER);
                    Path finalCipherFile = Paths.get(DB_CIPHER_NAME);
                    
                    try {
                        encryptDatabaseAtomic(dbFile, tempCipherFile, password);
                        Files.deleteIfExists(finalCipherFile);
                        Files.move(tempCipherFile, finalCipherFile);
                        Files.deleteIfExists(dbFile);
                        System.out.println("[LocalDB] Base de datos cifrada y llave purgada.");
                    } finally {
                        java.util.Arrays.fill(password, (char) 0);
                    }
                    isDbDecrypted = false;
                }
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al cifrar base de datos: " + e.getMessage());
            secureOverwriteAndDelete(Paths.get(DB_NAME));
        }
    }
    
    private static void encryptDatabaseAtomic(Path dbFile, Path tempFile, char[] password) throws Exception {
        byte[] plainData = Files.readAllBytes(dbFile);
        byte[] iv = new byte[GCM_IV_SIZE];
        new SecureRandom().nextBytes(iv);
        
        SecretKeySpec key = deriveKeyFromPassword(password);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
        
        byte[] encrypted = cipher.doFinal(plainData);
        java.util.Arrays.fill(plainData, (byte) 0);
        
        ByteBuffer output = ByteBuffer.allocate(GCM_IV_SIZE + encrypted.length);
        output.put(iv);
        output.put(encrypted);
        
        Files.write(tempFile, output.array());
    }
    
    private static void secureOverwriteAndDelete(Path file) {
        if (!Files.exists(file)) {
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
            System.out.println("[LocalDB] Archivo seguro eliminado.");
        } catch (Exception e) {
            System.err.println("[LocalDB] Advertencia: No se pudo sobrescribir seguro: " + e.getMessage());
            try {
                Files.deleteIfExists(file);
            } catch (Exception ex) {
                System.err.println("[LocalDB] CRÍTICO: Archivo no pudo ser eliminado: " + file);
            }
        }
    }

    private static SecretKeySpec deriveKeyFromPassword(char[] password) throws Exception {
        byte[] passwordBytes = toBytes(password);
        try {
            SecretKeySpec key = RSALogica.derivarLlavePBKDF2(password, "Blackpassword_DB_Key_2026".getBytes(StandardCharsets.UTF_8), 300000);
            return key;
        } finally {
            java.util.Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    private static byte[] toBytes(char[] chars) {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }

    private static synchronized void ensureConnected() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error verificando conexion: " + e.getMessage());
        }
    }

    private static void createTables() throws Exception {
        String sqlBoveda = "CREATE TABLE IF NOT EXISTS boveda (" +
                "id TEXT PRIMARY KEY, " +
                "u_h TEXT, " +
                "d_c TEXT, " +
                "ts INTEGER)";

        String sqlSemillas = "CREATE TABLE IF NOT EXISTS boveda_semillas (" +
                "id TEXT PRIMARY KEY, " +
                "u_h TEXT NOT NULL, " +
                "codigo_wallet TEXT NOT NULL, " +
                "red TEXT NOT NULL, " +
                "direccion TEXT, " +
                "semilla_cifrada TEXT NOT NULL, " +
                "salt_b64 TEXT NOT NULL, " +
                "ts INTEGER NOT NULL)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlBoveda);
            stmt.execute(sqlSemillas);
        }
    }

    public static boolean savePassword(String email, String service, String username, String password) {
        if (!SecuritySession.isSessionActive() || !SecuritySession.isIdentityValid(email)) {
            System.err.println("[LocalDB] Intento de escritura bloqueado: Identidad no validada.");
            return false;
        }
        
        try {
            String serviceUpper = service.trim().toUpperCase();
            
            // Usar formato prefijado s:|u:|p: consistente con VaultManager
            String raw = "s:" + serviceUpper + "|u:" + username + "|p:" + password;
            byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = RSALogica.cifrarBytesHibrido(rawBytes, SecuritySession.getPublicKey());
            String encryptedDataAsBase64 = Base64.getEncoder().encodeToString(encryptedBytes);

            String sql = "INSERT OR REPLACE INTO boveda (id, u_h, d_c, ts) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, serviceUpper);
                pstmt.setString(2, RSALogica.hashConstantToHex(email));
                pstmt.setString(3, encryptedDataAsBase64);
                pstmt.setLong(4, System.currentTimeMillis());
                pstmt.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al guardar credencial: " + e.getMessage());
            return false;
        }
    }

    public static List<Map<String, Object>> getPasswords(String email) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        if (!SecuritySession.isSessionActive() || !SecuritySession.isIdentityValid(email)) {
            return list;
        }

        String sql = "SELECT id, d_c FROM boveda WHERE u_h = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, RSALogica.hashConstantToHex(email));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String service = rs.getString("id");
                String encryptedData = rs.getString("d_c");
                Map<String, Object> row = new HashMap<>();
                
                try {
                    // Re-try with proper byte-to-string conversion
                    byte[] decBytes = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(encryptedData), SecuritySession.getPrivateKey());
                    String raw = new String(decBytes, StandardCharsets.UTF_8);
                    
                    String s = service != null ? service : "DESCONOCIDO";
                    String u = "DESCONOCIDO";
                    
                    if (raw.startsWith("{")) {
                        // Soporte Legacy JSON
                        Type type = new TypeToken<Map<String, String>>(){}.getType();
                        Map<String, String> dataMap = gson.fromJson(raw, type);
                        u = dataMap.getOrDefault("username", "(No disponible)");
                    } else {
                        // Nuevo formato prefijado
                        for (String part : raw.split("\\|")) {
                            if (part.startsWith("s:")) s = part.substring(2);
                            if (part.startsWith("u:")) u = part.substring(2);
                        }
                    }
                    
                    row.put("service", s.toUpperCase());
                    row.put("username", u);
                    row.put("encrypted_payload", encryptedData);
                    list.add(row);

                } catch (Exception e) {
                    Map<String, Object> fallbackRow = new HashMap<>();
                    fallbackRow.put("service", service != null ? service.toUpperCase() : "ERROR");
                    fallbackRow.put("encrypted_payload", encryptedData);
                    fallbackRow.put("username", "(Error de descifrado)");
                    list.add(fallbackRow);
                }
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error de lectura: " + e.getMessage());
        }
        return list;
    }

    public static void syncSecret(String id, String email, String dataCifradaB64) {
        if (!SecuritySession.isSessionActive() || !SecuritySession.isIdentityValid(email)) return;
        try {
            String idUpper = id.trim().toUpperCase();
            String sql = "INSERT OR REPLACE INTO boveda (id, u_h, d_c, ts) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, idUpper);
                pstmt.setString(2, RSALogica.hashConstantToHex(email));
                pstmt.setString(3, dataCifradaB64);
                pstmt.setLong(4, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error en sincronización: " + e.getMessage());
        }
    }

    public static void deleteLocalSecret(String id) {
        String idUpper = id.trim().toUpperCase();
        String sql = "DELETE FROM boveda WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, idUpper);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al eliminar secreto local: " + e.getMessage());
        }
    }
    
    public static void clearCache(String email) {
        try {
            String sql = "DELETE FROM boveda WHERE u_h = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, RSALogica.hashConstantToHex(email));
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al limpiar caché: " + e.getMessage());
        }
    }

    // --- BÓVEDA DE SEMILLAS DE BILLETERAS BITCOIN ---

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
        ensureConnected();
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

            String encB64 = Base64.getEncoder().encodeToString(encryptedPayload);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            String userHash = RSALogica.hashConstantToHex(userEmail);
            String id = "SEED_" + userHash.substring(0, Math.min(8, userHash.length())) + "_" + System.currentTimeMillis();

            String sql = "INSERT OR REPLACE INTO boveda_semillas (id, u_h, codigo_wallet, red, direccion, semilla_cifrada, salt_b64, ts) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, userHash);
                pstmt.setString(3, codigoWallet.trim().toUpperCase());
                pstmt.setString(4, red.toUpperCase());
                pstmt.setString(5, direccion != null ? direccion : "");
                pstmt.setString(6, encB64);
                pstmt.setString(7, saltB64);
                pstmt.setLong(8, System.currentTimeMillis());
                pstmt.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al guardar semilla en boveda: " + e.getMessage());
            return false;
        }
    }

    public static List<BtcSeedEntry> obtenerSemillasWallet(String userEmail) {
        List<BtcSeedEntry> list = new ArrayList<>();
        if (userEmail == null || userEmail.isEmpty()) return list;
        ensureConnected();
        try {
            String userHash = RSALogica.hashConstantToHex(userEmail);
            String sql = "SELECT id, u_h, codigo_wallet, red, direccion, semilla_cifrada, salt_b64, ts FROM boveda_semillas WHERE u_h = ? ORDER BY ts DESC";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userHash);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new BtcSeedEntry(
                            rs.getString("id"),
                            rs.getString("u_h"),
                            rs.getString("codigo_wallet"),
                            rs.getString("red"),
                            rs.getString("direccion"),
                            rs.getString("semilla_cifrada"),
                            rs.getString("salt_b64"),
                            rs.getLong("ts")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al obtener semillas: " + e.getMessage());
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
        ensureConnected();
        try {
            String sql = "DELETE FROM boveda_semillas WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al eliminar semilla: " + e.getMessage());
            return false;
        }
    }
}
