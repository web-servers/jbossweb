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

import java.security.MessageDigest;

/**
 * ArcFour (a.k.a RC4) AJP Protocol message encryption
 * This is modified RC4.
 *
 * @author Mladen Turk
 */
public class ArcFourSecure extends Secure
{
    private static final int IV_LENGTH = 256;
    private byte[] iv                  = new byte[IV_LENGTH];
    
    private int x;
    private int y;
    
    public ArcFourSecure()
    {
        super();
    }

    public ArcFourSecure(byte[] seed)
    {
        super(seed);
    }
    
    public void setKey(String secret)
    {
        byte[] km = digest(secret);
        if (km == null)
            return;
        // reset the state of the engine
        for (int i = 0; i < IV_LENGTH; i++) {
            iv[i] = (byte)i;
        }
        x      = 0;
        y      = 0;
        int i1 = 0;
        int i2 = 0;

        for (int i = 0; i < IV_LENGTH; i++) {
            i2 = (km[i1] + iv[i] + i2) & 0xff;
            // swap
            byte t = iv[i];
            iv[i]  = iv[i2];
            iv[i2] = t;
            i1 = (i1 + 1) % km.length;
        }
    }
    
    private void processBytes(byte[] src, int srcPos, byte[] dest,
                              int destPos, int length)
        throws IndexOutOfBoundsException
    {
        if ((srcPos + length) > src.length) {
            throw new IndexOutOfBoundsException("input buffer too short");
        }

        if ((destPos + length) > dest.length) {
            throw new IndexOutOfBoundsException("output buffer too short");
        }

        for (int i = 0; i < length; i++) {
            x = (x + 1) & 0xff;
            y = (iv[x] + y) & 0xff;

            // swap
            byte t = iv[x];
            iv[x]  = iv[y];
            iv[y]  = t;
            // xor
            dest[i + destPos] = (byte)(src[i + srcPos] ^
                                       iv[(iv[x] + iv[y]) & 0xff]);
        }
    }    

    public int encrypt(byte[] src, int srcPos, byte[] dest,
                        int destPos, int length)
        throws IndexOutOfBoundsException
    {
        processBytes(src, srcPos, dest, destPos, length);
        return length;
    }

    public int decrypt(byte[] src, int srcPos, byte[] dest,
                        int destPos, int length)
        throws IndexOutOfBoundsException
    {
        processBytes(src, srcPos, dest, destPos, length);
        return length;
    }

}
