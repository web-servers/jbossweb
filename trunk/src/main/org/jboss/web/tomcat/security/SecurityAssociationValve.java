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
import java.security.Principal;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.security.auth.Subject;

import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.Manager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.jboss.metadata.WebMetaData;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.plugins.JaasSecurityManagerServiceMBean;

/**
 * A Valve that sets/clears the SecurityAssociation information associated with
 * the request thread for identity propagation.
 *
 * @author Scott.Stark@jboss.org
 * @author Thomas.Diesler@jboss.org
 * @version $Revision: 1.25 $
 */
public class SecurityAssociationValve extends ValveBase
{
   private static Logger log = Logger.getLogger(SecurityAssociationValve.class);
   public static ThreadLocal userPrincipal = new ThreadLocal();
   /** Maintain the active WebMetaData for request security checks */
   public static ThreadLocal activeWebMetaData = new ThreadLocal();

   /** The web app metadata */
   private WebMetaData metaData;
   /** The name in the session under which the Subject is stored */
   private String subjectAttributeName = null;
   /** The service used to flush authentication cache on session invalidation. */
   private JaasSecurityManagerServiceMBean secMgrService;
   private boolean trace;

   public SecurityAssociationValve(WebMetaData metaData,
      JaasSecurityManagerServiceMBean secMgrService)
   {
      this.metaData = metaData;
      this.secMgrService = secMgrService;
      this.trace = log.isTraceEnabled();
   }

   /**
    * The name of the request attribute under with the authenticated JAAS
    * Subject is stored on successful authentication. If null or empty then
    * the Subject will not be stored.
    */
   public void setSubjectAttributeName(String subjectAttributeName)
   {
      this.subjectAttributeName = subjectAttributeName;
      if (subjectAttributeName != null && subjectAttributeName.length() == 0)
         this.subjectAttributeName = null;
   }

   public void invoke(Request request, Response response)
           throws IOException, ServletException
   {
      Session session = null;
      // Get the request caller which could be set due to SSO
      //Principal caller = request.getUserPrincipal();
      Principal caller = request.getPrincipal();
      // The cached web container principal
      JBossGenericPrincipal principal = null;
      HttpSession hsession = request.getSession(false);

      if( trace )
         log.trace("Begin invoke, caller"+caller);
      // Set the active meta data
      activeWebMetaData.set(metaData);
      try
      {
         try
         {
            Wrapper servlet = request.getWrapper();
            if (servlet != null)
            {
               String name = servlet.getName();
               RunAsIdentity identity = metaData.getRunAsIdentity(name);
               if (identity != null)
               {
                  if (trace)
                     log.trace(name + ", runAs: " + identity);
               }
               SecurityAssociationActions.pushRunAsIdentity(identity);
            }
            userPrincipal.set(caller);

            // If there is a session, get the tomcat session for the principal
            Manager manager = container.getManager();
            if (manager != null && hsession != null)
            {
               try
               {
                  session = manager.findSession(hsession.getId());
               }
               catch (IOException ignore)
               {
               }
            }

            if (caller == null || (caller instanceof JBossGenericPrincipal) == false)
            {
               // Look to the session for the active caller security context
               if (session != null)
               {
                  principal =
                     (JBossGenericPrincipal) session.getPrincipal();
               }
            }
            else
            {
               // Use the request principal as the caller identity
               principal = (JBossGenericPrincipal) caller;
            }

            // If there is a caller use this as the identity to propagate
            if (principal != null)
            {
               if (trace)
                  log.trace("Restoring principal info from cache");
               SecurityAssociationActions.setPrincipalInfo(principal.getAuthPrincipal(),
                  principal.getCredentials(), principal.getSubject());
            }
            // Put the authenticated subject in the session if requested
            if (subjectAttributeName != null)
            {
               javax.naming.Context securityCtx = getSecurityContext();
               if (securityCtx != null)
               {
                  // Get the JBoss security manager from the ENC context
                  AuthenticationManager securityMgr = (AuthenticationManager) securityCtx.lookup("securityMgr");
                  Subject subject = securityMgr.getActiveSubject();
                  request.getRequest().setAttribute(subjectAttributeName, subject);
               }
            }
         }
         catch (Throwable e)
         {
            log.debug("Failed to determine servlet", e);
         }
         // Perform the request
         getNext().invoke(request, response);
         SecurityAssociationActions.popRunAsIdentity();

         /* If the security domain cache is to be kept in synch with the
         session then flush the cache if the session has been invalidated.
         */
         if( secMgrService != null &&
            session != null && session.isValid() == false &&
            metaData.isFlushOnSessionInvalidation() == true )
         {
            if( principal != null )
            {
               String securityDomain = metaData.getSecurityDomain();
               if (trace)
               {
                  log.trace("Session is invalid, security domain: "+securityDomain
                     +", user="+principal);
               }
               try
               {
                  Principal authPrincipal = principal.getAuthPrincipal();
                  secMgrService.flushAuthenticationCache(securityDomain, authPrincipal);
               }
               catch(Exception e)
               {
                  log.debug("Failed to flush auth cache", e);
               }
            }
         }
      }
      finally
      {
         if( trace )
            log.trace("End invoke, caller"+caller);
         activeWebMetaData.set(null);
         userPrincipal.set(null);
      }
   }

   private javax.naming.Context getSecurityContext()
   {
      javax.naming.Context securityCtx = null;
      // Get the JBoss security manager from the ENC context
      try
      {
         InitialContext iniCtx = new InitialContext();
         securityCtx = (javax.naming.Context) iniCtx.lookup("java:comp/env/security");
      }
      catch (NamingException e)
      {
         // Apparently there is no security context?
      }
      return securityCtx;
   }
}
