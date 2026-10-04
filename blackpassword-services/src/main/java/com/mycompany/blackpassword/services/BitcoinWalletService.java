package com.mycompany.blackpassword.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 * Servicio Inteligente de Billetera Bitcoin HD (BIP-39 / BIP-84 SegWit) impulsado por BitcoinJ.
 * Proporciona soporte multired (Mainnet y Testnet), libreta de direcciones etiquetadas,
 * historial de transacciones en tiempo real con estado de confirmación y blindaje AES-256 GCM.
 */
public class BitcoinWalletService {

    private static final Preferences prefs = Preferences.userNodeForPackage(BitcoinWalletService.class);
    private static final String PREF_WALLET_DATA = "btc_wallet_payload_";
    private static final String PREF_WALLET_TESTNET = "btc_wallet_testnet_";
    private static final String PREF_ADDRESS_BOOK = "btc_address_book_";

    // --- Clases DTO para UI ---

    public static class BtcTxInfo {
        public final String txid;
        public final boolean isIncoming;
        public final long sats;
        public final boolean isConfirmed;
        public final int blockHeight;
        public final long blockTime; // timestamp en segundos

        public BtcTxInfo(String txid, boolean isIncoming, long sats, boolean isConfirmed, int blockHeight, long blockTime) {
            this.txid = txid;
            this.isIncoming = isIncoming;
            this.sats = sats;
            this.isConfirmed = isConfirmed;
            this.blockHeight = blockHeight;
            this.blockTime = blockTime;
        }

        public String getBtcAmount() {
            double btc = (double) sats / 100_000_000.0;
            return (isIncoming ? "+ " : "- ") + String.format(java.util.Locale.US, "%.8f BTC", btc);
        }

        public String getFormattedDate() {
            if (blockTime <= 0) return "En Mempool (Reciente)";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new Date(blockTime * 1000));
        }

