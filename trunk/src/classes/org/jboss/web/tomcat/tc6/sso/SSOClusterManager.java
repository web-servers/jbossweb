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
package org.jboss.web.tomcat.tc6.sso;

import org.apache.catalina.Session;
import org.apache.catalina.Lifecycle;

/**
 * Provides communications support between a SingleSignOn valve and other
 * such valves configured for the same hostname within a server cluster.
 * <p/>
 * Implementations of this interface must declare a public no-arguments
 * constructor.
 *
 * @author Brian E. Stansberry
 * @version $Revision: 1.1 $ $Date: 2006/06/21 19:39:00 $
 * @see ClusteredSingleSignOn
 */
public interface SSOClusterManager
   extends Lifecycle
{

   /**
    * Notify the cluster of the addition of a Session to an SSO session.
    *
    * @param ssoId   the id of the SSO session
    * @param session the Session that has been added
    */
   void addSession(String ssoId, Session session);

   /**
    * Gets the SingleSignOn valve for which this object is handling
    * cluster communications.
    *
    * @return the <code>SingleSignOn</code> valve.
    */
   ClusteredSingleSignOn getSingleSignOnValve();

   /**
    * Sets the SingleSignOn valve for which this object is handling
    * cluster communications.
    * <p><b>NOTE:</b> This method must be called before calls can be
    * made to the other methods of this interface.
    *
    * @param valve a <code>SingleSignOn</code> valve.
    */
   void setSingleSignOnValve(ClusteredSingleSignOn valve);

   /**
    * Notifies the cluster that a single sign on session has been terminated
    * due to a user logout.
    *
    * @param ssoId the id of the SSO session
    */
   void logout(String ssoId);

   /**
    * Queries the cluster for the existence of a SSO session with the given
    * id, returning a <code>SingleSignOnEntry</code> if one is found.
    *
    * @param ssoId the id of the SSO session
    * @return a <code>SingleSignOnEntry</code> created using information
    *         found on another cluster node, or <code>null</code> if no
    *         entry could be found.
    */
   SingleSignOnEntry lookup(String ssoId);

   /**
    * Notifies the cluster of the creation of a new SSO entry.
    *
    * @param ssoId    the id of the SSO session
    * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST
    *                 or FORM) used to authenticate the SSO.
    * @param username the username (if any) used for the authentication
    * @param password the password (if any) used for the authentication
    */
   void register(String ssoId, String authType, String username,
      String password);

   /**
    * Notify the cluster of the removal of a Session from an SSO session.
    *
    * @param ssoId   the id of the SSO session
    * @param session the Session that has been removed
    */
   void removeSession(String ssoId, Session session);

   /**
    * Notifies the cluster of an update of the security credentials
    * associated with an SSO session.
    *
    * @param ssoId    the id of the SSO session
    * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST
    *                 or FORM) used to authenticate the SSO.
    * @param username the username (if any) used for the authentication
    * @param password the password (if any) used for the authentication
    */
   void updateCredentials(String ssoId, String authType, String username,
      String password);


}
