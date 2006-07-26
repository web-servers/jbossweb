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

import java.io.IOException;
import java.net.URL;

import org.jboss.logging.Logger;
import org.jboss.util.xml.JBossEntityResolver;
import org.jboss.xb.binding.JBossXBException;
import org.jboss.xb.binding.ObjectModelFactory;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.UnmarshallingContext;
import org.xml.sax.Attributes;

import org.jboss.metamodel.descriptor.DDObjectFactory;
import org.jboss.metamodel.descriptor.EjbRef;
import org.jboss.metamodel.descriptor.EjbLocalRef;
import org.jboss.metamodel.descriptor.EnvEntry;
import org.jboss.metamodel.descriptor.Listener;
import org.jboss.metamodel.descriptor.MessageDestination;
import org.jboss.metamodel.descriptor.MessageDestinationRef;
import org.jboss.metamodel.descriptor.NameValuePair;
import org.jboss.metamodel.descriptor.ResourceEnvRef;
import org.jboss.metamodel.descriptor.ResourceRef;
import org.jboss.metamodel.descriptor.RunAs;
import org.jboss.metamodel.descriptor.SecurityRoleRef;

/**
 * org.jboss.xb.binding.ObjectModelFactory implementation that accepts data
 * chuncks from unmarshaller and assembles them into an WebDD instance.
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 1.3 $</tt>
 */
public class WebDDObjectFactory extends DDObjectFactory
{
   private static final Logger log = Logger
           .getLogger(WebDDObjectFactory.class);
   
   public static WebDD parse(URL ddResource)
      throws JBossXBException, IOException
   {
      ObjectModelFactory factory = null;
      Unmarshaller unmarshaller = null;
      WebDD dd = null;
      
      if (ddResource != null)
      {
         log.debug("found web.xml " + ddResource);
      
         factory = new WebDDObjectFactory();
         UnmarshallerFactory unmarshallerFactory = UnmarshallerFactory
               .newInstance();
 //        unmarshallerFactory.setFeature(Unmarshaller.SCHEMA_VALIDATION, Boolean.TRUE);
         unmarshaller = unmarshallerFactory.newUnmarshaller();
         JBossEntityResolver entityResolver = new JBossEntityResolver();
         unmarshaller.setEntityResolver(entityResolver);
      
         dd = (WebDD) unmarshaller.unmarshal(ddResource.openStream(),
               factory, null);
      }
   
      return dd;
   }

   /**
    * Return the root.
    */
   public Object newRoot(Object root, UnmarshallingContext navigator,
                         String namespaceURI, String localName, Attributes attrs)
   {

      final WebDD dd;
      if (root == null)
         root = dd = new WebDD();
      else
         dd = (WebDD) root;

      return root;
   }

   public Object completeRoot(Object root, UnmarshallingContext ctx,
                              String uri, String name)
   {
      return root;
   }

   public Object newChild(WebDD dd, UnmarshallingContext navigator,
                          String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;

      if ((child = newEnvRefGroupChild(localName)) != null)
         return child;
      
      if (localName.equals("filter"))
      {
         child = new Filter();
      }
      else if (localName.equals("filter-mapping"))
      {
         child = new FilterMapping();
      }
      else if (localName.equals("listener"))
      {
         child = new Listener();
      }
      else if (localName.equals("servlet"))
      {
         child = new Servlet();
      }
      else if (localName.equals("servlet-mapping"))
      {
         child = new ServletMapping();
      }
      else if (localName.equals("session-config"))
      {
         child = new SessionConfig();
      }
      else if (localName.equals("error-page"))
      {
         child = new ErrorPage();
      }
      else if (localName.equals("security-role"))
      {
         child = new SecurityRole();
      }
      else if (localName.equals("security-constraint"))
      {
         child = new SecurityConstraint();
      }
      else if (localName.equals("login-config"))
      {
         child = new LoginConfig();
      }
      else if (localName.equals("message-destination"))
      {
         child = new MessageDestination();
      }

      return child;
   }
   
   public Object newChild(Filter filter, UnmarshallingContext navigator,
         String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;
      
      if (localName.equals("init-param"))
      {
         child = new NameValuePair();
      }
      
      return child;
   }
   
   public Object newChild(Servlet servlet, UnmarshallingContext navigator,
         String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;
      
      if (localName.equals("init-param"))
      {
         child = new NameValuePair();
      }
      else if (localName.equals("run-as"))
      {
         child = new RunAs();
      }
      else if (localName.equals("security-role-ref"))
      {
         child = new SecurityRoleRef();
      }
      
      return child;
   }
   
   public Object newChild(SecurityConstraint constraint, UnmarshallingContext navigator,
         String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;
      
      if (localName.equals("web-resource-collection"))
      {
         child = new WebResourceCollection();
      }
      else if (localName.equals("auth-constraint"))
      {
         child = new AuthConstraint();
      }
      else if (localName.equals("user-data-constraint"))
      {
         child = new UserDataConstraint();
      }
      
      return child;
   }
   
   public Object newChild(LoginConfig config, UnmarshallingContext navigator,
         String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;
      
      if (localName.equals("form-login-config"))
      {
         child = new FormLoginConfig();
      }
      
      return child;
   }

   public void addChild(WebDD parent, Filter filter,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addFilter(filter);
   }
   
