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
package org.jboss.web.tomcat.security;

import java.io.IOException;
import java.security.CodeSource;
import javax.security.jacc.PolicyContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;

/**
 * A Valve that sets the JACC context id and HttpServletRequest policy
 * context handler value. The context id needs to be established prior to
 * any authorization valves.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.4 $
 */
public class JaccContextValve extends ValveBase
{
   private static Logger log = Logger.getLogger(JaccContextValve.class);
   public static ThreadLocal activeCS = new ThreadLocal();

   /** The web app metadata */
   private String contextID;
   /** The web app deployment code source */
   private CodeSource warCS;
   private boolean trace;

   public JaccContextValve(String contextID, CodeSource cs)
   {
      this.contextID = contextID;
      this.warCS = cs;
      this.trace = log.isTraceEnabled();
   }

   public void invoke(Request request, Response response)
      throws IOException, ServletException
   {
      activeCS.set(warCS);
      HttpServletRequest httpRequest = (HttpServletRequest) request.getRequest();

      try
      {
         // Set the JACC context id
         PolicyContext.setContextID(contextID);
         // Set the JACC HttpServletRequest PolicyContextHandler data
         HttpServletRequestPolicyContextHandler.setRequest(httpRequest);
         // Perform the request
         getNext().invoke(request, response);
      }
      finally
      {
         SecurityAssociationActions.clear();
         activeCS.set(null);
      }
   }

}
