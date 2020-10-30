package com.marchofer.compressor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class SubstitutionNew {
    private static boolean debug1 = false;
    private static boolean debug2 = false;

    private static int offsetSize = 32;
    private static int lengthSize = 7;

    public static void main(String[] args) {
        ArrayList<Byte> tempArray = new ArrayList<>();
        int offset = 0b11100011100011100011100010100011;
        int length = 0b1111001;
        int bitIndex = 3;
        tempArray.add((byte) 0b11100000);
        byte[] output = new byte[(int) ((offsetSize + lengthSize) / 8 + Math.signum((offsetSize + lengthSize) % 8))];
        output[0] = (byte) 0b10000000;
        output[0] |= (byte) ((offset) >>> offsetSize - 7);
        for (int j = 1; j < (int) ((offsetSize + 1) / 8 + Math.signum((offsetSize + 1) % 8)); j++) {
            int shift = offsetSize - 7 - 8 * j;
            if (shift >= 0) {
                output[j] |= ((offset) >>> shift);
            } else {
                output[j] |= ((offset) << -shift);
            }
        }
        for (int j = (int) ((offsetSize + 1) / 8 + Math.signum((offsetSize + 1) % 8) - 1); j < output.length; j++) {
            int shift = lengthSize + offsetSize - 7 - 8 * j;
            if (shift >= 0) {
                output[j] |= ((length) >>> shift);
            } else {
                output[j] |= ((length) << -shift);
            }
        }
        tempArray.set(tempArray.size() - 1, (byte) (tempArray.get(tempArray.size() - 1) |
                (output[0] & 0xFF) >> (bitIndex % 8 + 1)));
        for (int j = 0; j < output.length; j++) {
            tempArray.add((byte) 0b00000000);
            tempArray.set(tempArray.size() - 1, (byte) ((byte) (output[j] & 0xFF) << (8 - (output[j] % 8 + 1))));
            if (j != output.length - 1) {
                bitIndex += 8;
            } else {
                bitIndex += (offsetSize + lengthSize) % 8;
            }
        }
        if ((bitIndex + 1) % 8 == 0) tempArray.add((byte) 0b00000000);
        for (byte b: output) {
            System.out.print(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            System.out.print(" ");
        }
        System.out.println();
        for (Byte b: tempArray) {
            System.out.print(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            System.out.print(" ");
        }
        System.out.println();
    }

    static String decompress(byte[] output) {
        StringBuilder outputBuilder = new StringBuilder();
        int index = 0;
        for (int i = 0; i < output.length; i++) {
            //System.out.println((char)output[i]);
            //if (debug2) System.out.println("<" + outputBuilder.toString() + ">");
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

    static byte[] compress(byte[] input) {
        ArrayList<Byte> tempArray = new ArrayList<>();
        int bitIndex = 0;
        for (int i = 0; i < input.length; i++) {
            byte currentByte = input[i];

            if (i > 1 && i + 1 < input.length) {
                int start = 0;
                if (i >= Math.pow(2, lengthSize) - 1) start = (int) (i - Math.pow(2, lengthSize) - 1);

                boolean search = true;
                int offset = 0;
                int length = 1;
                while (search) {
                    search = false;
                    length++;
                    if (length > Math.pow(2, lengthSize) - 1) break;
                    byte[] searchArray = Arrays.copyOfRange(input, i, i + length);
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
                offsetSize = 4;
                lengthSize = 4;
                byte[] output = new byte[(int) ((offsetSize + lengthSize) / 8 + Math.signum((offsetSize + lengthSize) % 8))];
                if (length != 1) {
                    output[0] = (byte) 0b10000000;
                    output[0] |= (byte) ((offset) >>> offsetSize - 7);
                    for (int j = 1; j < (int) ((offsetSize + 1) / 8 + Math.signum((offsetSize + 1) % 8)); j++) {
                        int shift = offsetSize - 7 - 8 * j;
                        if (shift >= 0) {
                            output[j] |= ((offset) >>> shift);
                        } else {
                            output[j] |= ((offset) << -shift);
                        }
                    }
                    for (int j = (int) ((offsetSize + 1) / 8 + Math.signum((offsetSize + 1) % 8) - 1); j < output.length; j++) {
                        int shift = lengthSize + offsetSize - 7 - 8 * j;
                        if (shift >= 0) {
                            output[j] |= ((length) >>> shift);
                        } else {
                            output[j] |= ((length) << -shift);
                        }
                    }
                    tempArray.set(tempArray.size() - 1, (byte) (tempArray.get(tempArray.size() - 1) |
                            (currentByte & 0xFF) >> (bitIndex % 8 + 1)));
                    for (int j = 0; j < output.length; j++) {
                        tempArray.add((byte) 0b00000000);
                        tempArray.set(tempArray.size() - 1, (byte) ((byte) (currentByte & 0xFF) << (8 - (bitIndex % 8 + 1))));
                        if (j != output.length - 1) {
                            bitIndex += 8;
                        } else {
                            bitIndex += (offsetSize + lengthSize) % 8;
                        }
                    }
                    if ((bitIndex + 1) % 8 == 0) tempArray.add((byte) 0b00000000);
                    //bitIndex += offsetSize + lengthSize;
                } else {
                    tempArray.set(tempArray.size() - 1, (byte) (tempArray.get(tempArray.size() - 1) |
                            (currentByte & 0xFF) >> (bitIndex % 8 + 1)));
                    tempArray.add((byte) 0b00000000);
                    tempArray.set(tempArray.size() - 1, (byte) ((byte) (currentByte & 0xFF) << (8 - (bitIndex % 8 + 1))));
                    if ((bitIndex + 1) % 8 == 0) tempArray.add((byte) 0b00000000);
                    bitIndex += 9;
                }
            } else {
                tempArray.set(tempArray.size() - 1, (byte) (tempArray.get(tempArray.size() - 1) |
                        (currentByte & 0xFF) >> (bitIndex % 8 + 1)));
                tempArray.add((byte) 0b00000000);
                tempArray.set(tempArray.size() - 1, (byte) ((byte) (currentByte & 0xFF) << (8 - (bitIndex % 8 + 1))));
                if ((bitIndex + 1) % 8 == 0) tempArray.add((byte) 0b00000000);
                bitIndex += 9;
            }
        }
        byte[] output = new byte[tempArray.size()];
        for (int i = 0; i < tempArray.size(); i++) {
            output[i] = tempArray.get(i);
        }
        return output;
    }
}
