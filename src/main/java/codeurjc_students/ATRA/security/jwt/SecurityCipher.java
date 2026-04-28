package codeurjc_students.atra.security.jwt;

import codeurjc_students.atra.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * POJO providing encrypting and decrypting functionalities using AES-GCM authenticated encryption.
 * GCM provides both confidentiality and integrity protection, making it more secure than ECB/PKCS5Padding.
 */
public class SecurityCipher {

	private static final Logger logger = LoggerFactory.getLogger(SecurityCipher.class);
	private static final SecretKeySpec secretKey;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final int IV_LENGTH = 12;  // GCM nonce length (96 bits recommended)
	private static final int TAG_LENGTH = 128;  // Authentication tag length in bits

	static {
		try {
			byte[] key = "secureCDCKey".getBytes(StandardCharsets.UTF_8);
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed to initialize encryption key: ", e);
			throw new CustomException("Failed to initialize SecurityCipher", e);
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
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);  // Generate random IV
			GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
			byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
			
			// Prepend IV to encrypted data
			byte[] ivAndEncrypted = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
			System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length);
			
			return Base64.getEncoder().encodeToString(ivAndEncrypted);
		} catch (Exception e) {
			logger.error("An exception occurred during JWT encryption: {}", e.getMessage());
		}
		return null;
	}

	public static String decrypt(String strToDecrypt) {
		if (strToDecrypt == null) {
			return null;
		}

		try {
			byte[] ivAndEncrypted = Base64.getDecoder().decode(strToDecrypt);
			if (ivAndEncrypted.length < IV_LENGTH) {
				throw new IllegalArgumentException("Invalid encrypted data length");
			}
			
			// Extract IV from the beginning of the encrypted data
			byte[] iv = Arrays.copyOfRange(ivAndEncrypted, 0, IV_LENGTH);
			byte[] encrypted = Arrays.copyOfRange(ivAndEncrypted, IV_LENGTH, ivAndEncrypted.length);
			
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
			byte[] decrypted = cipher.doFinal(encrypted);
			
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.error("An exception occurred during JWT decryption: {}", e.getMessage());
		}
		return null;
	}

}
