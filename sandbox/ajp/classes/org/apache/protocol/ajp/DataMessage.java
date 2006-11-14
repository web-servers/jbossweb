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
 * Raw Data AJP Protocol message.
 *
 * @author Mladen Turk
 */
public final class DataMessage extends AjpMessage
{
    public DataMessage(int size)
    {
        dir  = Ajp.WS_HEADER;        
        size = Utils.align(size);
        pos  = len = Ajp.HEADER_LENGTH;
        if (size > Ajp.MAX_LENGTH)
            size = Ajp.MAX_LENGTH;
        buf = new byte[size];
        Encode.W(buf, 0, dir);
        this.size = size;
    }

    public DataMessage()
    {        
        this(Ajp.DEFAULT_LENGTH);
    }
             
    // --------------------------------------------------------- Public Methods

    /**
     * Prepare this packet for accumulating a message from the container to
     * the web server.  Set the write position to just after the header
     * (but leave the length unwritten, because it is as yet unknown).
     */
    public void reset()
    {
        pos = len = Ajp.HEADER_LENGTH;
    }

    public void end()
    {
        len = pos;
        Encode.W(buf, 2, len - Ajp.HEADER_LENGTH);
        pos = Ajp.HEADER_LENGTH;
    }
    
    public void addBytes(byte[] data, int offset, int length)
        throws OverflowException
    {
        if (!hasCapacity(length + 2)) {
            throw new OverflowException(
                "Array to big to hold in buffer.");
        }
        pos += Encode.W(buf, pos, length);
        if (length > 0) {
            System.arraycopy(data, offset, buf, pos, length);
            pos += length;
        }
    }
    
}
