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

import java.io.IOException;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import org.apache.catalina.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;

/**
 * This Valve detects all sessions that were used in a request. All sessions are given to a snapshot
 * manager that handles the distribution of modified sessions.
 * <p/>
 * TOMCAT 4.1.12 UPDATE: Added findLifecycleListeners() to comply with the latest
 * Lifecycle interface.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @version $Revision: 1.1 $
 */
public class ClusteredSessionValve extends ValveBase implements Lifecycle
{
   // The info string for this Valve
   private static final String info = "ClusteredSessionValve/1.0";

   // The SnapshotManager that is associated with this Valve
   protected SnapshotManager snapshot;

   // Valve-lifecycle_ helper object
   protected LifecycleSupport support = new LifecycleSupport(this);

   // store the request and response object for parts of the clustering code that
   // have no direct access to this objects
   protected static final ThreadLocal requestThreadLocal = new ThreadLocal();
   protected static final ThreadLocal responseThreadLocal = new ThreadLocal();
   // Manager may store a ref to session id's it has handled here
   protected static final ThreadLocal sessionIdThreadLocal = new ThreadLocal();
   
   /**
    * Create a new Valve.
    *
    * @param snapshot The SnapshotManager associated with this Valve
    */
   public ClusteredSessionValve(SnapshotManager snapshot)
   {
      super();
      this.snapshot = snapshot;
   }

   /**
    * Get information about this Valve.
    */
   public String getInfo()
   {
      return info;
   }

   /**
    * Valve-chain handler method.
    * This method gets called when the request goes through the Valve-chain. Our session replication mechanism replicates the
    * session after request got through the servlet code.
    *
    * @param request  The request object associated with this request.
    * @param response The response object associated with this request.
    */
   public void invoke(Request request, Response response) throws IOException, ServletException
   {
      // Store the request and response object for the clustering code that has no direct access to
      // this objects
      requestThreadLocal.set(request);
      responseThreadLocal.set(response);

      // let the servlet invocation go through
      getNext().invoke(request, response);

      // --> We are now after the servlet invocation

      // Get the session
      HttpSession session = request.getSession(false);

      if (session != null && session.getId() != null)
      {
         // tell the snapshot manager that this session was modified
         snapshot.snapshot(session.getId());
      }

      // don't leak references to the request and response objects
      requestThreadLocal.set(null);
      responseThreadLocal.set(null);
      
      // If JBossCacheManager stored a ref to a session id, we need to clear it
      sessionIdThreadLocal.set(null);
   }

   // Lifecylce-interface
   public void addLifecycleListener(LifecycleListener listener)
   {
      support.addLifecycleListener(listener);
   }

   public void removeLifecycleListener(LifecycleListener listener)
   {
      support.removeLifecycleListener(listener);
   }

   public LifecycleListener[] findLifecycleListeners()
   {
      return support.findLifecycleListeners();
   }

   public void start() throws LifecycleException
   {
      snapshot.start();
      support.fireLifecycleEvent(START_EVENT, this);
   }

   public void stop() throws LifecycleException
   {
      support.fireLifecycleEvent(STOP_EVENT, this);
      snapshot.stop();
   }

}
