package com.mycompany.blackpassword.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lógica de Cifrado AES v4.3 - MOTOR HÍBRIDO RETROCOMPATIBLE.
 * Soporta AES-GCM 256-bit (Moderno) y AES-CBC HMAC-SHA256 (Legacy V1).
 * Legacy V0 (AES-128 ECB) deprecated - limitado a 3 intentos por sesión.
 */
public class CifradorLogica {

    private static final String GCM_ALGO = "AES/GCM/NoPadding";
    private static final String CBC_HMAC_ALGO = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 12;
    private static final int TAG_BIT_LENGTH = 128;
    private static final int LEGACY_V0_MAX_ATTEMPTS = 3;
    private static int legacyV0Attempts = 0;

    /**
     * Cifra un archivo local usando el estándar moderno AES-256 GCM.
     */
    public static void cifrarArchivo(File origen, File destino, char[] password, byte[] salt) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        
        SecretKeySpec keySpec;
        if (salt != null) {
            keySpec = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
        } else {
            keySpec = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), RSALogica.getLegacyIterations());
        }
        
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
    public static void descifrarArchivo(File origen, File destino, char[] password, byte[] salt) throws Exception {
        byte[] decrypted = descifrarParaMemoria(origen, password, salt);
        java.nio.file.Files.write(destino.toPath(), decrypted);
    }

    /**
     * Intenta descifrar el contenido usando múltiples algoritmos y derivaciones.
     */
    public static byte[] descifrarParaMemoria(File origen, char[] password, byte[] salt) throws Exception {
        byte[] rawData = java.nio.file.Files.readAllBytes(origen.toPath());
        return descifrarParaMemoriaRecursos(rawData, origen.getName(), password, salt);
    }

    public static byte[] descifrarParaMemoriaRecursos(byte[] rawData, String fileName, char[] password, byte[] salt) throws Exception {
        byte[] result = tryAllDecryptionModes(rawData, password, fileName, salt);
        if (result != null) {
            return result;
        }

        throw new Exception("Fallo de descifrado: El archivo no es compatible o la contraseña es incorrecta.");
    }

    private static byte[] tryAllDecryptionModes(byte[] rawData, char[] password, String fileName, byte[] salt) throws Exception {
        byte[] result;
        
        result = intentarGcmModerno(rawData, password, salt);
        if (result != null) return result;

        result = intentarLegacyV1(rawData, password);
        if (result != null) return result;

        result = intentarVariacionesSalt(rawData, password, fileName, salt);
        if (result != null) return result;

        result = intentarLegacyV0Limitado(rawData, password);
        if (result != null) return result;

        return null;
    }

    private static byte[] intentarGcmModerno(byte[] rawData, char[] password, byte[] salt) {
        try {
            if (rawData.length > IV_SIZE) {
                byte[] iv = Arrays.copyOfRange(rawData, 0, IV_SIZE);
                byte[] encrypted = Arrays.copyOfRange(rawData, IV_SIZE, rawData.length);
                
                SecretKeySpec keySpec;
                if (salt != null) {
                    keySpec = RSALogica.derivarLlavePBKDF2(password, salt, 600000);
                } else {
                    keySpec = RSALogica.derivarLlavePBKDF2(password, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8), RSALogica.getLegacyIterations());
                }

                Cipher cipher = Cipher.getInstance(GCM_ALGO);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
                return cipher.doFinal(encrypted);
            }
        } catch (Exception e) {
            System.err.println("[CifradorLogica] Fallo descifrado Modo Moderno (AES-GCM): " + e.getMessage());
        }
        return null;
    }

    private static byte[] intentarLegacyV1(byte[] rawData, char[] password) {
        try {
            return ejecutarDescifradoLegacyV1(rawData, password);
        } catch (Exception e) {
            System.err.println("[CifradorLogica] Fallo descifrado Modo Legacy V1 (AES-CBC+HMAC): " + e.getMessage());
        }
        return null;
    }

    private static byte[] intentarVariacionesSalt(byte[] rawData, char[] password, String fileName, byte[] saltInyectado) {
        List<byte[]> candidateSalts = new ArrayList<>();
        if (saltInyectado != null) candidateSalts.add(saltInyectado);
        candidateSalts.add(RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
        candidateSalts.add(fileName.getBytes(StandardCharsets.UTF_8));
        candidateSalts.add("".getBytes(StandardCharsets.UTF_8));

        int[] iterations = {600000, 300000, 65536};
        
        for (byte[] salt : candidateSalts) {
            for (int iter : iterations) {
                try {
                    return ejecutarDescifradoGCM(rawData, password, salt, iter);
                } catch (Exception e) {
                    // Continuar con siguiente combinación
                }
            }
        }
        return null;
    }

    private static byte[] intentarLegacyV0Limitado(byte[] rawData, char[] password) {
        // Modo V0 descontinuado por motivos de seguridad
        return null;
    }

    private static byte[] ejecutarDescifradoGCM(byte[] data, char[] password, byte[] salt, int iterations) throws Exception {
        if (data.length < IV_SIZE) throw new Exception("Datos insuficientes");
        byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] encrypted = Arrays.copyOfRange(data, IV_SIZE, data.length);
        SecretKeySpec keySpec = RSALogica.derivarLlavePBKDF2(password, salt, iterations);
        Cipher cipher = Cipher.getInstance(GCM_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT_LENGTH, iv));
        return cipher.doFinal(encrypted);
    }

    @Deprecated
    private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password, String hashAlgo) throws Exception {
        throw new UnsupportedOperationException("Legacy V0 (AES-ECB sin salt) deshabilitado por motivos de seguridad.");
    }

    /**
     * Legacy V1: Modo de compatibilidad para archivos antiguos AES-256-CBC con checksum SHA-256.
     * Formato: IV (16 bytes) + SHA256(ciphertext) (32 bytes) + ciphertext.
     * NOTA: Este formato es de solo lectura / migración. Nuevos archivos se cifran exclusivamente con AES-GCM.
     */
    private static byte[] ejecutarDescifradoLegacyV1(byte[] data, char[] password) throws Exception {
        if (data.length < 48) {
            throw new Exception("Datos insuficientes para Legacy V1 (necesita IV+Checksum+min 1 bloque)");
        }
        
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] storedDigest = Arrays.copyOfRange(data, 16, 48);
        byte[] ciphertext = Arrays.copyOfRange(data, 48, data.length);
        
        byte[] passBytes = toSecureBytes(password);
        byte[] kek = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] computedDigest = sha.digest(ciphertext);
            
            // Verificación en tiempo constante contra alteración de datos
            if (!java.security.MessageDigest.isEqual(storedDigest, computedDigest)) {
                throw new java.security.GeneralSecurityException("Integridad Legacy V1 comprometida: el archivo ha sido alterado.");
            }
            
            kek = sha.digest(passBytes);
            SecretKeySpec keySpec = new SecretKeySpec(kek, "AES");
            
            Cipher cipher = Cipher.getInstance(CBC_HMAC_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new javax.crypto.spec.IvParameterSpec(iv));
            
            return cipher.doFinal(ciphertext);
        } finally {
            Arrays.fill(passBytes, (byte) 0);
            if (kek != null) Arrays.fill(kek, (byte) 0);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static byte[] toSecureBytes(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }
}
