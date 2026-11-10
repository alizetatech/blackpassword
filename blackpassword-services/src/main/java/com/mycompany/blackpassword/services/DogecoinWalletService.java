package com.mycompany.blackpassword.services;

import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.blackpassword.crypto.RSALogica;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bouncycastle.util.encoders.Hex;

/**
 * Servicio Inteligente de Billetera Dogecoin HD (BIP-39 / BIP-44) para Blackpassword.
 * - Soporte multired: Dogecoin Mainnet (prefijo 'D') y Testnet (prefijo 'n').
 * - Cifrado AES-256-GCM con PBKDF2 (600,000 iteraciones).
 * - Sincronización en la Nube con Google Cloud Firestore y réplica local en SQLite.
 * - Libreta de direcciones etiquetadas y consulta de balance e historial en tiempo real.
 */
public class DogecoinWalletService {

    private static final Gson gson = new Gson();

    // --- DTO para la Billetera en Memoria ---
    public static class DogeWalletHolder {
        public List<String> mnemonic;
        public long creationTime;
        public int lastIndex;
        public boolean isTestnet;
        public String currentAddress;

        public DogeWalletHolder(List<String> mnemonic, long creationTime, int lastIndex, boolean isTestnet, String currentAddress) {
            this.mnemonic = mnemonic;
            this.creationTime = creationTime;
            this.lastIndex = lastIndex;
            this.isTestnet = isTestnet;
            this.currentAddress = currentAddress;
        }

        public List<String> getMnemonic() {
            return mnemonic;
        }

        public String getCurrentAddress() {
            return currentAddress;
        }
    }

    // --- DTOs para la UI ---
    public static class DogeTxInfo {
        public final String txid;
        public final boolean isIncoming;
        public final long koinus; // 1 DOGE = 100,000,000 koinus
        public final boolean isConfirmed;
        public final int blockHeight;
        public final long blockTime; // timestamp en segundos

        public DogeTxInfo(String txid, boolean isIncoming, long koinus, boolean isConfirmed, int blockHeight, long blockTime) {
            this.txid = txid;
            this.isIncoming = isIncoming;
            this.koinus = koinus;
            this.isConfirmed = isConfirmed;
            this.blockHeight = blockHeight;
            this.blockTime = blockTime;
        }

