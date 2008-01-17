/*
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class for Servlet: TestServlet
 *
 */
public class TestDispatch extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

        protected void writeHead(HttpServletResponse response) throws ServletException, IOException {
              response.setContentType("text/html");
              PrintWriter out = response.getWriter();
              out.println("<html>");
              out.println("<body bgcolor=\"white\">");
              out.println("<head>");

              String title = "src/TestDispatch.java";
              out.println("<title>" + title + "</title>");
              out.println("</head>");
              out.println("<body>");
              out.println("<h3>" + title + "</h3>");
        }

        protected void writeRest(HttpServletResponse response, String string) throws ServletException, IOException {
              PrintWriter out = response.getWriter();
              if (string != null) {
                  out.println(string);
              }
              out.println("</body>");
              out.println("</html>");
        }
        protected void writeEx(HttpServletResponse response, Exception e) throws ServletException, IOException {
              writeHead(response);
              PrintWriter out = response.getWriter();
	      e.printStackTrace(out);
              out.println("</body>");
              out.println("</html>");
        }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	      ServletContext sc = getServletContext();
              String context = request.getParameter("context");
              String servlet = request.getParameter("servlet");

              if (context == null || servlet == null) {
                 writeHead(response);
                 writeRest(response, "Please enter the context and the servlet: ?context=/php-examples&servlet=/index.php");
                 return;
              }
	      ServletContext sc2 = sc.getContext(context);
	      RequestDispatcher rd = sc2.getRequestDispatcher(servlet);
	      
	      try
	      {
	         rd.include(request, response);
	         response.flushBuffer();
	      }
	      catch (ServletException e)
	      {
	         writeEx(response, e);
	      }
	      catch (IOException e)
	      {
	         writeEx(response, e);
	      }
	}  	
}
