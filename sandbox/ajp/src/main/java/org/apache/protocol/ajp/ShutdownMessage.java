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
 * Shutdown AJP Protocol message.
 *
 * @author Mladen Turk
 */
public final class ShutdownMessage extends AjpMessage
{

    private MessageDigest md = null;

    public ShutdownMessage()
    {
        dir  = Ajp.WS_HEADER;
        size = Ajp.CTRL_SIZE * 2;
        buf  = new byte[size];
        pos  = len = Ajp.HEADER_LENGTH + 1;
        Encode.W(buf, 0, dir);
        Encode.W(buf, 2, 1);
        buf[4] = Ajp.SHUTDOWN;
        try {
            md = MessageDigest.getInstance(Defaults.HASH);
        }
        catch(Exception e) {
            // Nothing
        }
        
    }

    // --------------------------------------------------------- Public Methods

    public void reset()    
    {
        pos = len = Ajp.HEADER_LENGTH + 1;
    }

    public void end()
    {
        // Nothing. Everything is done in constructor.
    }

    public void setShutdownMode(int mode)
    {
        pos = Ajp.HEADER_LENGTH + 1;
        buf[pos++] = (byte)mode;
    }

    /**
     * Add Secure command to the message.
     */
    public void addSecure(String secret)
        throws OverflowException
    {
        byte[] hash = null;
        if (md != null) {
            byte[] b = new byte[secret.length()];
            Utils.convertStringToBytes(b, 0, secret);
            md.update(b);
            hash = md.digest();
            md.reset();
        }
        addBytes(hash);
    }

}
