package com.mycompany.blackpassword.crypto;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de Cifrado v4.9 - MOTOR DE RESCATE FINAL.
 * Incluye soporte para huellas digitales de llaves públicas (RSA-Binding).
 */
public class RSALogica {
    
    private static String scrubPEM(String pem) {
        if (pem == null) return null;
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
                  .replace("-----END PUBLIC KEY-----", "")
                  .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                  .replace("-----END RSA PUBLIC KEY-----", "")
                  .replace("-----BEGIN PRIVATE KEY-----", "")
                  .replace("-----END PRIVATE KEY-----", "")
                  .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                  .replace("-----END RSA PRIVATE KEY-----", "")
                  .replaceAll("\\s", "");
    }

    private static byte[] transmutarPKCS1aPKCS8(byte[] pkcs1) {
        if (pkcs1 == null || pkcs1.length < 10 || pkcs1[0] != 0x30) return pkcs1; // No es ASN.1/PKCS1 plausible
        
        byte[] oidHeader = {(byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09, (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00};
        int bitStringLen = pkcs1.length + 1;
        byte[] bitStringHeader;
        if (bitStringLen <= 127) bitStringHeader = new byte[]{(byte) 0x03, (byte) bitStringLen, (byte) 0x00};
        else if (bitStringLen <= 255) bitStringHeader = new byte[]{(byte) 0x03, (byte) 0x81, (byte) bitStringLen, (byte) 0x00};
        else bitStringHeader = new byte[]{(byte) 0x03, (byte) 0x82, (byte) (bitStringLen >> 8), (byte) (bitStringLen & 0xFF), (byte) 0x00};
        
        int totalLen = oidHeader.length + bitStringHeader.length + pkcs1.length;
        byte[] seqHeader;
        if (totalLen <= 127) seqHeader = new byte[]{(byte) 0x30, (byte) totalLen};
        else if (totalLen <= 255) seqHeader = new byte[]{(byte) 0x30, (byte) 0x81, (byte) totalLen};
        else seqHeader = new byte[]{(byte) 0x30, (byte) 0x82, (byte) (totalLen >> 8), (byte) (totalLen & 0xFF)};
        
        ByteBuffer bb = ByteBuffer.allocate(seqHeader.length + oidHeader.length + bitStringHeader.length + pkcs1.length);
        bb.put(seqHeader); bb.put(oidHeader); bb.put(bitStringHeader); bb.put(pkcs1);
        return bb.array();
    }

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
    
    /** @deprecated Usar sales dinámicas por usuario. Solo para compatibilidad legacy. */
    @Deprecated
    private static final String LEGACY_SALT = "Blackpassword_Vault_2026";

    public static KeyPair generarLlaves() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA);
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Genera una llave simétrica CEK (AES-256 bits) de alta entropía.
     */
    public static byte[] generarCEK() {
        byte[] cek = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(cek);
        return cek;
    }

    /**
     * Blinda una CEK usando una KEK derivada de una frase de paso y una SAL dinámica.
     */
    public static byte[] wrapCEK(byte[] cek, char[] passphrase, byte[] salt) throws Exception {
        SecretKeySpec kek = derivarLlavePBKDF2(passphrase, salt, ITER_MODERN);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, kek, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(cek);
        
        ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
        bb.put(iv);
        bb.put(encrypted);
        return bb.array();
    }

    /**
     * Desblinda una CEK usando la KEK derivada de la passphrase y sal correctas.
     */
    public static byte[] unwrapCEK(byte[] wrappedCek, char[] passphrase, byte[] salt) throws Exception {
        if (wrappedCek.length < 12) throw new Exception("Paquete CEK corrupto o insuficiente.");
        SecretKeySpec kek = derivarLlavePBKDF2(passphrase, salt, ITER_MODERN);
        byte[] iv = Arrays.copyOfRange(wrappedCek, 0, 12);
        byte[] content = Arrays.copyOfRange(wrappedCek, 12, wrappedCek.length);
        
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, kek, new GCMParameterSpec(128, iv));
        return cipher.doFinal(content);
    }

