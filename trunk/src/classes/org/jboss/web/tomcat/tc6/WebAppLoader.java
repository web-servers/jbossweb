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
package org.jboss.web.tomcat.tc6;

import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.LifecycleException;

/**
 * Override the tomcat WebappLoader to set the default class loader to the
 * WebAppClassLoader and pass the filtered packages to the WebAppClassLoader.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class WebAppLoader extends WebappLoader
{
   private String[] filteredPackages = {
      "org.apache.commons.logging"
   };

   public WebAppLoader()
   {
      super();
      setLoaderClass(WebAppClassLoader.class.getName());
   }

   public WebAppLoader(ClassLoader parent, String[] filteredPackages)
   {
      super(parent);
      setLoaderClass(WebAppClassLoader.class.getName());
      this.filteredPackages = filteredPackages;
   }

   /**
    * Override to apply the filteredPackages to the jboss WebAppClassLoader
    * 
    * @throws LifecycleException
    */ 
   public void start() throws LifecycleException
   {
      super.start();
      ClassLoader loader = getClassLoader();
      if( loader instanceof WebAppClassLoader )
      {
         WebAppClassLoader webLoader = (WebAppClassLoader) loader;
         webLoader.setFilteredPackages(filteredPackages);
      }
   }
}
