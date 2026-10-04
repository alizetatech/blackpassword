package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.crypto.RSALogica;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;

/**
 * Almacén de Seguridad en Memoria v3.2 (Nivel 11 - ProtectedPrivateKey).
 * Centraliza la validación de identidad y el anclaje HSM doblemente blindado.
 */
public class SecuritySession {
    
    private static PublicKey currentPublicKey;
    private static PrivateKey currentPrivateKey;
    private static String sessionPinHash; 
    private static String userPinHash;    
    private static String localUidHash;   
    private static Map<String, Object> userClaims; 
    private static String typingProfile;  
    private static String currentLocation; 
    private static final String HSM_ALIAS = "BP_SESSION_KEY_" + java.util.UUID.randomUUID().toString();
    private static byte[] userSalt;

    public static boolean isIdentityValid(String email) {
        if (userClaims == null || userClaims.isEmpty()) return false;
        try {
            String status = (String) userClaims.get("account_status");
            if (!"ACTIVE".equals(status)) return false;

            String localEmailHash = RSALogica.hashConstantToHex(email);
            String claimEmailHash = (String) userClaims.get("email_hash");
            if (claimEmailHash == null || !claimEmailHash.equalsIgnoreCase(localEmailHash)) return false;

            String claimUidHash = (String) userClaims.get("uid_hash");
            if (claimUidHash == null || localUidHash == null || !claimUidHash.equalsIgnoreCase(localUidHash)) return false;

            if (currentPublicKey != null) {
                String currentKeyHash = RSALogica.hashPublicKey(currentPublicKey);
                String claimKeyHash = (String) userClaims.get("pubkey_hash");
                if (claimKeyHash == null || !currentKeyHash.equalsIgnoreCase(claimKeyHash)) return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }

    public static void setKeySession(PublicKey pub, PrivateKey priv, char[] sessionPin) {
        currentPublicKey = pub;
        currentPrivateKey = priv;
        if (sessionPin != null && sessionPin.length > 0) {
            try {
                sessionPinHash = RSALogica.hashSHA256(sessionPin, RSALogica.getUniversalSalt().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) { 
                sessionPinHash = null; 
            } finally { 
                Arrays.fill(sessionPin, (char) 0); 
            }
        } else {
            sessionPinHash = null;
        }
    }

    // --- ACCESSORS ---
    public static void setUserPinHash(String pinHash) { userPinHash = pinHash; }
    public static void setUserSalt(byte[] salt) { userSalt = salt; }
    public static byte[] getUserSalt() { return userSalt; }
    public static void setLocalUidHash(String uidHash) { localUidHash = uidHash; }
    public static String getLocalUidHash() { return localUidHash; }
    public static void setUserClaims(Map<String, Object> claims) { userClaims = claims; }
    public static Map<String, Object> getUserClaims() { return userClaims; }
    public static void setTypingProfile(String profile) { typingProfile = profile; }
    public static String getTypingProfile() { return typingProfile; }
    public static void setCurrentLocation(String location) { currentLocation = location; }
    public static String getCurrentLocation() { return currentLocation != null ? currentLocation : "Ubicación no detectada"; }
    public static PublicKey getPublicKey() { return currentPublicKey; }
    public static PrivateKey getPrivateKey() { return currentPrivateKey; }

    // --- PROTECTED HSM FLOW ---
    
    public static void setMasterPassword(char[] pwd) {
        if (pwd == null) {
            com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance().destroyAll();
            return;
        }
        if (sessionPinHash == null) {
            System.err.println("[SecuritySession] Falla: PIN de sesión ausente.");
            return;
        }
        try {
            byte[] encoded = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(pwd)).array();
            char[] protector = sessionPinHash.toCharArray();
            
            com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance().sealSessionKey(HSM_ALIAS, encoded, protector);
            
            Arrays.fill(encoded, (byte) 0);
            Arrays.fill(pwd, (char) 0);
            Arrays.fill(protector, (char) 0);
        } catch (Exception e) {
            System.err.println("[SecuritySession] Error HSM Seal: " + e.getMessage());
        }
    }

    public static char[] unsealMasterPassword() throws Exception {
        if (sessionPinHash == null) throw new Exception("Pérdida de integridad de sesión (PIN)");
        
        char[] protector = sessionPinHash.toCharArray();
        byte[] unsealed = com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance().unsealSessionKey(HSM_ALIAS, protector);
        
        String decoded = new String(unsealed, java.nio.charset.StandardCharsets.UTF_8);
        Arrays.fill(unsealed, (byte) 0);
        Arrays.fill(protector, (char) 0);
        return decoded.toCharArray();
    }

    public static boolean verifyUserPin(char[] inputPin) {
        if (userPinHash == null || inputPin == null || inputPin.length == 0) return false;
        try {
            byte[] salt = (userSalt != null) ? userSalt : RSALogica.getUniversalSalt().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            // Si el PIN almacenado es Hexadecimal (Legacy, 64 chars)
            if (userPinHash.length() == 64) {
                char[] hexHash = RSALogica.hashToHex(inputPin, salt);
                byte[] computed = new String(hexHash).toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] stored = userPinHash.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                boolean match = java.security.MessageDigest.isEqual(computed, stored);
                com.mycompany.blackpassword.crypto.SecureCharArray.clear(hexHash);
                return match;
            }
            
            // Si el PIN almacenado es Base64 (Moderno, ~44 chars)
            String hash = RSALogica.hashSHA256(inputPin, salt);
            byte[] computed = hash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] stored = userPinHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return java.security.MessageDigest.isEqual(computed, stored);
        } catch (Exception e) { 
            return false; 
        } finally { 
            if (inputPin != null) Arrays.fill(inputPin, (char) 0); 
        }
    }

    public static void clear() {
        currentPublicKey = null; currentPrivateKey = null; sessionPinHash = null;
        userPinHash = null; userClaims = null; localUidHash = null;
        typingProfile = null; currentLocation = null; userSalt = null;
        com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance().destroyAll();
    }

    public static boolean isSessionActive() {
        return currentPublicKey != null && currentPrivateKey != null;
    }

    public static boolean isFullSessionActive() {
        return currentPublicKey != null && currentPrivateKey != null && sessionPinHash != null;
    }
}
