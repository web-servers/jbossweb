/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

package org.jboss.web.tomcat.tc6.session;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.jboss.aop.Advised;
import org.jboss.cache.aop.CachedType;

/**
 * Utility methods related to JBoss distributed sessions.
 * 
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public abstract class Util
{

   /**
    * Returns a session id with any trailing jvmRoute removed.
    * 
    * @param sessionId the raw session id
    * 
    * @return <code>sessionId</code> with the final '.' and any
    *         characters thereafter removed.
    */
   public static String getRealId(String sessionId)
   {
      int index = sessionId.lastIndexOf(".");
      if (index > 0)
      {
         return sessionId.substring(0, index);
      }
      else
      {
         return sessionId;
      }
   }

   /**
    * Checks whether the given object is usable for FIELD granularity 
    * replication.
    * 
    * @param pojo  the pojo
    * @return <code>true</code> if the attribute type is acceptable,
    *         <code>false</code> otherwise
    */
   public static boolean checkPojoType(Object pojo)
   {
      return (   (pojo instanceof Serializable)
              || (pojo instanceof Collection) 
              || (pojo instanceof Map) 
              || (pojo instanceof Advised)
              || (CachedType.isImmediate(pojo.getClass())));
   }

   /**
    * Prevent instantiation.
    */
   private Util() {}

}
