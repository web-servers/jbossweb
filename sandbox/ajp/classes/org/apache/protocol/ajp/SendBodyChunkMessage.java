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
 * Response BodyChunk AJP Protocol message.
 *
 * @author Mladen Turk
 */
public final class SendBodyChunkMessage extends AjpMessage
{

    public SendBodyChunkMessage(byte[] data)
    {
        buf  = data;
        size = data.length;
        pos  = len = Ajp.HEADER_LENGTH;
        Encode.W(buf, 0, Ajp.SW_HEADER);
        buf[4] = Ajp.SEND_BODY_CHUNK;
    }

    public SendBodyChunkMessage(int size)
    {
        size = Utils.align(size);
        if (size > Ajp.MAX_LENGTH)
            size = Ajp.MAX_LENGTH;
        buf = new byte[size];
        pos = len = Ajp.HEADER_LENGTH;
        this.size = size;

        Encode.W(buf, 0, Ajp.SW_HEADER);
        buf[4] = Ajp.SEND_BODY_CHUNK;
    }

    public SendBodyChunkMessage()
    {
       this(Ajp.DEFAULT_LENGTH);
    }

    // --------------------------------------------------------- Public Methods
    
    public void reset()    
    {
        pos  = len = Ajp.HEADER_LENGTH;
    }
    
    public void end()
    {
        len = pos;
        Encode.W(buf, 2, len - Ajp.HEADER_LENGTH);
    }

}
