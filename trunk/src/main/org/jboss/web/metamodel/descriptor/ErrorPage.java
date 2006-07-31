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

import org.jboss.logging.Logger;

/**
 * Represents a <error-page> element of the web.xml deployment descriptor for the
 * 2.5 schema
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 1.1 $</tt>
 */
public class ErrorPage
{
   private static final Logger log = Logger.getLogger(ErrorPage.class);
   
   protected String errorCode;
   protected String location;
   
   public String getErrorCode()
   {
      return errorCode;
   }
   
   public void setErrorCode(String errorCode)
   {
      this.errorCode = errorCode;
   }
   
   public String getLocation()
   {
      return location;
   }
   
   public void setLocation(String location)
   {
      this.location = location;
   }
   
   public String toString()
   {
      StringBuffer sb = new StringBuffer(100);
      return sb.toString();
   }
}
