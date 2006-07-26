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
import org.jboss.metamodel.descriptor.MessageDestinationRef;
import org.jboss.metamodel.descriptor.ResourceEnvRef;
import org.jboss.metamodel.descriptor.ResourceRef;

/**
 * org.jboss.xb.binding.ObjectModelFactory implementation that accepts data
 * chuncks from unmarshaller and assembles them into an WebDD instance.
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 1.2 $</tt>
 */
public class JBossWebDDObjectFactory extends DDObjectFactory
{
   private static final Logger log = Logger
           .getLogger(JBossWebDDObjectFactory.class);
   
   private WebDD dd;
   
   public static WebDD parse(URL ddResource, WebDD dd)
      throws JBossXBException, IOException
   {
      ObjectModelFactory factory = null;
      Unmarshaller unmarshaller = null;
      
      if (ddResource != null)
      {
         log.debug("found jboss-web.xml " + ddResource);
         
         if (dd == null)
            dd = new WebDD();
      
         factory = new JBossWebDDObjectFactory(dd);
         UnmarshallerFactory unmarshallerFactory = UnmarshallerFactory
               .newInstance();
  //       unmarshallerFactory.setFeature(Unmarshaller.SCHEMA_VALIDATION, Boolean.TRUE);
         unmarshaller = unmarshallerFactory.newUnmarshaller();
         JBossEntityResolver entityResolver = new JBossEntityResolver();
         unmarshaller.setEntityResolver(entityResolver);
      
         dd = (WebDD) unmarshaller.unmarshal(ddResource.openStream(),
               factory, null);
      }
   
      return dd;
   }
   
   public JBossWebDDObjectFactory(WebDD dd)
   {
      super();
      this.dd = dd;
   }

   /**
    * Return the root.
    */
   public Object newRoot(Object root, UnmarshallingContext navigator,
                         String namespaceURI, String localName, Attributes attrs)
   {
      return dd;
   }

   public Object completeRoot(Object root, UnmarshallingContext ctx,
                              String uri, String name)
   {
      return root;
   }

   // Methods discovered by introspection

   /**
    * Called when parsing of a new element started.
    */
   public Object newChild(WebDD dd, UnmarshallingContext navigator,
                          String namespaceURI, String localName, Attributes attrs)
   {
      Object child = null;

      if ((child = newEnvRefGroupChild(localName)) != null)
         return child;
      else if (localName.equals("security-role"))
         child = new SecurityRole();
      else if (localName.equals("servlet"))
         child = new Servlet();
      else if (localName.equals("replication-config"))
         child = new ReplicationConfig();

      return child;
   }
   
   public void addChild(WebDD parent, ReplicationConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
    parent.setReplicationConfig(config);
   }

   public void addChild(WebDD parent, EjbLocalRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateEjbLocalRef(ref);
   }
   
   public void addChild(WebDD parent, EjbRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateEjbRef(ref);
   }
  
   public void addChild(WebDD parent, EnvEntry ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.addEnvEntry(ref);
   }
   
   public void addChild(WebDD parent, MessageDestinationRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateMessageDestinationRef(ref);
   }
   
   public void addChild(WebDD parent, ResourceEnvRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateResourceEnvRef(ref);
   }
   
   public void addChild(WebDD parent, ResourceRef ref,
                        UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateResourceRef(ref);
   }
   
   public void addChild(WebDD parent, SecurityRole role,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateSecurityRole(role);
   }
   
   public void addChild(WebDD parent, Servlet servlet,
         UnmarshallingContext navigator, String namespaceURI, String localName)
   {
      parent.updateServlet(servlet);
   }
   
   public void setValue(WebDD dd,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("depends"))
      {
         dd.addDependency(value);
      }
      else if (localName.equals("security-domain"))
      {
         dd.setSecurityDomain(value);
      }
   }
   
   public void setValue(ReplicationConfig config,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("replication-trigger"))
      {
         config.setTrigger(value);
      }
      else if (localName.equals("replication-granularity"))
      {
         config.setGranularity(value);
      }
      else if (localName.equals("replication-field-batch-mode"))
      {
         config.setFieldBatchMode(value);
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
      else if (localName.equals("run-as-principal"))
      {
         servlet.addRunAsPrincipal(value);
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
   
   public void setValue(SecurityRole role,
         UnmarshallingContext navigator, String namespaceURI, String localName,
         String value)
   {
      if (localName.equals("principal-name"))
      {
         role.setPrincipalName(value);
      }
      else 
         super.setValue(role, navigator, namespaceURI, localName, value);
   }
}
