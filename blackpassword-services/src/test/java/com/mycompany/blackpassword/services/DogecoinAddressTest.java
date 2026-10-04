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
}
