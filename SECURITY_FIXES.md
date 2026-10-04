# Corrección de Vulnerabilidades de Seguridad

## VULNERABILIDAD 4: masterPassword en Memoria Estática

### Ubicación: SecuritySession.java:98-116

### Problema Actual

```java
public static void setMasterPassword(char[] pwd) {
    // ❌ Convierte a byte[] y pasa a HSM
    // Pero no valida que el HSM realmente funcione
    // Si falla, el password sigue en memoria
    byte[] encoded = java.nio.charset.StandardCharsets.UTF_8.encode(
        java.nio.CharBuffer.wrap(pwd)).array();
    com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance()
        .sealSessionKey(HSM_ALIAS, encoded);
    java.util.Arrays.fill(encoded, (byte) 0);
    java.util.Arrays.fill(pwd, (char) 0);
}

public static char[] unsealMasterPassword() throws Exception {
    byte[] unsealed = ...unsealSessionKey(HSM_ALIAS);
    String decoded = new String(unsealed, ...);  // ← NUEVO String en Pool
    return decoded.toCharArray();
}
```

### Corrección: SecureMasterPasswordManager

```java
package com.mycompany.blackpassword.services;

import com.mycompany.blackpassword.services.hsm.KeyAnchorService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Gestor seguro de contraseña maestra.
 * Resuelve CWE-316: Cleartext Storage of Sensitive Information
 */
public class MasterPasswordManager {
    
    private static final String HSM_ALIAS = "BP_MASTER_KEY_V2";
    private static volatile boolean isInitialized = false;
    private static final Object lock = new Object();
    
    private MasterPasswordManager() {}
    
    /**
     * Sella la contraseña maestra en HSM de forma segura.
     * @param password char[] con la contraseña (se limpia al salir)
     * @throws Exception si el HSM no está disponible
     */
    public static void seal(char[] password) throws Exception {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        KeyAnchorService hsm = KeyAnchorService.getInstance();
        
        if (!hsm.isHardwareSecured()) {
            throw new SecurityException(
                "Hardware security not available. " +
                "Cannot store master password securely."
            );
        }
        
        try {
            synchronized (lock) {
                hsm.destroyAll();
                
                // Convertir char[] a byte[] de forma segura
                byte[] encoded = encodePassword(password);
                
                // Limpiar inmediatamente después de la conversión
                Arrays.fill(password, (char) 0);
                
                // Sellar en HSM
                hsm.sealSessionKey(HSM_ALIAS, encoded);
                
                // Limpiar copia en RAM
                Arrays.fill(encoded, (byte) 0);
                
                isInitialized = true;
            }
        } catch (Exception e) {
            isInitialized = false;
            throw new SecurityException("Failed to seal master password: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recupera la contraseña maestra desde HSM.
     * @return char[] con la contraseña (el llamador debe limpiarla)
     * @throws Exception si no hay contraseña almacenada o HSM falla
     */
    public static char[] unseal() throws Exception {
        if (!isInitialized) {
            throw new SecurityException("No master password has been sealed");
        }
        
        KeyAnchorService hsm = KeyAnchorService.getInstance();
        byte[] unsealed = hsm.unsealSessionKey(HSM_ALIAS);
        
        try {
            return decodePassword(unsealed);
        } finally {
            // Limpiar datos sensibles de RAM inmediatamente
            if (unsealed != null) {
                Arrays.fill(unsealed, (byte) 0);
            }
        }
    }
    
    /**
     * Destruye la contraseña maestra de forma segura.
     */
    public static void destroy() {
        synchronized (lock) {
            try {
                KeyAnchorService.getInstance().destroyAll();
            } finally {
                isInitialized = false;
            }
        }
    }
    
    public static boolean isSealed() {
        return isInitialized;
    }
    
    /**
     * Codifica password a bytes sin crear Strings intermedios.
     */
    private static byte[] encodePassword(char[] password) {
        CharBuffer cb = CharBuffer.wrap(password);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] result = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
        Arrays.fill(bb.array(), (byte) 0);
        return result;
    }
    
    /**
     * Decodifica bytes a char[] sin crear Strings.
     */
    private static char[] decodePassword(byte[] encoded) {
        CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded));
        char[] result = Arrays.copyOfRange(cb.array(), cb.position(), cb.limit());
        Arrays.fill(cb.array(), '\u0000');
        return result;
    }
}
```

---

## VULNERABILIDAD 5: Conversiones char[] a String Innecesarias

### Ubicaciones Críticas

| Archivo | Línea | Código Problemático |
|---------|-------|-------------------|
| `RSALogica.java` | 281 | `new String(password).getBytes()` |
| `VaultManager.java` | 91 | `new String(password)` |
| `ConexionServidor.java` | 88, 128 | `new String(txtPassword.getPassword())` |
| `SecuritySession.java` | 123 | `new String(unsealed, ...)` |

