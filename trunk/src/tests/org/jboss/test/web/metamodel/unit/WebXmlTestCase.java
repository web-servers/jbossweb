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
package org.jboss.test.web.metamodel.unit;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import org.jboss.logging.Logger;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jboss.metamodel.descriptor.DDObjectFactory;
import org.jboss.metamodel.descriptor.EjbRef;
import org.jboss.metamodel.descriptor.EjbLocalRef;
import org.jboss.metamodel.descriptor.EnvEntry;
import org.jboss.metamodel.descriptor.InjectionTarget;
import org.jboss.metamodel.descriptor.Listener;
import org.jboss.metamodel.descriptor.MessageDestination;
import org.jboss.metamodel.descriptor.MessageDestinationRef;
import org.jboss.metamodel.descriptor.NameValuePair;
import org.jboss.metamodel.descriptor.ResourceEnvRef;
import org.jboss.metamodel.descriptor.ResourceRef;
import org.jboss.metamodel.descriptor.RunAs;
import org.jboss.metamodel.descriptor.SecurityRoleRef;

import org.jboss.web.metamodel.descriptor.*;

/**
 * JUnit TestCase for JbossXB usage for web deployment descriptor for
 * version 2.5 schema
 * 
 * @version <tt>$Revision: 45417 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */

public class WebXmlTestCase extends TestCase
{

   private static final Logger log = Logger.getLogger(WebXmlTestCase.class);

   public WebXmlTestCase(String name)
   {

      super(name);

   }

   // Tests
   public void testUnmarshalDDXsd() throws Exception
   {
      // create an object model factory
      URL xmlUrl = getResourceUrl("metamodel/WEB-INF/web.xml");
      assertNotNull(xmlUrl);
      
      WebDD dd = WebDDObjectFactory.parse(xmlUrl);
      assertNotNull(dd);
      
      xmlUrl = getResourceUrl("metamodel/WEB-INF/jboss-web.xml");
      assertNotNull(xmlUrl);
      
      dd = JBossWebDDObjectFactory.parse(xmlUrl, dd);
      assertNotNull(dd);

      checkUnmarshalledDD(dd);
   }
  
