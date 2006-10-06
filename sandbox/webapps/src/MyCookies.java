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
 * Servlet example showing sessionid and using session parameters.
 */

public class MyCookies extends HttpServlet {

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

        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(int i=0;i<cookies.length;i++) {
                out.println("Name=" + cookies[i].getName() + "<br>");
                out.println("Value=" + cookies[i].getValue() + "<br>");
            }
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("<xml><name>John Doe</name><age attribute=\"this breaks\">45</age></xml>");
        Cookie cookie = new Cookie("xmlCookie",buffer.toString());
        response.addCookie(cookie);
        Cookie cookie2 = new Cookie("Multiparam","I am testing...");
        response.addCookie(cookie2);
        Cookie cookie3 = new Cookie("SingleParam","I_am_testing...");
        response.addCookie(cookie3);
        Cookie cookie4 = new Cookie("Quoted1","\"I am testing...\"");
        response.addCookie(cookie4);
        Cookie cookie5 = new Cookie("Quoted2","\"I_am _esting...\"");
        response.addCookie(cookie5);
        Cookie cookie6 = new Cookie("Quoted3","I_am\"_esting...");
        response.addCookie(cookie6);
        Cookie cookie7 = new Cookie("Quoted4","\\\"I am\"_esting...\\\"");
        response.addCookie(cookie7);

        out.println("<P>");

        out.println("<P>");
        out.print("<form action=\"");
	out.print(response.encodeURL("MySession"));
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
	out.print(response.encodeURL("MySession"));
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
	out.print(response.encodeURL("MySession?dataname=foo&datavalue=bar"));
	out.println("\" >URL encoded </a>");
	
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
