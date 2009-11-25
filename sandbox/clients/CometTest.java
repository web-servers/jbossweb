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

public class CometTest 
{

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

		URL u = new URL(strURL);
                Socket s = new Socket(u.getHost(), u.getPort());
                OutputStream os = s.getOutputStream();
                os.write(("POST " + u.getPath() + " HTTP/1.1\n").getBytes());
                os.write(("User-Agent: " + CometTest.class.getClass().getName() + " (chunked-test)\n").getBytes());
                os.write("Host: localhost\n".getBytes());
                os.write("Transfer-Encoding: chunked\n".getBytes());
                os.write("\n".getBytes());
                os.flush();

                InputStream is = s.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                while (true) {
	            writechunk(os, "Testing...");
                    String res = readchunk(in);
                }

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
                       len = Integer.valueOf(data, 16);
                       System.out.println("Got: " + len);
                   } catch (Exception ex) {
                       System.out.println("Ex: " + ex);
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
                   offset = recv;
               }
               data = new String(buf);
               System.out.println("DATA: " + recv + " : " + data);
               return data;
        }
}
