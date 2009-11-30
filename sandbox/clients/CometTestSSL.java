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

import java.net.Socket;
import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CometTestSSL extends CometTest
{
    public CometTestSSL(String strURL, int max) {
		super(strURL, max);
	}
	static TrustManager[] trustAllCerts = new TrustManager[] { 
        new X509TrustManager() { 
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
                return null;
            }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };

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
        CometTestSSL comet[] = new CometTestSSL[1];
        for (int i=0; i<comet.length; i++) {
            comet[i] = new CometTestSSL(strURL, 1000);
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
    /**
      * Run the test
      */
    
    public void run()
    {
    	try {
                SSLContext sslCtx = SSLContext.getInstance("TLS");
                sslCtx.init(null, trustAllCerts, new java.security.SecureRandom());
                SSLSocketFactory socketFactory = sslCtx.getSocketFactory();
                URL u = new URL(strURL);
                Socket s = (Socket) socketFactory.createSocket(u.getHost(), u.getPort());

                runit(s, u);
    	} catch (Exception ex) {
    	    this.ex = ex;
    	}
    }
}
