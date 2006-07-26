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
package org.jboss.web.tomcat.tc6.session;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Session;
import org.apache.catalina.Manager;
import org.jboss.metadata.WebMetaData;

/** Common interface for the http session replication managers.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public interface AbstractJBossManager extends Manager
{
   /** Initialize the manager with the web metadata and 
    * @param name
    * @param webMetaData
    * @param useJK
    * @param useLocalCache
    * @throws ClusteringNotSupportedException
    */ 
   public void init(String name, WebMetaData webMetaData, boolean useJK,
      boolean useLocalCache)
      throws ClusteringNotSupportedException;

   /** The session invalidation policy. One of:
    SESSION_INVALIDATE_ACCESS =0;
    SESSION_INVALIDATE_SET_AND_GET =1;
    SESSION_INVALIDATE_SET_AND_NON_PRIMITIVE_GET =2;
    SESSION_INVALIDATE_SET =3;
    * @return the invalidation policy constant
    */ 
   public int getInvalidateSessionPolicy();

   /**
    * Retrieve the JvmRoute for the enclosing Engine.
    *
    * @return the JvmRoute or null.
    */
   public String getJvmRoute();

   /**
    * Sets a new cookie for the given session id and response
    *
    * @param sessionId The session id
    */
   public void setNewSessionCookie(String sessionId, HttpServletResponse response);

   /**
    * Remove the active session locally from the manager without replicating to the cluster. This can be
    * useful when the session is exipred, for example, where there is not need to propagate the expiration.
    *
    * @param session
    */
   public void removeLocal(Session session);

   /**
    * Store the modified session.
    *
    * @param session
    */
   public boolean storeSession(Session session);
}
