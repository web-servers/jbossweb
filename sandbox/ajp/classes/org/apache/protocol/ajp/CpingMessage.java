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

/**
 * CPING AJP Protocol message.
 * The extended CPING message contains additional
 * protocol data like encryption seed.
 *
 * @author Mladen Turk
 */
public final class CpingMessage extends AjpMessage
{

    public CpingMessage()
    {
        dir  = Ajp.WS_HEADER;        
        size = Ajp.CTRL_SIZE;
        buf  = new byte[size];
        pos  = len = 5;
        Encode.W(buf, 0, dir);
        Encode.W(buf, 2, 1);
        buf[4] = Ajp.CPING_REQUEST;
    }

    // --------------------------------------------------------- Public Methods

    public void reset()
    {
        pos = len = Ajp.HEADER_LENGTH;
    }

    public void end()
    {
        len = pos;
        Encode.W(buf, 2, len - Ajp.HEADER_LENGTH);
    }
    
    /**
     * Add Secure command to the message.
     * This will add nbytes of Secure key seed
     * used for the data encryption for all
     * data transfer between server and container
     */
    public void addSecure(Secure secure)
        throws OverflowException
    {
        byte[] seed = secure.getSeed();
        if (seed == null)
            return;
        pos = 5;
        int length = seed.length;
        if ((length + pos) > size)
            length = size - pos;
        System.arraycopy(seed, 0, buf, pos, length);
        pos += length;
        end();
    }

}
