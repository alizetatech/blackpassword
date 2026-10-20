package com.mycompany.blackpassword.services;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.crypto.SecureCharArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Túnel VPN WireGuard v1.0 - Zero-Disk Cloud Architecture.
 * Los perfiles se cifran en memoria con RSA-4096 híbrido y se almacenan exclusivamente
 * en Firebase Firestore. Cero archivos residuales o bases de datos locales.
 */
public class WireguardService {

    private static final String TUNNEL_NAME = "bpvpn";
    private static volatile boolean tunnelActive = false;
    private static volatile WireguardProfile activeProfile = null;
    private static volatile long connectedTimestamp = 0;

    /**
     * Modelo de Perfil de Conexión WireGuard.
     */
    public static class WireguardProfile {
        public String id;
        public String name;
        public String address;
        public String privateKey;
        public String dns;
        public String mtu;
        public String publicKey;
        public String presharedKey;
        public String allowedIps;
        public String endpoint;
        public String persistentKeepalive;
        public String rawConfig;
        public long timestamp;

        public WireguardProfile() {}

        @Override
        public String toString() {
            return name != null ? name : "Perfil WireGuard";
        }
    }

    /**
     * Par de llaves Curve25519 para WireGuard.
     */
    public static class KeyPairWireguard {
        public final String privateKey;
        public final String publicKey;
        public final String presharedKey;

        public KeyPairWireguard(String privateKey, String publicKey, String presharedKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.presharedKey = presharedKey;
        }
    }

    // --- GENERADOR DE LLAVES CURVE25519 NATIVO ---

    public static KeyPairWireguard generarParLlaves() throws Exception {
        byte[] privBytes = new byte[32];
        byte[] pubBytes = new byte[32];
        byte[] pskBytes = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(pskBytes);

        try {
            org.bouncycastle.crypto.generators.X25519KeyPairGenerator gen = new org.bouncycastle.crypto.generators.X25519KeyPairGenerator();
            gen.init(new org.bouncycastle.crypto.KeyGenerationParameters(sr, 256));
            org.bouncycastle.crypto.AsymmetricCipherKeyPair pair = gen.generateKeyPair();
            org.bouncycastle.crypto.params.X25519PrivateKeyParameters priv = (org.bouncycastle.crypto.params.X25519PrivateKeyParameters) pair.getPrivate();
            org.bouncycastle.crypto.params.X25519PublicKeyParameters pub = (org.bouncycastle.crypto.params.X25519PublicKeyParameters) pair.getPublic();
            privBytes = priv.getEncoded();
            pubBytes = pub.getEncoded();
        } catch (Throwable t) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
            KeyPair kp = kpg.generateKeyPair();
            privBytes = Arrays.copyOf(kp.getPrivate().getEncoded(), 32);
            pubBytes = Arrays.copyOf(kp.getPublic().getEncoded(), 32);
        }

        String privB64 = Base64.getEncoder().encodeToString(privBytes);
        String pubB64 = Base64.getEncoder().encodeToString(pubBytes);
        String pskB64 = Base64.getEncoder().encodeToString(pskBytes);

        Arrays.fill(privBytes, (byte) 0);
        Arrays.fill(pubBytes, (byte) 0);
        Arrays.fill(pskBytes, (byte) 0);

