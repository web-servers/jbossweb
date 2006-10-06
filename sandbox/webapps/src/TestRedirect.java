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

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;



/**
 * Example servlet showing redirect url.
 *
 */

public class TestRedirect extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        String location = null;
        String path;
        String query;

        if ((path = request.getParameter("location")) != null) {
              System.out.println("TestRedirect.java: " + path);
	      location = decode(path);
        } else {
              location = "http://localhost:8080/myapp/index.html";
        }

        if (location == null) {
            response.sendError(503,
			  "Destination not set for redirect; " +
			  "please inform system admin");
            return;
        }
        response.sendRedirect(location);
    }

    private String decode(String encoded) {
        
        //speedily leave if we're not needed
        if (encoded.indexOf('%') == -1 ) return encoded;

        StringBuffer holdstring = new StringBuffer(encoded.length());
        char holdchar;

        for (int count = 0; count < encoded.length(); count++) {
            if (encoded.charAt(count) == '%') {
            //add check for out of bounds
                holdstring.append((char)Integer.parseInt(encoded.substring(count+1,count+3),16));
                if (count + 2 >= encoded.length()) 
                    count = encoded.length();
                else
                    count += 2;
            } else {
                holdstring.append(encoded.charAt(count));
            }
        }
        return holdstring.toString();
    }


    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
