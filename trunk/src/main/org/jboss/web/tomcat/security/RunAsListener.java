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

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Wrapper;
import org.jboss.logging.Logger;
import org.jboss.metadata.WebMetaData;
import org.jboss.security.RunAsIdentity;

/**
 * An InstanceListener used to push/pop the servlet run-as identity for the
 * init/destroy lifecycle events.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.5 $
 */
public class RunAsListener implements InstanceListener
{
   /** There is no api to install an initialized listener so the
    * WebMetaData had to be passed via a thread local.
    */
   public static ThreadLocal metaDataLocal = new ThreadLocal();

   private static Logger log = Logger.getLogger(RunAsListener.class);
   private WebMetaData metaData;

   public RunAsListener()
   {
      this.metaData = (WebMetaData) metaDataLocal.get();
   }

   /**
    * Push the run-as identity on the before init/destroy, pop it on the
    * after init/destroy events.
    * 
    * @param event - the type of instance event
    */ 
   public void instanceEvent(InstanceEvent event)
   {
      Wrapper servlet = event.getWrapper();
      String type = event.getType();
      if (servlet != null && metaData != null)
      {
         boolean trace = log.isTraceEnabled();
         String name = servlet.getName();
         RunAsIdentity identity = metaData.getRunAsIdentity(name);
         if (trace)
            log.trace(name + ", runAs: " + identity);
         // Push the identity on the before init/destroy
         if( type.equals(InstanceEvent.BEFORE_INIT_EVENT)
            || type.equals(InstanceEvent.BEFORE_DESTROY_EVENT) )
         {
            SecurityAssociationActions.pushRunAsIdentity(identity);
         }
         // Pop the identity on the after init/destroy
         else if( type.equals(InstanceEvent.AFTER_INIT_EVENT)
            || type.equals(InstanceEvent.AFTER_DESTROY_EVENT) )
         {
            SecurityAssociationActions.popRunAsIdentity();
         }
      }
   }
}
