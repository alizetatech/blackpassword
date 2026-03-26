package com.mycompany.blackpassword;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Servicio de Certificación SSL/X.509 v2.0.
 * Genera certificados digitales estándar de la industria para identidades soberanas.
 */
public class CertificateService {

    /**
     * Genera un certificado SSL/X.509 auto-firmado para el usuario.
     */
    public static String generateSSLCertificate(String nombre, String email, PublicKey pub, PrivateKey priv) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        
        // Validez de 2 años para el pasaporte digital
        Date endDate = new Date(now + (2L * 365 * 24 * 60 * 60 * 1000));
        
        X500Name dnName = new X500Name("CN=" + nombre + ", E=" + email + ", O=Blackpassword Authority, C=ES");
        BigInteger certSerialNumber = new BigInteger(64, new SecureRandom());
        
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(priv);
        
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, pub);
        
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        
        return java.util.Base64.getEncoder().encodeToString(cert.getEncoded());
    }

    /**
     * Cifra el certificado X.509 para almacenamiento seguro en la nube.
     */
    public static String encryptCertificate(String certBase64, PublicKey pub) throws Exception {
        byte[] encrypted = RSALogica.cifrarBytesHibrido(certBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8), pub);
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * Convierte el Base64 a un objeto X509Certificate para lectura de metadatos.
     */
    public static X509Certificate parseCertificate(String certBase64) throws Exception {
        byte[] decoded = java.util.Base64.getDecoder().decode(certBase64);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(decoded));
    }
}
