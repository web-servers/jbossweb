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

public class TestUtils {

    public static void main(String args[]) 
    {
        byte i;
        for (i = 1; i < Headers.SC_M_MAX; i++) {
            try { 
                String name = Headers.getHttpMethodName(i);
                int idx = Headers.getHttpMethodIndex(name);                

                System.out.println("Method\t" + i + "\t" + idx + "\t" + name);            
            }
            catch (Exception e) {
                
            }
        }
        try { 
            System.out.println("Method\t" +  Utils.hex(Headers.SC_M_STORED) + "\t" + Headers.getHttpMethodName(Headers.SC_M_STORED));            
        }
        catch (Exception e) {
            
        }
        
        ArcFourSecure ss = new ArcFourSecure();
        ss.setKey("foo");

        ArcFourSecure ds = new ArcFourSecure(ss.getSeed());
        ds.setKey("foo");
        
        byte sd[] = Utils.convertStringToBytes("Hello World\u1234s");
        
        byte ems[] = new byte[10];
        String emss = Decode.S(ems, 0, 4);        
        
        System.out.println("Empty string is -" + emss + "-");
        
        int loops = 2000;
        try { 
            System.out.println("Encrypting:");
            long encs = System.currentTimeMillis();
            for (int x = 0; x < loops; x++) {
                ss.encrypt(sd, 0, sd, 0, sd.length);
            }
            long encd = System.currentTimeMillis();
            long decs = System.currentTimeMillis();
            for (int x = 0; x < loops; x++) {
                ds.decrypt(sd, 0, sd, 0, sd.length);
            }
            long decd = System.currentTimeMillis();
//            Utils.dumpBuffer(sd, 0, sd.length, System.out);
            System.out.println("Decrypted:");
            Utils.dumpBuffer(sd, 0, sd.length, System.out);
            System.out.println();
            long ebt = sd.length * loops / (encd - encs);
            long dbt = sd.length * loops / (decd - decs);
            System.out.println("Encoded\t" + (encd - encs) + " ms\tDecoded\t"  + (decd - decs) + " ms");
            System.out.println("Encoded\t" + ebt + " Kb/s\tDecoded\t"  + dbt + " Kb/s");
        }
        catch (Exception e) {
            
        }
        

        CpingMessage dm = new CpingMessage();                
        Messages.CPING.dump(System.out);
        Messages.CPONG.dump(System.out);
        Messages.FLUSH.dump(System.out);
        try {
            dm.addSecure(ss);
        }
        catch (OverflowException e) {
            e.printStackTrace();    
        }
        dm.dump(System.out);

    }        
    
}
