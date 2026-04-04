package com.mycompany.blackpassword.crypto;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Date;

/**
 * Generador de Llaves OpenPGP con compatibilidad máxima (BC 1.80).
 */
public class PGPLogica {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static class PGPKeyPairResult {
        public final String publicKey;
        public final String privateKey;

        public PGPKeyPairResult(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }

    public static PGPKeyPairResult generatePGPKeyPair(String identity, char[] passphrase, int keySize) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(keySize);
        KeyPair kp = kpg.generateKeyPair();

        // 1. Usar BcDigestCalculatorProvider que es el núcleo de OpenPGP en BC
        PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
        
        // 2. Crear par de llaves OpenPGP
        PGPKeyPair rsaKeyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());

        // 3. Cifrado de llave secreta
        org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor encryptor = null;
        if (passphrase != null && passphrase.length > 0) {
            encryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc)
                    .setProvider("BC")
                    .build(passphrase);
        }

        // 4. Generación estable de anillos
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                rsaKeyPair,
                identity,
                sha1Calc,
                null,
                null,
                new JcaPGPContentSignerBuilder(rsaKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256).setProvider("BC"),
                encryptor
        );

        PGPPublicKeyRing pubRing = keyRingGen.generatePublicKeyRing();
        PGPSecretKeyRing secRing = keyRingGen.generateSecretKeyRing();

        return new PGPKeyPairResult(
            exportToArmored(pubRing), 
            exportToArmored(secRing)
        );
    }

    private static String exportToArmored(Object ring) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ArmoredOutputStream aos = new ArmoredOutputStream(baos)) {
            if (ring instanceof PGPPublicKeyRing) {
                ((PGPPublicKeyRing) ring).encode(aos);
            } else if (ring instanceof PGPSecretKeyRing) {
                ((PGPSecretKeyRing) ring).encode(aos);
            }
        }
        return baos.toString();
    }
}
