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
 * Example servlet to test huge post.
 *
 */

public class BigField  extends HttpServlet {


    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<head>");

        String title = "big input field via " + request.getMethod();;
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body bgcolor=\"white\">");

        // img stuff not req'd for source code html showing

	// all links relative

        // XXX
        // making these absolute till we work out the
        // addition of a PathInfo issue 
	
        out.println("<h3>" + title + "</h3>");

/* It is not possible to read the parameter and use The InputStream of getInputStream().
        String firstName = request.getParameter("firstname");
        String lastName = request.getParameter("lastname");
        if (firstName != null || lastName != null) {
            out.println("requestparams.firstname");
            out.println(" = " + firstName + "<br>");
            out.println("requestparams.lastname");
            out.println(" = " + lastName);
        } else {
            Enumeration e = request.getParameterNames();
            out.println("requestparams.unex-params " + e);
            while (e.hasMoreElements()) {
                out.println("requestparams.unex-params name: " + e.nextElement());
            }
        }
        out.println("<P>");
 */

/*
        // ObjectInputStream.
        try {
            ObjectInputStream objInStream = 
				new ObjectInputStream(request.getInputStream());
            out.println("Size of input: " + objInStream.available());
        } catch (IOException ex) {
            ex.printStackTrace(out);
            out.println("ObjectInputStream: Merde bug velu!!!");
            out.println("<P>");
        }

*/
/* read */
        // Read the inputstream.
        InputStream in = request.getInputStream();
        if (in!=null) {
            byte[] buff =  new byte[128];
            int i=0;
            try {
                int ret=0;
                while (ret!=-1) {
                    ret  = in.read(buff);
                    if (ret>0) {
                        i = i + ret;
                        String str = new String(buff,0,ret);
                        // JFC try out.println(str);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(out);
                out.println("Merde bug velu!!!");
                out.println("<P>");
            }
            out.println("Size of input: " + i);
        } else {
            out.println("No input");
        }

        /* Try to read a user certificate */
        Object object = request.getAttribute("javax.servlet.request.X509Certificate");
        if (object != null)
            out.println("<P> Has a javax.servlet.request.X509Certificate </P>");

/* read */

        out.println("<P>");
        out.print("<form action=\"");
        out.print("BigField\" ");
        out.println("method=POST>");
        out.println("requestparams.firstname");
        out.println("<input type=text size=20 name=firstname>");
        out.println("<br>");
        out.println("requestparams.lastname");
        out.println("<input type=text size=20 name=lastname>");
        out.println("<br>");
        out.println("<textarea WRAP=HARD NAME=comment ROWS=10 COLS=80>");
        out.println("</textarea>");
        out.println("<br>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

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
/*
<!-- Something like that is needed!!!
<textarea WRAP=HARD NAME=comment ROWS=10 COLS=80></textarea><br><br>
 -->
 */
