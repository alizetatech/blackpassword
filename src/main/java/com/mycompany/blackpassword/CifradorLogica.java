package com.mycompany.blackpassword;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Lógica de Cifrado AES v4.0 - COMPATIBILIDAD TOTAL.
 * Soporta AES-GCM con múltiples sales y Legacy V0.
 */
public class CifradorLogica {

    private static final String GCM_ALGO = "AES/GCM/NoPadding";
    private static final String LEGACY_ALGO = "AES";

    public static byte[] descifrarParaMemoria(File origen, char[] password) throws Exception {
        byte[] rawData = java.nio.file.Files.readAllBytes(origen.toPath());
        
        // 1. Intentar Descifrado Moderno (GCM) con múltiples combinaciones
        String[] salts = {RSALogica.getUniversalSalt(), origen.getName(), ""};
        int[] iters = {RSALogica.getLegacyIterations(), 600000};

        for (int iter : iters) {
            for (String salt : salts) {
                try {
                    return ejecutarDescifradoGCM(rawData, password, salt, iter);
                } catch (Exception ignored) {}
            }
        }

        // 2. Intentar Descifrado Legacy V0 (AES Simple)
        try {
            return ejecutarDescifradoLegacyV0(rawData, password);
        } catch (Exception ignored) {}

        throw new Exception("No se pudo descifrar el archivo de configuración. Verifique su contraseña maestra.");
    }

    private static byte[] ejecutarDescifradoGCM(byte[] data, char[] password, String salt, int iterations) throws Exception {
        if (data.length < 12) throw new Exception("Datos insuficientes");
        
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);

        SecretKeySpec keySpec = RSALogica.derivarLlavePBKDF2(password, salt, iterations);
        Cipher cipher = Cipher.getInstance(GCM_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) throws Exception {
        byte[] passBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(sha.digest(passBytes), 16);
        
        SecretKeySpec secretKey = new SecretKeySpec(key, LEGACY_ALGO);
        Cipher cipher = Cipher.getInstance(LEGACY_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }
}
