/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.avatar.js.crypto;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.oracle.avatar.js.buffer.HexUtils;

public final class DiffieHellman {

    private static final BigInteger BI_24 = BigInteger.valueOf(24);
    private static final BigInteger BI_11 = BigInteger.valueOf(11);
    private static final BigInteger BI_12 = BigInteger.valueOf(12);
    private static final BigInteger BI_5 = BigInteger.valueOf(5);
    private static final BigInteger BI_2 = BigInteger.valueOf(2);
    private static final Random rand = new Random();
    // Some primes to reuse to speedup Prime generation.
    private static final String PRIME_256 = "00fa86c08f5d4fbb7a7b8cc50f911bc592d4cb48d2ff2afa5fb897dde499f488a3";
    private static final String PRIME_512 =
            "00970281119a1002efa1082d7435d40b38568dbf3c29359ddac282636f76b4969a89a7fd8392668735ffffb65e1609cf9ae9819f1758805d50922198788a8af253";
    private static final String PRIME_1024 =
            "00a43609eb16ff094f30f4d2dba050c4363e496746e4b92f4983b337a1fc3ba2e322bf2ccf7284fd5184afa51aa9d79ce0df40097b3d51fe530521302ed8f77deb72"
            + "c2a4e959718353bb6a4b913d25d6e5c5657c30a49ee3f86929bfc86ed4bd6279814ca73ff563103da2e67d713ea252e4cf4d193a9032497a331f868faeaf03";
    private static final Map<Integer, BigInteger> PRIMES = new HashMap<>();

    static {
        PRIMES.put(256, new BigInteger(1, HexUtils.decode(PRIME_256)));
        PRIMES.put(512, new BigInteger(1, HexUtils.decode(PRIME_512)));
        PRIMES.put(1024, new BigInteger(1, HexUtils.decode(PRIME_1024)));
    }
    /*
     * RFC 2412 (groups 1 and 2)
     * RFC 3526 (groups 5, 14, 15, 16, 17 and 18)
     */
    private static final String group_modp1 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF";
    private static final String group_modp2 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381"
            + "FFFFFFFFFFFFFFFF";
    private static final String group_modp5 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";
    private static final String group_modp14 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
            + "15728E5A8AACAA68FFFFFFFFFFFFFFFF";
    private static final String group_modp15 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
            + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
            + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
            + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
            + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
            + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
            + "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF";
    private static final String group_modp16 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
            + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
            + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
            + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
            + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
            + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
            + "43DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D7"
            + "88719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA"
            + "2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6"
            + "287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED"
            + "1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA9"
            + "93B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934063199"
            + "FFFFFFFFFFFFFFFF";
    private static final String group_modp17 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08"
            + "8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B"
            + "302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9"
            + "A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6"
            + "49286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8"
            + "FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C"
            + "180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF695581718"
            + "3995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D"
            + "04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7D"
            + "B3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D226"
            + "1AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
            + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFC"
            + "E0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B26"
            + "99C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB"
            + "04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2"
            + "233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127"
            + "D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934028492"
            + "36C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406"
            + "AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918"
            + "DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B33205151"
            + "2BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03"
            + "F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97F"
            + "BEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AA"
            + "CC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58B"
            + "B7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632"
            + "387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E"
            + "6DCC4024FFFFFFFFFFFFFFFF";
    private static final String group_modp18 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
            + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
            + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
            + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
            + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
            + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
            + "43DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D7"
            + "88719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA"
            + "2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6"
            + "287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED"
            + "1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA9"
            + "93B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934028492"
            + "36C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BD"
            + "F8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831"
            + "179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1B"
            + "DB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF"
            + "5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6"
            + "D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F3"
            + "23A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AA"
            + "CC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE328"
            + "06A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55C"
            + "DA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE"
            + "12BF2D5B0B7474D6E694F91E6DBE115974A3926F12FEE5E4"
            + "38777CB6A932DF8CD8BEC4D073B931BA3BC832B68D9DD300"
            + "741FA7BF8AFC47ED2576F6936BA424663AAB639C5AE4F568"
            + "3423B4742BF1C978238F16CBE39D652DE3FDB8BEFC848AD9"
            + "22222E04A4037C0713EB57A81A23F0C73473FC646CEA306B"
            + "4BCBC8862F8385DDFA9D4B7FA2C087E879683303ED5BDD3A"
            + "062B3CF5B3A278A66D2A13F83F44F82DDF310EE074AB6A36"
            + "4597E899A0255DC164F31CC50846851DF9AB48195DED7EA1"
            + "B1D510BD7EE74D73FAF36BC31ECFA268359046F4EB879F92"
            + "4009438B481C6CD7889A002ED5EE382BC9190DA6FC026E47"
            + "9558E4475677E9AA9E3050E2765694DFC81F56E880B96E71"
            + "60C980DD98EDD3DFFFFFFFFFFFFFFFFF";
    private static final Map<String, BigInteger> WELL_KNOWN_GROUPS = new HashMap<>();

