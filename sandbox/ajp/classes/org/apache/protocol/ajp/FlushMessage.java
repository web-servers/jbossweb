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
 * Flush AJP Protocol message.
 * Send from container to the server for explicit
 * flushes. This is actually a BodyMessage with zero length. 
 *
 * @author Mladen Turk
 */
public final class FlushMessage extends AjpMessage
{

    public FlushMessage()
    {
        size = Ajp.CTRL_SIZE;
        buf  = new byte[size];
        pos  = len = 8;
        Encode.W(buf, 0, Ajp.SW_HEADER);
        Encode.W(buf, 2, 4);
        buf[4] = Ajp.SEND_BODY_CHUNK;
    }

    // --------------------------------------------------------- Public Methods

    public void reset()    
    {
        // Reset is unusable for fixed size messages.
    }

    public void end()
    {
        // Nothing. Everything is done in constructor.
    }

}