   private void checkUnmarshalledDD(WebDD dd)
   {
      log.debug("unmarshalled DD: " + dd);
      
      assertEquals("java:/jaas/jbosstest-web", dd.getSecurityDomain());
      
      Collection filters = dd.getFilters();
      assertEquals(1, filters.size());
      Filter filter = (Filter)filters.iterator().next();
      assertEquals("JASPISecurityFilter", filter.getName());
      assertEquals("org.jboss.test.web.security.JASPISecurityFilter", filter.getFilterClass());
      Collection initParams = filter.getInitParams();
      assertEquals(1, initParams.size());
      NameValuePair param = (NameValuePair)initParams.iterator().next();
      assertEquals("testJASPIServerAuthContext", param.getName());
      assertEquals("TRUE", param.getValue());
      
      Collection mappings = dd.getFilterMappings();
      assertEquals(1, mappings.size());
      FilterMapping filterMapping = (FilterMapping)mappings.iterator().next();
      assertEquals("SubjectFilter", filterMapping.getFilterName());
      assertEquals("SubjectServlet", filterMapping.getServletName());
      
      Collection listeners = dd.getListeners();
      assertEquals(1, listeners.size());
      Listener listener = (Listener)listeners.iterator().next();
      assertEquals("org.jboss.test.cluster.web.SessionListener", listener.getListenerClass());
      
      Collection servlets = dd.getServlets();
      assertEquals(2, servlets.size());
      Iterator servletIterator = servlets.iterator();
      Servlet servlet = (Servlet)servletIterator.next();
      assertEquals("EJBJsp", servlet.getName());
      assertEquals("/test.jsp", servlet.getJspFile());
      servlet = (Servlet)servletIterator.next();
      assertEquals("EJBServlet", servlet.getName());
      assertEquals("org.jboss.ejb3.test.servlet.servlets.EJBServlet", servlet.getServletClass());
      Collection principals = servlet.getRunAsPrincipals();
      assertEquals(1, principals.size());
      String principal = (String)principals.iterator().next();
      assertEquals("runAsUser", principal);
      assertEquals("1", servlet.getLoadOnStartup());
      RunAs runAs = servlet.getRunAs();
      assertNotNull(runAs);
      assertEquals("identitySubstitutionCaller", runAs.getRoleName());
      Collection roleRefs = servlet.getSecurityRoleRefs();
      assertEquals(1, roleRefs.size());
      SecurityRoleRef roleRef = (SecurityRoleRef)roleRefs.iterator().next();
      assertEquals("ServletUser", roleRef.getRoleName());
      assertEquals("ServletUserRole", roleRef.getRoleLink());
      initParams = servlet.getInitParams();
      assertEquals(1, initParams.size());
      param = (NameValuePair)initParams.iterator().next();
      assertEquals("scope", param.getName());
      assertEquals("1", param.getValue());
      
      mappings = dd.getServletMappings();
      assertEquals(1, mappings.size());
      ServletMapping servletMapping = (ServletMapping)mappings.iterator().next();
      assertEquals("EJBServlet", servletMapping.getName());
      assertEquals("/EJBServlet", servletMapping.getUrlPattern());
      
      Collection configs = dd.getSessionConfigs();
      assertEquals(1, configs.size());
      SessionConfig config = (SessionConfig)configs.iterator().next();
      assertEquals("1", config.getSessionTimeout());
      
      Collection roles = dd.getSecurityRoles();
      assertEquals(2, roles.size());
      SecurityRole role = (SecurityRole)roles.iterator().next();
      assertEquals("JBossUser", role.getRoleName());
      assertEquals("UnsecureRunAsServletWithPrincipalNameAndRolesPrincipal", role.getPrincipalName());
      
      Collection constraints = dd.getSecurityConstraints();
      assertEquals(1, constraints.size());
      SecurityConstraint constraint = (SecurityConstraint)constraints.iterator().next();
      WebResourceCollection collection = constraint.getWebResourceCollection();
      assertNotNull(collection);
      assertEquals("Restricted", collection.getWebResourceName());
      assertEquals("/*", collection.getUrlPattern());
      AuthConstraint auth = constraint.getAuthConstraint();
      assertNotNull(auth);
      assertEquals("AuthorizedUser", auth.getRoleName());
      UserDataConstraint user = constraint.getUserDataConstraint();
      assertNotNull(user);
      assertEquals("NONE", user.getTransportGuarantee());
      
      LoginConfig loginConfig = dd.getLoginConfig();
      assertNotNull(loginConfig);
      assertEquals("FORM", loginConfig.getAuthMethod());
      assertEquals("WebConstraintsUnitTestCase", loginConfig.getRealmName());
      FormLoginConfig formLoginConfig = loginConfig.getFormLoginConfig();
      assertNotNull(formLoginConfig);
      assertEquals("/login.html", formLoginConfig.getLoginPage());
      assertEquals("/error.html", formLoginConfig.getErrorPage());
      
      Collection errorPages = dd.getErrorPages();
      assertEquals(1, errorPages.size());
      ErrorPage page = (ErrorPage)errorPages.iterator().next();
      assertEquals("404", page.getErrorCode());
      assertEquals("/ErrorPagesServlet/404.jsp", page.getLocation());
      
      Collection ejbRefs = dd.getEjbRefs();
      assertEquals(1, ejbRefs.size());
      Iterator ejbRefIterator = ejbRefs.iterator();
      while (ejbRefIterator.hasNext())
      {
         EjbRef ref = (EjbRef)ejbRefIterator.next();
         assertEquals("ejb/remote/Session30", ref.getEjbRefName());
         assertEquals("Session", ref.getEjbRefType());
         assertEquals("org.jboss.ejb3.test.servlet.Session30Home", ref.getHome());
         assertEquals("org.jboss.ejb3.test.servlet.Session30", ref.getRemote());
         assertEquals("jbosstest/ejbs/UnsecuredEJB", ref.getMappedName());
         
         InjectionTarget target = ref.getInjectionTarget();
         assertNotNull(target);
         assertEquals("org.jboss.ejb3.test.metamodel.EjbRef", target.getTargetClass());
         assertEquals("ejbRef", target.getTargetName());
      }
      
      Collection ejbLocalRefs = dd.getEjbLocalRefs();
      assertEquals(1, ejbLocalRefs.size());
      Iterator ejbLocalRefIterator = ejbLocalRefs.iterator();
      while (ejbLocalRefIterator.hasNext())
      {
         EjbLocalRef ref = (EjbLocalRef)ejbLocalRefIterator.next();
         assertEquals("ejb/local/Session30", ref.getEjbRefName());
         assertEquals("Session", ref.getEjbRefType());
         assertEquals("org.jboss.ejb3.test.servlet.Session30LocalHome", ref.getLocalHome());
         assertEquals("org.jboss.ejb3.test.servlet.Session30", ref.getLocal());
         assertEquals("jbosstest/ejbs/local/ENCBean1", ref.getMappedName());
         
         InjectionTarget target = ref.getInjectionTarget();
         assertNotNull(target);
         assertEquals("org.jboss.ejb3.test.metamodel.EjbLocalRef", target.getTargetClass());
         assertEquals("ejbLocalRef", target.getTargetName());
      }
      
      Collection resourceRefs = dd.getResourceRefs();
      assertEquals(1, resourceRefs.size());
      Iterator resourceRefIterator = resourceRefs.iterator();
      while (ejbLocalRefIterator.hasNext())
      {
         ResourceRef ref = (ResourceRef)resourceRefIterator.next();
         assertEquals("jms/ConnectionFactory", ref.getResRefName());
         assertEquals("javax.jms.ConnectionFactory", ref.getResType());
         assertEquals("Container", ref.getResAuth());
         assertEquals("Shareable", ref.getResSharingScope());
         assertEquals("java:ConnectionFactory", ref.getMappedName());
         
         InjectionTarget target = ref.getInjectionTarget();
         assertNotNull(target);
         assertEquals("org.jboss.ejb3.test.metamodel.ResourceRef", target.getTargetClass());
         assertEquals("resourceRef", target.getTargetName());
      }
      
      Collection messageDestinationRefs = dd.getMessageDestinationRefs();
      assertEquals(1, messageDestinationRefs.size());
      Iterator messageDestinationRefIterator = messageDestinationRefs.iterator();
      while (messageDestinationRefIterator.hasNext())
      {
         MessageDestinationRef ref = (MessageDestinationRef)messageDestinationRefIterator.next();
         assertEquals("jms/callerQueue", ref.getMessageDestinationRefName());
         assertEquals("javax.jms.Queue", ref.getMessageDestinationType());
         assertEquals("ConsumesProduces", ref.getMessageDestinationUsage());
         assertEquals("queue/testQueue", ref.getMappedName());
         
         InjectionTarget target = ref.getInjectionTarget();
         assertNotNull(target);
         assertEquals("org.jboss.ejb3.test.metamodel.MessageDestinationRef", target.getTargetClass());
         assertEquals("messageDestinationRef", target.getTargetName());
      }
      
      Collection messageDestinations = dd.getMessageDestinations();
      assertEquals(1, messageDestinations.size());
      MessageDestination destination = (MessageDestination)messageDestinations.iterator().next();
      assertEquals("TestQueue", destination.getMessageDestinationName());
      
      Collection dependencies = dd.getDependencies();
      assertEquals(1, dependencies.size());
      String dependency = (String)dependencies.iterator().next();
      assertEquals("jboss.ws:service=WebServiceDeployerJSE", dependency);
      
      ReplicationConfig replication = dd.getReplicationConfig();
      assertNotNull(replication);
      assertEquals("SET_AND_GET", replication.getTrigger());
      assertEquals("SESSION", replication.getGranularity());
   }

   private static URL getResourceUrl(String name)
   {
      URL url = Thread.currentThread().getContextClassLoader()
            .getResource(name);
      if (url == null)
      {
         throw new IllegalStateException("Resource not found: " + name);
      }
      return url;
   }

   public static Test suite() throws Exception
   {
      return new TestSuite(WebXmlTestCase.class);
   }

}
