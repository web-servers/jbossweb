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

    public class Test {
      String Name;
      String Value;
      public Test(String Name, String Value) {
        this.Name = Name;
        this.Value = Value;
      }
    }
    public Cookie CreateCookie(Test test) {
      Cookie cookie = new Cookie(test.Name, test.Value);
      return cookie;
    }
    public String GetVal(Test[] test, String Name) {
      for (int i=0; i<test.length; i++) {
        if (Name.equals(test[i].Name))
          return test[i].Value;
      }
      return "Failed: Unknown";
    }

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

        /*
         * create the name/value pairs
         */
        Test[] mytest = new Test[11];
        StringBuffer buffer = new StringBuffer();
        buffer.append("<xml><name>John Doe</name><age attribute=\"this breaks\">45</age></xml>");
        mytest[0] = new Test("xmlCookie",buffer.toString());
        mytest[1] = new Test("Multiparam","I am testing...");
        mytest[2] = new Test("SingleParam","I_am_testing...");
        mytest[3] = new Test("Quoted1","\"I am testing...\"");
        mytest[4] = new Test("Quoted2","I am \"testing...");
        // A " in a not quoted-string is not ok, see rfc 2965 and 2616 */
        mytest[5] = new Test("Quoted3","I_am\"_esting...");
        mytest[6] = new Test("Quoted4","\\\"I am\"_esting...\\\"");
        mytest[7] = new Test("Quoted5","'");
        mytest[8] = new Test("Quoted6","A");
        mytest[9] = new Test("Quoted7","val'ue");
        mytest[10] = new Test("Quoted8","I am \" testing...");
        mytest[10] = new Test("Quoted9","I am \r\n testing...");

        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(int i=0;i<cookies.length;i++) {
                out.println("Name=" + cookies[i].getName() + "<br>");
                out.println("Value=" + cookies[i].getValue() + "<br>");
                out.println("Expected=" + GetVal(mytest, cookies[i].getName()) + "<br>");
            }
        }

        /* create the cookies */
        for (int i=0; i<mytest.length; i++) {
          try {
            Cookie cookie = CreateCookie(mytest[i]);
            response.addCookie(cookie);
          } catch (Exception ex) {
            out.println("Cookie test: " + i + " Failed");
          }
        }
        Cookie cookie = new Cookie("commented", "commented cookie");
        cookie.setComment("This is a comment");
        response.addCookie(cookie);

        out.println("<P>");

        out.println("<P>");
        out.print("<form action=\"");
	out.print(response.encodeURL("MyCookies"));
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
	out.print(response.encodeURL("MyCookies"));
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
	out.print(response.encodeURL("MyCookies?dataname=foo&datavalue=bar"));
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
