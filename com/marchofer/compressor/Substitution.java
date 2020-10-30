package com.marchofer.compressor;

import java.util.ArrayList;
import java.util.Arrays;

public class Substitution {
    private static boolean debug1 = false;
    private static boolean debug2 = false;

    static String decompress(byte[] output) {
        StringBuilder outputBuilder = new StringBuilder();
        int index = 0;
        for (int i = 0; i < output.length; i++) {
            if (((int)output[i] & 0B10000000) == 0B00000000) {
                outputBuilder.append((char)output[i]);
                if (debug2) System.out.println("<" +(char)output[i] + ">");
            } else {
                byte firstByte = output[i];
                byte secondByte = output[i + 1];
                firstByte &= 0B01111111;
                int offset = (firstByte <<3) + (secondByte >> 5 & 0B00000111);
                int length = secondByte & 0B00011111;
                try {
                    if (debug2) System.out.println(i);
                    if (debug2) System.out.println(index);
                    if (debug2) System.out.println(offset);
                    if (debug2) System.out.println(index -  offset);
                    if (debug2) System.out.println(length);
                    String substring = outputBuilder.toString().substring(index - offset, index - offset + length);
                    if (debug2) System.out.println("<" + substring + ">");
                    outputBuilder.append(substring);
                } catch (StringIndexOutOfBoundsException e) {
                    if (debug2) System.err.println(e);
                }
                index += length - 1;
                i++;
            }
            index++;
            if (debug2) System.out.println();
        }
        return outputBuilder.toString();
    }

    static byte[] compress(char[] input) {
        ArrayList<Byte> tempArray = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            if (debug1) System.out.println(i);
            int currentChar = (int) input[i];
            if (debug1) System.out.println(input[i]);
            currentChar = currentChar & 0B01111111;
            int start = 0;
            if (i >= 1023) start = i - 1023;
            if (i != 0 && i + 1 < input.length) {
                boolean search = true;
                int offset = 0;
                int length = 1;
                while (search) {
                    search = false;
                    length++;
                    if (length > 31) break;
                    char[] searchArray = Arrays.copyOfRange(input, i, i + length);
                    if (debug1) System.out.println(Arrays.toString(searchArray));
                    for (int j = start; j < i - length + 1; j++) {
                        //System.out.println(Arrays.copyOfRange(input, j, j + length));
                        if (Arrays.equals(Arrays.copyOfRange(input, j, j + length), searchArray)) {
                            if (debug1) System.out.println(">>>" + new String(input).substring(j, j + length));
                            offset = j;
                            search = true;
                            break;
                        }
                    }
                }
                if (debug1) System.out.println(i-start);
                if (debug1) System.out.println(offset);
                offset = i - offset;
                length--;
                if (length != 1) {
                    if (debug1) System.out.println("Length: " + length + ", Offset: " + offset);
                    i += length - 1;
                    byte firstByte = (byte) 0B10000000;
                    firstByte |= (byte) (offset >> 3);
                    byte secondByte = (byte) ((offset << 5) & 0xFF);
                    secondByte |= (byte) length;
                    tempArray.add(firstByte);
                    tempArray.add(secondByte);
                } else {
                    tempArray.add((byte)currentChar);
                }
            } else {
                tempArray.add((byte)currentChar);
            }
        }
        byte[] output = new byte[tempArray.size()];
        for (int i = 0; i < tempArray.size(); i++) {
            output[i] = tempArray.get(i);
        }
        return output;
    }
}
