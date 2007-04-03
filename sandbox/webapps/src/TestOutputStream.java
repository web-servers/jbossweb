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
 * Example servlet for tests:
 * 0 - explain how to use it.
 * 1 - to send a big amount of data using OutputStream
 * 2 - to close the ServletOutputStream.
 *
 */

public class TestOutputStream  extends HttpServlet {

    public void doTest1(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        InputStream in = request.getInputStream();
        response.setContentType("text/html");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i=0; i<90112;i++)
           baos.write('A');
        
        OutputStream outs = response.getOutputStream();
        baos.writeTo(outs);
        outs.flush();
        outs.close();

    }

    public void doTest2(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        ServletOutputStream responseStream = null;
        try {
            responseStream = response.getOutputStream();
            responseStream.print("ok....");
            responseStream.close();
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        String test = request.getParameter("test");
        if (test == null) {
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<body>");
            out.println("<head>");

            String title = "Testing outputstream";
            out.println("<title>" + title + "</title>");
            out.println("</head>");
            out.println("<body bgcolor=\"white\">");
            out.println("<h3>" + title + "</h3>");
            out.println("use bla?test=1 for outputstream<br/>");
            out.println("use bla?test=2 for closing it<br/>");
            out.println("</body>");
            out.println("</html>");
        } else if (test.equals("1")) {
            doTest1(request, response);
        } else if (test.equals("2")) {
            doTest2(request, response);
        }
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
