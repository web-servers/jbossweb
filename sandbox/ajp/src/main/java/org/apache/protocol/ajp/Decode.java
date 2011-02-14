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

import java.nio.charset.Charset;
import java.nio.ByteBuffer;

/**
 * AJP Decoding.
 *
 * @author Mladen Turk
 */
public final class Decode
{

    private Decode()
    {
        // Do not allow object creation.
    }

    /**
     * Decode boolean from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static boolean Z(byte[] buf, int offset)
    {
        return (buf[offset] != 0 ? true : false);
    }

    /**
     * Decode short from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static int W(byte[] buf, int offset)
    {
        int value;
        value  = ((buf[offset+0] << 8) & 0xff00);
        value |= ((buf[offset+1] << 0) & 0x00ff);
        return value;
    }

    /**
     * Decode int from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static int I(byte[] buf, int offset)
    {
        int value;
        value  = ((buf[offset+0] << 24) & 0xff000000);
        value |= ((buf[offset+1] << 16) & 0x00ff0000);
        value |= ((buf[offset+2] <<  8) & 0x0000ff00);
        value |= ((buf[offset+3] <<  0) & 0x000000ff);
        return value;
    }

    /**
     * Decode long from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static long L(byte[] buf, int offset)
    {
        long value;
        value  = ((buf[offset+0] << 56) & 0xff00000000000000L);
        value |= ((buf[offset+1] << 48) & 0x00ff000000000000L);
        value |= ((buf[offset+2] << 40) & 0x0000ff0000000000L);
        value |= ((buf[offset+3] << 32) & 0x000000ff00000000L);
        value |= ((buf[offset+4] << 24) & 0x00000000ff000000L);
        value |= ((buf[offset+5] << 16) & 0x0000000000ff0000L);
        value |= ((buf[offset+6] <<  8) & 0x000000000000ff00L);
        value |= ((buf[offset+7] <<  0) & 0x00000000000000ffL);
        return value;
    }

    /**
     * Decode float from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static float F(byte[] buf, int offset)
    {
        float   value = 0.0f;
        int     fraction;
        int     exponent;
        int     i;
        int     sign;


        sign =  (buf[offset+0] & 0x80) >> 7;

        exponent  = (buf[offset+0] & 0x7f) << 1;
        exponent += (buf[offset+1] & 0x80) >> 7;

        fraction  = (buf[offset+1] & 0x7f) << 16;
        fraction += buf[offset+2] << 8;
        fraction += buf[offset+3];

        if (fraction == 0 && exponent == 0)
            return value;

        for (i = 23; i > 0; --i) {
            if ((fraction & 0x01) == 1)
                value += 1.0f;
            value /= 2.0f;
            fraction /= 2;
        }
        value += 1.0f;

        if (exponent > 127) {
            for (exponent -= 127; exponent > 0; --exponent)
                value *= 2.0f;
        }
        else {
            for (exponent = 127 - exponent; exponent > 0; --exponent)
                value /= 2.0f;
        }

        if (sign == 1)
            value = 0.0f - value;

        return value;
    }

    /**
     * Decode double from byte array
     *
     * @param buf Byte buffer used for decoding.
     * @param offset Offset inside buf where decoding will start.
     */
    public static double D(byte[] buf, int offset)
    {
        double  value = 0.0;
        long    fraction;
        int     exponent;
        int     i;
        int     sign;


        sign =  (buf[offset+0] & 0x80) >> 7;

        exponent  = (buf[offset+0] & 0x7f) << 4;
        exponent += (buf[offset+1] & 0xf0) >> 4;

        fraction  = (buf[offset+1] & 0x0f) << 48;
        fraction += (buf[offset+2] << 40);
        fraction += (buf[offset+3] << 32);
        fraction += (buf[offset+4] << 24);
        fraction += (buf[offset+5] << 16);
        fraction += (buf[offset+6] <<  8);
        fraction += (buf[offset+7]);

        if (fraction == 0 && exponent == 0)
            return 0.0;

        for (i = 52; i > 0; --i) {
            if ((fraction & 0x01) == 1)
                value += 1.0;
            value /= 2.0;
            fraction /= 2;
        }
        value += 1.0;

        if (exponent > 1023) {
            for (exponent -= 1023; exponent > 0; --exponent)
                value *= 2.0;
        }
        else {
            for (exponent = 1023 - exponent; exponent > 0; --exponent)
                value /= 2.0;
        }

        if (sign == 1)
            value = 0.0 - value;

        return value;
    }

    public static int B(byte[] buf, int offset, byte[] dst, int pos, int len)
    {
        if (dst != null)
            System.arraycopy(buf, offset, dst, pos, len);
        return len;
    }

    public static int B(byte[] buf, int offset, byte[] dst, int pos)
    {
        int len = W(buf, offset);
        if (len > 0 && len != 0xFFFF && dst != null)
            System.arraycopy(buf, offset + 2, dst, pos, len);
        return len + 2;
    }

    public static int B(byte[] buf, int offset, IBytesProcessor dst, int len)
    {
        if (dst != null)
            dst.processBytes(buf, offset, len);
        return len;
    }

    public static int B(byte[] buf, int offset, IBytesProcessor dst)
    {
        int len = W(buf, offset);
        if (len > 0 && len != 0xFFFF && dst != null)
            dst.processBytes(buf, offset + 2, len);
        return len + 2;
    }

    public static int S(byte[] buf, int offset, byte[] dst, int pos, int len)
    {
        if (dst != null)
            System.arraycopy(buf, offset, dst, pos, len);
        return len + 1;
    }

    public static int S(byte[] buf, int offset, byte[] dst, int pos)
    {
        int len = W(buf, offset);
        if (len > 0 && len != 0xFFFF && dst != null)
            System.arraycopy(buf, offset + 2, dst, pos, len);
        return len + 3;
    }

    public static int S(byte[] buf, int offset, IBytesProcessor dst, int len)
    {
        if (dst != null)
            dst.processBytes(buf, offset, len);
        return len + 1;
    }

    public static int S(byte[] buf, int offset, IBytesProcessor dst)
    {
        int len = W(buf, offset);
        if (len > 0 && len != 0xFFFF && dst != null)
            dst.processBytes(buf, offset + 2, len);
        return len + 3;
    }

    public static String S(byte[] buf, int offset, int len)
    {
        if (buf[offset] == 0) {
            return "";
        }
        else {
            ByteBuffer bb = ByteBuffer.wrap(buf, offset, len);
            return Defaults.CHARSET.decode(bb).toString();
        }
    }

}
