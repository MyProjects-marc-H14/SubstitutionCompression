package com.marchofer.compressor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Compressor {
    public static void main(String[] args) throws URISyntaxException, IOException, NoSuchAlgorithmException {
        String content = "TestTestTestTestTest";
        compress(content);
    }

    private static void compress(String content) throws NoSuchAlgorithmException {
        char[] contentArray = content.toCharArray();
        for (int i = 0; i < contentArray.length; i++) {
            if ((int)contentArray[i] > 127) contentArray[i] = (char) 0;
        }
        content = new String(contentArray);
        long start = System.currentTimeMillis();
        System.out.println("Raw size:          " + (content.length() * 7) / 8 + " bytes");
        byte[] output = Substitution.compress(content.toCharArray());
        long compress = System.currentTimeMillis();
        System.out.println("Compressed size:   " + output.length + " bytes");
        System.out.println("Compression ratio: " + (float)output.length / ((content.length() * 7) / 8) * 100 + "%");
        String decompressed = Substitution.decompress(output);
        long decompress = System.currentTimeMillis();
        System.out.println("Match:             " + content.equals(decompressed));
        long match = System.currentTimeMillis();

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(content.getBytes());
        System.out.println("Original Hash:     " + bytesToHex(messageDigest.digest()));
        messageDigest.update(decompressed.getBytes());
        System.out.println("Decompressed Hash: " + bytesToHex(messageDigest.digest()));
        System.out.println("Duration:");
        System.out.println("    Compression:   " + (compress - start) + " ms");
        System.out.println("    Decompression: " + (decompress - compress) + " ms");
        System.out.println("    Match:         " + (match - decompress) + " ms");
        System.out.println("Speed:");
        System.out.println("    Compression:   " + (float)(compress - start)/((content.length() * 7) / 8) + " ms/byte");
        System.out.println("    Decompression: " + (float)(decompress - compress)/(output.length) + " ms/byte");
        System.out.println("    Match:         " + (float)(match - decompress)/((content.length() * 7) / 8) + " ms/byte");
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
