package com.example.InteractiveDesignBackend.Entity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HtmlEncryptor {

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String IV = "1234567890123456";

	public static String encrypt(String plainText, String key) throws Exception {
		if (key.length() != 32)
			throw new IllegalArgumentException("Key must be 32 chars for AES-256");

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
		byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
		return Base64.getEncoder().encodeToString(encrypted);
	}
}