    static {
        WELL_KNOWN_GROUPS.put("modp1", new BigInteger(1, HexUtils.decode(group_modp1)));
        WELL_KNOWN_GROUPS.put("modp2", new BigInteger(1, HexUtils.decode(group_modp2)));
        WELL_KNOWN_GROUPS.put("modp5", new BigInteger(1, HexUtils.decode(group_modp5)));
        WELL_KNOWN_GROUPS.put("modp14", new BigInteger(1, HexUtils.decode(group_modp14)));
        WELL_KNOWN_GROUPS.put("modp15", new BigInteger(1, HexUtils.decode(group_modp15)));
        WELL_KNOWN_GROUPS.put("modp16", new BigInteger(1, HexUtils.decode(group_modp16)));
        WELL_KNOWN_GROUPS.put("modp17", new BigInteger(1, HexUtils.decode(group_modp17)));
        WELL_KNOWN_GROUPS.put("modp18", new BigInteger(1, HexUtils.decode(group_modp18)));
    }

    public static void checkPrime(BigInteger prime) throws Exception {
        // Is it a known Prime?
        for(BigInteger bi : PRIMES.values()) {
            if (bi.equals(prime)) {
                return;
            }
        }
        checkSafePrimeForGenerator2(prime);
    }

    public static BigInteger getKnownGroup(String name) throws Exception {
        BigInteger group = WELL_KNOWN_GROUPS.get(name);
        if (group == null) {
            throw new Exception("Unknown group " + name);
        }
        return group;
    }

    private static void checkSafePrimeForGenerator2(BigInteger bi) throws Exception {
        // This is a property of Prime numbers with 2 generator expected by DH.
        // This is what makes https://github.com/joyent/node/issues/2338
        if (!bi.mod(BI_24).equals(BI_11)) {
            throw new Exception("Invalid Prime for 2 generator");
        }

        boolean isPrime = bi.isProbablePrime(Integer.MAX_VALUE);
        if (!isPrime) {
            throw new Exception("Not a prime");
        }
        // Check (prime/2) - 1 is a prime, this is what makes a prime a safe prime;
        BigInteger q = bi.shiftRight(1);
        boolean isSafePrime = q.isProbablePrime(Integer.MAX_VALUE);
        if (!isSafePrime) {
            throw new Exception("Not a safe prime");
        }
    }

    /*
     * This generation first lookups wellknown Primes
     * then fallback on computing one.
     * Returning the same prime is fine, randomness is added when generating
     * private and public keys.
     * Newly computed primes are reused.
     *
     * XXX If a better algorithm is found, then reuse of prime
     * can be removed.
     */
    public static BigInteger generateSafePrime(int key_size) {
        BigInteger bi = PRIMES.get(key_size);
        if (bi == null) {
            bi = generateSafePrimeForGenerator2(key_size);
            PRIMES.put(key_size, bi);
        }
        return bi;
    }

    /*
     * From RFC rfc4419, seems that we could do better.
     * The prime generation is not done in the critical path. People are doing
     * it offline, it is known has taking time.
     * People seems to use DiffieHellman group much more often than
     * generating prime.
     */
    private static BigInteger generateSafePrimeForGenerator2(int key_length) {
        BigInteger p = null;
        int len = key_length - 1;
        do {
            BigInteger q = null;
            do {
                q = BigInteger.probablePrime(len, rand);
            } while (!q.mod(BI_12).equals(BI_5));
            p = q.multiply(BI_2).add(BigInteger.ONE);
        } while (!p.isProbablePrime(Integer.MAX_VALUE));
        return p;
    }
}
