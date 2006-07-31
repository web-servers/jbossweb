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
package org.jboss.web.metamodel.descriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.metamodel.descriptor.EnvironmentRefGroup;
import org.jboss.metamodel.descriptor.Listener;
import org.jboss.metamodel.descriptor.MessageDestination;

/**
 * Represents the web.xml deployment descriptor for the 2.5 schema
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 1.3 $</tt>
 */
public class WebDD extends EnvironmentRefGroup
{
   private static final Logger log = Logger.getLogger(WebDD.class);
   
   protected String securityDomain;
   protected HashMap filters = new HashMap();
   protected HashMap filterMappings = new HashMap();
   protected HashMap listeners = new HashMap();
   protected HashMap servlets = new HashMap();
   protected HashMap servletMappings = new HashMap();
   protected List sessionConfigs = new ArrayList();
   protected List securityConstraints = new ArrayList();
   protected HashMap securityRoles = new HashMap();
   protected LoginConfig loginConfig;
   protected HashMap errorPages = new HashMap();
   protected HashMap messageDestinations = new HashMap();
   protected List dependencies = new ArrayList();
   protected ReplicationConfig replicationConfig;
   
   public String getSecurityDomain()
   {
      return securityDomain;
   }
   
   public void setSecurityDomain(String securityDomain)
   {
      this.securityDomain = securityDomain;
   }
   
   public Collection getFilters()
   {
      return filters.values();
   }

   public void addFilter(Filter filter)
   {
      filters.put(filter.getName(), filter);
   }
   
   public Collection getFilterMappings()
   {
      return filterMappings.values();
   }

   public void addFilterMapping(FilterMapping mapping)
   {
      filterMappings.put(mapping.getFilterName(), mapping);
   }
   
   public Collection getListeners()
   {
      return listeners.values();
   }

   public void addListener(Listener listener)
   {
      listeners.put(listener.getListenerClass(), listener);
   }
   
   public Collection getServlets()
   {
      return servlets.values();
   }

   public void addServlet(Servlet servlet)
   {
      servlets.put(servlet.getName(), servlet);
   }
   
   public void updateServlet(Servlet updatedServlet)
   {
      Servlet servlet = (Servlet)servlets.get(updatedServlet.getName());
      if (servlet != null)
      {
         servlet.setRunAsPrincipals(updatedServlet.getRunAsPrincipals());
      }
      else
      {
         servlets.put(updatedServlet.getName(), updatedServlet);
      }
   }
   
   public Collection getServletMappings()
   {
      return servletMappings.values();
   }

   public void addServletMapping(ServletMapping mapping)
   {
      servletMappings.put(mapping.getName(), mapping);
   }
   
   public Collection getSessionConfigs()
   {
      return sessionConfigs;
   }

   public void addSessionConfig(SessionConfig config)
   {
      sessionConfigs.add(config);
   }
   
   public Collection getSecurityRoles()
   {
      return securityRoles.values();
   }

   public void addSecurityRole(SecurityRole securityRole)
   {
      securityRoles.put(securityRole.getRoleName(), securityRole);
   }
   
   public void updateSecurityRole(SecurityRole updatedRole)
   {
      SecurityRole role = (SecurityRole)securityRoles.get(updatedRole.getRoleName());
      if (role != null)
      {
         role.setPrincipalName(updatedRole.getPrincipalName());
      }
      else
      {
         securityRoles.put(updatedRole.getRoleName(), updatedRole);
      }
   }
   
   public Collection getSecurityConstraints()
   {
      return securityConstraints;
   }

   public void addSecurityConstraint(SecurityConstraint constraint)
   {
      securityConstraints.add(constraint);
   }
   
   public LoginConfig getLoginConfig()
   {
      return loginConfig;
   }
   
   public void setLoginConfig(LoginConfig loginConfig)
   {
      this.loginConfig = loginConfig;
   }
   
   public Collection getErrorPages()
   {
      return errorPages.values();
   }

   public void addErrorPage(ErrorPage errorPage)
   {
      errorPages.put(errorPage.getErrorCode(), errorPage);
   }
   
   public Collection getMessageDestinations()
   {
      return messageDestinations.values();
   }

   public void addMessageDestination(MessageDestination destination)
   {
      messageDestinations.put(destination.getMessageDestinationName(), destination);
   }
   
   public void addDependency(String depends)
   {
      dependencies.add(depends);
   }

   public Collection getDependencies()
   {
      return dependencies;
   }
   
   public ReplicationConfig getReplicationConfig()
   {
      return replicationConfig;
   }
   
   public void setReplicationConfig(ReplicationConfig replicationConfig)
   {
      this.replicationConfig = replicationConfig;
   }
   
   public String toString()
   {
      StringBuffer sb = new StringBuffer(100);
      sb.append('[');
      sb.append(']');
      return sb.toString();
   }
}
