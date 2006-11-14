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
 * Request AJP Protocol message.
 *
 * @author Mladen Turk
 */
public final class RequestMessage extends AjpMessage
{
 
    private String storedMethod = null;   

    public RequestMessage(int size)
    {
        dir  = Ajp.SW_HEADER;
        size = Utils.align(size);
        pos  = len = Ajp.HEADER_LENGTH + 1;
        if (size > Ajp.MAX_LENGTH)
            size = Ajp.MAX_LENGTH;
        buf = new byte[size];
        Encode.W(buf, 0, dir);
        buf[4] = Ajp.FORWARD_REQUEST;
        this.size = size;
    }

    public RequestMessage()
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
        pos = len = Ajp.HEADER_LENGTH + 1;
    }

    public void end()
    {
        if (storedMethod != null) {
            if (hasCapacity(6)) {
                addShort(Ajp.STRING_PREFIX + Headers.SC_A_STORED_METHOD);
                try {
                    addString(storedMethod);
                }
                catch (OverflowException ex) {
                    // Add null string
                    // TODO: Make sure it is send.
                    pos += Encode.S(buf, pos, null);
                }
            }
        }

        // Add terminating header char
        if (pos < size) {
            buf[pos++] = Headers.SC_A_ARE_DONE;    
        }
        len = pos;
        Encode.W(buf, 2, len - Ajp.HEADER_LENGTH - 1);
        pos = Ajp.HEADER_LENGTH + 1;
    }
    
    public void addMethod(String method)
    {
        byte mid = Headers.getHttpMethodIndex(method);
        if (mid == 0) {
            storedMethod = method;
            mid = Headers.SC_M_STORED;    
        }
        buf[pos++] = mid;
    }

    public void addAttribute(String name, String value)
        throws OverflowException
    {
        int hdr = Headers.getRequestHeaderIndex(name);
        if (hdr != 0)
            addShort(Ajp.STRING_PREFIX + hdr);
        else
            addString(name);
 
        addString(value);
        // Increment the header counter
        inc();        
    }
    
    public void addHeader(String name, String value)
        throws OverflowException
    {
        addAttribute(name, value);
        // Increment the header counter
        inc();        
    }

}
