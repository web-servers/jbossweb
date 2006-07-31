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

import java.util.Map;  

import javax.security.jacc.PolicyContext;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.SecurityConstraint;
import org.jboss.logging.Logger; 
import org.jboss.security.authorization.AuthorizationContext; 
import org.jboss.security.authorization.Resource; 
import org.jboss.security.authorization.PolicyRegistration;
import org.jboss.security.authorization.modules.AuthorizationModuleDelegate;
import org.jboss.security.authorization.sunxacml.JBossXACMLUtil;

import com.sun.xacml.Policy;
import com.sun.xacml.ctx.RequestCtx; 

//$Id: WebXACMLPolicyModuleDelegate.java,v 1.1 2006/07/26 03:35:23 asaldhana Exp $

/**
 *  XACML based authorization module helper that deals with the web layer 
 *  authorization decisions
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Jun 13, 2006 
 *  @version $Revision: 1.1 $
 */
public class WebXACMLPolicyModuleDelegate extends AuthorizationModuleDelegate
{  
   
   public WebXACMLPolicyModuleDelegate()
   {  
      log = Logger.getLogger(getClass());
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
      Request request = (Request)map.get("catalina.request");
      SecurityConstraint[] constraints = (SecurityConstraint[])map.get("catalina.constraints");
      PolicyRegistration pr = (PolicyRegistration)map.get("authorizationManager");
      if(pr != null)
        this.authzManager = pr;
      return process(request, constraints);
   }

   /**
    * @see AuthorizationModuleDelegate#setPolicyRegistrationManager(PolicyRegistration)
    */
   public void setPolicyRegistrationManager(PolicyRegistration authzM)
   {  
      this.authzManager =  authzM;
   }
   
   /**
    * Process the web request
    * @param request
    * @param sc
    * @return
    */
   private int process(Request request, SecurityConstraint[] sc) 
   { 
      int result = AuthorizationContext.DENY;
      WebXACMLUtil util = new WebXACMLUtil();
      try
      {
         RequestCtx requestCtx = util.createXACMLRequest(request,this.authzManager);
         String contextID = PolicyContext.getContextID();
         Policy policy = (Policy)authzManager.getPolicy(contextID,null);
         if(policy == null)
            throw new IllegalStateException("Missing xacml policy for contextid:"+contextID);
         result = JBossXACMLUtil.checkXACMLAuthorization(requestCtx,policy);
      }
      catch(Exception e)
      {
         if(trace)
            log.trace("Exception in processing:",e);
         result = AuthorizationContext.DENY;
      }  
      return result;
   } 
 }
