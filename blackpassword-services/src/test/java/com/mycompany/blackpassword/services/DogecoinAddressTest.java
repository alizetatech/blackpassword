package com.mycompany.blackpassword.services;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DogecoinAddressTest {

    @Test
    public void testDogecoinMainnetAddressPrefix() {
        ECKey key = new ECKey();
        LegacyAddress address = LegacyAddress.fromKey(DogecoinMainNetParams.get(), key);
        String addrStr = address.toBase58();
        System.out.println("Generated Dogecoin Mainnet Address: " + addrStr);
        assertNotNull(addrStr);
        assertTrue(addrStr.startsWith("D"), "Dogecoin Mainnet address must start with 'D', got: " + addrStr);
        assertEquals(34, addrStr.length());
    }

    @Test
    public void testDogecoinTestnetAddressPrefix() {
        ECKey key = new ECKey();
        LegacyAddress address = LegacyAddress.fromKey(DogecoinTestNetParams.get(), key);
        String addrStr = address.toBase58();
        System.out.println("Generated Dogecoin Testnet Address: " + addrStr);
        assertNotNull(addrStr);
        assertTrue(addrStr.startsWith("n") || addrStr.startsWith("m"), "Dogecoin Testnet address must start with 'n' or 'm', got: " + addrStr);
    }

    @Test
    public void testDogecoinBip44Derivation() throws Exception {
        java.security.SecureRandom random = new java.security.SecureRandom();
        org.bitcoinj.wallet.DeterministicSeed seed = new org.bitcoinj.wallet.DeterministicSeed(random, 128, "");
        java.util.List<String> mnemonic = seed.getMnemonicCode();
        assertNotNull(mnemonic);
        assertEquals(12, mnemonic.size());

        org.bitcoinj.crypto.DeterministicKey masterKey = org.bitcoinj.crypto.HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
        org.bitcoinj.crypto.DeterministicKey purpose44 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(masterKey, new org.bitcoinj.crypto.ChildNumber(44, true));
        org.bitcoinj.crypto.DeterministicKey coinType3 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(purpose44, new org.bitcoinj.crypto.ChildNumber(3, true));
        org.bitcoinj.crypto.DeterministicKey account0 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(coinType3, new org.bitcoinj.crypto.ChildNumber(0, true));
        org.bitcoinj.crypto.DeterministicKey external0 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(account0, org.bitcoinj.crypto.ChildNumber.ZERO);
        org.bitcoinj.crypto.DeterministicKey addressKey0 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(external0, org.bitcoinj.crypto.ChildNumber.ZERO);

        LegacyAddress dogeAddr0 = LegacyAddress.fromKey(DogecoinMainNetParams.get(), addressKey0);
        System.out.println("BIP-44 m/44'/3'/0'/0/0 Dogecoin Address: " + dogeAddr0.toBase58());
        assertTrue(dogeAddr0.toBase58().startsWith("D"));

        org.bitcoinj.crypto.DeterministicKey addressKey1 = org.bitcoinj.crypto.HDKeyDerivation.deriveChildKey(external0, new org.bitcoinj.crypto.ChildNumber(1, false));
        LegacyAddress dogeAddr1 = LegacyAddress.fromKey(DogecoinMainNetParams.get(), addressKey1);
        System.out.println("BIP-44 m/44'/3'/0'/0/1 Dogecoin Address: " + dogeAddr1.toBase58());
        assertTrue(dogeAddr1.toBase58().startsWith("D"));
        assertNotEquals(dogeAddr0.toBase58(), dogeAddr1.toBase58());
    }

    @Test
    public void testCrearYFirmarTxDoge() throws Exception {
        ECKey key = new ECKey();
        LegacyAddress fromAddr = LegacyAddress.fromKey(DogecoinMainNetParams.get(), key);
        LegacyAddress toAddr = LegacyAddress.fromBase58(DogecoinMainNetParams.get(), "DH5yaieqoZN36fDVciNyRueRGvGLR3mr7L");

        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(DogecoinMainNetParams.get());
        tx.addOutput(org.bitcoinj.core.Coin.valueOf(100_000_000L), toAddr);

        org.bitcoinj.core.TransactionOutPoint outPoint = new org.bitcoinj.core.TransactionOutPoint(
                DogecoinMainNetParams.get(), 0, org.bitcoinj.core.Sha256Hash.wrap("484c94145e97c0b2dd96912c1f056c9e05c8d358ab96ea429c51039d75d0d3c4")
        );
        org.bitcoinj.script.Script scriptPubKey = org.bitcoinj.script.ScriptBuilder.createOutputScript(fromAddr);
        tx.addInput(outPoint.getHash(), outPoint.getIndex(), scriptPubKey);

        org.bitcoinj.crypto.TransactionSignature sig = tx.calculateSignature(0, key, scriptPubKey, org.bitcoinj.core.Transaction.SigHash.ALL, false);
        org.bitcoinj.script.Script inputScript = org.bitcoinj.script.ScriptBuilder.createInputScript(sig, key);
        tx.getInput(0).setScriptSig(inputScript);

        String hex = org.bouncycastle.util.encoders.Hex.toHexString(tx.bitcoinSerialize());
        assertNotNull(hex);
        System.out.println("Dogecoin raw tx hex: " + hex);
    }

    @Test
    public void testCrearYFirmarTxBtcSegwit() throws Exception {
        org.bitcoinj.params.TestNet3Params params = org.bitcoinj.params.TestNet3Params.get();
        ECKey key = new ECKey();
        org.bitcoinj.core.SegwitAddress fromAddr = org.bitcoinj.core.SegwitAddress.fromKey(params, key);
        org.bitcoinj.core.Address toAddr = org.bitcoinj.core.Address.fromString(params, "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx");

        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(params);
        tx.addOutput(org.bitcoinj.core.Coin.valueOf(50_000L), toAddr);

        org.bitcoinj.core.Sha256Hash prevHash = org.bitcoinj.core.Sha256Hash.wrap("484c94145e97c0b2dd96912c1f056c9e05c8d358ab96ea429c51039d75d0d3c4");
        tx.addInput(prevHash, 0, new org.bitcoinj.script.Script(new byte[0]));

        org.bitcoinj.script.Script scriptCode = org.bitcoinj.script.ScriptBuilder.createP2PKHOutputScript(key);
        org.bitcoinj.crypto.TransactionSignature sig = tx.calculateWitnessSignature(
                0, key, scriptCode, org.bitcoinj.core.Coin.valueOf(100_000L), org.bitcoinj.core.Transaction.SigHash.ALL, false
        );

        org.bitcoinj.core.TransactionWitness witness = new org.bitcoinj.core.TransactionWitness(2);
        witness.setPush(0, sig.encodeToBitcoin());
        witness.setPush(1, key.getPubKey());
        tx.getInput(0).setWitness(witness);

        String hex = org.bouncycastle.util.encoders.Hex.toHexString(tx.bitcoinSerialize());
        assertNotNull(hex);
        System.out.println("Bitcoin SegWit raw tx hex: " + hex);
    }
}
