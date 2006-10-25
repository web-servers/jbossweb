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



/*
 * Test servlet for AJP buffer.
 * For each request a new cookie is added.
 */

public class GrowCookies extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {


        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");

        String title = "sessions.title";
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h3>" + title + "</h3>");

        out.println("Servlet name: " + request.getServletPath() + "<br>");

        Cookie[] cookies = request.getCookies();
        int i = 0;
        int size = 0;
        if(cookies != null) {
            for(i=0;i<cookies.length;i++) {

                size = size + cookies[i].getName().length();
                size = size + cookies[i].getValue().length();

                /* add them to the reponse */
                response.addCookie(cookies[i]);
            }
        }
        out.println("Size (names+values)=" + size + "<br>");

        /* value of the new cookie */
        StringBuffer buffer = new StringBuffer();
        // buffer.append("<xml><name>John Doe</name><age attribute=\"this breaks\">45</age></xml>");
        buffer.append("<xml><name>John Doe</name><age attribute=this_breaks>45</age></xml>");
        buffer.append("<xml><name>John Doe</name><age attribute=this_breaks>45</age></xml>");
        buffer.append("<xml><name>John Doe</name><age attribute=this_breaks>45</age></xml>");
        buffer.append("<xml><name>John Doe</name><age attribute=this_breaks>45</age></xml>");
        buffer.append("<xml><name>John Doe</name><age attribute=this_breaks>45</age></xml>");
        /* name of the new cookie */
        String name = String.valueOf(i);
        name.concat("Test");

        Cookie cookie = new Cookie(name, buffer.toString());
        response.addCookie(cookie);

        /* Get the servlet name */
        String servletname =  request.getServletPath().substring(1);

        out.println("<P>");
        out.print("<form action=\"");
	out.print(response.encodeURL(servletname));
        out.print("\" ");
        out.println("method=POST>");
        out.println("sessions.dataname");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("sessions.datavalue");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.println("<P>GET based form:<br>");
        out.print("<form action=\"");
	out.print(response.encodeURL(servletname));
        out.print("\" ");
        out.println("method=GET>");
        out.println("sessions.dataname");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("sessions.datavalue");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.print("<p><a href=\"");
	out.print(response.encodeURL( servletname + "?dataname=foo&datavalue=bar"));
	out.println("\" >URL encoded </a>");

        out.println("<h3> received cookies </h3>");
        if(cookies != null) {
            for(i=0;i<cookies.length;i++) {
                out.println("Name=" + cookies[i].getName() + "<br>");
                out.println("Size=" + cookies[i].getValue().length() + "<br>");
            }
        }
	
        out.println("</body>");
        out.println("</html>");
        
        out.println("</body>");
        out.println("</html>");
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
