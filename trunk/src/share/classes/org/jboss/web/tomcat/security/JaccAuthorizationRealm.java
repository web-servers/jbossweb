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
import java.lang.reflect.Method;
import java.security.Principal;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.security.Policy;
import java.security.CodeSource;
import java.util.Set;
import java.util.List;

import javax.security.jacc.WebUserDataPermission;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.PolicyContextException;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.jboss.logging.Logger;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.SecurityRoleRefMetaData;

/** A subclass of JBossSecurityMgrRealm that peforms authorization based on
 * the JACC permissions and active Policy implementation.
 * 
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @version $Revision: 1.10 $
 */
public class JaccAuthorizationRealm extends JBossSecurityMgrRealm
{
   static Logger log = Logger.getLogger(JaccAuthorizationRealm.class);

   /** The JACC PolicyContext key for the current Subject */
   private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";
   /** The current servlet request */
   private static ThreadLocal activeRequest = new ThreadLocal();
   private boolean trace;
   private Policy policy;

   /**
    * JBAS-2519:Delegate to JACC provider for unsecured resources in web.xml 
    */
   private boolean unprotectedResourceDelegation = false;
   private String securityConstraintProviderClass = "";

   public JaccAuthorizationRealm()
   {
      policy = Policy.getPolicy();
      trace = log.isTraceEnabled();
   }

   public boolean hasResourcePermission(Request request, Response response,
      SecurityConstraint[] securityConstraints, Context context)
      throws IOException
   {
      Wrapper servlet = request.getWrapper();
      if (servlet != null)
      {
         activeRequest.set(getServletName(servlet));
      }
      Principal requestPrincipal = request.getPrincipal();
      HttpServletRequest httpRequest = request.getRequest();
      String uri = requestURI(request);
      WebResourcePermission perm = new WebResourcePermission(uri, httpRequest.getMethod());
      boolean allowed = checkSecurityAssociation(perm, requestPrincipal);
      if( trace )
         log.trace("hasResourcePermission, perm="+perm+", allowed="+allowed);
      if( allowed == false )
      {
         response.sendError(HttpServletResponse.SC_FORBIDDEN,
            sm.getString("realmBase.forbidden"));
      }
      return allowed;
   }

   public boolean hasRole(Principal principal, String name)
   {
      // 
      String servletName = (String) activeRequest.get();
      WebMetaData metaData = (WebMetaData) SecurityAssociationValve.activeWebMetaData.get();
      List roleRefs = metaData.getSecurityRoleRefs(servletName);
      String roleName = name;
      int len = roleRefs != null ? roleRefs.size() : 0;
      for(int n = 0; n < len; n ++)
      {
         SecurityRoleRefMetaData ref = (SecurityRoleRefMetaData) roleRefs.get(n);
         if( ref.getLink().equals(name) )
         {
            roleName = ref.getName();
            break;
         }
      }
      
      WebRoleRefPermission perm = new WebRoleRefPermission(servletName, roleName);
      Principal[] principals = {principal};
      Set roles = getPrincipalRoles(principal);
      if( roles != null )
      {
         principals = new Principal[roles.size()];
         roles.toArray(principals);
      }
      boolean allowed = checkSecurityAssociation(perm, principals);
      if( trace )
         log.trace("hasRole, perm="+perm+", allowed="+allowed);
      return allowed;
   }

   public boolean hasUserDataPermission(Request request, Response response,
      SecurityConstraint[] constraints) throws IOException
   {
      HttpServletRequest httpRequest = request.getRequest();
      Principal requestPrincpal = request.getPrincipal();
      establishSubjectContext(requestPrincpal);
      String uri = requestURI(request);
      WebUserDataPermission perm = new WebUserDataPermission(uri, httpRequest.getMethod());
      if( trace )
         log.trace("hasUserDataPermission, p="+perm);
      boolean ok = false;
      try
      {
         Principal[] principals = null;
         ok = checkSecurityAssociation(perm, principals);
      }
      catch(Exception e)
      {
         if( trace )
            log.trace("Failed to checkSecurityAssociation", e);
      }

      /* If the constraint is not valid delegate to super to redirect to the
      ssl port if allowed
      */
      if( ok == false )
         ok = super.hasUserDataPermission(request, response, constraints);
      return ok;
   }