        public String getDogeAmount() {
            double doge = (double) koinus / 100_000_000.0;
            return (isIncoming ? "+ " : "- ") + String.format(java.util.Locale.US, "%.4f DOGE", doge);
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

    public static class DogeAddressEntry {
        public String address;
        public String label;
        public String date;

        public DogeAddressEntry(String address, String label, String date) {
            this.address = address;
            this.label = (label != null) ? label.trim() : "";
            this.date = (date != null) ? date : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }
    }

    public static class DogeUtxo {
        public String txid;
        public int vout;
        public long value; // en Koinu
        public boolean confirmed;
    }

    public static class SendResult {
        public boolean success;
        public String txid;
        public String errorMessage;
        public long amountKoinus;
        public long feeKoinus;

        public SendResult(boolean success, String txid, String errorMessage, long amountKoinus, long feeKoinus) {
            this.success = success;
            this.txid = txid;
            this.errorMessage = errorMessage;
            this.amountKoinus = amountKoinus;
            this.feeKoinus = feeKoinus;
        }
    }

    /**
     * Obtiene los parámetros de red correspondientes a Dogecoin.
     */
    public static NetworkParameters getNetwork(boolean testnet) {
        return testnet ? DogecoinTestNetParams.get() : DogecoinMainNetParams.get();
    }

    /**
     * Genera una nueva Billetera Dogecoin HD desde entropía aleatoria (BIP-39 / BIP-44).
     */
    public static DogeWalletHolder crearBilletera(boolean testnet) throws Exception {
        SecureRandom random = new SecureRandom();
        DeterministicSeed seed = new DeterministicSeed(random, 128, "");
        List<String> palabras = seed.getMnemonicCode();
        long now = System.currentTimeMillis() / 1000;
        String address = derivarDireccionBip44(seed.getSeedBytes(), testnet, 0);
        return new DogeWalletHolder(palabras, now, 0, testnet, address);
    }

    /**
     * Restaura una Billetera Dogecoin HD desde sus 12 o 24 palabras mnemónicas.
     */
    public static DogeWalletHolder restaurarBilletera(List<String> palabras, boolean testnet) throws Exception {
        if (palabras == null || (palabras.size() != 12 && palabras.size() != 24)) {
            throw new IllegalArgumentException("La frase semilla debe contener exactamente 12 o 24 palabras.");
        }
        long creationTime = 1386500000L; // Época génesis de Dogecoin (Diciembre 2013)
        DeterministicSeed seed = new DeterministicSeed(palabras, null, "", creationTime);
        String address = derivarDireccionBip44(seed.getSeedBytes(), testnet, 0);
        return new DogeWalletHolder(palabras, creationTime, 0, testnet, address);
    }

    /**
     * Deriva una dirección pública Dogecoin según el estándar oficial BIP-44:
     * - Mainnet: m / 44' / 3' / 0' / 0 / index
     * - Testnet: m / 44' / 1' / 0' / 0 / index
     */
    public static String derivarDireccionBip44(byte[] seedBytes, boolean testnet, int index) {
        NetworkParameters params = getNetwork(testnet);
        DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seedBytes);
        int coinType = testnet ? 1 : 3; // 3 es el coin_type oficial de Dogecoin (SLIP-0044)

        DeterministicKey purpose = HDKeyDerivation.deriveChildKey(masterKey, new ChildNumber(44, true));
        DeterministicKey coin = HDKeyDerivation.deriveChildKey(purpose, new ChildNumber(coinType, true));
        DeterministicKey account = HDKeyDerivation.deriveChildKey(coin, new ChildNumber(0, true));
        DeterministicKey change = HDKeyDerivation.deriveChildKey(account, ChildNumber.ZERO); // external chain
        DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(change, new ChildNumber(index, false));

        LegacyAddress address = LegacyAddress.fromKey(params, addressKey);
        return address.toBase58();
    }

    /**
     * Genera una nueva dirección de recepción para la billetera activa.
     */
    public static String generarNuevaDireccion(DogeWalletHolder wallet) throws Exception {
        DeterministicSeed seed = new DeterministicSeed(wallet.mnemonic, null, "", wallet.creationTime);
        wallet.lastIndex++;
        String newAddr = derivarDireccionBip44(seed.getSeedBytes(), wallet.isTestnet, wallet.lastIndex);
        wallet.currentAddress = newAddr;
        return newAddr;
    }

    /**
     * Ruta canónica en Firebase Firestore para la billetera Dogecoin del usuario.
     */
    public static String getDogeFirestorePath(String userEmail) {
        String colUsuarios = RSALogica.hashConstantToHex("usuarios");
        String userHash = RSALogica.hashConstantToHex(userEmail.trim().toLowerCase());
        String colDogeWallet = RSALogica.hashConstantToHex("billetera_doge");
        return colUsuarios + "/" + userHash + "/" + colDogeWallet;
    }

