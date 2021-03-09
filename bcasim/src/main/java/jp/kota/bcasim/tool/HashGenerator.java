package jp.kota.bcasim.tool;

import java.security.MessageDigest;

public class HashGenerator {
	
	private final static String MD2 = "MD2";
	private final static String MD5 = "MD5";
	private final static String SHA_1 = "SHA-1";
	private final static String SHA_256 = "SHA-256";
	private final static String SHA_384 = "SHA-384";
	private final static String SHA_512 = "SHA-512";
	
	
	public static String generateHash(String data) {
		MessageDigest md = null;
		StringBuilder sb = null;
		try {
	        md = MessageDigest.getInstance(SHA_256);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }	
		md.update(data.getBytes());
	    sb = new StringBuilder();
	    for (byte b : md.digest()) {
	        String hex = String.format("%02x", b);
	        sb.append(hex);
	    }
	    return sb.toString();
		
	}
	
}
