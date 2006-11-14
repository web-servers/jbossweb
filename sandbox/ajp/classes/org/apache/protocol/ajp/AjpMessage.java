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

import java.io.PrintStream;

/**
 * Abstract AJP Protocol message.
 *
 * @author Mladen Turk
 */
public abstract class AjpMessage
{
    /**
     * The buffer holding a AJP message.
     */
    protected byte buf[]    = null;

    /**
     * The length of the AJP message excluding header.
     */
    protected int len;

    /**
     * The current read or write position.
     */
    protected int pos;

    /**
     * The size of the buffer.
     */
    protected int size;

    /**
     * The mark position.
     */
    protected int mark;

    /**
     * Curent name-value pair counter.
     */
    protected int cnt;
    
    protected int dir;

    /**
     * Set to true if the message is encrypted
     */
    protected boolean isEncrypted = false;

    //------------------------------------------------------------ Constructors

    public AjpMessage(int size)
    {
        size  = Utils.align(size);
        if (size > Ajp.MAX_LENGTH)
            size = Ajp.MAX_LENGTH;
        buf = new byte[size];
        this.size = size;
    }

    public AjpMessage()
    {

        this(Ajp.DEFAULT_LENGTH);
    }

    public AjpMessage(byte[] data)
    {
        buf  = data;
        size = data.length;
    }

