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
 * AJP Protocol definitions.
 *
 * @author Mladen Turk
 */
public final class Ajp
{

    private Ajp()
    {
        // Private to prevent creation.
    }

    /**
     * Represents the AJP protocol version.
     */
    public static final int  VERSION            = 13;

    /**
     * Represents the control AJP package message size.
     */
    public static final int  CTRL_SIZE          = 16;

    /**
     * Represents the largest possible length of a single
     * AJP message packet.
     */
    public static final int  MAX_LENGTH         = 65536;

    /**
     * Represents the default length of a single
     * AJP message packet.
     */
    public static final int  DEFAULT_LENGTH     = 8192;

    /**
     * Represents the AJP packet header size
     */
    public static final int  HEADER_LENGTH      = 4;

    /**
     * Represents the AJP packet header size send from
     * server to the container.
     * Header + length + code + [data] + zero.
     */
    public static final int  HEADER_META        = 6;

    /**
     * Represents the AJP packet header size send from
     * container to the server for GET_BODY_CKUNK
     * Header + length + code + body length + [data] + zero.
     */
    public static final int  BODY_HEADER_META   = 8;

    /**
     * Represents the packets sent from the server to the
     * container.
     */
    public static final int  WS_HEADER          = 0x1234;

    /**
     * Represents the packets sent from the container to the
     * server (AB).
     */
    public static final int  SW_HEADER          = 0x4142;

    /*
     * Prefix codes for message types from server to container
     */
    public static final byte FORWARD_REQUEST    = 2;
    public static final byte SHUTDOWN           = 7;
    public static final byte PING_REQUEST       = 8;
    public static final byte CPING_REQUEST      = 10;

    /*
     * Prefix codes for message types from container to server
     */
    public static final byte SEND_BODY_CHUNK    = 3;
    public static final byte SEND_HEADERS       = 4;
    public static final byte END_RESPONSE       = 5;
    public static final byte GET_BODY_CHUNK     = 6;
    public static final byte CPONG_REPLY        = 9;

    /*
     * Send from container to server for the next huge header.
     * The server responds with header name followed by the
     * total header value length.
     */
    public static final byte GET_HEADER         = 11;

    /* Send from container to server for the next
     * header value chunk
     */
    public static final byte GET_HEADER_CHUNK   = 12;

    /* Send from server to the container to notify that
     * the transport channel must be closed.
     */
    public static final byte CLOSE              = 13;

    /*
     * Offset for secure AJP protocol prefix codes
     */
    public static final byte SECURE_OFFSET      = (byte)0x80;
    
    public static final int  STRING_PREFIX      = 0xA000;

}
