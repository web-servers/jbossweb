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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.Principal;
import java.security.acl.Group; 
import java.util.Enumeration; 
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.security.jacc.PolicyContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request;
import org.jboss.logging.Logger;
import org.jboss.security.AuthorizationManager; 
import org.jboss.security.SimplePrincipal; 

import com.sun.xacml.Indenter;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.TimeAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

//$Id: WebXACMLUtil.java,v 1.1 2006/07/26 03:35:23 asaldhana Exp $

/**
 *  Utility class for creating XACML Requests
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Jun 21, 2006 
 *  @version $Revision: 1.1 $
 */
public class WebXACMLUtil
{
   private static Logger log = Logger.getLogger(WebXACMLUtil.class);
   private boolean trace = log.isTraceEnabled();
   
   public WebXACMLUtil()
   {   
   }
   
   public RequestCtx createXACMLRequest(Request request,
         AuthorizationManager authzManager) throws Exception
   {
      HttpServletRequest httpRequest = (HttpServletRequest)request;
      String httpMethod = httpRequest.getMethod();
      String action = "GET".equals(httpMethod)?"read":"write";
      
      //Non-standard uri
      String actionURIBase = "urn:oasis:names:tc:xacml:2.0:request-param:attribute:";
      
      RequestCtx requestCtx = null;
      Principal principal = request.getPrincipal();
      String username = getUserName(); 
      //Get the roles from the authorization manager
      Set roles = authzManager.getUserRoles(principal);
      //Create the subject set
      URI subjectAttrUri = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
      Attribute subjectAttr = new Attribute(subjectAttrUri,null,null,
            new StringAttribute(username));
      Set subjectAttrSet = new HashSet();
      subjectAttrSet.add(subjectAttr);
      subjectAttrSet.addAll(getXACMLRoleSet(roles));
      
      Set subjectSet = new HashSet();
      subjectSet.add(new Subject(subjectAttrSet));
      
      //Create the resource set
      URI resourceUri = new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id");
      Attribute resourceAttr = new Attribute(resourceUri,null,null,
            new AnyURIAttribute(new URI(getRequestURI(request))));
      Set resourceSet = new HashSet();
      resourceSet.add(resourceAttr); 
      
      //Create the action set
      Set actionSet = new HashSet();
      actionSet.add(new Attribute(new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
             null,null, new StringAttribute(action)));
      
      Enumeration enumer = request.getParameterNames();
      while(enumer.hasMoreElements())
      {
         String paramName = (String)enumer.nextElement();
         String paramValue = request.getParameter(paramName);
         URI actionUri = new URI(actionURIBase + paramName);
         Attribute actionAttr = new Attribute(actionUri,null,null,
               new StringAttribute(paramValue));
         actionSet.add(actionAttr); 
      }
      //Create the Environment set
      Set environSet = new HashSet();
      //Current time
      URI currentTimeUri = new URI("urn:oasis:names:tc:xacml:1.0:environment:current-time");
      Attribute currentTimeAttr = new Attribute(currentTimeUri,null,null,
            new TimeAttribute());
      environSet.add(currentTimeAttr);
      
      //Create the request context
      requestCtx = new RequestCtx(subjectSet,resourceSet,actionSet,environSet);
      
      if(trace)
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         requestCtx.encode(baos, new Indenter());
         log.trace("XACML Request:"+baos.toString());
         baos.close();
      }
      return requestCtx;
   } 
   
   private Set getXACMLRoleSet(Set roles) throws Exception
   {
      URI roleURI = new URI("urn:oasis:names:tc:xacml:2.0:example:attribute:role");
   
      Set roleset = new HashSet();
      Iterator iter = roles != null ? roles.iterator(): null;
      while(iter != null && iter.hasNext())
      {
         Principal role = (Principal)iter.next();
         if(role instanceof SimplePrincipal)
         {
            SimplePrincipal sp = (SimplePrincipal)role;
            Attribute roleAttr = new Attribute(roleURI,null,null,
                new StringAttribute(sp.getName()));
            roleset.add(roleAttr); 
         }
      }
      return roleset;
   }
   
   private String getRequestURI(Request request)
   {
      String requestUri = request.getRequestURI();
      return requestUri;
   }
   
   private String getUserName() throws Exception
   {
      String user = "";
      String key = "javax.security.auth.Subject.container";
      javax.security.auth.Subject caller = (javax.security.auth.Subject) PolicyContext.getContext(key);
      Iterator iter = caller.getPrincipals().iterator();
      while(iter.hasNext())
      {
         Principal p = (Principal)iter.next();
         if(p instanceof SimplePrincipal && !(p instanceof Group))
         {
            SimplePrincipal sp = (SimplePrincipal)p;
            user= sp.getName();
         }
      }
      return user;
   } 
}