    /**
     * Verifica si el usuario ya tiene una billetera Dogecoin configurada en la red seleccionada.
     */
    public static boolean existeBilletera(String userEmail, boolean testnet) {
        if (userEmail == null || userEmail.isEmpty()) return false;

        // 1. Comprobar en Firebase Cloud Firestore
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("payload_cifrado")) {
                    String p = docSnap.getString("payload_cifrado");
                    if (p != null && !p.isEmpty()) return true;
                }
            }
        } catch (Throwable ignored) {}

        // 2. Comprobar en BD Local SQLite
        return LocalDatabaseService.existeDogeWalletActiva(userEmail, testnet);
    }

    /**
     * Guarda la billetera Dogecoin cifrada con AES-256-GCM en Firebase Firestore y réplica SQLite.
     */
    public static void guardarBilleteraCifrada(DogeWalletHolder wallet, String userEmail, char[] pin, boolean testnet) throws Exception {
        String jsonPlain = gson.toJson(wallet);
        byte[] rawBytes = jsonPlain.getBytes(StandardCharsets.UTF_8);

        String key = userEmail.trim().toLowerCase() + (testnet ? "_doge_testnet" : "_doge_mainnet");
        byte[] salt = ("DOGE_SALT_" + key).getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = cifrarBytes(rawBytes, pin, salt);

        // Capa Híbrida Asimétrica RSA-4096 (Estándar Bóveda de Contraseñas)
        byte[] finalPayload = encryptedPayload;
        PublicKey pubKey = SecuritySession.getPublicKey();
        if (pubKey != null) {
            try {
                finalPayload = RSALogica.cifrarBytesHibrido(encryptedPayload, pubKey);
            } catch (Exception ex) {
                System.err.println("[DogecoinWalletService] Nota: Error aplicando capa RSA, usando capa simétrica: " + ex.getMessage());
            }
        }

        String encoded = java.util.Base64.getEncoder().encodeToString(finalPayload);

        // Guardar exclusivamente en Firebase Cloud Firestore (Zero-Disk)
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";

                Map<String, Object> cloudData = new HashMap<>();
                cloudData.put("payload_cifrado", encoded);
                cloudData.put("red", testnet ? "TESTNET" : "MAINNET");
                cloudData.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                db.collection(path).document(docId).set(cloudData, SetOptions.merge()).get();
                System.out.println("[DogecoinWalletService] Billetera Doge guardada en Firebase Cloud Firestore (" + (testnet ? "TESTNET" : "MAINNET") + ")");
            }
        } catch (Throwable t) {
            System.err.println("[DogecoinWalletService] Error al guardar en Firebase Cloud: " + t.getMessage());
        }
    }

    /**
     * Carga y descifra la billetera Dogecoin del usuario exclusivamente desde Firebase Firestore.
     */
    public static DogeWalletHolder cargarBilleteraCifrada(String userEmail, char[] pin, boolean testnet) throws Exception {
        String key = userEmail.trim().toLowerCase() + (testnet ? "_doge_testnet" : "_doge_mainnet");
        String encoded = null;

        // Cargar desde Firebase Cloud Firestore
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists()) {
                    String cloudPayload = docSnap.getString("payload_cifrado");
                    if (cloudPayload != null && !cloudPayload.isEmpty()) {
                        encoded = cloudPayload;
                        System.out.println("[DogecoinWalletService] Billetera cargada desde Firebase Cloud Firestore.");
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[DogecoinWalletService] Error conectando con Firebase Cloud: " + t.getMessage());
        }

        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalStateException("No existe una billetera Dogecoin guardada en esta red para este usuario.");
        }

        byte[] encryptedPayload = java.util.Base64.getDecoder().decode(encoded);

        // Si el payload contiene envoltura RSA-4096, se descifra primero con la llave privada RSA de sesión
        PrivateKey privKey = SecuritySession.getPrivateKey();
        if (privKey != null) {
            try {
                encryptedPayload = RSALogica.descifrarBytesHibrido(encryptedPayload, privKey);
            } catch (Exception ignored) {
                // Retrocompatibilidad con payloads sin envoltura RSA
            }
        }

        byte[] salt = ("DOGE_SALT_" + key).getBytes(StandardCharsets.UTF_8);

        byte[] decryptedBytes = null;
        boolean rescued = false;
        try {
            decryptedBytes = descifrarBytes(encryptedPayload, pin, salt);
        } catch (Exception primaryEx) {
            // Auto-rescate si el PIN fue zeroed (\0\0\0\0)
            int pinLen = (pin != null && pin.length > 0) ? pin.length : 4;
            char[] zeroPin = new char[pinLen];
            Arrays.fill(zeroPin, (char) 0);
            try {
                decryptedBytes = descifrarBytes(encryptedPayload, zeroPin, salt);
                rescued = true;
            } catch (Exception rescueEx) {
                throw primaryEx;
            }
        }

        String jsonPlain = new String(decryptedBytes, StandardCharsets.UTF_8);
        Arrays.fill(decryptedBytes, (byte) 0);

        DogeWalletHolder holder = gson.fromJson(jsonPlain, DogeWalletHolder.class);

        if (rescued && pin != null && pin.length > 0) {
            try {
                guardarBilleteraCifrada(holder, userEmail, pin, testnet);
                System.out.println("[DogecoinWalletService] Billetera Doge reasegurada tras rescate de PIN.");
            } catch (Exception ignored) {}
        }

        return holder;
    }

    /**
     * Elimina de forma permanente la billetera Dogecoin en Firebase y en la base de datos local.
     */
    public static void eliminarBilletera(String userEmail, boolean testnet) {
        // 1. Eliminar de Firebase Cloud Firestore
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                db.collection(path).document(docId).delete().get();
                System.out.println("[DogecoinWalletService] Billetera Dogecoin eliminada de Firebase.");
            }
        } catch (Throwable t) {
            System.err.println("[DogecoinWalletService] Advertencia al eliminar en Firebase: " + t.getMessage());
        }

        // 2. Eliminar de la base de datos local SQLite
        LocalDatabaseService.eliminarDogeWalletActiva(userEmail, testnet);
    }

    // --- LIBRETA DE DIRECCIONES DOGECOIN ---

    public static List<DogeAddressEntry> cargarDireccionesEtiquetadas(String userEmail, boolean testnet) {
        List<DogeAddressEntry> list = new ArrayList<>();
        String rawJson = null;

        // 1. Intentar cargar desde Firebase Cloud Firestore
        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                var docSnap = db.collection(path).document(docId).get().get();
                if (docSnap.exists() && docSnap.contains("address_book_json")) {
                    String cloudJson = docSnap.getString("address_book_json");
                    if (cloudJson != null && !cloudJson.isEmpty() && !cloudJson.equals("[]")) {
                        rawJson = cloudJson;
                        LocalDatabaseService.guardarDogeAddressBook(userEmail, testnet, cloudJson);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Cargar desde la base de datos local SQLite
        if (rawJson == null || rawJson.trim().isEmpty() || rawJson.trim().equals("[]")) {
            rawJson = LocalDatabaseService.obtenerDogeAddressBook(userEmail, testnet);
        }

        if (rawJson != null && !rawJson.isEmpty() && !rawJson.equals("[]")) {
            try {
                JsonArray arr = JsonParser.parseString(rawJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String addr = obj.get("address").getAsString();
                    String lbl = obj.has("label") ? obj.get("label").getAsString() : "";
                    String date = obj.has("date") ? obj.get("date").getAsString() : "";
                    list.add(new DogeAddressEntry(addr, lbl, date));
                }
            } catch (Exception ex) {
                System.err.println("[DogecoinWalletService] Error cargando libreta Doge: " + ex.getMessage());
            }
        }
        return list;
    }

    public static void guardarDireccionEtiquetada(String userEmail, boolean testnet, String address, String label) {
        List<DogeAddressEntry> list = cargarDireccionesEtiquetadas(userEmail, testnet);
        boolean found = false;
        for (DogeAddressEntry entry : list) {
            if (entry.address.equalsIgnoreCase(address)) {
                entry.label = (label != null) ? label.trim() : "";
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, new DogeAddressEntry(address, label, null));
        }

        JsonArray arr = new JsonArray();
        for (DogeAddressEntry entry : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("address", entry.address);
            obj.addProperty("label", entry.label);
            obj.addProperty("date", entry.date);
            arr.add(obj);
        }
        String json = arr.toString();

        LocalDatabaseService.guardarDogeAddressBook(userEmail, testnet, json);

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("address_book_json", json);
                updateMap.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(updateMap, SetOptions.merge());
            }
        } catch (Throwable ignored) {}
    }

    public static void eliminarDireccionEtiquetada(String userEmail, boolean testnet, String address) {
        List<DogeAddressEntry> list = cargarDireccionesEtiquetadas(userEmail, testnet);
        list.removeIf(entry -> entry.address.equalsIgnoreCase(address));
        JsonArray arr = new JsonArray();
        for (DogeAddressEntry entry : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("address", entry.address);
            obj.addProperty("label", entry.label);
            obj.addProperty("date", entry.date);
            arr.add(obj);
        }
        String json = arr.toString();
        LocalDatabaseService.guardarDogeAddressBook(userEmail, testnet, json);

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db != null) {
                String path = getDogeFirestorePath(userEmail);
                String docId = testnet ? "testnet" : "mainnet";
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("address_book_json", json);
                updateMap.put("fecha_actualizacion", com.google.cloud.Timestamp.now());
                db.collection(path).document(docId).set(updateMap, SetOptions.merge());
            }
        } catch (Throwable ignored) {}
    }

    // --- CONSULTA EN VIVO DE BALANCE Y TRANSACCIONES ---

    /**
     * Consulta el balance en Koinus (1 DOGE = 100,000,000 Koinu) con redundancia multi-proveedor:
     * 1. BlockCypher API (alta velocidad y precisión directa en Koinus)
     * 2. Dogechain.info API (explorer histórico oficial)
     * 3. Blockchair API (fallback con User-Agent estándar)
     */
    public static long consultarBalanceKoinus(String address, boolean testnet) {
        if (address == null || address.isEmpty()) return 0;
        if (testnet) {
            // Dogecoin Testnet explorer fallback (retorna 0 para no romper la UI)
            return 0;
        }

        // 1. Intento con BlockCypher API
        try {
            String endpoint = "https://api.blockcypher.com/v1/doge/main/addrs/" + address + "/balance";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("final_balance")) {
                        return root.get("final_balance").getAsLong();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] BlockCypher balance error: " + e.getMessage());
        }

        // 2. Intento con Dogechain.info API
        try {
            String endpoint = "https://dogechain.info/api/v1/address/balance/" + address;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("balance")) {
                        String balStr = root.get("balance").getAsString();
                        double doge = Double.parseDouble(balStr);
                        return Math.round(doge * 100_000_000.0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] Dogechain.info balance error: " + e.getMessage());
        }

        // 3. Fallback con Blockchair
        try {
            String endpoint = "https://api.blockchair.com/dogecoin/dashboards/address/" + address;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject data = root.getAsJsonObject("data");
                    if (data.has(address)) {
                        JsonObject addrObj = data.getAsJsonObject(address);
                        JsonObject addrData = addrObj.getAsJsonObject("address");
                        return addrData.get("balance").getAsLong();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] Blockchair balance error: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Consulta transacciones recientes de la dirección Dogecoin con redundancia multi-proveedor:
     * 1. Dogechain.info (estable y con bajo rechazo de cuotas)
     * 2. BlockCypher (completo con confirmaciones y timestamps)
     * 3. Blockchair (fallback)
     */
    public static List<DogeTxInfo> consultarTransacciones(String address, boolean testnet) {
        List<DogeTxInfo> list = new ArrayList<>();
        if (address == null || address.isEmpty() || testnet) return list;

        // 1. Intento con Dogechain.info API
        try {
            String endpoint = "https://dogechain.info/api/v1/address/transactions/" + address;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("transactions")) {
                        JsonArray txs = root.getAsJsonArray("transactions");
                        for (int i = 0; i < txs.size(); i++) {
                            JsonObject tx = txs.get(i).getAsJsonObject();
                            String txid = tx.has("hash") ? tx.get("hash").getAsString() : "tx_" + i;
                            String valStr = tx.has("value") ? tx.get("value").getAsString() : "0";
                            double dogeAmt = Double.parseDouble(valStr);
                            long absAmount = Math.round(Math.abs(dogeAmt) * 100_000_000.0);
                            int blockId = tx.has("block") ? tx.get("block").getAsInt() : 0;
                            boolean confirmed = blockId > 0;
                            long time = tx.has("time") ? tx.get("time").getAsLong() : 0;
                            boolean isIncoming = dogeAmt >= 0;

                            list.add(new DogeTxInfo(txid, isIncoming, absAmount, confirmed, blockId, time));
                        }
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] Dogechain tx error: " + e.getMessage());
        }

        // 2. Intento con BlockCypher API
        try {
            String endpoint = "https://api.blockcypher.com/v1/doge/main/addrs/" + address + "?limit=25";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("txrefs")) {
                        JsonArray txs = root.getAsJsonArray("txrefs");
                        for (int i = 0; i < txs.size(); i++) {
                            JsonObject tx = txs.get(i).getAsJsonObject();
                            String txid = tx.has("tx_hash") ? tx.get("tx_hash").getAsString() : "tx_" + i;
                            long value = tx.has("value") ? tx.get("value").getAsLong() : 0;
                            int blockHeight = tx.has("block_height") ? tx.get("block_height").getAsInt() : 0;
                            int confirms = tx.has("confirmations") ? tx.get("confirmations").getAsInt() : 0;
                            boolean confirmed = confirms > 0;
                            boolean isIncoming = tx.has("tx_input_n") && tx.get("tx_input_n").getAsInt() == -1;

                            long time = 0;
                            if (tx.has("confirmed")) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                    time = sdf.parse(tx.get("confirmed").getAsString()).getTime() / 1000;
                                } catch (Exception ignored) {}
                            }
                            list.add(new DogeTxInfo(txid, isIncoming, value, confirmed, blockHeight, time));
                        }
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] BlockCypher tx error: " + e.getMessage());
        }

        // 3. Fallback con Blockchair API
        try {
            String endpoint = "https://api.blockchair.com/dogecoin/dashboards/address/" + address + "?transaction_details=true";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject data = root.getAsJsonObject("data");
                    if (data.has(address)) {
                        JsonObject addrObj = data.getAsJsonObject(address);
                        if (addrObj.has("transactions")) {
                            JsonArray txs = addrObj.getAsJsonArray("transactions");
                            for (int i = 0; i < txs.size(); i++) {
                                JsonObject tx = txs.get(i).getAsJsonObject();
                                String txid = tx.has("hash") ? tx.get("hash").getAsString() : "tx_" + i;
                                long balanceChange = tx.has("balance_change") ? tx.get("balance_change").getAsLong() : 0;
                                boolean isIncoming = balanceChange >= 0;
                                long absAmount = Math.abs(balanceChange);
                                int blockId = tx.has("block_id") && !tx.get("block_id").isJsonNull() ? tx.get("block_id").getAsInt() : 0;
                                boolean confirmed = blockId > 0;
                                long time = tx.has("time") ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tx.get("time").getAsString()).getTime() / 1000 : 0;

                                list.add(new DogeTxInfo(txid, isIncoming, absAmount, confirmed, blockId, time));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] Blockchair tx error: " + e.getMessage());
        }

        return list;
    }

    public static String koinusToDoge(long koinus) {
        if (koinus < 0) return "-- DOGE";
        double doge = (double) koinus / 100_000_000.0;
        return String.format(java.util.Locale.US, "%.4f DOGE", doge);
    }

    // --- ENVÍO DE TRANSACCIONES DOGECOIN (LEGACY P2PKH / BIP-44) ---

    /**
     * Obtiene los UTXOs (salidas no gastadas) de una dirección Dogecoin desde BlockCypher API.
     */
    public static List<DogeUtxo> obtenerUtxos(String address, boolean testnet) {
        List<DogeUtxo> list = new ArrayList<>();
        if (address == null || address.trim().isEmpty() || testnet) return list;

        try {
            String endpoint = "https://api.blockcypher.com/v1/doge/main/addrs/" + address.trim() + "?unspentOnly=true";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("txrefs")) {
                        JsonArray txs = root.getAsJsonArray("txrefs");
                        for (int i = 0; i < txs.size(); i++) {
                            JsonObject t = txs.get(i).getAsJsonObject();
                            boolean spent = t.has("spent") && t.get("spent").getAsBoolean();
                            if (!spent) {
                                DogeUtxo u = new DogeUtxo();
                                u.txid = t.get("tx_hash").getAsString();
                                u.vout = t.has("tx_output_n") ? t.get("tx_output_n").getAsInt() : 0;
                                u.value = t.get("value").getAsLong();
                                int conf = t.has("confirmations") ? t.get("confirmations").getAsInt() : 0;
                                u.confirmed = conf > 0;
                                list.add(u);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] BlockCypher utxo error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Transmite una transacción raw en hexadecimal a la red Dogecoin a través de BlockCypher y Dogechain.info.
     */
    public static String transmitirTransaccionRaw(String rawTxHex, boolean testnet) throws Exception {
        if (testnet) {
            throw new UnsupportedOperationException("El envío en red Testnet de Dogecoin requiere faucet/nodo de pruebas activo.");
        }

        // 1. Intento con BlockCypher push API
        try {
            String endpoint = "https://api.blockcypher.com/v1/doge/main/txs/push";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Blackpassword");

            JsonObject payload = new JsonObject();
            payload.addProperty("tx", rawTxHex);
            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject resp = JsonParser.parseReader(reader).getAsJsonObject();
                    if (resp.has("tx") && resp.getAsJsonObject("tx").has("hash")) {
                        return resp.getAsJsonObject("tx").get("hash").getAsString();
                    }
                }
            } else {
                try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                    JsonObject err = JsonParser.parseReader(reader).getAsJsonObject();
                    if (err.has("error")) {
                        throw new Exception(err.get("error").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DogecoinWalletService] BlockCypher push error: " + e.getMessage());
            throw e;
        }

        throw new Exception("No se recibió confirmación de envío del nodo Dogecoin.");
    }

    /**
     * Deriva la llave privada ECKey asociada a la dirección de origen Dogecoin (BIP-44 m/44'/3'/0'/0/i).
     */
    public static ECKey obtenerLlavePrivada(DogeWalletHolder wallet, String fromAddress, boolean testnet) {
        NetworkParameters params = getNetwork(testnet);
        DeterministicSeed seed = new DeterministicSeed(wallet.mnemonic, null, "", wallet.creationTime);
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
        DeterministicKey purpose44 = HDKeyDerivation.deriveChildKey(master, new ChildNumber(44, true));
        DeterministicKey coinType3 = HDKeyDerivation.deriveChildKey(purpose44, new ChildNumber(3, true));
        DeterministicKey account0 = HDKeyDerivation.deriveChildKey(coinType3, new ChildNumber(0, true));
        DeterministicKey external0 = HDKeyDerivation.deriveChildKey(account0, ChildNumber.ZERO);

        for (int i = 0; i <= Math.max(wallet.lastIndex, 30); i++) {
            DeterministicKey child = HDKeyDerivation.deriveChildKey(external0, new ChildNumber(i, false));
            LegacyAddress addr = LegacyAddress.fromKey(params, child);
            if (addr.toBase58().equalsIgnoreCase(fromAddress)) {
                return child;
            }
        }
        return HDKeyDerivation.deriveChildKey(external0, ChildNumber.ZERO);
    }

    /**
     * Construye, firma criptográficamente y transmite una transacción Dogecoin P2PKH.
     */
    public static SendResult enviarDogecoin(DogeWalletHolder wallet, String fromAddress, String toAddress, long amountKoinus, long feeKoinus, boolean testnet) {
        NetworkParameters params = getNetwork(testnet);

        // 1. Validar dirección destino
        LegacyAddress destAddr;
        try {
            destAddr = LegacyAddress.fromBase58(params, toAddress.trim());
        } catch (Exception e) {
            return new SendResult(false, null, "Dirección Dogecoin inválida para esta red: " + e.getMessage(), amountKoinus, feeKoinus);
        }

        // 2. Validar monto (mínimo 0.01 DOGE = 1,000,000 Koinu)
        if (amountKoinus < 1_000_000) {
            return new SendResult(false, null, "El monto mínimo a enviar es 0.01 DOGE.", amountKoinus, feeKoinus);
        }
        if (feeKoinus < 50_000_000) {
            feeKoinus = 100_000_000; // 1 DOGE comisión estándar
        }

        // 3. Obtener UTXOs disponibles
        List<DogeUtxo> utxos = obtenerUtxos(fromAddress, testnet);
        if (utxos.isEmpty()) {
            return new SendResult(false, null, "No hay saldo confirmado ni UTXOs disponibles en la blockchain para esta dirección. Debe recibir DOGE primero.", amountKoinus, feeKoinus);
        }

        long requiredTotal = amountKoinus + feeKoinus;
        List<DogeUtxo> selectedUtxos = new ArrayList<>();
        long totalIn = 0;

        for (DogeUtxo u : utxos) {
            selectedUtxos.add(u);
            totalIn += u.value;
            if (totalIn >= requiredTotal) break;
        }

        if (totalIn < requiredTotal) {
            return new SendResult(false, null,
                String.format(java.util.Locale.US, "Saldo insuficiente. Disponible: %.4f DOGE. Requerido (monto + comisión): %.4f DOGE.",
                    (double) totalIn / 100_000_000.0, (double) requiredTotal / 100_000_000.0),
                amountKoinus, feeKoinus);
        }

        // 4. Obtener clave privada
        ECKey key = obtenerLlavePrivada(wallet, fromAddress, testnet);
        if (key == null) {
            return new SendResult(false, null, "No se pudo derivar la llave privada Dogecoin para firmar el envío.", amountKoinus, feeKoinus);
        }

        try {
            // 5. Construir Transacción
            Transaction tx = new Transaction(params);

            // Destino
            tx.addOutput(Coin.valueOf(amountKoinus), destAddr);

            // Cambio (vuelto)
            long change = totalIn - amountKoinus - feeKoinus;
            LegacyAddress changeAddr = LegacyAddress.fromBase58(params, fromAddress);
            if (change > 1_000_000) {
                tx.addOutput(Coin.valueOf(change), changeAddr);
            }

            // Inputs
            Script scriptPubKey = ScriptBuilder.createOutputScript(changeAddr);
            for (DogeUtxo u : selectedUtxos) {
                Sha256Hash prevHash = Sha256Hash.wrap(u.txid);
                tx.addInput(prevHash, u.vout, scriptPubKey);
            }

            // Firmar inputs (P2PKH)
            for (int i = 0; i < selectedUtxos.size(); i++) {
                TransactionSignature sig = tx.calculateSignature(i, key, scriptPubKey, Transaction.SigHash.ALL, false);
                Script inputScript = ScriptBuilder.createInputScript(sig, key);
                tx.getInput(i).setScriptSig(inputScript);
            }

            // 6. Serializar a hex
            String rawTxHex = Hex.toHexString(tx.bitcoinSerialize());

            // 7. Transmitir a la red Dogecoin
            String txid = transmitirTransaccionRaw(rawTxHex, testnet);
            return new SendResult(true, txid, null, amountKoinus, feeKoinus);

        } catch (Exception ex) {
            return new SendResult(false, null, "Error al firmar o transmitir transacción Dogecoin: " + ex.getMessage(), amountKoinus, feeKoinus);
        }
    }

    // --- Helpers Criptográficos AES-256 GCM con PBKDF2 ---

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
        if (data.length < 12) throw new IllegalArgumentException("Datos corruptos de billetera Doge.");
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] enc = Arrays.copyOfRange(data, 12, data.length);
        SecretKeySpec key = RSALogica.derivarLlavePBKDF2(pin, salt, 600000);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(enc);
    }
}