   /** 
    * Get the Security Constraints Provider Class
    * @return
    */
   public String getSecurityConstraintProviderClass()
   {
      return securityConstraintProviderClass;
   }

   /**
    * Set the Security Constraints Provider Class 
    * @param securityConstraintProviderClass
    */
   public void setSecurityConstraintProviderClass(String securityConstraintProviderClass)
   {
      this.securityConstraintProviderClass = securityConstraintProviderClass;
   }

   /**
    * Whether the delegation to JACC provider
    * for unprotected resources is enabled
    * 
    * @return
    */
   public boolean isUnprotectedResourceDelegation()
   {
      return unprotectedResourceDelegation;
   }

   /**
    * Set whether the delegation to JACC provider
    * for unprotected resources must be enabled
    * 
    * @param unprotectedResourceDelegation
    */
   public void setUnprotectedResourceDelegation(boolean unprotectedResourceDelegation)
   {
      this.unprotectedResourceDelegation = unprotectedResourceDelegation;
   }  
   
   /**
    * JBAS-2519:Delegate to JACC provider for unsecured resources in web.xml
    */
   public SecurityConstraint[] findSecurityConstraints(Request request, Context context)
   {  
      SecurityConstraint[] scarr = super.findSecurityConstraints(request, context);
      if( (scarr == null || scarr.length == 0) 
            && this.unprotectedResourceDelegation)
      {
         scarr = getSecurityConstraintsFromProvider(request, context);
      }
      return scarr;
   }

   /** See if the given JACC permission is implied using the caller as
    * obtained from either the
    * PolicyContext.getContext(javax.security.auth.Subject.container) or
    * the info associated with the requestPrincipal.
    * 
    * @param perm - the JACC permission to check
    * @param requestPrincpal - the http request getPrincipal
    * @return true if the permission is allowed, false otherwise
    */ 
   private boolean checkSecurityAssociation(Permission perm, Principal requestPrincpal)
   {
      // Get the caller
      Subject caller = establishSubjectContext(requestPrincpal);

      // Get the caller principals, its null if there is no caller
      Principal[] principals = null;
      if( caller != null )
      {
         if( trace )
            log.trace("No active subject found, using ");
         Set principalsSet = caller.getPrincipals();
         principals = new Principal[principalsSet.size()];
         principalsSet.toArray(principals);
      }
      return checkSecurityAssociation(perm, principals);
   }
   /** See if the given permission is implied by the Policy. This calls
    * Policy.implies(pd, perm) with the ProtectionDomain built from the
    * active CodeSource set by the JaccContextValve, and the given
    * principals.
    * 
    * @param perm - the JACC permission to evaluate
    * @param principals - the possibly null set of principals for the caller
    * @return true if the permission is allowed, false otherwise
    */ 
   private boolean checkSecurityAssociation(Permission perm, Principal[] principals)
   {
      CodeSource webCS = (CodeSource) JaccContextValve.activeCS.get();
      ProtectionDomain pd = new ProtectionDomain(webCS, null, null, principals);
      boolean allowed = policy.implies(pd, perm);
      if( trace )
      {
         String msg = (allowed ? "Allowed: " : "Denied: ") +perm;
         log.trace(msg);
      }
      return allowed;
   }

