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
package org.jboss.web.tomcat.security.config;

//$Id: JBossContextConfig.java,v 1.1 2006/03/07 05:25:42 asaldhana Exp $

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Authenticator;
import org.apache.catalina.startup.ContextConfig;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;

/**
 *  Extension of Catalina ContextConfig that will allow
 *  plugging custom authenticators at the host level
 *  in the least intrusive way to the tomcat layer.
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Mar 6, 2006
 *  @version $Revision: 1.1 $
 */
public class JBossContextConfig extends ContextConfig
{ 
   private static Logger log = Logger.getLogger(JBossContextConfig.class);
   
   private boolean isTrace = log.isTraceEnabled();
   
   /**
    * Create a new JBossContextConfig.
    */
   public JBossContextConfig()
   {
      super();  
      try
      {
         Map authMap = this.getAuthenticators();
         if(authMap.size() > 0)
            customAuthenticators = authMap; 
      }catch(Exception e)
      {
         log.error("Failed to customize authenticators::",e); 
      } 
   } 
   
   /**
    * Map of Authenticators
    * @return
    * @throws Exception
    */
   private Map getAuthenticators() throws Exception
   {
      Map cmap = new HashMap();
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      
      Properties authProps = this.getAuthenticatorsFromJMX();
      if(authProps != null)
      {
         Set keys = authProps.keySet();
         Iterator iter = keys != null ? keys.iterator() : null;
         while(iter != null && iter.hasNext())
         {
            String key = (String)iter.next();
            String authenticatorStr = (String)authProps.get(key);
            Class authClass = tcl.loadClass(authenticatorStr);
            cmap.put(key, (Authenticator)authClass.newInstance()); 
         }
      }
      if(isTrace)
         log.trace("Authenticators plugged in::"+cmap);
      return cmap; 
   }
   
   /**
    * Get the key-pair of authenticators from the
    * TomcatAuthenticatorConfig MBean
    * 
    * @return
    * @throws JMException
    */
   private Properties getAuthenticatorsFromJMX() throws JMException
   {
      Properties props = null;
      MBeanServer server = MBeanServerLocator.locateJBoss();
      props = (Properties)server.getAttribute(new ObjectName("jboss.web:service=WebServer"),
                 "Authenticators"); 
      return props; 
   }
}
