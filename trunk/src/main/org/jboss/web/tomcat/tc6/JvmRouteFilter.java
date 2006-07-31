/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.web.tomcat.tc6;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Session;
import org.jboss.logging.Logger;
import org.jboss.util.NestedRuntimeException;
import org.jboss.web.tomcat.tc6.session.AbstractJBossManager;

/**
 * Web request filter to specifically handle Tomcat jvmRoute using mod_jk(2)
 * module. We assume that the session is set by cookie only for now, i.e., no
 * support of that from URL. Furthermore, the session id has a format of
 * id.jvmRoute where jvmRoute is used by JK module to determine sticky session
 * during load balancing.
 *
 * @author Ben Wang
 * @author Marco Antonioni
 * @version $Revision: 1.1 $
 * @deprecated 4.0.3
 */
public class JvmRouteFilter
   implements Filter
{
   protected AbstractJBossManager manager_;
   protected static Logger log_ = Logger.getLogger(JvmRouteFilter.class);

   public void init(FilterConfig filterConfig) throws ServletException
   {
      if (log_.isDebugEnabled())
      {
         ServletContext sc = filterConfig.getServletContext();
         Enumeration names = sc.getAttributeNames();
         while (names.hasMoreElements())
         {
            String name = (String) names.nextElement();
            Object value = sc.getAttribute(name);
            log_.debug("name=" + name + ", value.className: [" + value.getClass().getName() + "] value.toString: [" + value.toString() + "]");
         }
      }
      manager_ = (AbstractJBossManager) filterConfig.getServletContext().getAttribute("AbstractJBossManager");
      if (manager_ == null)
      {
         throw new RuntimeException("JvmRouteFilter.init(): No AbstractJBossManager found for clustering support.");
      }

      if (log_.isDebugEnabled())
         log_.debug("JvmRouteFilter.init(): initializing JvmRouteFilter");
   }

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
   {
      // Check if request and response is valid
      if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse))
      {
         // Don't know how to handle. There is a mistake.
         throw new RuntimeException("JvmRouteFilter.doFilter(): Not a http request and response type.");
      }

      // get session id
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse res = (HttpServletResponse) response;
      HttpSession session = req.getSession(false);
      if (session != null)
      {
         String sessionId = session.getId();
   
         // Obtain JvmRoute
         String jvmRoute = manager_.getJvmRoute();
         if (log_.isDebugEnabled())
         {
            log_.debug("doFilter(): check if need to re-route based on JvmRoute. Session id: " +
               sessionId + " jvmRoute: " + jvmRoute);
         }

         if (jvmRoute == null)
         {
            throw new RuntimeException("JvmRouteFilter.doFilter(): Tomcat JvmRoute is null. " +
               "Need to assign a value in Tomcat server.xml for load balancing.");
         }
   
         // Check if incoming session id has JvmRoute appended. If not, append it.
         // TODO. We handle only get session id by cookie only
         if (req.isRequestedSessionIdFromURL())
         {
            // Warning but do nothing
            log_.error("JvmRouteFilter.doFilter(): Can't handle clustering where session id is from URL. Will skip.");
         }
         else
         {
            handleJvmRoute(sessionId, jvmRoute, res);
         }
      }
      chain.doFilter(request, response);
   }

   protected void handleJvmRoute(String sessionId, String jvmRoute, HttpServletResponse response)
   {
      // get requested jvmRoute.
      // TODO. The current format is assumed to be id.jvmRoute. Can be generalized later.
      String requestedJvmRoute = null;
      int ind = sessionId.indexOf(".");
      if (ind > 0)
      {
         requestedJvmRoute = sessionId.substring(sessionId.indexOf(".") + 1, sessionId.length());
      }

      String sid = sessionId;
      if (requestedJvmRoute == null)
      {
         // If this filter is turned on, we assume we have an appendix of jvmRoute. So this request is new.
         sid = sessionId + "." + jvmRoute;
         manager_.setNewSessionCookie(sid, response);
      }
      else if (requestedJvmRoute.equals(jvmRoute))
      {
         return;  // Nothing more needs to be done.
      }
      else
      {
         // We just have a failover since jvmRoute does not match. We will replace the old one with the new one.
         if (log_.isDebugEnabled())
         {
            log_.debug("handleJvmRoute(): We have detected a failover with differen jvmRoute." +
               " old one: " + requestedJvmRoute + " new one: " + jvmRoute + ". Will reset the session id.");
         }
         int index = sessionId.indexOf(".");
         if (index > 0)
         {
            String base = sessionId.substring(0, sessionId.indexOf("."));
            sid = base + "." + jvmRoute;
         }
         else
         {
            throw new RuntimeException("JvmRouteFilter.handleJvmRoute(): session id doesn't JvmRoute.");
         }

         manager_.setNewSessionCookie(sid, response);
         // Change the sessionId with the new one using local jvmRoute
         Session catalinaSession = null;
         try
         {
            catalinaSession = manager_.findSession(sessionId);
            // change session id with the new one using local jvmRoute and update cluster if needed.
            if( catalinaSession != null )
            {
               catalinaSession.setId(sid);
               if (log_.isDebugEnabled())
               {
                  log_.debug("handleJvmRoute(): changed catalina session to= [" + sid + "] old one= [" + sessionId + "]");
               }
            }
         }
         catch (IOException e)
         {
            if (log_.isDebugEnabled())
            {
               log_.debug("handleJvmRoute(): manager_.findSession() unable to find session= [" + sessionId + "]", e);
            }
            throw new NestedRuntimeException("JvmRouteFilter.handleJvmRoute(): cannot find session [" + sessionId + "]", e);
         }
      }
   }


   public void destroy()
   {
      manager_ = null;
   }
}
