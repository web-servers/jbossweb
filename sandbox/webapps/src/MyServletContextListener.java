/*
 *  Copyright(c) 2010 Red Hat Middleware, LLC,
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

import javax.servlet.annotation.WebListener;

/**
 * ServletContextListener that adds a Servlet.
 * 3.0 example.
 */

@WebListener
public class MyServletContextListener implements ServletContextListener {
  public void contextInitialized(ServletContextEvent event) {
    System.out.println("contextInitialized");
    try {
      ServletContext sc = event.getServletContext();
      Class<MyCount> servletCl = (Class<MyCount>) Class.forName("MyCount");
      MyCount servlet = sc.createServlet(servletCl);
      ServletRegistration.Dynamic sr = (ServletRegistration.Dynamic) sc.addServlet("test.MyCount", servlet);
      sr.addMapping("/newMyCount");
    } catch (Exception e) {
      System.out.println("contextInitialized failed: " + e);
    }
  }
  public void contextDestroyed(ServletContextEvent event) {
    System.out.println("contextDestroyed");
  }
}
