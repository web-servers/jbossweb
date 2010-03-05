/*
 Copyright 2009 Red Hat Middleware, LLC.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License ati

      http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributedi
 under the License is distributed on an "AS IS" BASIS,i
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*
 * Client test for comet webapp
 *
 * @author      jfclere
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Random;

public class CometTest extends Thread
{

    String strURL = "http://localhost:8080/comet/CometServletTest1";
    Exception ex = null;
    int max = 0;
    boolean failed = true;
    String lastPartialSess = null;
    /**
     *  
     * Usage:
     *          java KeepAliveTest http://mywebserver:80/
     * 
     *  @param args command line arguments
     *                 Argument 0 is a URL to a web server
     * 
     */
    public static void main(String[] args) throws Exception
    {
	if (args.length != 1)
	{
		System.err.println("missing command line arguments");
		System.exit(1);
	}
		
	String strURL = args[0];
        CometTest comet[] = new CometTest[150];
        for (int i=0; i<comet.length; i++) {
            comet[i] = new CometTest(strURL, 1000);
        }
        for (int i=0; i<comet.length; i++) {
            comet[i].start();
        }
        for (int i=0; i<comet.length; i++) {
            comet[i].join();
            if (comet[i].failed) {
	        System.err.println("Test failed! " + comet[i].ex);
	        System.exit(1);
            }
        }
    }
    public CometTest(String strURL, int max)
    {
        this.strURL = strURL;
        Random generator = new Random();
        this.max = generator.nextInt( max );
    }
    /**
      * Run the test
      */
    
    public void run()
    {
    	try {
		URL u = new URL(strURL);
                Socket s = new Socket(u.getHost(), u.getPort());
    		runit(s, u);
    	} catch (Exception ex) {
    	    this.ex = ex;
    	}
    }
    public void runit(Socket s, URL u)throws Exception
        {

                OutputStream os = s.getOutputStream();
                os.write(("POST " + u.getPath() + " HTTP/1.1\n").getBytes());
                os.write(("User-Agent: " + CometTest.class.getClass().getName() + " (chunked-test)\n").getBytes());
                os.write("Host: localhost\n".getBytes());
                os.write("Transfer-Encoding: chunked\n".getBytes());
                os.write("\n".getBytes());
                os.flush();

                InputStream is = s.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String sess = null;
                while (max>0) {
	            writechunk(os, "Testing...");
                    String res = readchunk(in);
                    String cursess = readsess(res);
                    if (sess != null && (cursess == null || sess.compareTo(cursess) != 0)) {
                        System.out.println("Session changed: " + cursess + " " + sess);
                        break;
                    }
                    if (sess == null)
                       sess = cursess;
                    if (sess == null) {
                        System.out.println("Can't find Session");
                        break;
                    }
                    max--;
                }
                if (max == 0)
                    failed = false;

	}
        /* Write chunk a "" is the last chunk */
        static void writechunk(OutputStream out, String data) throws Exception {
                String chunkSize = Integer.toHexString(data.length());
                out.write((chunkSize + "\r\n").getBytes());
                out.write((data + "\r\n").getBytes());
        }
        /* Read a chunk and return it as a String */
        static String readchunk(BufferedReader in) throws Exception {
               String data = null;
               int len = -1;
               while (len == -1) {
                   try {
                       data = in.readLine();
                       // System.out.println("DATA (len): " +  data);
                       len = Integer.valueOf(data, 16);
                   } catch (Exception ex) {
                       System.out.println("Ex: " + ex);
                   } finally {
                       if (len == 0) {
                            System.out.println("End chunk");
                            throw new Exception("End chunk");
                       }
                   }
               }
               len++; // For the CR...
               len++; // For the LF...
               char buf[] = new char[len];
               int offset = 0;
               int recv = 0;
               while (recv != len) {
                   int i = in.read(buf, offset, len-offset);
                   recv = recv + i;
                   // System.out.println("DATA: " + recv + ":" + len);
                   offset = recv;
               }
               data = new String(buf);
               // System.out.println("DATA: " + recv + " : " + data);
               // System.out.println("DATA: " + recv);
               return data;
        }
        static String getsess(String in)
        {
               String data = null;
               int start = in.indexOf('[');
               if (start != -1) {
                    int end = in.indexOf(']');
                    if (end != -1) {
                        if (end > start) {
                            data = in.substring(start+1, end);
                        } else {
                            start = in.indexOf('[', end);
                            if (start != -1) {
                                end = in.indexOf(']', start);
                                if (end != -1) {
                                    data = in.substring(start+1, end);
                                }
                            }
                        }
                    }
               }
               return data;
        }
        String readsess(String input)
        {
               String data = null;
               String in = input;
              
               data = getsess(in); 
               if (data == null) {
                    /* A small chunk without a complete sessionid */
                    if (this.lastPartialSess == null)
                        this.lastPartialSess = input;
                    else
                        this.lastPartialSess = this.lastPartialSess + input;
                    in = this.lastPartialSess;
                    data = getsess(in);
               }
               /* Store the last part of session (for the next "small" chunk) */
               if (data != null) {
                    int start = in.lastIndexOf("["+data+"]");
                    if (start>=0)
                        this.lastPartialSess = in.substring(start);
                    else
                        this.lastPartialSess = null;
               }
               if (data == null)
                   System.out.println("SESSION not found in: " + in);
               return data;
        }
}