### Corrección: RSALogica - Eliminar Legacy V0

```java
// REEMPLAZAR ejecutarDescifradoLegacyV0 COMPLETAMENTE
// Esta función es inherentemente insegura y debe ser eliminada

/**
 * @deprecated Esta función ha sido eliminada por razones de seguridad.
 * AES-128 ECB es vulnerable a ataques de patrón y padding oracle.
 * Use wrapCEK/unwrapCEK con sales dinámicas.
 */
@Deprecated
private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) {
    throw new UnsupportedOperationException(
        "Legacy V0 decryption is no longer supported due to security vulnerabilities. " +
        "Migrate your encrypted files to the current format."
    );
}
```

### Corrección: VaultManager.java

```java
// ANTES (línea 91):
String passStr = new String(password); // ❌ String en Pool
String raw = "s:" + service + "|u:" + username + "|p:" + passStr;

// DESPUÉS:
String raw = "s:" + service + "|u:" + username + "|p:" + new String(password, 0, Math.min(password.length, 8)) + "***";

// MEJOR: Evitar completamente el String
private static String passwordToMaskedString(char[] password) {
    if (password == null || password.length == 0) return "";
    int showChars = Math.min(4, password.length);
    StringBuilder sb = new StringBuilder(showChars);
    for (int i = 0; i < showChars; i++) sb.append(password[i]);
    return sb + "***";
}

// Uso:
String raw = "s:" + service + "|u:" + username + "|p:" + passwordToMaskedString(password);
```

### Corrección: ConexionServidor.java

```java
// ANTES (líneas 88 y 128):
String pass = new String(txtPassword.getPassword());

// DESPUÉS: Pasar char[] directamente al servicio
// Opción 1: Modificar ServerService para aceptar char[]
private void conectar() {
    char[] passwordChars = txtPassword.getPassword();
    String user = txtUsuario.getText();
    
    // Pasar directamente, NO convertir a String
    new SwingWorker<Boolean, Void>() {
        @Override
        protected Boolean doInBackground() throws Exception {
            ServerService service = ServerService.getInstance();
            return service.login(user, passwordChars);  // Cambiar firma
        }
        // ...
    }.execute();
    
    // Limpiar después de usar
    Arrays.fill(passwordChars, (char) 0);
}

// Opción 2: Hashear localmente y limpiar inmediatamente
private void conectar() {
    String user = txtUsuario.getText();
    char[] passwordChars = txtPassword.getPassword();
    
    try {
        String passHash = hashPasswordLocally(passwordChars);
        // Enviar hash en lugar de password
        ServerService service = ServerService.getInstance();
        service.loginHashed(user, passHash);
    } finally {
        Arrays.fill(passwordChars, (char) 0);
    }
}

private String hashPasswordLocally(char[] password) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        CharBuffer cb = CharBuffer.wrap(password);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] hash = md.digest(Arrays.copyOfRange(bb.array(), bb.position(), bb.limit()));
        Arrays.fill(bb.array(), (byte) 0);
        return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
        return null;
    }
}
```

### Corrección: SecuritySession.java - Método unsealMasterPassword

```java
// ANTES (línea 121-126):
public static char[] unsealMasterPassword() throws Exception {
    byte[] unsealed = KeyAnchorService.getInstance().unsealSessionKey(HSM_ALIAS);
    String decoded = new String(unsealed, StandardCharsets.UTF_8);  // ❌ String leak
    Arrays.fill(unsealed, (byte) 0);
    return decoded.toCharArray();
}

// DESPUÉS: Usar MasterPasswordManager
public static char[] unsealMasterPassword() throws Exception {
    return MasterPasswordManager.unseal();
}

// Y eliminar setMasterPassword, usar:
public static void setMasterPassword(char[] pwd) throws Exception {
    MasterPasswordManager.seal(pwd);
}
```

---

## Resumen de Cambios Requeridos

| Archivo | Cambio | Prioridad |
|---------|--------|-----------|
| `SecuritySession.java` | Reemplazar por `MasterPasswordManager` | 🔴 CRÍTICA |
| `RSALogica.java` | Eliminar `ejecutarDescifradoLegacyV0` | 🟠 ALTA |
| `VaultManager.java` | Eliminar `new String(password)` | 🟠 ALTA |
| `ConexionServidor.java` | No convertir a String | 🟠 ALTA |
| `ServerService.java` | Modificar firma para `char[]` | 🟡 MEDIA |

### Notas Importantes

1. **Nunca usar `new String(char[])`** para contraseñas
2. **Siempre limpiar** arrays con `Arrays.fill(array, (char) 0)`
3. **El HSM debe ser obligatorio**, no opcional
4. **Eliminar soporte legacy** que usa AES-128 ECB
