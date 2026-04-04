package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.crypto.RSALogica;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;

/**
 * Almacén de Seguridad en Memoria v3.1.
 * Centraliza la validación de identidad cruzada, RSA-Binding, Typing DNA y Ubicación.
 * Acceso total sin restricciones de suscripción.
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
    private static char[] masterPassword; 

    /**
     * Valida que la identidad en el token coincida con la identidad local, de la DB y la llave RSA.
     */
    public static boolean isIdentityValid(String email) {
        if (userClaims == null || userClaims.isEmpty()) {
            System.err.println("[SecuritySession] Falla: No hay claims de usuario.");
            return false;
        }
        
        try {
            String status = (String) userClaims.get("account_status");
            if (!"ACTIVE".equals(status)) {
                System.err.println("[SecuritySession] Falla: Cuenta no activa (" + status + ")");
                return false;
            }

            String localEmailHash = RSALogica.hashToHex(email);
            String claimEmailHash = (String) userClaims.get("email_hash");
            if (claimEmailHash == null || !claimEmailHash.equals(localEmailHash)) {
                System.err.println("[SecuritySession] Falla: Hash de Email no coincide.");
                return false;
            }

            String claimUidHash = (String) userClaims.get("uid_hash");
            if (claimUidHash == null || localUidHash == null || !claimUidHash.equals(localUidHash)) {
                System.err.println("[SecuritySession] Falla: Hash de UID no coincide.");
                return false;
            }

            String claimPubKeyHash = (String) userClaims.get("pubkey_hash");
            if (claimPubKeyHash != null && currentPublicKey != null) {
                String currentKeyHash = RSALogica.hashPublicKey(currentPublicKey);
                if (!claimPubKeyHash.equals(currentKeyHash)) {
                    System.err.println("[SecuritySession] Falla: RSA_BINDING_VIOLATION (Token: " + claimPubKeyHash + " vs Local: " + currentKeyHash + ")");
                    IntrusionService.reportFailure(email, "RSA_BINDING_VIOLATION");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[SecuritySession] Error en validación: " + e.getMessage());
            return false;
        }
    }

    public static void setKeySession(PublicKey pub, PrivateKey priv, char[] sessionPin) {
        currentPublicKey = pub;
        currentPrivateKey = priv;
        try {
            sessionPinHash = RSALogica.hashSHA256(sessionPin, RSALogica.getUniversalSalt());
        } catch (Exception e) {
            sessionPinHash = null;
        } finally {
            if (sessionPin != null) Arrays.fill(sessionPin, (char) 0);
        }
    }

    public static void setUserPinHash(String pinHash) { userPinHash = pinHash; }
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
    
    public static void setMasterPassword(char[] pwd) {
        if (masterPassword != null) Arrays.fill(masterPassword, (char)0);
        masterPassword = pwd != null ? Arrays.copyOf(pwd, pwd.length) : null;
    }
    
    public static char[] getMasterPassword() { return masterPassword; }
    
    public static boolean verifySessionPin(char[] inputPin) {
        try {
            return RSALogica.hashSHA256(inputPin, RSALogica.getUniversalSalt()).equals(sessionPinHash);
        } catch (Exception e) { return false; }
        finally { if (inputPin != null) Arrays.fill(inputPin, (char) 0); }
    }

    public static boolean verifyUserPin(char[] inputPin) {
        if (userPinHash == null) return false;
        try {
            // Soportar legacy (null) y nuevo (universal)
            String hashConSal = RSALogica.hashSHA256(inputPin, RSALogica.getUniversalSalt());
            String hashSinSal = RSALogica.hashSHA256(inputPin, null);
            return hashConSal.equals(userPinHash) || hashSinSal.equals(userPinHash);
        } catch (Exception e) { return false; }
        finally { if (inputPin != null) Arrays.fill(inputPin, (char) 0); }
    }

    public static void clear() {
        currentPublicKey = null; currentPrivateKey = null; sessionPinHash = null;
        userPinHash = null; userClaims = null; localUidHash = null;
        typingProfile = null; currentLocation = null;
        if (masterPassword != null) {
            Arrays.fill(masterPassword, (char) 0);
            masterPassword = null;
        }
    }

    public static boolean isSessionActive() {
        return currentPublicKey != null && currentPrivateKey != null && sessionPinHash != null;
    }
}
