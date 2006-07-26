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

import org.apache.catalina.loader.WebappClassLoader;
import org.jboss.logging.Logger;

/**
 * Subclass the tomcat web app class loader to override the filter method
 * to exclude classes which cannot be override by the web app due to their
 * use in the tomcat web container/integration.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class WebAppClassLoader extends WebappClassLoader
{
   static Logger log = Logger.getLogger(WebAppClassLoader.class);
   private String[] filteredPackages = {
      "org.apache.commons.logging"
   };

   public WebAppClassLoader()
   {
   }

   public WebAppClassLoader(ClassLoader parent)
   {
      super(parent);
   }

   public String[] getFilteredPackages()
   {
      return filteredPackages;
   }
   public void setFilteredPackages(String[] pkgs)
   {
      this.filteredPackages = pkgs;
   }

   /**
    * Overriden to filter out classes in the packages listed in the
    * filteredPackages settings.
    * 
    * @param name
    * @return true if the class should be loaded from the parent class loader,
    *    false if it can be loaded from this class loader.
    */ 
   protected boolean filter(String name)
   {
      boolean excludeClass = super.filter(name);
      if( excludeClass == false )
      {
         // Check class against our filtered packages
         int length = filteredPackages != null ? filteredPackages.length : 0;
         for(int n = 0; n < length; n ++)
         {
            String pkg = filteredPackages[n];
            if( name.startsWith(pkg) )
            {
               excludeClass = true;
               break;
            }
         }
      }
      log.trace("filter name="+name+", exclude="+excludeClass);
      return excludeClass;
   }
}
