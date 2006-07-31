/*
 * Copyright 1999-2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.web.tomcat.tc6.sso;

import java.security.Principal;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SingleSignOn;

/**
 * A class that represents entries in the cache of authenticated users.
 *
 * @author Brian E. Stansberry, based on work by Craig R. McClanahan
 * @version $Revision: 1.1 $ $Date: 2006/06/21 19:39:00 $
 * @see SingleSignOn
 */
class SingleSignOnEntry
{
   // ------------------------------------------------------  Instance Fields

   private String authType = null;

   private String password = null;

   private Principal principal = null;

   private Session sessions[] = new Session[0];

   private String username = null;

   private boolean canReauthenticate = false;

   // ---------------------------------------------------------  Constructors

   /**
    * Creates a new SingleSignOnEntry
    *
    * @param principal the <code>Principal</code> returned by the latest
    *                  call to <code>Realm.authenticate</code>.
    * @param authType  the type of authenticator used (BASIC, CLIENT-CERT,
    *                  DIGEST or FORM)
    * @param username  the username (if any) used for the authentication
    * @param password  the password (if any) used for the authentication
    */
   SingleSignOnEntry(Principal principal, String authType,
      String username, String password)
   {
      updateCredentials(principal, authType, username, password);
   }

   // ------------------------------------------------------- Package Methods

   /**
    * Adds a <code>Session</code> to the list of those associated with
    * this SSO.
    *
    * @param sso     The <code>SingleSignOn</code> valve that is managing
    *                the SSO session.
    * @param session The <code>Session</code> being associated with the SSO.
    * @return <code>true</code> if the given Session was a new addition (i.e.
    *         was not previously associated with this entry);
    *         <code>false</code> otherwise.
    */
   synchronized boolean addSession(SingleSignOn sso, Session session)
   {
      for (int i = 0; i < sessions.length; i++)
      {
         if (session == sessions[i])
            return false;
      }
      Session results[] = new Session[sessions.length + 1];
      System.arraycopy(sessions, 0, results, 0, sessions.length);
      results[sessions.length] = session;
      sessions = results;
      session.addSessionListener(sso);
      return true;
   }

   /**
    * Removes the given <code>Session</code> from the list of those
    * associated with this SSO.
    *
    * @param session the <code>Session</code> to remove.
    * @return <code>true</code> if the given Session needed to be removed
    *         (i.e. was in fact previously associated with this entry);
    *         <code>false</code> otherwise.
    */
   synchronized boolean removeSession(Session session)
   {
      if (sessions.length == 0)
         return false;

      boolean removed = false;
      Session[] nsessions = new Session[sessions.length - 1];
      for (int i = 0, j = 0; i < sessions.length; i++)
      {
         if (session == sessions[i])
         {
            removed = true;
            continue;
         }
         else if (!removed && i == nsessions.length)
         {
            // We have tested all our sessions, and have not had to
            // remove any; break loop now so we don't cause an
            // ArrayIndexOutOfBounds on nsessions
            break;
         }
         nsessions[j++] = sessions[i];
      }
      sessions = nsessions;
      // Only if we removed a session, do we replace our session list
      if (removed)
         sessions = nsessions;
      return removed;
   }

   /**
    * Returns the <code>Session</code>s associated with this SSO.
    */
   synchronized Session[] findSessions()
   {
      return (this.sessions);
   }

   /**
    * Gets the name of the authentication type originally used to authenticate
    * the user associated with the SSO.
    *
    * @return "BASIC", "CLIENT-CERT", "DIGEST", "FORM" or "NONE"
    */
   String getAuthType()
   {
      return (this.authType);
   }

   /**
    * Gets whether the authentication type associated with the original
    * authentication supports reauthentication.
    *
    * @return <code>true</code> if <code>getAuthType</code> returns
    *         "BASIC" or "FORM", <code>false</code> otherwise.
    */
   boolean getCanReauthenticate()
   {
      return (this.canReauthenticate);
   }

   /**
    * Gets the password credential (if any) associated with the SSO.
    *
    * @return the password credential associated with the SSO, or
    *         <code>null</code> if the original authentication type
    *         does not involve a password.
    */
   String getPassword()
   {
      return (this.password);
   }

   /**
    * Gets the <code>Principal</code> that has been authenticated by
    * the SSO.
    * <p/>
    * <b>NOTE: </b> May return <code>null</code> if this object was
    * retrieved via a lookup from another node in a cluster. Interface
    * <code>Principal</code> does not extend <code>Serializable</code>,
    * so a <code>SingleSignOnEntry</code>'s principal member cannot be
    * serialized as part of SSO management in a cluster.  A
    * <code>Principal</code> cannot be bound to a
    * <code>SingleSignOnEntry</code> until the SSO has been authenticated
    * by the local node.
    *
    * @return The <code>Principal</code> that has been authenticated by
    *         the local SSO, or <code>null</code> if no authentication
    *         has been performed yet in this cluster node.
    */
   Principal getPrincipal()
   {
      return (this.principal);
   }

   /**
    * Sets the <code>Principal</code> that has been authenticated by
    * the SSO.
    */
   void setPrincipal(Principal principal)
   {
      this.principal = principal;
   }

   /**
    * Returns the number of sessions associated with this SSO, either
    * locally or remotely.
    */
   int getSessionCount()
   {
      return (sessions.length);
   }

   /**
    * Gets the username provided by the user as part of the authentication
    * process.
    */
   String getUsername()
   {
      return (this.username);
   }


   /**
    * Updates the SingleSignOnEntry to reflect the latest security
    * information associated with the caller.
    *
    * @param principal the <code>Principal</code> returned by the latest
    *                  call to <code>Realm.authenticate</code>.
    * @param authType  the type of authenticator used (BASIC, CLIENT-CERT,
    *                  DIGEST or FORM)
    * @param username  the username (if any) used for the authentication
    * @param password  the password (if any) used for the authentication
    */
   synchronized boolean updateCredentials(Principal principal, String authType,
      String username, String password)
   {

      boolean changed =
         (safeEquals(this.principal, principal)
         || safeEquals(this.authType, authType)
         || safeEquals(this.username, username)
         || safeEquals(this.password, password));

      this.principal = principal;
      this.authType = authType;
      this.username = username;
      this.password = password;
      this.canReauthenticate =
         (Constants.BASIC_METHOD.equals(authType)
         || Constants.FORM_METHOD.equals(authType));
      return changed;
   }

   // -------------------------------------------------------  Private Methods

   private boolean safeEquals(Object a, Object b)
   {
      return ((a == b)
         || (a != null && a.equals(b))
         || (b != null && b.equals(a)));
   }
}
