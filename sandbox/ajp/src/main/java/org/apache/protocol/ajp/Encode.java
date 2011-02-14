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
 * AJP Encoding.
 *
 * @author Mladen Turk
 */
public final class Encode
{
    private Encode()
    {
        // Do not allow object creation.
    }

    /**
     * Encode boolean to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int Z(byte[] buf, int offset, boolean value)
    {
        buf[offset] = (byte)(value ? 1 : 0);
        return 1;
    }

    /**
     * Encode short to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int W(byte[] buf, int offset, short value)
    {
        buf[offset+0] = (byte)(value >>  8);
        buf[offset+1] = (byte)(value >>  0);
        return 2;
    }

    /**
     * Encode short to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int W(byte[] buf, int offset, int value)
    {
        buf[offset+0] = (byte)(value >>  8);
        buf[offset+1] = (byte)(value >>  0);
        return 2;
    }

    /**
     * Encode int to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int I(byte[] buf, int offset, int value)
    {
        buf[offset+0] = (byte)(value >> 24);
        buf[offset+1] = (byte)(value >> 16);
        buf[offset+2] = (byte)(value >>  8);
        buf[offset+3] = (byte)(value >>  0);
        return 4;
    }

    /**
     * Encode long to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int L(byte[] buf, int offset, long value)
    {
        buf[offset+0] = (byte)(value >> 56);
        buf[offset+1] = (byte)(value >> 48);
        buf[offset+2] = (byte)(value >> 40);
        buf[offset+3] = (byte)(value >> 32);
        buf[offset+4] = (byte)(value >> 24);
        buf[offset+5] = (byte)(value >> 16);
        buf[offset+6] = (byte)(value >>  8);
        buf[offset+7] = (byte)(value >>  0);
        return 8;
    }

    /**
     * Encode float to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int F(byte[] buf, int offset, float value)
    {
        byte    sign     = 0;
        int     fraction = 0;
        int     exponent = 0;

        float   tmp;
        int     i;
        int     x;

        if (value == 0.0f) {
            fraction = 0;
            exponent = -127;
        }
        else {
            /* Determine the sign of the number. */
            if (value < 0.0f) {
                sign = (byte)0x80;
                value = 0.0f - value;
            }
            /* Canonify the number before conversion. */
            while (value < 1.0f) {
                value *= 2.0f;
                --exponent;
            }

            /* Find the exponent. */
            for (i = 0, tmp = 1.0f; i <= 128; ++i, tmp *= 2.0f) {
                if (tmp * 2.0f > value)
                    break;
            }
            if (i <= 128) {
                value = value / tmp - 1.0f;
                exponent += i;
                /* Calculate the fraction part. */
                for (fraction = 0, i = 0; i < 23; ++i) {
                    fraction *= 2;
                    if (value >= 1.0f / 2.0f) {
                        fraction += 1;
                        value = value * 2.0f - 1.0f;
                    }
                    else
                        value *= 2.0f;
                }
            }
        }
        buf[offset+0] = sign;
        x = exponent + 127;

        buf[offset+0] |= (byte)(x >> 1);
        buf[offset+1]  = (byte)((x & 0x01) << 7);
        buf[offset+1] |= (byte)((fraction & 0x7fffff) >> 16);
        buf[offset+2]  = (byte)((fraction & 0x00ffff) >>  8);
        buf[offset+3]  = (byte)((fraction & 0x0000ff) >>  0);
        return 4;
    }


    /**
     * Encode double to byte array
     *
     * @param buf Byte buffer used for encoding.
     * @param offset Offset inside buf where encoding will start.
     * @param value Value to encode.
     */
    public static int D(byte[] buf, int offset, double value)
    {
        byte    sign     = 0;
        long    fraction = 0L;
        int     exponent = 0;

        double  tmp;
        int     i;
        int     x;

        if (value == 0.0) {
            fraction = 0;
            exponent = -1023;
        }
        else {
            /* Determine the sign of the number. */
            if (value < 0.0) {
                sign = (byte)0x80;
                value = 0.0 - value;
            }
            /* Canonify the number before conversion. */
            while (value < 1.0) {
                value *= 2.0;
                --exponent;
            }

            /* Find the exponent. */
            for (i = 0, tmp = 1.0; i <= 1024; ++i, tmp *= 2.0) {
                if (tmp * 2.0 > value)
                    break;
            }
            if (i <= 1024) {
                value = value / tmp - 1.0;
                exponent += i;
                /* Calculate the fraction part. */
                for (fraction = 0, i = 0; i < 52; ++i) {
                    fraction *= 2;
                    if (value >= 1.0 / 2.0) {
                        fraction += 1;
                        value = value * 2.0 - 1.0;
                    }
                    else
                        value *= 2.0;
                }
            }
        }
        buf[offset+0] = sign;
        x = exponent + 1023;
        buf[offset+0] |= (byte)((x >> 4) & 0x7f);
        buf[offset+1]  = (byte)((x & 0x0f) << 4);

        buf[offset+1] |= (byte)((fraction & 0x0f000000000000L) >> 48);
        buf[offset+2]  = (byte)((fraction & 0x00ff0000000000L) >> 40);
        buf[offset+3]  = (byte)((fraction & 0x0000ff00000000L) >> 32);
        buf[offset+4]  = (byte)((fraction & 0x000000ff000000L) >> 24);
        buf[offset+5]  = (byte)((fraction & 0x00000000ff0000L) >> 16);
        buf[offset+6]  = (byte)((fraction & 0x0000000000ff00L) >>  8);
        buf[offset+7]  = (byte)((fraction & 0x000000000000ffL) >>  0);

        return 8;
    }

    public static int B(byte[] buf, int offset, byte[] src, int pos, int len)
    {
        W(buf, offset, len);
        if (len > 0 && src != null) {
            System.arraycopy(src, pos, buf, offset + 2, len);
        }
        return len;
    }

    public static int S(byte[] buf, int offset, String value)
    {
        int len = 0;

        if (value != null) {
            if (value.length() > 0) {
                ByteBuffer bb = Defaults.CHARSET.encode(value);
                len = bb.remaining();
                if ((offset + len + 3) > buf.length) {
                    return 0;     
                }
                bb.get(buf, offset + 2, len);
            }
            else {
                /* XXX: Check if this breaks the protocol.
                 * Empty string is encoded as 1 byte length string
                 * with zero data.
                 * This is different from standard AJP13 where both
                 * empty and null strings are encoded as 00 00 00
                 * Here we have:
                 * Empty : 00 01 00 00
                 * Null  : 00 00 00
                 */
                len = 1;
                buf[offset + 2] = 0;
            }
        }
        W(buf, offset, len);
        buf[offset + len + 2] = 0;

        return len + 3;
    }

    public static int S(byte[] buf, int offset, String value,
                        int pos, int length)
    {
        if (value != null) {
            if (length > 0) {
                Utils.convertStringToBytes(buf, offset + 2, value,
                                           pos, length);
            }
            else {
                /* XXX: Check if this breaks the protocol.
                 * Empty string is encoded as 1 byte length string
                 * with zero data.
                 * This is different from standard AJP13 where both
                 * empty and null strings are encoded as 00 00 00
                 * Here we have:
                 * Empty : 00 01 00 00
                 * Null  : 00 00 00
                 */
                length = 1;
                buf[offset + 2] = 0;
            }
        }
        W(buf, offset, length);
        buf[offset + length + 2] = 0;

        return length + 3;
    }

}
