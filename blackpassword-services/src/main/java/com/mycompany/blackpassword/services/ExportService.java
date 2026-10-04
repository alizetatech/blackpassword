package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.crypto.RSALogica;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.nio.ByteBuffer;

/**
 * Servicio de Exportación Segura v1.0.
 * Maneja la generación de reportes CSV con múltiples capas de cifrado (Plano, AES, KEK).
 */
public class ExportService {

    /**
     * Genera el contenido CSV a partir de los secretos descifrados.
     */
    public static String generateCsvContent(List<Map<String, Object>> rawSecrets, java.security.PrivateKey privateKey) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("SITIO/SERVICIO,USUARIO/EMAIL,CONTRASEÑA,FECHA ACTUALIZACION\n");

        for (Map<String, Object> doc : rawSecrets) {
            String enc = (String) doc.get("encrypted_payload");
            if (enc == null) continue;

            try {
                byte[] decBytes = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(enc), privateKey);
                String raw = new String(decBytes, StandardCharsets.UTF_8);
                
                String s = "N/A", u = "N/A", p = "N/A";
                for (String part : raw.split("\\|")) {
                    if (part.startsWith("s:")) s = part.substring(2);
                    if (part.startsWith("u:")) u = part.substring(2);
                    if (part.startsWith("p:")) p = part.substring(2);
                }
                
                // Escapar comas para formato CSV
                s = s.replace(",", ";");
                u = u.replace(",", ";");
                p = p.replace(",", ";");
                
                csv.append(s).append(",").append(u).append(",").append(p).append(",")
                   .append(doc.getOrDefault("date", "N/A")).append("\n");
                
            } catch (Exception e) {
                csv.append("ERROR,DESCIFRADO FALLIDO,,N/A\n");
            }
        }
        return csv.toString();
    }

    /**
     * Guarda el contenido cifrado con AES-256-GCM estándar.
     */
    public static void exportAesEncrypted(String content, File target, char[] password) throws Exception {
        byte[] salt = SecuritySession.getUserSalt();
        SecretKeySpec key;
        if (salt != null) {
            key = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
        } else {
            key = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), 600000);
        }
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        
        ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
        bb.put(iv);
        bb.put(encrypted);
        
        Files.write(target.toPath(), bb.array());
    }

    /**
     * Guarda el contenido blindado con KEK (Key Wrapping logic).
     */
    public static void exportKekEncrypted(String content, File target, char[] passphrase) throws Exception {
        byte[] salt = SecuritySession.getUserSalt();
        byte[] wrapped;
        if (salt != null) {
            wrapped = RSALogica.wrapCEK(content.getBytes(StandardCharsets.UTF_8), passphrase, salt);
        } else {
            wrapped = RSALogica.wrapCEK(content.getBytes(StandardCharsets.UTF_8), passphrase, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
        }
        Files.write(target.toPath(), wrapped);
    }

    /**
     * Descifra contenido previamente cifrado con AES-256-GCM.
     */
    public static String decryptAes(byte[] data, char[] password) throws Exception {
        if (data.length < 12) throw new Exception("Archivo corrupto.");
        byte[] salt = SecuritySession.getUserSalt();
        SecretKeySpec key;
        if (salt != null) {
            key = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
        } else {
            key = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), 600000);
        }
        byte[] iv = java.util.Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = java.util.Arrays.copyOfRange(data, 12, data.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    /**
     * Descifra contenido blindado con KEK.
     */
    public static String decryptKek(byte[] data, char[] passphrase) throws Exception {
        byte[] salt = SecuritySession.getUserSalt();
        byte[] unwrapped;
        if (salt != null) {
            unwrapped = RSALogica.unwrapCEK(data, passphrase, salt);
        } else {
            unwrapped = RSALogica.unwrapCEK(data, passphrase, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
        }
        return new String(unwrapped, StandardCharsets.UTF_8);
    }

    /**
     * Procesa una cadena CSV y retorna una lista de mapas (servicio, usuario, password).
     */
    public static java.util.List<java.util.Map<String, String>> parseCsv(String csv) {
        java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();
        String[] lines = csv.split("\n");
        if (lines.length <= 1) return list;
        
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split(",");
            if (cols.length >= 3) {
                java.util.Map<String, String> row = new java.util.HashMap<>();
                row.put("s", cols[0].trim().replace(";", ","));
                row.put("u", cols[1].trim().replace(";", ","));
                row.put("p", cols[2].trim().replace(";", ","));
                list.add(row);
            }
        }
        return list;
    }

    private static final byte[] WFILE_MAGIC = new byte[]{0x57, 0x4B, 0x45, 0x4B}; // 'WKEK' v2 magic header

    /**
     * Blinda un archivo local con una KEK utilizando Streaming (bajo consumo RAM).
     * Estructura v2: MAGIC(4B) + SALT(16B) + IV(12B) + ciphertext(AES-256-GCM)
     */
    public static void blindFile(File src, File dst, char[] pass, java.util.function.Consumer<Double> progress) throws Exception {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pass, salt, 600000);
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        
        try (java.io.InputStream in = new java.io.FileInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            
            out.write(WFILE_MAGIC); // 4 bytes magic
            out.write(salt);        // 16 bytes salt autónomo
            out.write(iv);          // 12 bytes IV GCM
            
            byte[] buffer = new byte[8192];
            int read;
            long totalRead = 0;
            long fileSize = src.length();
            
            while ((read = in.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, read);
                if (output != null) out.write(output);
                totalRead += read;
                if (progress != null) progress.accept((double) totalRead / fileSize);
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) out.write(finalBytes);
        }
    }

    /**
     * Desblinda un archivo .wfile restaurando su contenido original.
     * Soporta automáticamente contenedores v2 (con salt propio) y v1 (con salt de sesión / universal).
     */
    public static void unblindFile(File src, File dst, char[] pass, java.util.function.Consumer<Double> progress) throws Exception {
        try (java.io.InputStream in = new java.io.FileInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            
            byte[] header = new byte[4];
            if (in.read(header) < 4) throw new Exception("Archivo corrupto.");
            
            SecretKeySpec key;
            byte[] iv = new byte[12];
            
            if (java.util.Arrays.equals(header, WFILE_MAGIC)) {
                // Formato v2 con salt autónomo
                byte[] salt = new byte[16];
                if (in.read(salt) < 16) throw new Exception("Encabezado de salt incompleto.");
                if (in.read(iv) < 12) throw new Exception("Vector IV incompleto.");
                key = RSALogica.derivarLlavePBKDF2(pass, salt, 600000);
            } else {
                // Formato legacy v1 (los primeros 4 bytes leídos eran parte del IV de 12 bytes)
                System.arraycopy(header, 0, iv, 0, 4);
                if (in.read(iv, 4, 8) < 8) throw new Exception("Vector IV legacy incompleto.");
                byte[] salt = SecuritySession.getUserSalt();
                if (salt != null) {
                    key = RSALogica.derivarLlavePBKDF2(pass, salt, 600000);
                } else {
                    key = RSALogica.derivarLlavePBKDF2(pass, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), 600000);
                }
            }
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            
            byte[] buffer = new byte[8192];
            int read;
            long totalRead = 32;
            long fileSize = src.length();
            
            while ((read = in.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, read);
                if (output != null) out.write(output);
                totalRead += read;
                if (progress != null) progress.accept((double) totalRead / fileSize);
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) out.write(finalBytes);
        }
    }

    /**
     * Desblinda un archivo .wfile o .kek directamente hacia la memoria RAM.
     * Soporta automáticamente formato autónomo v2 y formato legacy v1.
     */
    public static byte[] unblindFileToMemory(File src, char[] pass) throws Exception {
        try (java.io.InputStream in = new java.io.FileInputStream(src)) {
            byte[] header = new byte[4];
            if (in.read(header) < 4) throw new Exception("Archivo encapsulado corrupto o incompleto.");
            
            SecretKeySpec key;
            byte[] iv = new byte[12];
            
            if (java.util.Arrays.equals(header, WFILE_MAGIC)) {
                // Formato v2 autónomo
                byte[] salt = new byte[16];
                if (in.read(salt) < 16) throw new Exception("Salt de contenedor KEK incompleto.");
                if (in.read(iv) < 12) throw new Exception("IV de contenedor KEK incompleto.");
                key = RSALogica.derivarLlavePBKDF2(pass, salt, 600000);
            } else {
                // Formato legacy v1
                System.arraycopy(header, 0, iv, 0, 4);
                if (in.read(iv, 4, 8) < 8) throw new Exception("IV legacy incompleto.");
                byte[] salt = SecuritySession.getUserSalt();
                if (salt != null) {
                    key = RSALogica.derivarLlavePBKDF2(pass, salt, 600000);
                } else {
                    key = RSALogica.derivarLlavePBKDF2(pass, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), 600000);
                }
            }
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            
            while ((read = in.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, read);
                if (output != null) out.write(output);
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) out.write(finalBytes);
            return out.toByteArray();
        }
    }

    /**
     * Desblinda un archivo .wcek (Legacy) directamente hacia la memoria RAM.
     */
    public static byte[] unblindWcekToMemory(File src, char[] passphrase) throws Exception {
        byte[] data = java.nio.file.Files.readAllBytes(src.toPath());
        byte[] salt = SecuritySession.getUserSalt();
        if (salt != null) {
            return RSALogica.unwrapCEK(data, passphrase, salt);
        } else {
            return RSALogica.unwrapCEK(data, passphrase, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
        }
    }
}
