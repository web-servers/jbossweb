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
 * Test servlet...:
 * Long init (via configuration web.xml)
 * Long processing (via parameter).
 *
 */

public class TestServlet extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        String swait = config.getInitParameter("wait");
        Integer iwait = new Integer(swait);
        int wait = iwait.intValue();
        Thread me = Thread.currentThread();
        try {
            me.sleep(wait);
        } catch(Exception e) {
            throw new ServletException("sleep interrupted");
        }
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        /* errorcode is set */
        String errcodeValue = request.getParameter("errcodevalue");
        int ierrcode = 0;
        if (errcodeValue != null) {
            Integer iwait = new Integer(errcodeValue);
            ierrcode = iwait.intValue();
            if (ierrcode < 0) {
                response.sendError(-ierrcode);
            return;
            }
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");

        String title = "src/TestServlet.java";
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h3>" + title + "</h3>");
        String createValue = request.getParameter("create");
        int icreate = 0;
        if (createValue != null) {
            Integer iwait = new Integer(createValue);
            icreate = iwait.intValue();
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
          // Create it.
          out.println("create");
          session = request.getSession(true);
        } else {
          session.invalidate();
          // Create a new one.
          out.println("delete+create");
          session = request.getSession(true);
        }
        for (int i=0; i<icreate; i++) {
          out.println("delete+create more");
          session.invalidate();
          session = request.getSession(true);
        }
        out.println("sessions.id " + session.getId());
        out.println("<br>");
        out.println("sessions.created ");
        out.println(new Date(session.getCreationTime()) + "<br>");
        out.println("sessions.lastaccessed ");
        out.println(new Date(session.getLastAccessedTime()));

        String dataName = request.getParameter("dataname");
        String dataValue = request.getParameter("datavalue");
        String waitValue = request.getParameter("waitvalue");
        String countValue = request.getParameter("countvalue");

        if (dataName != null && dataValue != null) {
            session.setAttribute(dataName, dataValue);
        }

        /* errorcode is set */
        if (ierrcode != 0) {
            response.setStatus(ierrcode);
        }

        /* wait if waitvalue is set */
        if (waitValue != null) {
            Integer iwait = new Integer(waitValue);
            int wait = iwait.intValue();
            Thread me = Thread.currentThread();
            try {
                me.sleep(wait);
            } catch(Exception e) {
                throw new ServletException("sleep interrupted");
            }
        }
        /* loop if countvalue is set */
        int l = 0;
        if (countValue != null) {
            Integer iwait = new Integer(countValue);
            int wait = iwait.intValue();
            for (int i=0; i<iwait; i++) {
                for (int j=0; j<1000000;j++) {
                   l = l + 1;
                }
            }
        }
        l = l + l;

        out.println("<P>");
        out.println("param: countvalue " + countValue + " counted: " + l);
        out.println("param: waitvalue " + waitValue);
        out.println("param: create " + createValue);
        out.println("param: errcodevalue " + errcodeValue);
        out.println("negative errcodevalue should display the error page (404.html)");
        out.println("<P>");
        out.println("sessions.data<br>");
        out.println("<P>");
        Enumeration names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement(); 
            if (name.equals("stop")) {
                session.invalidate();
                out.println("<br> INVALIDATED <br>");
                break;
            }
            String value = session.getAttribute(name).toString();
            out.println(name + " = " + value + "<br>");
        }

        out.println("<P>");
        out.print("<form action=\"");
	out.print(response.encodeURL("TestServlet"));
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
	out.print(response.encodeURL("TestServlet"));
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
	out.print(response.encodeURL("TestServlet?dataname=foo&datavalue=bar"));
	out.println("\" >URL encoded </a>");
	
        out.println("</body>");
        out.println("</html>");
        
        out.println("</body>");
        out.println("</html>");

        //response.addHeader(name, value);
        response.addHeader("MYHEAD", "test\r\n casse: toto");
        response.addHeader("TOTO", "TITI");
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
