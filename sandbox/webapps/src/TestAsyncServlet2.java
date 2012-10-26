/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, JBoss Inc., and individual contributors as indicated
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
@WebServlet(urlPatterns = {"/TestAsyncServlet2"}, asyncSupported = true)
public class TestAsyncServlet2 extends HttpServlet {
   public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.getWriter().println("ASYNC_NOT_STARTED_dispatchContextPathTest");
        response.getWriter().println("IsAsyncSupported=" +
                request.isAsyncSupported());
        response.getWriter().println("IsAsyncStarted=" +
                request.isAsyncStarted());
        response.getWriter().println("DispatcherType=" +
                request.getDispatcherType());
        AsyncContext ac = request.startAsync();
   }
}
