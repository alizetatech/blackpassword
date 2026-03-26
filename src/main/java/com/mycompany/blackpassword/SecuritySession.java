package com.mycompany.blackpassword;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Almacén de Seguridad en Memoria v2.0.
 * Guarda las llaves, el PIN de sesión y el hash del PIN de usuario.
 * Implementa purga de memoria para máxima protección.
 */
public class SecuritySession {
    
    private static PublicKey currentPublicKey;
    private static PrivateKey currentPrivateKey;
    private static String sessionPinHash; // Hash del PIN de la sesión de llaves
    private static String userPinHash;    // Hash del PIN de seguridad del usuario

    /**
     * Guarda las llaves y el PIN de la sesión de llaves.
     * @param pub Llave pública
     * @param priv Llave privada
     * @param sessionPin PIN para proteger las llaves en esta sesión (se purga tras el uso)
     */
    public static void setKeySession(PublicKey pub, PrivateKey priv, char[] sessionPin) {
        currentPublicKey = pub;
        currentPrivateKey = priv;
        try {
            sessionPinHash = RSALogica.hashSHA256(sessionPin);
        } catch (Exception e) {
            sessionPinHash = null;
        } finally {
            if (sessionPin != null) Arrays.fill(sessionPin, (char) 0);
        }
        System.out.println("[SecuritySession] Identidad RSA y PIN de sesión cargados en el Almacén Seguro.");
    }

    /**
     * Guarda el hash del PIN de seguridad del usuario en la sesión.
     * @param pinHash El hash del PIN del usuario obtenido de Firebase.
     */
    public static void setUserPinHash(String pinHash) {
        userPinHash = pinHash;
        System.out.println("[SecuritySession] Hash de PIN de usuario cargado en el Almacén Seguro.");
    }

    public static PublicKey getPublicKey() {
        return currentPublicKey;
    }

    public static PrivateKey getPrivateKey() {
        return currentPrivateKey;
    }
    
    /**
     * Verifica el PIN de la sesión de llaves.
     */
    public static boolean verifySessionPin(char[] inputPin) {
        try {
            String inputHash = RSALogica.hashSHA256(inputPin);
            return inputHash.equals(sessionPinHash);
        } catch (Exception e) {
            return false;
        } finally {
            if (inputPin != null) Arrays.fill(inputPin, (char) 0);
        }
    }

    /**
     * Verifica el PIN de seguridad del usuario.
     * @param inputPin El PIN introducido por el usuario.
     * @return true si el PIN es correcto, false en caso contrario.
     */
    public static boolean verifyUserPin(char[] inputPin) {
        if (userPinHash == null) return false;
        try {
            String inputHash = RSALogica.hashSHA256(inputPin);
            return inputHash.equals(userPinHash);
        } catch (Exception e) {
            return false;
        } finally {
            if (inputPin != null) Arrays.fill(inputPin, (char) 0);
        }
    }

    /**
     * Limpia la memoria (útil al cerrar sesión).
     */
    public static void clear() {
        currentPublicKey = null;
        currentPrivateKey = null;
        sessionPinHash = null;
        userPinHash = null;
        System.out.println("[SecuritySession] Almacén de memoria limpiado.");
    }

    public static boolean isSessionActive() {
        return currentPublicKey != null && currentPrivateKey != null && sessionPinHash != null;
    }
    
    public static String getSessionStatus() {
        return isSessionActive() ? "PROTEGIDO (Llaves en RAM)" : "VULNERABLE (Sin llaves)";
    }
}
