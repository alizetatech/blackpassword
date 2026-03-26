package com.mycompany.blackpassword;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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

/**
 * Servicio de Base de Datos Local v8.0 - ESTRUCTURA ORIGINAL DE 3 CAMPOS RESTAURADA.
 */
public class LocalDatabaseService {

    private static final String DB_NAME = "blackvault_local.db";
    private static Connection connection;
    private static final Gson gson = new Gson();

    public static void init() {
        try {
            String url = "jdbc:sqlite:" + DB_NAME;
            connection = DriverManager.getConnection(url);
            createTables();
            System.out.println("[v8.0] Base de datos local restaurada a la estructura original.");
        } catch (Exception e) {
            System.err.println("[LocalDB] Error crítico: " + e.getMessage());
        }
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
     * Guarda una credencial empaquetando usuario y contraseña en un JSON, como en el formato original.
     */
    public static boolean savePassword(String email, String service, String username, String password) {
        if (!SecuritySession.isSessionActive()) return false;
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
     * Recupera y descifra las credenciales, extrayendo el usuario y la contraseña del JSON interno.
     */
    public static List<Map<String, Object>> getPasswords(String email) {
        // Línea de diagnóstico temporal
        System.out.println("\n\n>>> [DIAGNÓSTICO BLACKPASSWORD] Verificando estado de sesión en LocalDatabaseService.getPasswords(). Activa: " + SecuritySession.isSessionActive() + " <<<\n\n");

        List<Map<String, Object>> list = new ArrayList<>();
        if (!SecuritySession.isSessionActive()) return list;

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

                    // Parsea el JSON para obtener los campos internos
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> dataMap = gson.fromJson(decryptedJson, type);
                    
                    row.put("username", dataMap.getOrDefault("username", "(No disponible)"));
                    // La contraseña real no se añade aquí por seguridad, solo el payload cifrado
                    
                    list.add(row);

                } catch (Exception e) {
                    // Fallback por si un dato no es JSON (debido a mis cambios anteriores)
                    Map<String, Object> fallbackRow = new HashMap<>();
                    fallbackRow.put("service", service);
                    fallbackRow.put("encrypted_payload", encryptedData);
                    fallbackRow.put("username", "(Dato antiguo - Sin usuario)");
                    list.add(fallbackRow);
                    System.err.println("[LocalDB] Registro en formato no estándar detectado: " + service);
                }
            }
        } catch (Exception e) {
            System.err.println("[LocalDB] Error de lectura: " + e.getMessage());
        }
        return list;
    }

    // Métodos de sync, delete y clear se mantienen.
    public static void syncSecret(String id, String email, String dataCifradaB64) {
        if (!SecuritySession.isSessionActive()) return;
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
        } catch (Exception e) {}
    }
    
    public static void clearCache(String email) {
        try {
            String sql = "DELETE FROM boveda WHERE u_h = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, RSALogica.hashToHex(email));
                pstmt.executeUpdate();
            }
        } catch (Exception e) {}
    }
}
