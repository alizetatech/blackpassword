package com.mycompany.blackpassword.services;

import org.bitcoinj.core.Block;
import org.bitcoinj.params.AbstractBitcoinNetParams;

/**
 * Parámetros de red para Dogecoin Mainnet (Producción).
 * - Prefijo P2PKH: 30 (0x1E) -> Direcciones que comienzan con 'D'.
 * - Prefijo P2SH: 22 (0x16) -> Direcciones que comienzan con '9' o 'A'.
 * - Prefijo WIF Private Key: 158 (0x9E).
 * - URI Scheme: dogecoin.
 */
public class DogecoinMainNetParams extends AbstractBitcoinNetParams {

    private static DogecoinMainNetParams instance;

    public DogecoinMainNetParams() {
        super();
        this.id = "org.dogecoin.production";
        this.addressHeader = 30; // 0x1E -> Inicia con 'D'
        this.p2shHeader = 22;    // 0x16 -> Inicia con '9' o 'A'
        this.dumpedPrivateKeyHeader = 158; // 0x9E
        this.segwitAddressHrp = null; // Dogecoin opera en Legacy P2PKH / P2SH
        this.port = 22556;
        this.packetMagic = 0xc0c0c0c0L;
        this.bip32HeaderP2PKHpub = 0x02facafd;
        this.bip32HeaderP2PKHpriv = 0x02fac398;
    }

    public static synchronized DogecoinMainNetParams get() {
        if (instance == null) {
            instance = new DogecoinMainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return "main";
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
