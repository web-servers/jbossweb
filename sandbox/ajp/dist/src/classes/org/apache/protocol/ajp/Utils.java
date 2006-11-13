/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.protocol.ajp;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * General purpose utilities for AJP protocol.
 *
 * @author Mladen Turk
 */
public final class Utils
{
    /**
     * Byte array initialize to all zeros used for secure clearing
     * of AJP message packets.
     */
    private static final byte[]  zero   = new byte[Ajp.MAX_LENGTH];


    private Utils()
    {
        // Private to prevent creation.
    }

    private static long     cnt         = 256L;
    private static Random   rnd         = new Random(System.currentTimeMillis());

    /**
     * Returns the unique 64 bit id.
     * Values lower then 256 are reseved for system ID's.
     */
    public static long Id()
    {
        long id;
        synchronized (Utils.class) {
            id = cnt++;
        }
        return id;
    }

    /**
     * Generate random bytes
     */
    public static void generateRandom(byte[] dest)
    {
        rnd.nextBytes(dest);
    }

    /**
     * Generate random bytes
     */
    public static byte[] generateRandom(int len)
    {
        byte[] b = new byte[len];
        rnd.nextBytes(b);
        return b;
    }

    /**
     * Set the byte array to zero
     */
    public static void zeroBytes(byte[] b, int offset, int len)
    {
        if (len > zero.length)
            len = zero.length;
        System.arraycopy(zero, 0, b, offset, len);
    }

    /**
     * Spread string to byte array.
     */
    public static void spreadStringCharsToBytes(String s, byte[] b, int offset)
    {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            b[i*2+offset+0] = (byte)(c >> 8);
            b[i*2+offset+1] = (byte)(c >> 0);
        }
    }

    protected static String hex(byte val)
    {
        String h = Integer.toHexString(val).toUpperCase();
        if (h.length() == 1)
            h = "0" + h;
        return h.substring(h.length() - 2);
    }

    protected static String hex(short val)
    {
        String h = Integer.toHexString(val).toUpperCase();
        return ("0000" + h).substring(h.length());
    }

    protected static String hex(int val)
    {
        String h = Integer.toHexString(val).toUpperCase();
        return ("00000000" + h).substring(h.length());
    }

    protected static String dumpLine(byte[] data, int offset)
    {
        int i;
        int p = offset;

        if (offset > data.length)
            return "";
        StringBuffer sb = new StringBuffer();
        sb.append(hex((short)offset));
        sb.append(": ");

        for (i = offset; i < offset + 16; i++) {
            if (i < data.length) {
                sb.append(hex(data[i]));
                if (i == (offset + 7))
                    sb.append("|");
                else
                    sb.append(" ");
            }
            else {
                sb.append("   ");
            }
        }
        
        sb.append(" | ");
        for (i = offset; i < offset + 16; i++) {
            if (i < data.length) {
                if (!Character.isISOControl((char)data[i]))
                    sb.append(new Character((char)data[i]));
                else
                    sb.append(".");
            }
            else {
                sb.append("   ");
            }
        }
        
        return sb.toString();
    }

    protected static String dumpBuffer(byte[] data, int offset, int len)
    {
        int i;
        StringBuffer sb = new StringBuffer();
        if ((len + offset) > data.length)
            len = data.length - offset;
        for (i = 0; i < len; i += 16) {
            sb.append(dumpLine(data, offset + i));
            sb.append('\n');
        }
        return sb.toString();
    }

    protected static void dumpBuffer(byte[] data, int offset, int len,
                                     PrintStream out)
    {
        int i;
        StringBuffer sb = new StringBuffer();
        if (len > data.length)
            len = data.length;
        for (i = 0; i < len; i += 16) {
            out.println(dumpLine(data, offset + i));
        }
    }
    
    protected static int align(int n)
    {
        return ((n + 1023) & 0xFFFFFC00);
    }
    
    protected static byte[] convertStringToBytes(String str)
    {
        ByteBuffer bb = Defaults.CHARSET.encode(str);
        return bb.array();
    }
    
    /**
     * Convert String to bytes
     * This presumes the charset is ISO charset
     */
    protected static void convertStringToBytes(byte[] dest, int offset,
                                               String str, int pos,
                                               int length)
    {
        for (int i = pos; i < length; i++) {
            char c = str.charAt(i);
            if (c < 0x0100)
                dest[offset++] = (byte)c;
            else
                dest[offset++] = 63; // ?
        }
    }

    protected static void convertStringToBytes(byte[] dest, int offset,
                                               String str)
    {
        convertStringToBytes(dest, offset, str, 0, str.length());
    }

}
