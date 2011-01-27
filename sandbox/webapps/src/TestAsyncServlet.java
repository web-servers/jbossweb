/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


// package org.jboss.web.comet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.annotation.WebServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.AsyncListener;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;

/* Asynchronous example */
// @WebServlet("/TestAsyncServlet")
@WebServlet(urlPatterns = {"/TestAsyncServlet"}, asyncSupported = true)
public class TestAsyncServlet extends HttpServlet {
   public void doGet(HttpServletRequest req, HttpServletResponse res) {
        // Servlet Code
        // ...........
        // Call startAsync
        AsyncContext context = req.startAsync();
        // Give AsyncContext to the Listener MonListener
        context.addListener(new MonListener());
        // ...........
        context.complete();
   }
   public class MonListener implements AsyncListener {

       public  void onComplete(AsyncEvent event) throws IOException {
         System.out.println("onComplete");
          event.getAsyncContext().getResponse().getWriter().println("onComplete");
       }
       public  void onError(AsyncEvent event) throws IOException {
         ServletResponse res = event.getSuppliedResponse();
         System.out.println("onError: " + res);
         event.getAsyncContext().getResponse().getWriter().println("onError");
       }
       public  void onTimeout(AsyncEvent event) throws IOException {
         ServletResponse res = event.getSuppliedResponse();
         try {
           ServletOutputStream os = res.getOutputStream();
           System.out.println("onTimeout: " + res);
         } catch (Exception e) {
         }
         event.getAsyncContext().getResponse().getWriter().println("onTimeout");
       }
       public  void onStartAsync(AsyncEvent event) throws IOException {
         System.out.println("onStartAsync");
         event.getAsyncContext().getResponse().getWriter().println("onStartAsync");
       }
   }

}