        public String getStatusDescription() {
            if (isConfirmed) {
                return "✅ Confirmada (Bloque " + blockHeight + ")";
            } else {
                return "⏳ Pendiente (0 conf.)";
            }
        }
    }

    public static class BtcAddressEntry {
        public String address;
        public String label; // Título opcional ej: "Pago de Juan", o ""
        public String date;

        public BtcAddressEntry(String address, String label, String date) {
            this.address = address;
            this.label = (label != null) ? label.trim() : "";
            this.date = (date != null) ? date : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }
    }

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
     * Genera una nueva dirección de recepción fresca de la cadena HD
     */
    public static String generarNuevaDireccion(Wallet wallet) {
        Address addr = wallet.freshReceiveAddress();
        return addr.toString();
    }

    /**
     * Comprueba si el usuario ya tiene una billetera guardada para la red indicada
     */
    public static boolean existeBilletera(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return false;
        String key = getWalletKey(userEmail, testnet);
        String data = prefs.get(PREF_WALLET_DATA + key, "");
        if (!data.isEmpty()) return true;

        // Fallback a clave legacy si coincide la red
        String legacy = prefs.get(PREF_WALLET_DATA + userEmail.trim().toLowerCase(), "");
        boolean legacyIsTestnet = prefs.getBoolean(PREF_WALLET_TESTNET + userEmail.trim().toLowerCase(), true);
        return !legacy.isEmpty() && (legacyIsTestnet == testnet);
    }

    /**
     * Guarda la billetera cifrada con AES-256-GCM derivado del PIN del usuario
     */
    public static void guardarBilleteraCifrada(Wallet wallet, String userEmail, char[] pin, boolean testnet) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wallet.saveToFileStream(baos);
        byte[] rawBytes = baos.toByteArray();

        String key = getWalletKey(userEmail, testnet);
        byte[] salt = ("BTC_SALT_" + key).getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = cifrarBytes(rawBytes, pin, salt);

        String encoded = java.util.Base64.getEncoder().encodeToString(encryptedPayload);
        prefs.put(PREF_WALLET_DATA + key, encoded);
        prefs.putBoolean(PREF_WALLET_TESTNET + key, testnet);
    }

    /**
     * Carga y descifra la billetera del usuario para la red indicada
     */
    public static Wallet cargarBilleteraCifrada(String userEmail, char[] pin, boolean testnet) throws Exception {
        String key = getWalletKey(userEmail, testnet);
        String encoded = prefs.get(PREF_WALLET_DATA + key, "");
        boolean isLegacyKey = false;
        
        // Fallback legacy
        if (encoded.isEmpty()) {
            String legacyKey = userEmail.trim().toLowerCase();
            String legacyEncoded = prefs.get(PREF_WALLET_DATA + legacyKey, "");
            boolean legacyIsTestnet = prefs.getBoolean(PREF_WALLET_TESTNET + legacyKey, true);
            if (!legacyEncoded.isEmpty() && legacyIsTestnet == testnet) {
                encoded = legacyEncoded;
                key = legacyKey;
                isLegacyKey = true;
            }
        }

        if (encoded.isEmpty()) {
            throw new IllegalStateException("No existe una billetera guardada en esta red para este usuario.");
        }

        byte[] encryptedPayload = java.util.Base64.getDecoder().decode(encoded);
        byte[] salt = ("BTC_SALT_" + key).getBytes(StandardCharsets.UTF_8);

        byte[] decryptedBytes = null;
        boolean rescued = false;
        try {
            decryptedBytes = descifrarBytes(encryptedPayload, pin, salt);
        } catch (Exception primaryEx) {
            // Intento de auto-rescate si la billetera fue guardada con el PIN limpiado en ceros (\0\0\0\0)
            int pinLen = (pin != null && pin.length > 0) ? pin.length : 4;
            char[] zeroPin = new char[pinLen];
            Arrays.fill(zeroPin, (char) 0);
            try {
                decryptedBytes = descifrarBytes(encryptedPayload, zeroPin, salt);
                rescued = true;
            } catch (Exception rescueEx1) {
                // Probar también con sal alternativa (por si key tenía sufijo y sal era legacy)
                byte[] altSalt = ("BTC_SALT_" + userEmail.trim().toLowerCase()).getBytes(StandardCharsets.UTF_8);
                try {
                    decryptedBytes = descifrarBytes(encryptedPayload, zeroPin, altSalt);
                    rescued = true;
                } catch (Exception rescueEx2) {
                    if (pinLen != 4) {
                        char[] zeroPin4 = new char[]{0, 0, 0, 0};
                        try {
                            decryptedBytes = descifrarBytes(encryptedPayload, zeroPin4, salt);
                            rescued = true;
                        } catch (Exception rescueEx3) {
                            try {
                                decryptedBytes = descifrarBytes(encryptedPayload, zeroPin4, altSalt);
                                rescued = true;
                            } catch (Exception rescueEx4) {
                                throw primaryEx;
                            }
                        }
                    } else {
                        throw primaryEx;
                    }
                }
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
        Wallet wallet = Wallet.loadFromFileStream(bais);
        Arrays.fill(decryptedBytes, (byte) 0);

        // Si fue rescatada o estaba en la clave legacy, re-cifrar inmediatamente con el PIN real
        if (pin != null && pin.length > 0 && (rescued || isLegacyKey)) {
            try {
                guardarBilleteraCifrada(wallet, userEmail, pin, testnet);
                if (isLegacyKey) {
                    String legacyKey = userEmail.trim().toLowerCase();
                    prefs.remove(PREF_WALLET_DATA + legacyKey);
                    prefs.remove(PREF_WALLET_TESTNET + legacyKey);
                }
                System.out.println("[BitcoinWalletService] Billetera asegurada/migrada exitosamente con el PIN del usuario.");
            } catch (Exception reEncEx) {
                System.err.println("[BitcoinWalletService] Advertencia al re-cifrar billetera rescatada: " + reEncEx.getMessage());
            }
        }

        return wallet;
    }

    /**
     * Elimina de forma segura la billetera de una red específica.
     * Sobrescribe con ruido criptográfico antes de eliminar para impedir recuperación forense.
     */
    public static void eliminarBilletera(String userEmail, boolean testnet) {
        String key = getWalletKey(userEmail, testnet);
        String prefData = PREF_WALLET_DATA + key;
        String prefNet = PREF_WALLET_TESTNET + key;

        byte[] junk = new byte[1024];
        new SecureRandom().nextBytes(junk);
        prefs.put(prefData, java.util.Base64.getEncoder().encodeToString(junk));
        try {
            prefs.flush();
        } catch (Exception ignored) {}

        prefs.remove(prefData);
        prefs.remove(prefNet);

        // Limpieza de clave legacy si existía
        String legacyKey = userEmail.trim().toLowerCase();
        boolean legacyIsTestnet = prefs.getBoolean(PREF_WALLET_TESTNET + legacyKey, true);
        if (legacyIsTestnet == testnet) {
            prefs.put(PREF_WALLET_DATA + legacyKey, java.util.Base64.getEncoder().encodeToString(junk));
            try {
                prefs.flush();
            } catch (Exception ignored) {}
            prefs.remove(PREF_WALLET_DATA + legacyKey);
            prefs.remove(PREF_WALLET_TESTNET + legacyKey);
        }

        try {
            prefs.flush();
        } catch (Exception ignored) {}
    }

    public static boolean isTestnet(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) return true;
        String key = userEmail.trim().toLowerCase();
        return prefs.getBoolean(PREF_WALLET_TESTNET + key, true);
    }

    private static String getWalletKey(String userEmail, boolean testnet) {
        return userEmail.trim().toLowerCase() + (testnet ? "_testnet" : "_mainnet");
    }

    // --- LIBRETA DE DIRECCIONES CON ETIQUETAS ---

    public static List<BtcAddressEntry> cargarDireccionesEtiquetadas(String userEmail, boolean testnet) {
        List<BtcAddressEntry> list = new ArrayList<>();
        String key = PREF_ADDRESS_BOOK + getWalletKey(userEmail, testnet);
        String rawJson = prefs.get(key, "");
        if (!rawJson.isEmpty()) {
            try {
                JsonArray arr = JsonParser.parseString(rawJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String addr = obj.get("address").getAsString();
                    String lbl = obj.has("label") ? obj.get("label").getAsString() : "";
                    String date = obj.has("date") ? obj.get("date").getAsString() : "";
                    list.add(new BtcAddressEntry(addr, lbl, date));
                }
            } catch (Exception ex) {
                System.err.println("[BitcoinWalletService] Error cargando libreta: " + ex.getMessage());
            }
        }
        return list;
    }

    public static void guardarDireccionEtiquetada(String userEmail, boolean testnet, String address, String label) {
        List<BtcAddressEntry> list = cargarDireccionesEtiquetadas(userEmail, testnet);
        // Verificar si ya existe para actualizar label
        boolean found = false;
        for (BtcAddressEntry entry : list) {
            if (entry.address.equalsIgnoreCase(address)) {
                entry.label = (label != null) ? label.trim() : "";
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, new BtcAddressEntry(address, label, null));
        }

        // Serializar a JSON
        JsonArray arr = new JsonArray();
        for (BtcAddressEntry entry : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("address", entry.address);
            obj.addProperty("label", entry.label);
            obj.addProperty("date", entry.date);
            arr.add(obj);
        }
        prefs.put(PREF_ADDRESS_BOOK + getWalletKey(userEmail, testnet), arr.toString());
    }

    public static void eliminarDireccionEtiquetada(String userEmail, boolean testnet, String address) {
        List<BtcAddressEntry> list = cargarDireccionesEtiquetadas(userEmail, testnet);
        list.removeIf(entry -> entry.address.equalsIgnoreCase(address));
        JsonArray arr = new JsonArray();
        for (BtcAddressEntry entry : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("address", entry.address);
            obj.addProperty("label", entry.label);
            obj.addProperty("date", entry.date);
            arr.add(obj);
        }
        prefs.put(PREF_ADDRESS_BOOK + getWalletKey(userEmail, testnet), arr.toString());
    }

    // --- HISTORIAL DE TRANSACCIONES EN TIEMPO REAL ---

    /**
     * Consulta las transacciones entrantes y salientes con confirmación desde Mempool API
     */
    public static List<BtcTxInfo> consultarTransacciones(String address, boolean testnet) {
        List<BtcTxInfo> list = new ArrayList<>();
        if (address == null || address.isEmpty()) return list;

        try {
            String baseUrl = testnet 
                ? "https://mempool.space/testnet/api/address/" 
                : "https://mempool.space/api/address/";

            URL url = new URL(baseUrl + address + "/txs");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Blackpassword-Client");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject txObj = array.get(i).getAsJsonObject();
                        String txid = txObj.get("txid").getAsString();
                        
                        JsonObject statusObj = txObj.getAsJsonObject("status");
                        boolean confirmed = statusObj.get("confirmed").getAsBoolean();
                        int blockHeight = confirmed && statusObj.has("block_height") ? statusObj.get("block_height").getAsInt() : 0;
                        long blockTime = confirmed && statusObj.has("block_time") ? statusObj.get("block_time").getAsLong() : 0;

                        long inputFromAddress = 0;
                        if (txObj.has("vin")) {
                            JsonArray vin = txObj.getAsJsonArray("vin");
                            for (int v = 0; v < vin.size(); v++) {
                                JsonObject in = vin.get(v).getAsJsonObject();
                                if (in.has("prevout") && !in.get("prevout").isJsonNull()) {
                                    JsonObject prev = in.getAsJsonObject("prevout");
                                    if (prev.has("scriptpubkey_address") && address.equalsIgnoreCase(prev.get("scriptpubkey_address").getAsString())) {
                                        inputFromAddress += prev.get("value").getAsLong();
                                    }
                                }
                            }
                        }

                        long outputToAddress = 0;
                        if (txObj.has("vout")) {
                            JsonArray vout = txObj.getAsJsonArray("vout");
                            for (int v = 0; v < vout.size(); v++) {
                                JsonObject out = vout.get(v).getAsJsonObject();
                                if (out.has("scriptpubkey_address") && address.equalsIgnoreCase(out.get("scriptpubkey_address").getAsString())) {
                                    outputToAddress += out.get("value").getAsLong();
                                }
                            }
                        }

                        if (inputFromAddress > 0) {
                            // Saliente
                            long sentAmount = inputFromAddress - outputToAddress;
                            if (sentAmount <= 0) sentAmount = inputFromAddress;
                            list.add(new BtcTxInfo(txid, false, sentAmount, confirmed, blockHeight, blockTime));
                        } else if (outputToAddress > 0) {
                            // Entrante
                            list.add(new BtcTxInfo(txid, true, outputToAddress, confirmed, blockHeight, blockTime));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BitcoinWalletService] Error al obtener transacciones: " + e.getMessage());
        }
        return list;
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
        return -1;
    }

    /**
     * Convierte Satoshis a formato BTC (String con 8 decimales)
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
