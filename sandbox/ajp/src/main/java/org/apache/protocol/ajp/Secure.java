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
 * AJP Secure Protocol abstract class
 *
 * @author Mladen Turk
 */
public abstract class Secure
{
    
    public static final int SEED_LENGTH = 8;

    private byte seed[]      = null;
    private MessageDigest md = null;
    private String        sk = null;

    public Secure(byte[] seed)
    {
        this.seed = seed;
        try {
            md = MessageDigest.getInstance(Defaults.HASH);
        }
        catch(Exception e) {
            // Nothing
        }
    }

    public Secure()
    {
        this(Utils.generateRandom(SEED_LENGTH));
    }


    public byte[] getSeed()
    {
        return seed;
    }

    public void setSeed(byte[] seed)
    {
        this.seed = seed;
    }

    public void newSeed()
    {
        if (seed != null)
            seed = Utils.generateRandom(seed.length);
        else
            seed = Utils.generateRandom(SEED_LENGTH);
    }

    protected byte[] digest(String secret)
    {
        byte[] rv = null;
        if (md != null && secret != null) {
            byte[] b = new byte[secret.length()];
            Utils.convertStringToBytes(b, 0, secret);
            md.update(b);
            md.update(seed);
            rv = md.digest();
            md.reset();
            sk = secret;
        }
        return rv;
    }

    protected byte[] hash(String secret)
    {
        byte[] rv = null;
        if (md != null && secret != null) {
            byte[] b = new byte[secret.length()];
            Utils.convertStringToBytes(b, 0, secret);
            md.update(b);
            rv = md.digest();
            md.reset();
        }
        return rv;
    }
    
    public abstract void setKey(String secret);

    public abstract int encrypt(byte[] src, int srcPos, byte[] dest,
                                int destPos, int length)
        throws IndexOutOfBoundsException;

    public abstract int decrypt(byte[] src, int srcPos, byte[] dest,
                                int destPos, int length)
        throws IndexOutOfBoundsException;

}
