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
package org.jboss.web.tomcat.security.authorization;

import java.util.HashMap;
import java.util.Map;

import org.jboss.security.authorization.Resource;

//$Id: WebResource.java,v 1.1 2006/06/20 04:51:47 asaldhana Exp $

/**
 *  Represents a Resource for the Web Layer
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Jun 18, 2006 
 *  @version $Revision: 1.1 $
 */
public class WebResource implements Resource
{
   private Map map = new HashMap();
   
   /**
    * Create a new WebResource.
    */
   public WebResource()
   {   
   }
   
   /**
    * 
    * Create a new WebResource.
    * 
    * @param map Contextual Map
    */
   public WebResource(Map map)
   {
      this.map = map;
   }

   /**
    * @see Resource#getLayer()
    */
   public String getLayer()
   {
      return Resource.WEB;
   }

   /**
    * @see Resource#getMap()
    */
   public Map getMap()
   {
      return map;
   }
 
   /**
    * Set the contextual map
    * @param m Contextual Map
    */
   public void setMap(Map m)
   {
      this.map = m;
   } 
}
