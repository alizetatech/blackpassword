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
 * Servicio de Base de Datos Local v9.0 - CIFRADO DE ARCHIVO COMPLETO.
 * El archivo SQLite se cifra con AES-256-GCM cuando no está en uso.
 */
public class LocalDatabaseService {

    private static final String DB_NAME = "blackvault_local.db";
    private static final String DB_CIPHER_NAME = "blackvault_local.db.enc";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_SIZE = 12;
    private static final int GCM_TAG_SIZE = 128;
    private static final int KEY_SIZE = 32;
    private static Connection connection;
    private static final Gson gson = new Gson();
    private static Path tempDecryptedDb;

    public static void init() {
        try {
            decryptDatabaseIfNeeded();
            
            String url = "jdbc:sqlite:" + DB_NAME;
            connection = DriverManager.getConnection(url);
            createTables();
            System.out.println("[v9.0] Base de datos local con cifrado AES-256-GCM.");
        } catch (Exception e) {
            System.err.println("[LocalDB] Error crítico: " + e.getMessage());
        }
    }

    private static void decryptDatabaseIfNeeded() throws Exception {
        Path cipherFile = Paths.get(DB_CIPHER_NAME);
        Path dbFile = Paths.get(DB_NAME);
        
        if (Files.exists(cipherFile) && !Files.exists(dbFile)) {
            System.out.println("[LocalDB] Descifrando base de datos...");
            char[] masterPassword = SecuritySession.getMasterPassword();
            if (masterPassword == null || masterPassword.length == 0) {
                throw new Exception("Se requiere la contraseña maestra para descifrar la base de datos.");
            }
            
            byte[] cipherData = Files.readAllBytes(cipherFile);
            
            if (cipherData.length < GCM_IV_SIZE + GCM_TAG_SIZE / 8) {
                throw new Exception("Archivo cifrado corrupto o inválido.");
            }
            
            byte[] iv = new byte[GCM_IV_SIZE];
            byte[] encryptedData = new byte[cipherData.length - GCM_IV_SIZE];
            System.arraycopy(cipherData, 0, iv, 0, GCM_IV_SIZE);
            System.arraycopy(cipherData, GCM_IV_SIZE, encryptedData, 0, encryptedData.length);
            
            SecretKeySpec key = deriveKeyFromPassword(masterPassword);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
            
            byte[] decrypted = cipher.doFinal(encryptedData);
            Files.write(dbFile, decrypted);
            
            System.out.println("[LocalDB] Base de datos descifrada exitosamente.");
        } else if (!Files.exists(dbFile)) {
            Files.createFile(dbFile);
        }
    }

    public static void closeAndEncrypt() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
            
            Path dbFile = Paths.get(DB_NAME);
            if (Files.exists(dbFile)) {
                char[] masterPassword = SecuritySession.getMasterPassword();
                if (masterPassword != null && masterPassword.length > 0) {
                    encryptDatabase(dbFile, masterPassword);
                    Files.deleteIfExists(dbFile);
                    System.out.println("[LocalDB] Base de datos cifrada y archivo temporal eliminado.");
                }
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al cifrar base de datos: " + e.getMessage());
        }
    }

    private static void encryptDatabase(Path dbFile, char[] password) throws Exception {
        byte[] plainData = Files.readAllBytes(dbFile);
        byte[] iv = new byte[GCM_IV_SIZE];
        new SecureRandom().nextBytes(iv);
        
        SecretKeySpec key = deriveKeyFromPassword(password);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_SIZE, iv));
        
        byte[] encrypted = cipher.doFinal(plainData);
        
        ByteBuffer output = ByteBuffer.allocate(GCM_IV_SIZE + encrypted.length);
        output.put(iv);
        output.put(encrypted);
        
        Files.write(Paths.get(DB_CIPHER_NAME), output.array());
    }

    private static SecretKeySpec deriveKeyFromPassword(char[] password) throws Exception {
        byte[] passwordBytes = toBytes(password);
        try {
            SecretKeySpec key = RSALogica.derivarLlavePBKDF2(password, "Blackpassword_DB_Key_2026", 300000);
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

    private static void createTables() throws Exception {
        String sqlBoveda = "CREATE TABLE IF NOT EXISTS boveda (" +
                "id TEXT PRIMARY KEY, " +
                "u_h TEXT, " +
                "d_c TEXT, " +
                "ts INTEGER)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlBoveda);
        }
    }

    /**
     * Guarda una credencial validando primero la identidad del operario.
     */
    public static boolean savePassword(String email, String service, String username, String password) {
        if (!SecuritySession.isSessionActive() || !SecuritySession.isIdentityValid(email)) {
            System.err.println("[LocalDB] Intento de escritura bloqueado: Identidad no validada.");
            return false;
        }
        
        try {
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("username", username);
            dataMap.put("password", password);
            String jsonData = gson.toJson(dataMap);

            byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = RSALogica.cifrarBytesHibrido(jsonBytes, SecuritySession.getPublicKey());
            String encryptedDataAsBase64 = Base64.getEncoder().encodeToString(encryptedBytes);

            String sql = "INSERT OR REPLACE INTO boveda (id, u_h, d_c, ts) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, service);
                pstmt.setString(2, RSALogica.hashToHex(email));
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

    /**
     * Recupera credenciales validando la integridad de la identidad.
     */
    public static List<Map<String, Object>> getPasswords(String email) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        if (!SecuritySession.isSessionActive() || !SecuritySession.isIdentityValid(email)) {
            System.err.println("[LocalDB] Intento de lectura bloqueado: Identidad no validada.");
            return list;
        }

        String sql = "SELECT id, d_c FROM boveda WHERE u_h = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, RSALogica.hashToHex(email));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String service = rs.getString("id");
                String encryptedData = rs.getString("d_c");
                Map<String, Object> row = new HashMap<>();
                
                try {
                    String decryptedJson = RSALogica.decrypt(encryptedData, SecuritySession.getPrivateKey());
                    
                    row.put("service", service);
                    row.put("encrypted_payload", encryptedData);

                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> dataMap = gson.fromJson(decryptedJson, type);
                    
                    row.put("username", dataMap.getOrDefault("username", "(No disponible)"));
                    list.add(row);

                } catch (Exception e) {
                    Map<String, Object> fallbackRow = new HashMap<>();
                    fallbackRow.put("service", service);
                    fallbackRow.put("encrypted_payload", encryptedData);
                    fallbackRow.put("username", "(Dato antiguo - Sin usuario)");
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
            String sql = "INSERT OR REPLACE INTO boveda (id, u_h, d_c, ts) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, RSALogica.hashToHex(email));
                pstmt.setString(3, dataCifradaB64);
                pstmt.setLong(4, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error en sincronización: " + e.getMessage());
        }
    }

    public static void deleteLocalSecret(String id) {
        String sql = "DELETE FROM boveda WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al eliminar secreto local: " + e.getMessage());
        }
    }
    
    public static void clearCache(String email) {
        try {
            String sql = "DELETE FROM boveda WHERE u_h = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, RSALogica.hashToHex(email));
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error al limpiar caché: " + e.getMessage());
        }
    }
}