    /**
     * Genera una huella digital SHA-256 de una llave pública.
     */
    public static String hashPublicKey(PublicKey pubKey) throws Exception {
        if (pubKey == null) return "null";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(pubKey.getEncoded());
        return bytesToHex(hash);
    }

    public static String decrypt(String base64Data, PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64Data);
        byte[] decryptedBytes = descifrarBytesHibrido(encryptedBytes, privateKey);
        try {
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(decryptedBytes, (byte) 0);
        }
    }

    /**
     * Hash SHA-256 con SAL DINÁMICA para evitar ataques de diccionario.
     * Usa SecureCharArray.toBytes para evitar crear Strings intermediarios.
     */
    public static String hashSHA256(char[] input, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        if (salt != null) md.update(salt);
        byte[] bytes = SecureCharArray.toBytes(input);
        try { 
            return Base64.getEncoder().encodeToString(md.digest(bytes)); 
        } finally { 
            SecureCharArray.clear(bytes); 
        }
    }
    
    @Deprecated
    public static String hashSHA256Legacy(char[] input, String saltStr) throws Exception {
        return hashSHA256(input, saltStr != null ? saltStr.getBytes(StandardCharsets.UTF_8) : null);
    }

    /**
     * Hash SHA-256 que retorna char[] (no String) para evitar memory leaks.
     * El llamador debe limpiar el resultado con SecureCharArray.clear().
     */
    public static char[] hashSHA256Chars(char[] input, byte[] salt) throws Exception {
        return SecureCharArray.hashSHA256(input, salt);
    }

    /**
     * Hash SHA-256 que retorna char[] con salt como String.
     */
    public static char[] hashSHA256Chars(char[] input, String saltStr) throws Exception {
        return hashSHA256Chars(input, saltStr != null ? saltStr.getBytes(StandardCharsets.UTF_8) : null);
    }

    /**
     * Hash SHA-256 convertido a Hex con soporte de SAL (Versión Nivel 11).
     * El resultado debe limpiarse con SecureCharArray.clear().
     */
    public static char[] hashToHex(char[] input, byte[] salt) throws Exception {
        if (input == null) return null;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        if (salt != null) md.update(salt);
        
        byte[] bytes = SecureCharArray.toBytes(input);
        byte[] hash = md.digest(bytes);
        SecureCharArray.clear(bytes);
        
        char[] hexChars = bytesToHexChars(hash);
        SecureCharArray.clear(hash);
        return hexChars;
    }

    private static char[] bytesToHexChars(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }

    /**
     * Versión simplificada para constantes (ej. nombres de colecciones).
     * No requiere manejo manual de buffers pero el resultado es inmutable.
     */
    public static String hashConstantToHex(String input) {
        try {
            char[] chars = input.toCharArray();
            char[] hashChars = hashToHex(chars, null);
            String result = new String(hashChars);
            SecureCharArray.clear(chars);
            SecureCharArray.clear(hashChars);
            return result;
        } catch (Exception e) {
            return "ERROR_HASH";
        }
    }

    @Deprecated
    public static String hashToHex(String input) throws Exception {
        throw new UnsupportedOperationException("El hashing de secretos via String es una vulnerabilidad de Nivel 11. Migrar a char[].");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    public static SecretKeySpec derivarLlavePBKDF2(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(skf.generateSecret(spec).getEncoded(), "AES");
        } finally { spec.clearPassword(); }
    }

    @Deprecated
    public static SecretKeySpec derivarLlavePBKDF2Legacy(char[] password, String saltStr, int iterations) throws Exception {
        return derivarLlavePBKDF2(password, saltStr != null ? saltStr.getBytes(StandardCharsets.UTF_8) : null, iterations);
    }

    @Deprecated
    public static byte[] wrapCEKLegacy(byte[] cek, char[] passphrase, String saltStr) throws Exception {
        return wrapCEK(cek, passphrase, saltStr != null ? saltStr.getBytes(StandardCharsets.UTF_8) : null);
    }

    @Deprecated
    public static byte[] unwrapCEKLegacy(byte[] wrappedCek, char[] passphrase, String saltStr) throws Exception {
        return unwrapCEK(wrappedCek, passphrase, saltStr != null ? saltStr.getBytes(StandardCharsets.UTF_8) : null);
    }
    
    public static byte[] generarSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static void guardarLlave(File archivo, byte[] keyBytes, boolean proteger, char[] password, byte[] salt) throws Exception {
        byte[] dataToSave = keyBytes;
        if (proteger && password != null && password.length > 0) {
            SecretKeySpec secretKey = derivarLlavePBKDF2(password, salt, ITER_MODERN);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(keyBytes);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(iv);
                baos.write(encrypted);
                dataToSave = baos.toByteArray();
            } finally {
                Arrays.fill(encrypted, (byte) 0);
            }
        }
        String encoded = Base64.getEncoder().encodeToString(dataToSave);
        Files.write(archivo.toPath(), encoded.getBytes(StandardCharsets.UTF_8));
    }

    public static PrivateKey cargarLlavePrivada(File archivo, char[] password) throws Exception {
        return cargarLlavePrivada(archivo, password, null);
    }
    
    public static PrivateKey cargarLlavePrivada(File archivo, char[] password, byte[] salt) throws Exception {
        byte[] decoded = cargarYDescifrarBytes(archivo, password, salt);
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            try {
                // Intento de rescate: Scrub PEM si el descifrado devolvió texto plano accidentalmente
                String text = scrubPEM(new String(decoded, StandardCharsets.UTF_8));
                byte[] cleaned = Base64.getDecoder().decode(text);
                return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(cleaned));
            } catch (Exception ex) {
                throw new Exception("v4.9.2: Error en formato de Llave Privada. " + e.getMessage());
            }
        } finally { Arrays.fill(decoded, (byte) 0); }
    }

    public static PublicKey cargarLlavePublica(File archivo, char[] password) throws Exception {
        return cargarLlavePublica(archivo, password, null);
    }
    
    public static PublicKey cargarLlavePublica(File archivo, char[] password, byte[] salt) throws Exception {
        byte[] decoded = cargarYDescifrarBytes(archivo, password, salt);
        try {
            // Intento 1: X.509 estándar
            try {
                return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(decoded));
            } catch (Exception e) {
                // Intento 2: Saneado de PEM y re-decodificación
                String text = scrubPEM(new String(decoded, StandardCharsets.UTF_8));
                byte[] cleaned = Base64.getDecoder().decode(text);
                try {
                    return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(cleaned));
                } catch (Exception e2) {
                    // Intento 3: Transmutación PKCS#1 a PKCS#8
                    byte[] pkcs8 = transmutarPKCS1aPKCS8(cleaned);
                    return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(pkcs8));
                }
            }
        } catch (Exception e) {
            throw new Exception("v4.9.2: Error en formato de Llave Publica. " + e.getMessage());
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    public static PrivateKey cargarLlavePrivadaDesdeBytes(byte[] fileData, String fileName, char[] password, byte[] saltInyectado) throws Exception {
        byte[] decoded = descifrarBytesInterno(fileData, fileName, password, saltInyectado);
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            try {
                String text = scrubPEM(new String(decoded, StandardCharsets.UTF_8));
                byte[] cleaned = Base64.getDecoder().decode(text);
                return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(cleaned));
            } catch (Exception ex) {
                throw new Exception("v4.9.2: Error en formato de Llave Privada. " + e.getMessage());
            }
        } finally { Arrays.fill(decoded, (byte) 0); }
    }

    public static PublicKey cargarLlavePublicaDesdeBytes(byte[] fileData, String fileName, char[] password, byte[] saltInyectado) throws Exception {
        byte[] decoded = descifrarBytesInterno(fileData, fileName, password, saltInyectado);
        try {
            try {
                return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(decoded));
            } catch (Exception e) {
                String text = scrubPEM(new String(decoded, StandardCharsets.UTF_8));
                byte[] cleaned = Base64.getDecoder().decode(text);
                try {
                    return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(cleaned));
                } catch (Exception e2) {
                    byte[] pkcs8 = transmutarPKCS1aPKCS8(cleaned);
                    return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(pkcs8));
                }
            }
        } catch (Exception e) {
            throw new Exception("v4.9.2: Error en formato de Llave Publica. " + e.getMessage());
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    private static byte[] cargarYDescifrarBytes(File archivo, char[] password, byte[] saltInyectado) throws Exception {
        byte[] raw = Files.readAllBytes(archivo.toPath());
        return descifrarBytesInterno(raw, archivo.getName(), password, saltInyectado);
    }

    private static byte[] descifrarBytesInterno(byte[] raw, String fileName, char[] password, byte[] saltInyectado) throws Exception {
        String text = new String(raw, StandardCharsets.UTF_8).trim();
        try { raw = Base64.getDecoder().decode(text); } catch (Exception e) {
            try { raw = Base64.getUrlDecoder().decode(text); } catch (Exception e2) {
                System.err.println("[RSALogica] Fallo en decodificación Base64 (estándar y URL): " + e2.getMessage());
            }
        }

        if (password != null && password.length > 0) {
            int[] iters = {ITER_LEGACY, ITER_MODERN};
            List<byte[]> candidateSalts = new ArrayList<>();
            if (saltInyectado != null) candidateSalts.add(saltInyectado);
            candidateSalts.add(fileName.getBytes(StandardCharsets.UTF_8));
            candidateSalts.add(LEGACY_SALT.getBytes(StandardCharsets.UTF_8));
            candidateSalts.add("".getBytes(StandardCharsets.UTF_8));

            for (int iter : iters) {
                for (byte[] salt : candidateSalts) {
                    try { return ejecutarDescifradoAES_GCM(raw, password, salt, iter); } catch (Exception e) {}
                }
            }
            throw new java.security.GeneralSecurityException("Error de descifrado: Credenciales incorrectas o archivo incompatible. El modo Legacy V0 (ECB/MD5) ha sido deshabilitado.");
        }
        return raw;
    }

    private static byte[] ejecutarDescifradoAES_GCM(byte[] data, char[] password, byte[] salt, int iterations) throws Exception {
        if (data.length < 12) throw new Exception("Datos insuficientes");
        SecretKeySpec key = derivarLlavePBKDF2(password, salt, iterations);
        Cipher c = Cipher.getInstance(AES_GCM);
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(data, 0, 12)));
        return c.doFinal(Arrays.copyOfRange(data, 12, data.length));
    }

    @Deprecated
    private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password, String hashAlgo) throws Exception {
        throw new UnsupportedOperationException("El descifrado Legacy V0 (AES-ECB sin salt) ha sido descontinuado por motivos de seguridad.");
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
                        } catch (Exception e1) {
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
                                 } catch (Exception e2) { // Renamed from e to e2
                                    System.err.println("[RSALogica] Fallo descifrado AES-CTR (counter=" + counter + "): " + e2.getMessage());
                                }
                            }
                        }
                    } catch (Exception e3) { // Renamed from e to e3
                        System.err.println("[RSALogica] Fallo descifrado híbrido (padding=" + pad + "): " + e3.getMessage());
                    }
                }
            }
        }

        for (String pad : paddings) {
            try {
                Cipher rsa = Cipher.getInstance(pad);
                rsa.init(Cipher.DECRYPT_MODE, priv);
                return rsa.doFinal(datos);
            } catch (Exception e) {
                System.err.println("[RSALogica] Fallo descifrado RSA puro (padding=" + pad + "): " + e.getMessage());
            }
        }
        throw new java.security.GeneralSecurityException("Fallo de descifrado híbrido: datos corruptos, alterados o llave incorrecta.");
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
    
    public static String getUniversalSalt() { return LEGACY_SALT; }
    public static int getLegacyIterations() { return ITER_LEGACY; }
    public static byte[] descifrarHibrido(File f, PrivateKey p) throws Exception { return descifrarBytesHibrido(Files.readAllBytes(f.toPath()), p); }
}
