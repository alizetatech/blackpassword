package com.mycompany.blackpassword.services;

import org.bitcoinj.core.Block;
import org.bitcoinj.params.AbstractBitcoinNetParams;

/**
 * Parámetros de red para Dogecoin Testnet (Pruebas).
 * - Prefijo P2PKH: 113 (0x71) -> Direcciones que comienzan con 'n'.
 * - Prefijo P2SH: 196 (0xC4) -> Direcciones que comienzan con '2'.
 * - Prefijo WIF Private Key: 241 (0xF1).
 * - URI Scheme: dogecoin.
 */
public class DogecoinTestNetParams extends AbstractBitcoinNetParams {

    private static DogecoinTestNetParams instance;

    public DogecoinTestNetParams() {
        super();
        this.id = "org.dogecoin.test";
        this.addressHeader = 113; // 0x71 -> Inicia con 'n'
        this.p2shHeader = 196;    // 0xC4 -> Inicia con '2'
        this.dumpedPrivateKeyHeader = 241; // 0xF1
        this.segwitAddressHrp = null;
        this.port = 44556;
        this.packetMagic = 0xfcc1b7dcL;
        this.bip32HeaderP2PKHpub = 0x043587cf;
        this.bip32HeaderP2PKHpriv = 0x04358394;
    }

    public static synchronized DogecoinTestNetParams get() {
        if (instance == null) {
            instance = new DogecoinTestNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return "test";
    }

    @Override
    public Block getGenesisBlock() {
        return null;
    }

    @Override
    public String getUriScheme() {
        return "dogecoin";
    }
}