    public AjpMessage(byte[] data, int offset, int length)
    {
        buf  = data;
        size = length;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Prepare this packet for accumulating a message from the container to
     * the web server.  Set the write position to just after the header
     * (but leave the length unwritten, because it is as yet unknown).
     */
    public abstract void reset();

    public abstract void end();

    public void setDirection(int direction)
    {
        dir = direction;    
    }

    public int getDirection()
    {
        return dir;    
    }

    public void clear()
    {
        reset();
        Utils.zeroBytes(buf, 0, size);
    }

    protected void inc()
    {
        if (mark > 0)
            cnt++;
    }

    protected void mark()
    {
        cnt  = 0;
        mark = pos;
        pos  += 2;
    }

    protected void unmark()
    {
        if (mark > 0) {
            Encode.W(buf, mark, cnt);
            mark = 0;
        }
    }

    public static void copy(AjpMessage src, AjpMessage dst)
        throws OverflowException
    {
        if (src.len > dst.size) {
            throw new OverflowException(
                "Destination buffer too small " + src.len +
                ", max size is " + dst.size);
        }
        System.arraycopy(src.buf, 0, dst.buf, 0, src.len);
        dst.len = src.len;
        dst.pos = src.pos;
    }


    /**
     * Return the underlying byte buffer.
     */
    public byte[] getBuffer()
    {
        return buf;
    }

    /**
     * Return the current message length. For read, it's the length of the
     * payload (excluding the header).  For write, it's the length of
     * the packet as a whole (counting the header).
     */
    public int getLength()
    {
        return len;
    }

    public int getCapacity()
    {
        return (size - pos - 3);
    }

    public boolean hasCapacity(int capacity)
    {
        if (pos + capacity < size)
            return false;
        else
            return true;
    }

    public void setEncryptionFlag(boolean on)
    {
        if (on)
            buf[4] = (byte)(buf[4] | Ajp.SECURE_OFFSET);
        else
            buf[4] = (byte)(buf[4] & ~Ajp.SECURE_OFFSET);
    }

    public boolean getEncryptionFlag()
    {
        return ((buf[4] & Ajp.SECURE_OFFSET) ==
                Ajp.SECURE_OFFSET ? true : false);
    }

    public void encrypt(Secure secure)
    {
        if (len > pos) {
            int el = secure.encrypt(buf, pos, buf,
                                    pos, len - pos);
            // Update encrypted length
            Encode.W(buf, 2, el);
            len = el + pos;
        }
    }

    public void decrypt(Secure secure)
    {
        if (len > pos) {
            len = secure.decrypt(buf, pos, buf,
                                 pos, len);
        }
    }

    public void addByte(int data)
    {
        buf[pos++] = (byte)data;
    }

    public byte getByte()
    {
        return buf[pos++];
    }

    public byte peekByte()
    {
        return buf[pos];
    }

    public void addBytes(byte[] data, int offset, int length)
        throws OverflowException
    {
        if (!hasCapacity(length + 3)) {
            throw new OverflowException(
                "Array to big to hold in buffer.");
        }
        pos += Encode.W(buf, pos, length);
        if (data != null)
            System.arraycopy(data, offset, buf, pos, length);
        pos += length;
        // Add terminating zero.
        buf[pos++] = 0;
    }

    public void addBytes(byte[] data)
        throws OverflowException
    {
        if (data != null)
            addBytes(data, 0, data.length);
        else {
            pos += Encode.W(buf, pos, 0);
            // XXX: Is terminating zero needed for null
            buf[pos++] = 0;
        }
    }

    public void addString(String str, int offset, int length)
        throws OverflowException
    {
        int siz = Encode.S(buf, pos, str, offset, length);
        if (siz == 0) {
            throw new OverflowException(
                "String to big to hold in buffer.");
        }
        pos += siz;
    }

    public void addShort(int data)
    {
        pos += Encode.W(buf, pos, data);    
    }
    
    public int getShort()
    {
        int rv = Decode.W(buf, pos);
        pos += 2;
        return rv;
    }

    public int peekShort()
    {
        return Decode.W(buf, pos);        
    }

    public void addBoolean(boolean data)
    {
        buf[pos] = (byte)(data ? 1 : 0);   
    }

    public boolean getBoolean()
    {
        boolean rv = Decode.Z(buf, pos);
        pos++;
        return rv;
    }

    public boolean peekBoolean()
    {
        return Decode.Z(buf, pos);
    }
    
    public void addString(String str)
        throws OverflowException
    {
        if (str != null)
            addString(str, 0, str.length());
        else
            addString(str, 0, 0);
    }

    public String getString()
        throws Exception
    {
        int sz    = getShort();
        String rv = Decode.S(buf, pos, sz);
        pos      += sz;
        // Skip terminating NUL byte.
        pos++;            
        return rv;
    }

    public String getResponseHeaderName()
        throws Exception
    {
        String rv = null;
        int sz = getShort();
        if ((sz & Ajp.STRING_PREFIX) == Ajp.STRING_PREFIX) {
            return Headers.getResponseHeaderName(sz & 0xFF);
        }
        else {
            rv   = Decode.S(buf, pos, sz);
            pos += sz;
            // Skip terminating NUL byte.
            pos++;            
        }
        return rv;
    }

    public String getRequestHeaderName()
        throws Exception
    {
        String rv = null;
        int sz = getShort();
        if ((sz & Ajp.STRING_PREFIX) == Ajp.STRING_PREFIX) {
            return Headers.getRequestHeaderName(sz & 0xFF);
        }
        else {
            rv   = Decode.S(buf, pos, sz);
            pos += sz;
            // Skip terminating NUL byte.
            pos++;            
        }
        return rv;
    }
    
    public String getHeaderName()
        throws Exception
    {
        if (dir == Ajp.WS_HEADER)
            return getRequestHeaderName();
        else if (dir == Ajp.SW_HEADER)
            return getResponseHeaderName();
        else
            return null;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[length=");
        sb.append(len);
        sb.append(" size=");
        sb.append(size);
        sb.append(" pos=");
        sb.append(pos);
        sb.append(" mark=");
        sb.append(mark);
        sb.append(" cnt=");
        sb.append(cnt);
        sb.append("]");
        return sb.toString();
    }

    public String dump()
    {
        return Utils.dumpBuffer(buf, 0, pos);
    }

    public void dump(PrintStream out)
    {
        Utils.dumpBuffer(buf, 0, pos, out);
    }

}
