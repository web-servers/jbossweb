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
package org.jboss.web.tomcat.security.authorization.delegates;

import java.io.IOException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.jboss.logging.Logger;
import org.jboss.security.authorization.AuthorizationContext;
import org.jboss.security.authorization.PolicyRegistration;
import org.jboss.security.authorization.Resource;
import org.jboss.security.authorization.ResourceKeys;
import org.jboss.security.authorization.modules.AuthorizationModuleDelegate;
import org.jboss.web.tomcat.security.JaccContextValve;


//$Id: WebJACCPolicyModuleDelegate.java,v 1.4 2006/07/26 03:35:23 asaldhana Exp $

/**
 *  JACC based authorization module helper that deals with the web layer 
 *  authorization decisions
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  July 7, 2006 
 *  @version $Revision: 1.4 $
 */
public class WebJACCPolicyModuleDelegate extends AuthorizationModuleDelegate
{   
   private Policy policy = Policy.getPolicy();

   public WebJACCPolicyModuleDelegate()
   {  
      log = Logger.getLogger(WebJACCPolicyModuleDelegate.class);
      trace = log.isTraceEnabled();
   }

   /**
    * @see AuthorizationModuleDelegate#authorize(Resource)
    */
   public int authorize(Resource resource)
   {
      //Get the contextual map
      Map map = resource.getMap();
      if(map == null)
         throw new IllegalStateException("Map from the Resource is null");

      if(map.size() == 0)
         throw new IllegalStateException("Map from the Resource is size zero");
      //Get the Catalina Request Object
      Request request = (Request)map.get(ResourceKeys.WEB_REQUEST);
      Response response = (Response)map.get(ResourceKeys.WEB_RESPONSE);
      SecurityConstraint[] constraints = (SecurityConstraint[])
                                    map.get(ResourceKeys.WEB_SECURITY_CONSTRAINTS);
      Context context = (Context)map.get(ResourceKeys.WEB_CONTEXT); 
      //Obtained by establishing subject context
      Subject callerSubject = (Subject)map.get(ResourceKeys.CALLER_SUBJECT); 
      String roleName = (String)map.get(ResourceKeys.ROLENAME);
      Principal principal = (Principal)map.get(ResourceKeys.HASROLE_PRINCIPAL);
      Set roles = (Set)map.get(ResourceKeys.PRINCIPAL_ROLES); 
      String servletName = (String)map.get(ResourceKeys.SERVLET_NAME);
      Boolean resourceCheck = checkBooleanValue((Boolean)map.get(ResourceKeys.RESOURCE_PERM_CHECK));
      Boolean userDataCheck = checkBooleanValue((Boolean)map.get(ResourceKeys.USERDATA_PERM_CHECK));
      Boolean roleRefCheck = checkBooleanValue((Boolean)map.get(ResourceKeys.ROLEREF_PERM_CHECK)); 
      
      validatePermissionChecks(resourceCheck,userDataCheck,roleRefCheck);
      
      boolean decision = false;
      
      try
      {
         if(resourceCheck)
            decision = this.hasResourcePermission(request, response, constraints, context, callerSubject);
         else
            if(userDataCheck)
               decision = this.hasUserDataPermission(request, response, constraints);
            else
               if(roleRefCheck)
                 decision = this.hasRole(principal, roleName, roles, servletName);
               else
                  if(trace)
                     log.trace("Check is not for resourcePerm, userDataPerm or roleRefPerm.");
      }
      catch(IOException ioe)
      {
         if(trace)
            log.trace("IOException:",ioe);
      } 
      return decision ? AuthorizationContext.PERMIT : AuthorizationContext.DENY;
   }

   /**
    * @see AuthorizationModuleDelegate#setPolicyRegistrationManager(PolicyRegistration)
    */
   public void setPolicyRegistrationManager(PolicyRegistration authzM)
   { 
     this.authzManager = authzM;
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

   //****************************************************************************
   //  PRIVATE METHODS
   //****************************************************************************
   /** See if the given JACC permission is implied using the caller as
    * obtained from either the
    * PolicyContext.getContext(javax.security.auth.Subject.container) or
    * the info associated with the requestPrincipal.
    * 
    * @param perm - the JACC permission to check
    * @param requestPrincpal - the http request getPrincipal
    * @param caller the authenticated subject obtained by establishSubjectContext
    * @return true if the permission is allowed, false otherwise
    */ 
   private boolean checkSecurityAssociation(Permission perm, Principal requestPrincpal,
         Subject caller)
   {  
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
    * Ensure that the bool is a valid value
    * @param bool
    * @return bool or Boolean.FALSE (when bool is null)
    */
   private Boolean checkBooleanValue(Boolean bool)
   {
      if(bool == null)
         return Boolean.FALSE;
      return bool;
   } 

   
   /**
    * Perform hasResourcePermission Check
    * @param request
    * @param response
    * @param securityConstraints
    * @param context
    * @param caller
    * @return
    * @throws IOException
    */
   private boolean hasResourcePermission(Request request, Response response,
         SecurityConstraint[] securityConstraints, Context context, Subject caller)
   throws IOException
   { 
      Principal requestPrincipal = request.getPrincipal();
      HttpServletRequest httpRequest = request.getRequest();
      String uri = requestURI(request);
      WebResourcePermission perm = new WebResourcePermission(uri, httpRequest.getMethod());
      boolean allowed = checkSecurityAssociation(perm, requestPrincipal, caller );
      if( trace )
         log.trace("hasResourcePermission, perm="+perm+", allowed="+allowed); 
      return allowed;
   }

   /**
    * Perform hasRole check
    * @param principal
    * @param role
    * @param roles
    * @return
    */
   private boolean hasRole(Principal principal, String roleName, Set roles, String servletName)
   {  
      WebRoleRefPermission perm = new WebRoleRefPermission(servletName, roleName);
      Principal[] principals = {principal}; 
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

   /**
    * Perform hasUserDataPermission check for the realm.
    * If this module returns false, the base class (Realm) will
    * make the decision as to whether a redirection to the ssl
    * port needs to be done
    * @param request
    * @param response
    * @param constraints
    * @return
    * @throws IOException
    */
   private boolean hasUserDataPermission(Request request, Response response,
         SecurityConstraint[] constraints) throws IOException
   {
      HttpServletRequest httpRequest = request.getRequest(); 
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
      return ok;
   }

   /**
    * Validate that the access check is made only for one of the 
    * following
    * @param resourceCheck
    * @param userDataCheck
    * @param roleRefCheck
    */
   private void validatePermissionChecks(Boolean resourceCheck,
         Boolean userDataCheck, Boolean roleRefCheck)
   {
      if(trace)
         log.trace("resourceCheck="+resourceCheck + " : userDataCheck=" + userDataCheck
               + " : roleRefCheck=" + roleRefCheck); 
      if((resourceCheck == Boolean.TRUE && userDataCheck == Boolean.TRUE && roleRefCheck == Boolean.TRUE ) 
           || (resourceCheck == Boolean.TRUE && userDataCheck == Boolean.TRUE) 
           || (userDataCheck == Boolean.TRUE && roleRefCheck == Boolean.TRUE))
         throw new IllegalStateException("Permission checks must be different"); 
   }
}