   /**
    * Ensure that the JACC PolicyContext Subject handler has access to the
    * authenticated Subject. The caching of the authentication state by tomcat
    * means that we need to retrieve the Subject from the JBossGenericPrincipal
    * if the realm was not invoked to authenticate the caller.
    * 
    * @param principal - the http request getPrincipal
    * @return the authenticated Subject is there is one, null otherwise
    */ 
   private Subject establishSubjectContext(Principal principal)
   {
      Subject caller = null;
      try
      {
         caller = (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
      }
      catch (PolicyContextException e)
      {
         if( trace )
            log.trace("Failed to get subject from PolicyContext", e);
      }

      if( caller == null )
      {
         // Test the request principal that may come from the session cache 
         if( principal instanceof JBossGenericPrincipal )
         {
            JBossGenericPrincipal jgp = (JBossGenericPrincipal) principal;
            caller = jgp.getSubject();
            // 
            if (trace)
               log.trace("Restoring principal info from cache");
            SecurityAssociationActions.setPrincipalInfo(jgp.getAuthPrincipal(),
               jgp.getCredentials(), jgp.getSubject());
         }
      }
      return caller;
   }
   
   /**
    * Jacc Specification : Appendix
    *  B.19 Calling isUserInRole from JSP not mapped to a Servlet
    *  Checking a WebRoleRefPermission requires the name of a Servlet to
    *  identify the scope of the reference to role translation. The name of a 
    *  scoping  servlet has not been established for an unmapped JSP.
    *  
    *  Resolution- For every security role in the web application add a
    *  WebRoleRefPermission to the corresponding role. The name of all such
    *  permissions shall be the empty string, and the actions of each
    *  permission shall be the corresponding role name. 
    *  When checking a WebRoleRefPermission from a JSP not mapped to a servlet, 
    *  use a permission with the empty string as its name and with the argument to is
    *  UserInRole as its actions.  
    * 
    * @param servlet Wrapper
    * @return empty string if it is for an unmapped jsp or name of the servlet for others 
    */
   private String getServletName(Wrapper servlet)
   {  
      //For jsp, the mapping will be (*.jsp, *.jspx)
      String[] mappings = servlet.findMappings();
      if(trace)
         log.trace("[getServletName:servletmappings="+mappings +
               ":servlet.getName()="+servlet.getName()+"]");
      if("jsp".equals(servlet.getName())
            && (mappings != null && mappings[0].indexOf("*.jsp")> -1))
      return "";
      else
         return servlet.getName();
   }
   
   /**
    * Get a set of SecurityConstraints from either the PolicyProvider
    * or the securityConstraintProviderClass class, via reflection
    * 
    * @param request
    * @param context 
    * @return an array of SecurityConstraints
    */
   private SecurityConstraint[] getSecurityConstraintsFromProvider(Request request, Context context)
   { 
      SecurityConstraint[] scarr = null;
      Class[] sig = {Request.class, Context.class};
      Object[] args = {request, context};
      
      Method findsc = null;
      
      //Try the Policy Provider 
      try
      {
         findsc = policy.getClass().getMethod("findSecurityConstraints", sig);
         scarr = (SecurityConstraint[])findsc.invoke(policy, args);
      }catch(Throwable t)
      {
         if(trace)
            log.error("Error obtaining security constraints from policy",t);
}
      //If the policy provider did not provide the security constraints
      //check if a seperate SC provider is plugged in
      if(scarr == null || scarr.length == 0)
      {
         if(securityConstraintProviderClass == "" ||
               securityConstraintProviderClass.length() == 0)
         {
            if(trace)
               log.trace("unprotectedResourceDelegation is true "+
               "but securityConstraintProviderClass is empty");
         }
         else
            //Try to call the method on the provider class
            try
         {
               Class clazz = Thread.currentThread().getContextClassLoader().loadClass(securityConstraintProviderClass);
               Object obj = clazz.newInstance(); 
               findsc = clazz.getMethod("findSecurityConstraints", sig); 
               if(trace)
                  log.trace("findSecurityConstraints method found in securityConstraintProviderClass");
               scarr = (SecurityConstraint[])findsc.invoke(obj, args);
         }
         catch (Throwable t)
         {
            log.error("Error instantiating "+securityConstraintProviderClass,t);
         }   
      } 
      return scarr;
   }

   /**
    * Get the canonical request uri from the request mapping data requestPath
    * @param request
    * @return the request URI path
    */
   static String requestURI(Request request)
   {
      String uri = request.getMappingData().requestPath.getString();
      if( uri == null || uri.equals("/") )
      {
         uri = "";
      }
      return uri;
   }
   
}
