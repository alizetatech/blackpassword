package com.mycompany.blackpassword.services.hsm;

/**
 * Interface para anclajes de seguridad de hardware (HSM / Secure Store).
 * Define el contrato para el manejo de llaves efímeras por sesión.
 * v11.0: Soporte para ProtectedPrivateKey (Doble Blindaje).
 */
public interface KeyAnchorProvider {
    
    /**
     * Sella un secreto en el hardware o almacén seguro del sistema operativo.
     * @param alias Identificador único de la llave.
     * @param secret Material sensible a proteger.
     * @param protector Parámetro de protección (PIN de sesión) para bloquear la entrada.
     * @throws Exception Si falla la comunicación con el hardware.
     */
    void seal(String alias, byte[] secret, char[] protector) throws Exception;
    
    /**
     * Recupera el secreto del hardware para su uso inmediato.
     * @param alias Identificador único de la llave.
     * @param protector Parámetro de protección requerido para desbloquear la entrada.
     * @return El material sensible recuperado.
     * @throws Exception Si la llave no existe o el hardware deniega el acceso.
     */
    byte[] unseal(String alias, char[] protector) throws Exception;
    
    /**
     * Elimina físicamente la llave del hardware / almacén seguro.
     * @param alias Identificador único de la llave.
     */
    void destroy(String alias);
    
    /**
     * Verifica si el hardware de seguridad está disponible en este sistema.
     * @return true si el anclaje es funcional.
     */
    boolean isAvailable();
}
