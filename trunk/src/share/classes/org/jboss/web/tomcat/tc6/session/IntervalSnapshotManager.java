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

import java.util.HashMap;
import java.util.Iterator;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.jboss.logging.Logger;

/**
 * A snapshot manager that collects all modified sessions over a given
 * period of time and distributes them en bloc.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @version $Revision: 1.1 $
 */
public class IntervalSnapshotManager extends SnapshotManager implements Runnable
{
   static Logger log = Logger.getLogger(IntervalSnapshotManager.class);

   // the interval in ms
   protected int interval = 1000;

   // the modified sessions
   protected HashMap sessions = new HashMap();

   // the distribute thread
   protected Thread thread = null;

   // has the thread finished?
   protected boolean threadDone = false;

   public IntervalSnapshotManager(AbstractJBossManager manager, String path)
   {
      super(manager, path);
   }

   public IntervalSnapshotManager(AbstractJBossManager manager, String path, int interval)
   {
      super(manager, path);
      this.interval = interval;
   }

   /**
    * Store the modified session in a hashmap for the distributor thread
    */
   public void snapshot(String id)
   {
      try
      {
         Session session = (Session) manager.findSession(id);
         synchronized (sessions)
         {
            sessions.put(id, session);
         }
      }
      catch (Exception e)
      {
         log.warn("Failed to replicate sessionID:" + id, e);
      }
   }

   /**
    * Distribute all modified sessions
    */
   protected void processSessions()
   {
      HashMap copy = new HashMap(sessions.size());

      synchronized (sessions)
      {
         copy.putAll(sessions);
         sessions.clear();
      }
      Iterator iter = copy.values().iterator();

      // distribute all modified sessions using default replication type
      while (iter.hasNext())
      {
         Session session = (Session) iter.next();
         manager.storeSession(session);
      }
      copy.clear();
   }

   /**
    * Start the snapshot manager
    */
   public void start()
   {
      startThread();
   }

   /**
    * Stop the snapshot manager
    */
   public void stop()
   {
      stopThread();
      synchronized (sessions)
      {
         sessions.clear();
      }
   }

   /**
    * Start the distributor thread
    */
   protected void startThread()
   {
      if (thread != null)
      {
         return;
      }

      thread = new Thread(this, "ClusteredSessionDistributor[" + contextPath + "]");
      thread.setDaemon(true);
      thread.setContextClassLoader(manager.getContainer().getLoader().getClassLoader());
      threadDone = false;
      thread.start();
   }

   /**
    * Stop the distributor thread
    */
   protected void stopThread()
   {
      if (thread == null)
      {
         return;
      }
      threadDone = true;
      thread.interrupt();
      try
      {
         thread.join();
      }
      catch (InterruptedException e)
      {
      }
      thread = null;
   }

   /**
    * Little Thread - sleep awhile...
    */
   protected void threadSleep()
   {
      try
      {
         Thread.sleep(interval);
      }
      catch (InterruptedException e)
      {
      }
   }

   /**
    * Thread-loop
    */
   public void run()
   {
      while (!threadDone)
      {
         threadSleep();
         processSessions();
      }
   }
}
