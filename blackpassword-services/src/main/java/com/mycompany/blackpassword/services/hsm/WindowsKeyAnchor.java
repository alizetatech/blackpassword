package com.mycompany.blackpassword.services.hsm;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Anclaje de Llaves nativo para Windows (Windows Credential Manager).
 * v4.0: Bypass directo a Advapi32 para máxima estabilidad sin dependencias externas.
 */
public class WindowsKeyAnchor implements KeyAnchorProvider {

    private static final String PROVIDER = "SunMSCAPI";
    private static final String KEYSTORE_TYPE = "Windows-My";
    private static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";
    private static final String CRED_PREFIX = "Blackpassword_HSM_";

    private interface DirectAdvapi32 extends StdCallLibrary {
        DirectAdvapi32 INSTANCE = Native.load("Advapi32", DirectAdvapi32.class);
        boolean CredWriteW(Credential credential, int flags);
        boolean CredReadW(String target, int type, int flags, PointerByReference pCred);
        boolean CredDeleteW(String target, int type, int flags);
        void CredFree(Pointer p);
        
        @SuppressWarnings("unused")
        class Credential extends Structure {
            public Credential() {}
            public Credential(Pointer p) { super(p); }
            public int Flags;
            public int Type;
            public String TargetName;
            public String Comment;
            public long LastWritten;
            public int CredentialBlobSize;
            public Pointer CredentialBlob;
            public int Persist;
            public int AttributeCount;
            public Pointer Attributes;
            public String TargetAlias;
            public String UserName;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("Flags", "Type", "TargetName", "Comment", "LastWritten", "CredentialBlobSize", "CredentialBlob", "Persist", "AttributeCount", "Attributes", "TargetAlias", "UserName");
            }
        }
    }

    private static boolean jnaAvailable = false;
    
    static {
        try {
            DirectAdvapi32.INSTANCE.toString();
            jnaAvailable = true;
        } catch (Throwable e) {
            System.err.println("[HSM-Win] JNA nativo no disponible. HSM deshabilitado.");
        }
    }

    @Override
    public void seal(String alias, byte[] secret, char[] protector) throws Exception {
        if (!jnaAvailable) throw new Exception("HSM no disponible.");
        
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", PROVIDER);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic());
        byte[] encryptedSecret = cipher.doFinal(secret);

        saveToWinCred(CRED_PREFIX + alias, encryptedSecret);
        
        Arrays.fill(encryptedSecret, (byte) 0);
        Arrays.fill(secret, (byte) 0);
        System.out.println("[HSM-Win] Secreto sellado de forma nativa (DirectAdvapi32).");
    }

    @Override
    public byte[] unseal(String alias, char[] protector) throws Exception {
        if (!jnaAvailable) throw new Exception("HSM no disponible.");
        
        byte[] encryptedSecret = readFromWinCred(CRED_PREFIX + alias);
        if (encryptedSecret == null) throw new Exception("Secreto no disponible en Windows Credential Manager.");

        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        ks.load(null, null);

        KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(protector);
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, protParam);
        
        if (entry == null) throw new Exception("Acceso HSM denegado: Entrada protegida no encontrada.");
        PrivateKey priv = entry.getPrivateKey();

        Cipher cipher = Cipher.getInstance(ALGO_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priv);
        
        byte[] result = cipher.doFinal(encryptedSecret);
        Arrays.fill(encryptedSecret, (byte) 0);
        return result;
    }

    private void saveToWinCred(String targetName, byte[] blob) {
        try (Memory mem = new Memory(blob.length)) {
            DirectAdvapi32.Credential cred = new DirectAdvapi32.Credential();
            cred.TargetName = targetName;
            cred.Type = 1; // CRED_TYPE_GENERIC
            cred.Persist = 2; // CRED_PERSIST_LOCAL_MACHINE
            cred.UserName = "Blackpassword_Session";
            cred.CredentialBlobSize = blob.length;
            mem.write(0, blob, 0, blob.length);
            cred.CredentialBlob = mem;
            
            DirectAdvapi32.INSTANCE.CredWriteW(cred, 0);
        } catch (Exception e) {
            System.err.println("[HSM-Win] Error escritura nativa: " + e.getMessage());
        }
    }

    private byte[] readFromWinCred(String targetName) {
        PointerByReference pCred = new PointerByReference();
        if (DirectAdvapi32.INSTANCE.CredReadW(targetName, 1, 0, pCred)) {
            Pointer ptr = pCred.getValue();
            try {
                DirectAdvapi32.Credential cred = new DirectAdvapi32.Credential(ptr);
                cred.read();
                if (cred.CredentialBlob != null && cred.CredentialBlobSize > 0) {
                    return cred.CredentialBlob.getByteArray(0, cred.CredentialBlobSize);
                }
            } catch (Exception e) {
                System.err.println("[HSM-Win] Error lectura estructura: " + e.getMessage());
            } finally {
                DirectAdvapi32.INSTANCE.CredFree(ptr);
            }
        }
        return null;
    }

    @Override
    public void destroy(String alias) {
        if (!jnaAvailable) return;
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
            ks.load(null, null);
            if (ks.containsAlias(alias)) ks.deleteEntry(alias);
            DirectAdvapi32.INSTANCE.CredDeleteW(CRED_PREFIX + alias, 1, 0);
        } catch (Exception e) {}
    }

    @Override
    public boolean isAvailable() {
        return jnaAvailable && System.getProperty("os.name").toLowerCase().contains("win");
    }
}
