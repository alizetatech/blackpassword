package com.mycompany.blackpassword.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * Lógica de Cifrado AES v4.2 - MOTOR HÍBRIDO RETROCOMPATIBLE.
 * Soporta AES-GCM 256-bit (Moderno) y AES-128 SHA-256 (Legacy).
 */
public class CifradorLogica {

    private static final String GCM_ALGO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_BIT_LENGTH = 128;

    /**
     * Cifra un archivo local usando el estándar moderno AES-256 GCM.
     */
    public static void cifrarArchivo(File origen, File destino, char[] password) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        
        SecretKeySpec keySpec = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt(), RSALogica.getLegacyIterations());
        Cipher cipher = Cipher.getInstance(GCM_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
        
        byte[] input = java.nio.file.Files.readAllBytes(origen.toPath());
        byte[] encrypted = cipher.doFinal(input);
        
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            fos.write(iv);
            fos.write(encrypted);
        }
    }

    /**
     * Descifra un archivo intentando primero el modo moderno y luego el modo Legacy.
     */
    public static void descifrarArchivo(File origen, File destino, char[] password) throws Exception {
        byte[] decrypted = descifrarParaMemoria(origen, password);
        java.nio.file.Files.write(destino.toPath(), decrypted);
    }

    /**
     * Intenta descifrar el contenido usando múltiples algoritmos y derivaciones.
     */
    public static byte[] descifrarParaMemoria(File origen, char[] password) throws Exception {
        byte[] rawData = java.nio.file.Files.readAllBytes(origen.toPath());
        
        // Intento 1: Modo Moderno (AES-GCM 256 + PBKDF2)
        try {
            if (rawData.length > IV_SIZE) {
                byte[] iv = Arrays.copyOfRange(rawData, 0, IV_SIZE);
                byte[] encrypted = Arrays.copyOfRange(rawData, IV_SIZE, rawData.length);
                SecretKeySpec keySpec = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt(), RSALogica.getLegacyIterations());
                Cipher cipher = Cipher.getInstance(GCM_ALGO);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
                return cipher.doFinal(encrypted);
            }
        } catch (Exception e) {
            System.err.println("[CifradorLogica] Fallo descifrado Modo Moderno (AES-GCM): " + e.getMessage());
        }

        // Intento 2: Modo Legacy (AES-128 + SHA-256) - Compatible con proyecto 'CifrarArchivo'
        try {
            return ejecutarDescifradoLegacyV0(rawData, password);
        } catch (Exception e) {
            System.err.println("[CifradorLogica] Fallo descifrado Modo Legacy: " + e.getMessage());
        }

        // Intento 3: Variaciones de Salt (Para archivos de versiones intermedias)
        String[] salts = {RSALogica.getUniversalSalt(), origen.getName(), ""};
        for (String salt : salts) {
            try {
                return ejecutarDescifradoGCM(rawData, password, salt, 600000);
            } catch (Exception e) {
                System.err.println("[CifradorLogica] Fallo descifrado GCM con salt=" + salt + ": " + e.getMessage());
            }
        }

        throw new Exception("Fallo de descifrado: El archivo no es compatible o la contraseña es incorrecta.");
    }

    private static byte[] ejecutarDescifradoGCM(byte[] data, char[] password, String salt, int iterations) throws Exception {
        if (data.length < IV_SIZE) throw new Exception("Datos insuficientes");
        byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] encrypted = Arrays.copyOfRange(data, IV_SIZE, data.length);
        SecretKeySpec keySpec = RSALogica.derivarLlavePBKDF2(password, salt, iterations);
        Cipher cipher = Cipher.getInstance(GCM_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
        return cipher.doFinal(encrypted);
    }

    /**
     * Implementación exacta del descifrado del proyecto 'CifrarArchivo'.
     */
    private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) throws Exception {
        byte[] passBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(passBytes);
        key = Arrays.copyOf(key, 16); // AES-128 bits
        
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES"); // Modo por defecto (ECB/PKCS5)
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }
}
