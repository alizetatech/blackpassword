package com.mycompany.blackpassword;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Lógica de Cifrado v4.9 - MOTOR DE RESCATE FINAL.
 * Incluye soporte para descifrado de cadenas Base64 para integración con Fortaleza.
 */
public class RSALogica {

    static {
        System.out.println("\n***************************************************");
        System.out.println("*** EJECUTANDO MOTOR DE RESCATE v4.9-FINAL ***");
        System.out.println("***************************************************\n");
    }

    private static final String RSA = "RSA";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String AES_CTR = "AES/CTR/NoPadding";
    private static final int ITER_LEGACY = 65536;
    private static final int ITER_MODERN = 600000;
    private static final String UNIVERSAL_SALT = "Blackpassword_Vault_2026";

    public static KeyPair generarLlaves() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA);
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Descifra una cadena Base64 usando la llave privada RSA.
     */
    public static String decrypt(String base64Data, PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64Data);
        byte[] decryptedBytes = descifrarBytesHibrido(encryptedBytes, privateKey);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String hashSHA256(char[] input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = toBytes(input);
        try { return Base64.getEncoder().encodeToString(md.digest(bytes)); }
        finally { Arrays.fill(bytes, (byte) 0); }
    }
    
    public static String hashToHex(String input) throws Exception {
        if (input == null) return "null";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    public static SecretKeySpec derivarLlavePBKDF2(char[] password, String sal, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, sal.getBytes(StandardCharsets.UTF_8), iterations, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(skf.generateSecret(spec).getEncoded(), "AES");
        } finally { spec.clearPassword(); }
    }

    public static void guardarLlave(File archivo, byte[] keyBytes, boolean proteger, char[] password) throws Exception {
        byte[] dataToSave = keyBytes;
        if (proteger && password != null && password.length > 0) {
            SecretKeySpec secretKey = derivarLlavePBKDF2(password, UNIVERSAL_SALT, ITER_MODERN);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(keyBytes);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(iv);
            baos.write(encrypted);
            dataToSave = baos.toByteArray();
        }
        String encoded = Base64.getEncoder().encodeToString(dataToSave);
        Files.write(archivo.toPath(), encoded.getBytes(StandardCharsets.UTF_8));
    }

    public static PrivateKey cargarLlavePrivada(File archivo, char[] password) throws Exception {
        byte[] decoded = cargarYDescifrarBytes(archivo, password);
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new Exception("v4.9: Error en formato de Llave Privada. " + e.getMessage());
        } finally { Arrays.fill(decoded, (byte) 0); }
    }

    public static PublicKey cargarLlavePublica(File archivo, char[] password) throws Exception {
        byte[] decoded = cargarYDescifrarBytes(archivo, password);
        try {
            return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            try {
                byte[] raw = Files.readAllBytes(archivo.toPath());
                String text = new String(raw, StandardCharsets.UTF_8).trim();
                byte[] data = Base64.getDecoder().decode(text);
                return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(data));
            } catch (Exception ignored) {}
            throw new Exception("v4.9: Error en formato de Llave Publica. " + e.getMessage());
        } finally { Arrays.fill(decoded, (byte) 0); }
    }

    private static byte[] cargarYDescifrarBytes(File archivo, char[] password) throws Exception {
        byte[] raw = Files.readAllBytes(archivo.toPath());
        String text = new String(raw, StandardCharsets.UTF_8).trim();
        try { raw = Base64.getDecoder().decode(text); } catch (Exception e) {
            try { raw = Base64.getUrlDecoder().decode(text); } catch (Exception e2) {}
        }

        if (password != null && password.length > 0) {
            int[] iters = {ITER_LEGACY, ITER_MODERN};
            String[] salts = {UNIVERSAL_SALT, archivo.getName(), ""};
            for (int iter : iters) {
                for (String salt : salts) {
                    try { return ejecutarDescifradoAES_GCM(raw, password, salt, iter); } catch (Exception ignored) {}
                }
            }
            try { return ejecutarDescifradoLegacyV0(raw, password); } catch (Exception ignored) {}
        }
        return raw;
    }

    private static byte[] ejecutarDescifradoAES_GCM(byte[] data, char[] password, String salt, int iterations) throws Exception {
        if (data.length < 12) throw new Exception("Datos insuficientes");
        SecretKeySpec key = derivarLlavePBKDF2(password, salt, iterations);
        Cipher c = Cipher.getInstance(AES_GCM);
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(data, 0, 12)));
        return c.doFinal(Arrays.copyOfRange(data, 12, data.length));
    }

    private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(md.digest(new String(password).getBytes(StandardCharsets.UTF_8)), 16);
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return c.doFinal(data);
    }

    public static void cifrarContenidoHibrido(byte[] contenido, File destino, PublicKey publicKey) throws Exception {
        byte[] cifrado = cifrarBytesHibrido(contenido, publicKey);
        Files.write(destino.toPath(), cifrado);
    }

    public static byte[] descifrarBytesHibrido(byte[] datos, PrivateKey priv) throws Exception {
        String[] paddings = {"RSA/ECB/PKCS1Padding", "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"};
        
        if (datos.length > 2) {
            int keyLen = ((datos[0] & 0xFF) << 8) | (datos[1] & 0xFF);
            if (keyLen > 0 && keyLen < datos.length) {
                for (String pad : paddings) {
                    try {
                        Cipher rsa = Cipher.getInstance(pad);
                        rsa.init(Cipher.DECRYPT_MODE, priv);
                        byte[] aesKey = rsa.doFinal(Arrays.copyOfRange(datos, 2, 2 + keyLen));
                        
                        byte[] iv = Arrays.copyOfRange(datos, 2 + keyLen, 14 + keyLen);
                        byte[] content = Arrays.copyOfRange(datos, 14 + keyLen, datos.length);
                        
                        try {
                            Cipher aes = Cipher.getInstance(AES_GCM);
                            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
                            return aes.doFinal(content);
                        } catch (Exception e) {
                            int[] counters = {1, 2};
                            for (int counter : counters) {
                                try {
                                    Cipher ctr = Cipher.getInstance(AES_CTR);
                                    byte[] nonce = new byte[16];
                                    System.arraycopy(iv, 0, nonce, 0, 12);
                                    nonce[15] = (byte) counter; 
                                    ctr.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(nonce));
                                    byte[] recovered = ctr.doFinal(content);
                                    return recovered;
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        for (String pad : paddings) {
            try {
                Cipher rsa = Cipher.getInstance(pad);
                rsa.init(Cipher.DECRYPT_MODE, priv);
                return rsa.doFinal(datos);
            } catch (Exception ignored) {}
        }
        return datos;
    }

    public static byte[] cifrarBytesHibrido(byte[] cont, PublicKey pub) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES"); kg.init(256);
        SecretKey sk = kg.generateKey();
        byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
        Cipher aes = Cipher.getInstance(AES_GCM);
        aes.init(Cipher.ENCRYPT_MODE, sk, new GCMParameterSpec(128, iv));
        byte[] encCont = aes.doFinal(cont);
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pub);
        byte[] encKey = rsa.doFinal(sk.getEncoded());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((encKey.length >> 8) & 0xFF); baos.write(encKey.length & 0xFF);
        baos.write(encKey); baos.write(iv); baos.write(encCont);
        return baos.toByteArray();
    }

    private static byte[] toBytes(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] b = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
        Arrays.fill(bb.array(), (byte) 0);
        return b;
    }
    
    public static String getUniversalSalt() { return UNIVERSAL_SALT; }
    public static int getLegacyIterations() { return ITER_LEGACY; }
    public static byte[] descifrarHibrido(File f, PrivateKey p) throws Exception { return descifrarBytesHibrido(Files.readAllBytes(f.toPath()), p); }
}
