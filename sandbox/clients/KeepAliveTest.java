/*
 Copyright 2006 Red Hat Middleware, LLC.
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
 * Test keepAlive feature of a server.
 *
 * @author      jfclere
 */


import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import java.io.*;
import java.net.URL;

public class KeepAliveTest 
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
		
		GetMethod get = new GetMethod(strURL);

                /* Set http1.1 and chunked */
                get.setHttp11(true);
				
		HttpClient hc = new HttpClient();
                int iResultCode = 0;

                for (int i=0; i<1000; i++) {

                    Thread.sleep(1000);

                    iResultCode = hc.executeMethod(get);
                    if (iResultCode != 200)
                        break;
		
                }
		System.out.println("iResultCode = " + iResultCode);

		byte[] yaResponse = get.getResponseBody();
		
		System.out.println("Server response:");
		
		System.out.println( new String(yaResponse) );

	}
}
