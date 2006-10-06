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
 * Test servlet... Long init ;-)
 *
 */

public class TestBuff extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        String swait = config.getInitParameter("wait");
        Integer iwait;

        if (swait == null)
            iwait = new Integer(5);
        else
            iwait = new Integer(swait);
        Thread me = Thread.currentThread();
        try {
            me.sleep(iwait.longValue());
        } catch(Exception e) {
            throw new ServletException("sleep interrupted");
        }
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        System.out.println("JFC: starting " + System.currentTimeMillis()); // JAVA 5: System.nanoTime());
        response.setContentType("text/html");

        BufferedWriter out = new BufferedWriter(response.getWriter());
        out.write("<html>");
        out.write("<body bgcolor=\"white\">");
        out.write("<head>");

        String title = "src/TestServlet.java";
        out.write("<title>" + title + "</title>");
        out.write("</head>");
        out.write("<body>");

        // img stuff not req'd for source code html showing
	// relative links everywhere!

        // XXX
        // making these absolute till we work out the
        // addition of a PathInfo issue 
	
        out.write("<a href=\"/examples/servlets/sessions.html\">");
        out.write("<img src=\"/examples/images/code.gif\" height=24 " +
                    "width=24 align=right border=0 alt=\"view code\"></a>");
        out.write("<a href=\"/examples/servlets/index.html\">");
        out.write("<img src=\"/examples/images/return.gif\" height=24 " +
                    "width=24 align=right border=0 alt=\"return\"></a>");

        out.write("<h3>" + title + "</h3>");

        HttpSession session = request.getSession(false);
        if (session == null) {
          // Create it.
          out.write("create");
          session = request.getSession(true);
        } else {
          session.invalidate();
          // Create a new one.
          out.write("delete+create");
          session = request.getSession(true);
        }
        out.write("sessions.id " + session.getId());
        out.write("<br>");
        out.write("sessions.created ");
        out.write(new Date(session.getCreationTime()) + "<br>");
        out.write("sessions.lastaccessed ");
        Date date = new Date(session.getLastAccessedTime());

        out.write(date.toString());

        String dataName = request.getParameter("dataname");
        String dataValue = request.getParameter("datavalue");
        if (dataName != null && dataValue != null) {
            session.setAttribute(dataName, dataValue);
        }

        out.write("<P>");
        out.write("sessions.data<br>");
        Enumeration names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement(); 
            if (name.equals("stop")) {
                session.invalidate();
                out.write("<br> INVALIDATED <br>");
                break;
            }
            String value = session.getAttribute(name).toString();
            out.write(name + " = " + value + "<br>");
        }

        out.write("<P>");
        out.write("<form action=\"");
	out.write(response.encodeURL("TestServlet"));
        out.write("\" ");
        out.write("method=POST>");
        out.write("sessions.dataname");
        out.write("<input type=text size=20 name=dataname>");
        out.write("<br>");
        out.write("sessions.datavalue");
        out.write("<input type=text size=20 name=datavalue>");
        out.write("<br>");
        out.write("<input type=submit>");
        out.write("</form>");

        out.write("<P>GET based form:<br>");
        out.write("<form action=\"");
	out.write(response.encodeURL("TestServlet"));
        out.write("\" ");
        out.write("method=GET>");
        out.write("sessions.dataname");
        out.write("<input type=text size=20 name=dataname>");
        out.write("<br>");
        out.write("sessions.datavalue");
        out.write("<input type=text size=20 name=datavalue>");
        out.write("<br>");
        out.write("<input type=submit>");
        out.write("</form>");

        out.write("<p><a href=\"");
	out.write(response.encodeURL("TestServlet?dataname=foo&datavalue=bar"));
	out.write("\" >URL encoded </a>");

        for (int i=0;i<5000;i++) 
            out.write("blablablablablablablablablablablablablablablablablablablablablablablabla<br>");
	
        out.write("</body>");
        out.write("</html>");
        
        out.write("</body>");
        out.write("</html>");
        out.flush();
        System.out.println("JFC: finished " + System.currentTimeMillis());
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