   public void addChild(WebDD parent, FilterMapping mapping,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addFilterMapping(mapping);
   }
   
   public void addChild(WebDD parent, Listener listener,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addListener(listener);
   }
   
   public void addChild(WebDD parent, Servlet servlet,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addServlet(servlet);
   }
   
   public void addChild(WebDD parent, ServletMapping mapping,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addServletMapping(mapping);
   }
   
   public void addChild(WebDD parent, SessionConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addSessionConfig(config);
   }
   
   public void addChild(WebDD parent, SecurityConstraint constraint,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addSecurityConstraint(constraint);
   }
   
   public void addChild(WebDD parent, ErrorPage page,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addErrorPage(page);
   }
   
   public void addChild(WebDD parent, SecurityRole role,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addSecurityRole(role);
   }
   
   public void addChild(WebDD parent, EjbLocalRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addEjbLocalRef(ref);
   }
   
   public void addChild(WebDD parent, EjbRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addEjbRef(ref);
   }
   
   public void addChild(WebDD parent, EnvEntry ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addEnvEntry(ref);
   }
   
   public void addChild(WebDD parent, MessageDestinationRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addMessageDestinationRef(ref);
   }
   
   public void addChild(WebDD parent, ResourceEnvRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addResourceEnvRef(ref);
   }
   
   public void addChild(WebDD parent, ResourceRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addResourceRef(ref);
   }
   
   public void addChild(WebDD parent, LoginConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setLoginConfig(config);
   }
   
   public void addChild(WebDD parent, MessageDestination destination,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addMessageDestination(destination);
   }
   
   public void addChild(Filter parent, NameValuePair param,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addInitParam(param);
   }
   
   public void addChild(Servlet parent, NameValuePair param,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addInitParam(param);
   }
   
   public void addChild(Servlet parent, RunAs runAs,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setRunAs(runAs);
   }
   
   public void addChild(Servlet parent, SecurityRoleRef ref,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addSecurityRoleRef(ref);
   }
   
   public void addChild(SecurityConstraint parent, WebResourceCollection collection,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setWebResourceCollection(collection);
   }
   
   public void addChild(SecurityConstraint parent, AuthConstraint constraint,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setAuthConstraint(constraint);
   }
   
   public void addChild(SecurityConstraint parent, UserDataConstraint constraint,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setUserDataConstraint(constraint);
   }
   
   public void addChild(LoginConfig parent, FormLoginConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.setFormLoginConfig(config);
   }
   
   public void setValue(Filter filter,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("filter-name"))
      {
         filter.setName(value);
      }
      else if (localName.equals("filter-class"))
      {
         filter.setFilterClass(value);
      }
   }
   
   public void setValue(FilterMapping mapping,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("filter-name"))
      {
         mapping.setFilterName(value);
      }
      else if (localName.equals("url-pattern"))
      {
         mapping.setUrlPattern(value);
      }
      else if (localName.equals("servlet-name"))
      {
         mapping.setServletName(value);
      }
   }

   public void setValue(Servlet servlet,
                        UnmarshallingContext navigator, String namespaceURI, String localName,
                        String value)
   {
      if (localName.equals("servlet-name"))
      {
         servlet.setName(value);
      }
      else if (localName.equals("servlet-class"))
      {
         servlet.setServletClass(value);
      }
      else if (localName.equals("jsp-file"))
      {
         servlet.setJspFile(value);
      }
      else if (localName.equals("load-on-startup"))
      {
         servlet.setLoadOnStartup(value);
      }
   }
  
   public void setValue(ServletMapping mapping,
                        UnmarshallingContext navigator, String namespaceURI, String localName,
                        String value)
   {
      if (localName.equals("servlet-name"))
      {
         mapping.setName(value);
      }
      else if (localName.equals("url-pattern"))
      {
         mapping.setUrlPattern(value);
      }
   }
   
   public void setValue(ErrorPage page,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("error-code"))
      {
         page.setErrorCode(value);
      }
      else if (localName.equals("location"))
      {
         page.setLocation(value);
      }
   }
   
   public void setValue(SessionConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("session-timeout"))
      {
         config.setSessionTimeout(value);
      }
   }
   
   public void setValue(NameValuePair param, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("param-name"))
      {
         param.setName(value);
      }
      else if (localName.equals("param-value"))
      {
         param.setValue(value);
      }
   }
   
   public void setValue(WebResourceCollection collection, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("web-resource-name"))
      {
         collection.setWebResourceName(value);
      }
      else if (localName.equals("url-pattern"))
      {
         collection.setUrlPattern(value);
      }
   }
   
   public void setValue(AuthConstraint contraint, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("role-name"))
      {
         contraint.setRoleName(value);
      }
   }
   
   public void setValue(UserDataConstraint contraint, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("transport-guarantee"))
      {
         contraint.setTransportGuarantee(value);
      }
   }
   
   public void setValue(LoginConfig config, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("auth-method"))
      {
         config.setAuthMethod(value);
      }
      else if (localName.equals("realm-name"))
      {
         config.setRealmName(value);
      }
   }
   
   public void setValue(FormLoginConfig config, UnmarshallingContext navigator,
         String namespaceURI, String localName, String value)
   {
      if (localName.equals("form-login-page"))
      {
         config.setLoginPage(value);
      }
      else if (localName.equals("form-error-page"))
      {
         config.setErrorPage(value);
      }
   }
}
