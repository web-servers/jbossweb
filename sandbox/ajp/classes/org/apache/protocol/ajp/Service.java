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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * AJP Service abstract class
 *
 * @author Mladen Turk
 */
public abstract class Service
{
    
    private InputStream         is = null;
    private OutputStream        os = null;
    
    private Service()
    {
    }

    public Service(InputStream in, OutputStream out)
    {
        is = in;
        os = out;
    }

    public int read(byte[] b, int off, int len)
        throws IOException
    {
        return is.read(b, off, len);    
    }

    public void write(byte[] b, int off, int len)
        throws IOException
    {
        os.write(b, off, len);    
    }

    public void flush()
        throws IOException
    {
        os.flush();    
    }

    public void setStreams(InputStream in, OutputStream out)
    {
        is = in;
        os = out;
    }
    
    /**
     * Start this service.
     * It is called once per reqest/respone.
     * All initializtion shuld be done at this call.
     */
    public abstract void start();

    /**
     * Finish this service
     * This is the last call to the Service.
     * All cleanups shuld be done at this call.
     */
    public abstract void end();
    
}
