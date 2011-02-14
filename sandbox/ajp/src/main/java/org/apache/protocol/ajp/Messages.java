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
 * Shared AJP Protocol messages.
 *
 * @author Mladen Turk
 */
public final class Messages
{

    private Messages()
    {
    }
    
    /**
     * Represents a AJP SHUTDOWN Message
     */
    public static AjpMessage SHUTDOWN       = new ShutdownMessage();

    /**
     * Represents a AJP CLOSE Message
     */
    public static AjpMessage CLOSE          = new CloseMessage();

    /**
     * Represents a AJP CPING_REQUEST Message
     */
    public static AjpMessage CPING          = new CpingMessage();

    /**
     * Represents a AJP CPONG_RESPONSE Message
     */
    public static AjpMessage CPONG          = new CpongMessage();

    /**
     * Represents a AJP FLUSH Message
     */
    public static AjpMessage FLUSH          = new FlushMessage();

    /**
     * Represents a AJP GET_HEADER Message
     */
    public static AjpMessage GET_HEADER     = new GetHeaderMessage();

    /**
     * Represents a non reusable AJP END_RESPONSE Message
     */
    public static AjpMessage END            = new EndResponseMessage(false);

    /**
     * Represents a reusable AJP END_RESPONSE Message
     */
    public static AjpMessage END_REUSABLE   = new EndResponseMessage(true);
    
}

