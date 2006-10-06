/*
 *  Copyright(c) 2006 Red Hat Middleware, LLC,
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
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;


/**
 * Simple servlet to test SSL User Certificate.
 * (From the well known HelloWorldExample.java).
 *
 * @author Jean-Frederic Clere jfclere@apache.org
 */

public final class X509 extends HttpServlet {


    /**
     * Respond to a GET request for the content produced by
     * this servlet.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are producing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
      throws IOException, ServletException {

	response.setContentType("text/html");
	PrintWriter writer = response.getWriter();

	if (!request.getMethod().equals("POST")) {
	  writer.println("<html>");
	  writer.println("<head>");
	  writer.println("<title>X509 Application Servlet Test Page</title>");
	  writer.println("</head>");
	  writer.println("<body bgcolor=white>");
	}

	writer.println("<h1>Sample Application Servlet</h1>");
	writer.println("This is the output of a servlet that is part of");
	writer.println("the X509 test application.  It displays the");
	writer.println("request headers from the request we are currently");
	writer.println("processing.<br>");

	writer.println("Methode:" + request.getMethod());

	writer.println("<table border=\"0\" width=\"100%\">");
	Enumeration names = request.getHeaderNames();
	while (names.hasMoreElements()) {
	    String name = (String) names.nextElement();
	    writer.println("<tr>");
	    writer.println("  <th align=\"right\">" + name + ":</th>");
	    writer.println("  <td>" + request.getHeader(name) + "</td>");
	    writer.println("</tr>");
	}
	writer.println("</table>");

	writer.println("<h1>User certificate information</h1>");
        writer.println("<hr>");

	Object object = request.getAttribute("javax.servlet.request.X509Certificate");
	if (object!=null)
          writer.println("object is :" + object.getClass());

	// Get the first certificate.
	X509Certificate jsseCerts[] = (X509Certificate [])
          request.getAttribute("javax.servlet.request.X509Certificate");
        if ( jsseCerts != null) {
          X509Certificate cert = jsseCerts[0];
          writer.println("Issuer: " + cert.getIssuerDN());
          writer.println("<br>");
          writer.println("SujectDN: " + cert.getSubjectDN());
	  writer.println("<hr>");
	  writer.println(cert); // .toString());
	} else {
          writer.println("NO user certificate<br>");
	}
        writer.println("<hr>");

	writer.println("<h1>Other SSL information</h1>");

        writer.println("cipher_suite: " +
          request.getAttribute("javax.servlet.request.cipher_suite"));
        writer.println("<br>");

        writer.println("key_size: " +
          request.getAttribute("javax.servlet.request.key_size"));
        writer.println("<br>");

        writer.println("ssl_session: " +
          request.getAttribute("javax.servlet.request.ssl_session"));
        writer.println("<br>");

        writer.println("isSecure: " +
          request.isSecure());
        writer.println("<br>");

        writer.println("getScheme: " +
          request.getScheme());

	if (request.getMethod().equals("POST"))
	  writer.println("<hr>");
        else {
	  writer.println("</body>");
	  writer.println("</html>");
        }

    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
	response.setContentType("text/html");
	PrintWriter writer = response.getWriter();
	writer.println("<html>");
	writer.println("<head>");
	writer.println("<title>X509 Application Servlet Test Page</title>");
	writer.println("</head>");
	writer.println("<body bgcolor=white>");

	writer.println("<br>Start of posted data<br>");
	ServletInputStream in = request.getInputStream();
	for (;;) {
            byte[] buff = new byte[128];
	    if (in.readLine(buff,0,128)==-1) {
		writer.println("<br>End of posted data");
		break;
	    }
            String buffet = new String(buff);
            writer.println("<br>" + buffet); 
	}

        doGet(request, response);

	writer.println("</body>");
	writer.println("</html>");
	
    }

}
