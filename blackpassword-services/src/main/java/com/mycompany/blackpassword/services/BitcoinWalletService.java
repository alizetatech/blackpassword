package com.mycompany.blackpassword.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.blackpassword.crypto.RSALogica;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

/**
 * Servicio de Billetera Bitcoin HD (BIP-39 / BIP-84 SegWit) impulsado por BitcoinJ.
 * Integrado con el esquema de seguridad Zero-Trust de Blackpassword.
 */
public class BitcoinWalletService {

    private static final Preferences prefs = Preferences.userNodeForPackage(BitcoinWalletService.class);
    private static final String PREF_WALLET_DATA = "btc_wallet_payload_";
    private static final String PREF_WALLET_TESTNET = "btc_wallet_testnet_";

    /**
     * Parámetros de red
     */
    public static NetworkParameters getNetwork(boolean testnet) {
        return testnet ? TestNet3Params.get() : MainNetParams.get();
    }

    /**
     * Genera una nueva Billetera Bitcoin HD nativa SegWit (P2WPKH / bc1q / tb1q)
     */
    public static Wallet crearBilletera(boolean testnet) {
        NetworkParameters params = getNetwork(testnet);
        SecureRandom random = new SecureRandom();
        // 128 bits de entropía = 12 palabras BIP-39
        DeterministicSeed seed = new DeterministicSeed(random, 128, "");
        return Wallet.fromSeed(params, seed, Script.ScriptType.P2WPKH);
    }

    /**
     * Restaura una Billetera Bitcoin HD desde sus 12 palabras semilla
     */
    public static Wallet restaurarBilletera(List<String> palabras, boolean testnet) throws Exception {
        if (palabras == null || (palabras.size() != 12 && palabras.size() != 24)) {
            throw new IllegalArgumentException("La frase semilla debe contener 12 o 24 palabras.");
        }
        NetworkParameters params = getNetwork(testnet);
        // Epoch aproximado para restauración rápida
        long creationTime = 1609459200L; // 2021-01-01
        DeterministicSeed seed = new DeterministicSeed(palabras, null, "", creationTime);
        return Wallet.fromSeed(params, seed, Script.ScriptType.P2WPKH);
    }

    /**
     * Obtiene la lista de palabras mnemónicas (BIP-39)
     */
    public static List<String> obtenerPalabras(Wallet wallet) {
        DeterministicSeed seed = wallet.getKeyChainSeed();
        if (seed != null) {
            return seed.getMnemonicCode();
        }
        return null;
    }

    /**
     * Obtiene la dirección actual de recepción (Bech32 SegWit: bc1q... / tb1q...)
     */
    public static String obtenerDireccion(Wallet wallet) {
        Address addr = wallet.currentReceiveAddress();
        return addr.toString();
    }

    /**
     * Comprueba si el usuario ya tiene una billetera guardada localmente
     */
    public static boolean existeBilletera(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) return false;
        String data = prefs.get(PREF_WALLET_DATA + userEmail.trim().toLowerCase(), "");
        return !data.isEmpty();
    }

    /**
     * Guarda la billetera cifrada con AES-256-GCM derivado del PIN del usuario
     */
    public static void guardarBilleteraCifrada(Wallet wallet, String userEmail, char[] pin, boolean testnet) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wallet.saveToFileStream(baos);
        byte[] rawBytes = baos.toByteArray();

        byte[] salt = ("BTC_SALT_" + userEmail.trim().toLowerCase()).getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = cifrarBytes(rawBytes, pin, salt);

        String encoded = java.util.Base64.getEncoder().encodeToString(encryptedPayload);
        String key = userEmail.trim().toLowerCase();
        prefs.put(PREF_WALLET_DATA + key, encoded);
        prefs.putBoolean(PREF_WALLET_TESTNET + key, testnet);
    }

    /**
     * Carga y descifra la billetera del usuario
     */
    public static Wallet cargarBilleteraCifrada(String userEmail, char[] pin) throws Exception {
        String key = userEmail.trim().toLowerCase();
        String encoded = prefs.get(PREF_WALLET_DATA + key, "");
        if (encoded.isEmpty()) {
            throw new IllegalStateException("No existe una billetera guardada para este usuario.");
        }

        boolean testnet = prefs.getBoolean(PREF_WALLET_TESTNET + key, true);
        byte[] encryptedPayload = java.util.Base64.getDecoder().decode(encoded);
        byte[] salt = ("BTC_SALT_" + key).getBytes(StandardCharsets.UTF_8);

        byte[] decryptedBytes = descifrarBytes(encryptedPayload, pin, salt);
        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);

        Wallet wallet = Wallet.loadFromFileStream(bais);
        // Limpieza de memoria
        Arrays.fill(decryptedBytes, (byte) 0);
        return wallet;
    }

    /**
     * Elimina la billetera almacenada localmente
     */
    public static void eliminarBilletera(String userEmail) {
        String key = userEmail.trim().toLowerCase();
        prefs.remove(PREF_WALLET_DATA + key);
        prefs.remove(PREF_WALLET_TESTNET + key);
    }

    /**
     * Es la billetera actual de testnet?
     */
    public static boolean isTestnet(String userEmail) {
        String key = userEmail.trim().toLowerCase();
        return prefs.getBoolean(PREF_WALLET_TESTNET + key, true);
    }

    /**
     * Consulta el balance en Satoshis en tiempo real desde la API pública de Mempool
     */
    public static long consultarBalanceSats(String address, boolean testnet) {
        try {
            String baseUrl = testnet 
                ? "https://mempool.space/testnet/api/address/" 
                : "https://mempool.space/api/address/";

            URL url = new URL(baseUrl + address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Blackpassword-Client");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject chainStats = json.getAsJsonObject("chain_stats");
                    long funded = chainStats.get("funded_txo_sum").getAsLong();
                    long spent = chainStats.get("spent_txo_sum").getAsLong();
                    
                    JsonObject mempoolStats = json.getAsJsonObject("mempool_stats");
                    long mempoolFunded = mempoolStats.get("funded_txo_sum").getAsLong();
                    long mempoolSpent = mempoolStats.get("spent_txo_sum").getAsLong();

                    return (funded - spent) + (mempoolFunded - mempoolSpent);
                }
            }
        } catch (Exception e) {
            System.err.println("[BitcoinWalletService] Error consultando balance: " + e.getMessage());
        }
        return -1; // -1 indica que no se pudo conectar o timeout
    }

    /**
     * Convierte Satoshis a formato BTC (String legible con 8 decimales)
     */
    public static String satsToBtc(long sats) {
        if (sats < 0) return "-- BTC";
        double btc = (double) sats / 100_000_000.0;
        return String.format(java.util.Locale.US, "%.8f BTC", btc);
    }

    // --- Helpers de Cifrado AES-256 GCM con PBKDF2 ---

    private static byte[] cifrarBytes(byte[] raw, char[] pin, byte[] salt) throws Exception {
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] enc = cipher.doFinal(raw);
        ByteBuffer bb = ByteBuffer.allocate(iv.length + enc.length);
        bb.put(iv);
        bb.put(enc);
        return bb.array();
    }

    private static byte[] descifrarBytes(byte[] data, char[] pin, byte[] salt) throws Exception {
        if (data.length < 12) throw new IllegalArgumentException("Datos corruptos de billetera.");
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] enc = Arrays.copyOfRange(data, 12, data.length);
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(enc);
    }
}
