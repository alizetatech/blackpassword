package com.mycompany.blackpassword.services.hsm;

import java.util.HashMap;
import java.util.Map;

/**
 * Orquestador Multi-Plataforma para Anclajes de Seguridad (HSM).
 * Gestiona el ciclo de vida de los secretos en el hardware del sistema operativo.
 */
public class KeyAnchorService {

    private static KeyAnchorService instance;
    private KeyAnchorProvider provider;
    private final Map<String, String> activeAliases = new HashMap<>();

    private KeyAnchorService() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            provider = new WindowsKeyAnchor();
        } else if (os.contains("mac")) {
            // Implementación macOS (Keychain) - vía JNA
            provider = new MacKeyAnchor();
        } else {
            // Implementación Linux (Secret Service) - vía JNA
            provider = new LinuxKeyAnchor();
        }
        
        if (provider != null && !provider.isAvailable()) {
            System.err.println("[KeyAnchorService] ⚠️ Soporte de hardware no disponible en este entorno.");
            provider = null;
        }
    }

    public static synchronized KeyAnchorService getInstance() {
        if (instance == null) instance = new KeyAnchorService();
        return instance;
    }

    /**
     * Sella el material sensible de forma segura en el hardware del sistema.
     */
    public void sealSessionKey(String alias, byte[] masterKey, char[] protector) throws Exception {
        if (provider == null) throw new Exception("No existe un anclaje de seguridad activo para esta plataforma.");
        provider.seal(alias, masterKey, protector);
        activeAliases.put(alias, "PROCESSED");
    }

    /**
     * Recupera el material sensible liberándolo momentáneamente del hardware.
     */
    public byte[] unsealSessionKey(String alias, char[] protector) throws Exception {
        if (provider == null) throw new Exception("Anclaje de seguridad no disponible.");
        return provider.unseal(alias, protector);
    }

    /**
     * Destruye todas las llaves físicas creadas durante esta sesión.
     */
    public void destroyAll() {
        if (provider != null) {
            for (String alias : activeAliases.keySet()) {
                provider.destroy(alias);
            }
            activeAliases.clear();
        }
    }

    public boolean isHardwareSecured() {
        return provider != null && provider.isAvailable();
    }
}
