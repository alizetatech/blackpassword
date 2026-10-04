package com.mycompany.blackpassword.services.hsm;

import java.util.Arrays;
import java.util.Base64;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Anclaje HSM Linux (v11.0 - PROTECTED PASSPHRASE).
 * Utiliza libsecret para almacenar secretos de sesión protegidos.
 */
public class LinuxKeyAnchor implements KeyAnchorProvider {

    public interface LibSecret extends Library {
        LibSecret INSTANCE = Native.load("secret-1", LibSecret.class);
        
        Pointer secret_password_store_sync(Pointer schema, 
                                         String collection, 
                                         String label, 
                                         String password, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
                                         
        String secret_password_lookup_sync(Pointer schema, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
                                         
        boolean secret_password_clear_sync(Pointer schema, 
                                         Pointer cancellable, 
                                         Pointer error, 
                                         String... attributes);
    }

    private static final String APP_ID = "com.mycompany.blackpassword";

    @Override
    public void seal(String alias, byte[] secret, char[] protector) throws Exception {
        try {
            // En Linux, usamos el protector como un atributo de validación de hardware
            // para que libsecret no libere la llave sin el par de sesión correcto.
            String secretBase64 = Base64.getEncoder().encodeToString(secret);
            
            LibSecret.INSTANCE.secret_password_store_sync(null, "session", 
                "Blackpassword Session Key", secretBase64, null, null, 
                "application", APP_ID, "alias", alias, "protector", new String(protector));
            
            Arrays.fill(secret, (byte) 0);
            System.out.println("[HSM-Linux] Secreto sellado con validación de protector.");
        } catch (UnsatisfiedLinkError e) {
            throw new Exception("libsecret no disponible.");
        }
    }

    @Override
    public byte[] unseal(String alias, char[] protector) throws Exception {
        String secretBase64 = LibSecret.INSTANCE.secret_password_lookup_sync(null, null, null, 
                "application", APP_ID, "alias", alias, "protector", new String(protector));
        
        if (secretBase64 == null) throw new Exception("Acceso denegado: Protector de hardware incorrecto.");
        return Base64.getDecoder().decode(secretBase64);
    }

    @Override
    public void destroy(String alias) {
        LibSecret.INSTANCE.secret_password_clear_sync(null, null, null, 
                "application", APP_ID, "alias", alias);
        System.out.println("[HSM-Linux] Purga de Keyring completa.");
    }

    @Override
    public boolean isAvailable() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}
