import java.util.Random;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * AES256 encrypter for 1 file using 1 PASSWORD + 1 SALT to create 1 KEY used for subsequent calls to encrypt() method.
 * 
 * @author <a href="mailto:olaf@merkert.de">Olaf Merkert</a>
 */
public class AESEncrypter {

    public static final int KEY_SIZE_BIT = 256;

    public static final int KEY_SIZE_BYTE = KEY_SIZE_BIT / 8;

    public static final int ITERATION_COUNT = 1000;

    // --------------------------------------------------------------------------

    protected byte[] salt;

    protected byte[] encryptionKey;

    protected byte[] authenticationCode;

    protected byte[] pwVerification;

    protected CipherParameters cipherParameters;

    protected SICBlockCipher aesCipher;

    protected int blockSize;

    protected int nonce;

    protected HMac mac;

    /**
     * Setup AES encryption based on pwBytes using WinZipAES approach with SALT and pwVerification bytes based on
     * password+salt.
     */
    public AESEncrypter(byte[] pwBytes) {
	PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
	this.salt = createSalt();
	generator.init(pwBytes, salt, ITERATION_COUNT);

	// create 2 byte[16] for two keys and one byte[2] for pwVerification
	// 1. encryption / 2. athentication (via HMAC/hash) /
	cipherParameters = generator.generateDerivedParameters(KEY_SIZE_BIT * 2 + 16);
	byte[] keyBytes = ((KeyParameter) cipherParameters).getKey();

	this.encryptionKey = new byte[KEY_SIZE_BYTE];
	System.arraycopy(keyBytes, 0, encryptionKey, 0, KEY_SIZE_BYTE);

	this.authenticationCode = new byte[KEY_SIZE_BYTE];
	System.arraycopy(keyBytes, KEY_SIZE_BYTE, authenticationCode, 0, KEY_SIZE_BYTE);

	// based on SALT + PASSWORD (password is probably correct)
	this.pwVerification = new byte[2];
	System.arraycopy(keyBytes, KEY_SIZE_BYTE * 2, pwVerification, 0, 2);

	// create the first 16 bytes of the key sequence again (using pw+salt)
	generator.init(pwBytes, salt, ITERATION_COUNT);
	cipherParameters = generator.generateDerivedParameters(KEY_SIZE_BIT);

	// checksum added to the end of the encrypted data, update on each encryption call
	this.mac = new HMac(new SHA1Digest());
	mac.init(new KeyParameter(authenticationCode));

	this.aesCipher = new SICBlockCipher(new AESEngine());
	this.blockSize = aesCipher.getBlockSize();

	// incremented on each 16 byte block and used as encryption NONCE (ivBytes)
	nonce = 1;
    }

    /**
     * perform pseudo "in-place" encryption
     */
    public void encrypt(byte[] in, int length) {
	int pos = 0;
	while (pos < in.length && pos < length) {
	    encryptBlock(in, pos, length);
	    pos += blockSize;
	}
    }

    /**
     * encrypt 16 bytes (AES standard block size) or less starting at "pos" within "in" byte[]
     */
    public void encryptBlock(byte[] in, int pos, int length) {
	byte[] encryptedIn = new byte[blockSize];
	byte[] ivBytes = ByteArrayHelper.toLEByteArray(nonce++, 16);
	ParametersWithIV ivParams = new ParametersWithIV(cipherParameters, ivBytes);
	aesCipher.init(true, ivParams);

	int remainingCount = length - pos;
	if (remainingCount >= blockSize) {
	    aesCipher.processBlock(in, pos, encryptedIn, 0);
	    System.arraycopy(encryptedIn, 0, in, pos, blockSize);
	    mac.update(encryptedIn, 0, blockSize);
	} else {
	    byte[] extendedIn = new byte[blockSize];
	    System.arraycopy(in, pos, extendedIn, 0, remainingCount);
	    aesCipher.processBlock(extendedIn, 0, encryptedIn, 0);
	    System.arraycopy(encryptedIn, 0, in, pos, remainingCount);
	    mac.update(encryptedIn, 0, remainingCount);
	}
    }

    /** 16 bytes (AES-256) set in constructor */
    public byte[] getSalt() {
	return salt;
    }

    /** 2 bytes for password verification set in constructor */
    public byte[] getPwVerification() {
	return pwVerification;
    }

    /** 10 bytes */
    public byte[] getFinalAuthentication() {
	// MAC / based on encIn + PASSWORD + SALT (encryption was successful)
	byte[] macBytes = new byte[mac.getMacSize()];
	mac.doFinal(macBytes, 0);
	byte[] macBytes10 = new byte[10];
	System.arraycopy(macBytes, 0, macBytes10, 0, 10);
	return macBytes10;
    }

    // --------------------------------------------------------------------------

    /**
     * create 16 bytes salt by using each 4 bytes of 2 random 32 bit numbers
     */
    protected static byte[] createSalt() {
	byte[] salt = new byte[16];
	for (int j = 0; j < 2; j++) {
	    Random rand = new Random();
	    int i = rand.nextInt();
	    salt[0 + j * 4] = (byte) (i >> 24);
	    salt[1 + j * 4] = (byte) (i >> 16);
	    salt[2 + j * 4] = (byte) (i >> 8);
	    salt[3 + j * 4] = (byte) i;
	}
	return salt;
    }

}
