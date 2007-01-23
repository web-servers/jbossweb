/*
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 * @version $Revision: 420067 $, $Date: 2006-07-08 09:16:58 +0200 (sub, 08 srp 2006) $
 */

import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.ServletException;

import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.StringManager;

/*
 * Valve to fill the SSL informations in the request
 * mod_header is used to fill the headers and the valve
 * will fill the parameters of the request.
 * In httpd.conf add the following:
 * <IfModule ssl_module>
 *   RequestHeader set SSL_CLIENT_CERT "%{SSL_CLIENT_CERT}s"
 *   RequestHeader set SSL_CIPHER "%{SSL_CIPHER}s"
 *   RequestHeader set SSL_SESSION_ID "%{SSL_SESSION_ID}s"
 *   RequestHeader set SSL_CIPHER_USEKEYSIZE "%{SSL_CIPHER_USEKEYSIZE}s"
 * </IfModule>
 */

public class SSLValve
    extends ValveBase {
/*
    private static final String info =
        "SSLValve/1.0";
    protected static StringManager sm =
        StringManager.getManager(Constants.Package);
    public String getInfo() {
        return (info);
    }
    public String toString() {
        StringBuffer sb = new StringBuffer("SSLValve[");
                if (container != null)
            sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
    }
 */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        /* mod_header converts the '\n' into ' ' so we have to rebuild the client certificate */
        String strcert0 = request.getHeader("ssl_client_cert");
        if (strcert0 != null) {
            String strcert1 = strcert0.replace(' ', '\n');
            String strcert2 = strcert1.substring(28, strcert1.length()-26);
            String strcert3 = new String("-----BEGIN CERTIFICATE-----\n");
            String strcert4 = strcert3.concat(strcert2);
            String strcerts = strcert4.concat("\n-----END CERTIFICATE-----\n");
            // ByteArrayInputStream bais = new ByteArrayInputStream(strcerts.getBytes("UTF-8"));
            ByteArrayInputStream bais = new ByteArrayInputStream(strcerts.getBytes());
            X509Certificate jsseCerts[] = null;
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);
                jsseCerts = new X509Certificate[1];
                jsseCerts[0] = cert;
            } catch (java.security.cert.CertificateException e) {
                System.out.println("SSLValve failed " + strcerts);
                System.out.println("SSLValve failed " + e);
            }
            request.setAttribute("javax.servlet.request.X509Certificate", jsseCerts);
        }
        strcert0 = request.getHeader("ssl_cipher");
        if (strcert0 != null) {
            request.setAttribute("javax.servlet.request.cipher_suite", strcert0);
        }
        strcert0 = request.getHeader("ssl_session_id");
        if (strcert0 != null) {
            request.setAttribute("javax.servlet.request.ssl_session", strcert0);
        }
        strcert0 = request.getHeader("ssl_cipher_usekeysize");
        if (strcert0 != null) {
            request.setAttribute("javax.servlet.request.key_size", strcert0);
        }
        getNext().invoke(request, response);
    }
}
