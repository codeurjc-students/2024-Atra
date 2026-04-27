package codeurjc_students.atra.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * POJO providing encrypting and decrypting functionalities.
 */
public class SecurityCipher {

	private static final Logger logger =
			LoggerFactory.getLogger(SecurityCipher.class);

	private static final SecretKeySpec secretKey;

	static {
		try {
			byte[] key = "secureCDCKey".getBytes(StandardCharsets.UTF_8);
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed to initialize encryption key: {}", e.getMessage());
			throw new RuntimeException("Failed to initialize SecurityCipher", e);
		}
	}

	private SecurityCipher() {
		throw new AssertionError("Static!");
	}

	public static String encrypt(String strToEncrypt) {
		if (strToEncrypt == null) {
			return null;
		}

		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			logger.error("An exception ocurred during JWT encryption: {}", e.getMessage());
		}
		return null;
	}

	public static String decrypt(String strToDecrypt) {
		if (strToDecrypt == null) {
			return null;
		}

		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
 		} catch (Exception e) {
			logger.error("An exception ocurred during JWT decryption: {}", e.getMessage());
			logger.error("Decryption failed for input: '{}' (length: {}, decoded bytes length: {})", strToDecrypt, strToDecrypt.length(), Base64.getDecoder().decode(strToDecrypt).length, e);

		}
		return null;
	}

}