        return new KeyPairWireguard(privB64, pubB64, pskB64);
    }

    // --- PARSEADOR Y CONSTRUCTOR DE ARCHIVOS .CONF ---

    public static WireguardProfile parseConfig(String rawConfig, String profileName) {
        WireguardProfile p = new WireguardProfile();
        p.name = (profileName != null && !profileName.trim().isEmpty()) ? profileName.trim() : "WireGuard Server";
        p.rawConfig = rawConfig != null ? rawConfig.trim() : "";
        p.timestamp = System.currentTimeMillis();

        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            return p;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(rawConfig))) {
            String line;
            String section = "";
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).trim().toLowerCase();
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx == -1) continue;
                String key = line.substring(0, idx).trim().toLowerCase();
                String val = line.substring(idx + 1).trim();

                if ("interface".equals(section)) {
                    switch (key) {
                        case "address": p.address = val; break;
                        case "privatekey": p.privateKey = val; break;
                        case "dns": p.dns = val; break;
                        case "mtu": p.mtu = val; break;
                    }
                } else if ("peer".equals(section)) {
                    switch (key) {
                        case "publickey": p.publicKey = val; break;
                        case "presharedkey": p.presharedKey = val; break;
                        case "allowedips": p.allowedIps = val; break;
                        case "endpoint": p.endpoint = val; break;
                        case "persistentkeepalive": p.persistentKeepalive = val; break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WireguardService] Error parseando config: " + e.getMessage());
        }

        return p;
    }

    public static String buildConfigString(WireguardProfile p) {
        if (p.rawConfig != null && !p.rawConfig.trim().isEmpty() && p.rawConfig.contains("[Interface]")) {
            return p.rawConfig;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[Interface]\n");
        if (p.address != null && !p.address.isEmpty()) sb.append("Address = ").append(p.address).append("\n");
        if (p.privateKey != null && !p.privateKey.isEmpty()) sb.append("PrivateKey = ").append(p.privateKey).append("\n");
        if (p.dns != null && !p.dns.isEmpty()) sb.append("DNS = ").append(p.dns).append("\n");
        if (p.mtu != null && !p.mtu.isEmpty()) sb.append("MTU = ").append(p.mtu).append("\n");

        sb.append("\n[Peer]\n");
        if (p.publicKey != null && !p.publicKey.isEmpty()) sb.append("PublicKey = ").append(p.publicKey).append("\n");
        if (p.presharedKey != null && !p.presharedKey.isEmpty()) sb.append("PresharedKey = ").append(p.presharedKey).append("\n");
        if (p.endpoint != null && !p.endpoint.isEmpty()) sb.append("Endpoint = ").append(p.endpoint).append("\n");
        if (p.allowedIps != null && !p.allowedIps.isEmpty()) sb.append("AllowedIPs = ").append(p.allowedIps).append("\n");
        if (p.persistentKeepalive != null && !p.persistentKeepalive.isEmpty()) sb.append("PersistentKeepalive = ").append(p.persistentKeepalive).append("\n");

        return sb.toString();
    }

    // --- ALMACENAMIENTO Y CIFRADO 100% EN FIREBASE CLOUD (ZERO-DISK) ---

    private static String getWireguardCloudPath(String email) throws Exception {
        char[] emailChars = email.toCharArray();
        char[] emailHashChars = RSALogica.hashToHex(emailChars, null);
        String emailHash = new String(emailHashChars);
        String colUsuarios = RSALogica.hashConstantToHex("usuarios");
        String colWireguard = RSALogica.hashConstantToHex("wireguard_vpn");
        String finalPath = colUsuarios + "/" + emailHash + "/" + colWireguard;
        SecureCharArray.clear(emailChars);
        SecureCharArray.clear(emailHashChars);
        return finalPath;
    }

    public static boolean guardarPerfilCloud(String email, String profileName, String rawConfig) {
        if (!SecuritySession.isIdentityValid(email) || SecuritySession.getPublicKey() == null) {
            System.err.println("[WireguardService] Identidad de sesión inválida o llave pública ausente.");
            return false;
        }

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db == null) {
                System.err.println("[WireguardService] Firestore no inicializado.");
                return false;
            }

            WireguardProfile parsed = parseConfig(rawConfig, profileName);
            byte[] plainBytes = rawConfig.getBytes(StandardCharsets.UTF_8);
            byte[] encBytes = RSALogica.cifrarBytesHibrido(plainBytes, SecuritySession.getPublicKey());
            Arrays.fill(plainBytes, (byte) 0);
            String encB64 = Base64.getEncoder().encodeToString(encBytes);

            String path = getWireguardCloudPath(email);
            String docId = RSALogica.hashConstantToHex(profileName.trim().toLowerCase());

            Map<String, Object> data = new HashMap<>();
            data.put("nombre_perfil", profileName.trim());
            data.put("payload_cifrado", encB64);
            data.put("address", parsed.address != null ? parsed.address : "");
            data.put("endpoint", parsed.endpoint != null ? parsed.endpoint : "");
            data.put("ts", System.currentTimeMillis());
            data.put("fecha_actualizacion", Timestamp.now());

            db.collection(path).document(docId).set(data).get();
            System.out.println("[WireguardService] Perfil '" + profileName + "' cifrado con RSA-4096 y guardado en Firebase.");
            return true;
        } catch (Exception e) {
            System.err.println("[WireguardService] Error guardando perfil en Firebase: " + e.getMessage());
            return false;
        }
    }

    public static List<WireguardProfile> cargarPerfilesCloud(String email) {
        List<WireguardProfile> list = new ArrayList<>();
        if (!SecuritySession.isIdentityValid(email) || SecuritySession.getPrivateKey() == null) {
            return list;
        }

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db == null) return list;

            String path = getWireguardCloudPath(email);
            var querySnap = db.collection(path).get().get();

            for (QueryDocumentSnapshot doc : querySnap.getDocuments()) {
                String encB64 = doc.getString("payload_cifrado");
                String name = doc.getString("nombre_perfil");
                if (encB64 != null) {
                    try {
                        byte[] encBytes = Base64.getDecoder().decode(encB64);
                        byte[] decBytes = RSALogica.descifrarBytesHibrido(encBytes, SecuritySession.getPrivateKey());
                        String raw = new String(decBytes, StandardCharsets.UTF_8);
                        Arrays.fill(decBytes, (byte) 0);

                        WireguardProfile p = parseConfig(raw, name != null ? name : doc.getId());
                        p.id = doc.getId();
                        Long ts = doc.getLong("ts");
                        if (ts != null) p.timestamp = ts;
                        list.add(p);
                    } catch (Exception ex) {
                        System.err.println("[WireguardService] Error descifrando perfil " + doc.getId() + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WireguardService] Error cargando perfiles desde Firebase: " + e.getMessage());
        }

        return list;
    }

    public static List<WireguardProfile> cargarPerfilesDeCloud(String email) {
        return cargarPerfilesCloud(email);
    }

    public static boolean guardarPerfilEnCloud(String email, String profileName, String rawConfig) {
        return guardarPerfilCloud(email, profileName, rawConfig);
    }

    public static boolean eliminarPerfilCloud(String email, String docId) {
        if (!SecuritySession.isIdentityValid(email)) return false;
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db == null) return false;
            String path = getWireguardCloudPath(email);
            db.collection(path).document(docId).delete().get();
            System.out.println("[WireguardService] Perfil " + docId + " eliminado de Firebase.");
            return true;
        } catch (Exception e) {
            System.err.println("[WireguardService] Error eliminando perfil de Firebase: " + e.getMessage());
            return false;
        }
    }

    // --- CONTROLADOR DEL TÚNEL EN WINDOWS ---

    public static File buscarEjecutableWireguard() {
        File[] posibles = {
            new File("C:\\Program Files\\WireGuard\\wireguard.exe"),
            new File("C:\\Program Files (x86)\\WireGuard\\wireguard.exe"),
            new File(System.getenv("LOCALAPPDATA") + "\\WireGuard\\wireguard.exe")
        };
        for (File f : posibles) {
            if (f.exists() && f.canExecute()) return f;
        }
        return null;
    }

    public static boolean isWireguardInstalado() {
        return buscarEjecutableWireguard() != null;
    }

    public static synchronized boolean activarTunel(WireguardProfile perfil) {
        if (perfil == null) return false;

        File wgExe = buscarEjecutableWireguard();
        if (wgExe == null) {
            System.err.println("[WireguardService] wireguard.exe no encontrado en el sistema.");
            return false;
        }

        Path tempConf = null;
        try {
            String confContent = buildConfigString(perfil);
            tempConf = Files.createTempFile("bp_wg_", ".conf");
            Files.writeString(tempConf, confContent, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                wgExe.getAbsolutePath(),
                "/installtunnelservice",
                tempConf.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            int exitCode = proc.waitFor();

            if (exitCode == 0 || isServicioTunelActivo()) {
                tunnelActive = true;
                activeProfile = perfil;
                connectedTimestamp = System.currentTimeMillis();
                System.out.println("[WireguardService] Túnel '" + perfil.name + "' activado exitosamente.");
                return true;
            } else {
                System.err.println("[WireguardService] Error levantando túnel, exitCode=" + exitCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[WireguardService] Excepción al activar túnel: " + e.getMessage());
            return false;
        } finally {
            if (tempConf != null) {
                try { Files.deleteIfExists(tempConf); } catch (Exception ignored) {}
            }
        }
    }

    public static synchronized boolean desactivarTunel() {
        File wgExe = buscarEjecutableWireguard();
        try {
            if (wgExe != null) {
                ProcessBuilder pb = new ProcessBuilder(
                    wgExe.getAbsolutePath(),
                    "/uninstalltunnelservice",
                    TUNNEL_NAME
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.waitFor();
            }
        } catch (Exception e) {
            System.err.println("[WireguardService] Error desinstalando servicio: " + e.getMessage());
        } finally {
            tunnelActive = false;
            activeProfile = null;
            connectedTimestamp = 0;
        }
        return true;
    }

    public static boolean isServicioTunelActivo() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "query", "WireGuardTunnel$" + TUNNEL_NAME);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("RUNNING")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isTunnelActivo() {
        return tunnelActive || isServicioTunelActivo();
    }

    public static WireguardProfile getActiveProfile() {
        return activeProfile;
    }

    public static long getConnectedTimestamp() {
        return connectedTimestamp;
    }
}
