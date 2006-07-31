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

import org.jboss.metamodel.descriptor.NameValuePair;
import org.jboss.metamodel.descriptor.RunAs;
import org.jboss.metamodel.descriptor.SecurityRoleRef;

import org.jboss.logging.Logger;

/**
 * Represents a <servlet> element of the web.xml deployment descriptor for the
 * 2.5 schema
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 1.3 $</tt>
 */
public class Servlet
{
   private static final Logger log = Logger.getLogger(Servlet.class);
   
   protected String name;
   protected String clazz;
   protected String jspFile;
   protected HashMap initParams = new HashMap();
   protected String loadOnStartup;
   protected RunAs runAs;
   protected HashMap securityRoleRefs = new HashMap();
   protected Collection runAsPrincipals = new ArrayList();
   
   public String getName()
   {
      return name;
   }
   
   public void setName(String name)
   {
      this.name = name;
   }
   
   public String getServletClass()
   {
      return clazz;
   }
   
   public void setServletClass(String clazz)
   {
      this.clazz = clazz;
   }
   
   public void setJspFile(String jspFile)
   {
      this.jspFile = jspFile;
   }
   
   public String getJspFile()
   {
      return jspFile;
   }
   
   public Collection getInitParams()
   {
      return initParams.values();
   }
   
   public void addInitParam(NameValuePair param)
   {
      initParams.put(param.getName(), param);
   }
   
   public String getLoadOnStartup()
   {
      return loadOnStartup;
   }
   
   public void setLoadOnStartup(String loadOnStartup)
   {
      this.loadOnStartup = loadOnStartup;
   }
   
   public RunAs getRunAs()
   {
      return runAs;
   }
   
   public void setRunAs(RunAs runAs)
   {
      this.runAs = runAs;
   }
   
   public Collection getSecurityRoleRefs()
   {
      return securityRoleRefs.values();
   }
   
   public void addSecurityRoleRef(SecurityRoleRef ref)
   {
      securityRoleRefs.put(ref.getRoleName(), ref);
   }
   
   public Collection getRunAsPrincipals()
   {
      return runAsPrincipals;
   }
   
   public void setRunAsPrincipals(Collection runAsPrincipals)
   {
      this.runAsPrincipals = runAsPrincipals;
   }
   
   public void addRunAsPrincipal(String principal)
   {
      runAsPrincipals.add(principal);
   }
   
   public String toString()
   {
      StringBuffer sb = new StringBuffer(100);
      sb.append('[');
      sb.append("name=" + name);
      sb.append(", class=" + clazz);
      sb.append(", jspFile=" + jspFile);
      sb.append(']');
      return sb.toString();
   }
}
