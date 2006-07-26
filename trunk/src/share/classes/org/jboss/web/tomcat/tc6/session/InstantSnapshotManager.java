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

import org.apache.catalina.Session;
import org.jboss.logging.Logger;

/**
 * A concrete implementation of the snapshot manager interface
 * that does instant replication of a modified session
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @version $Revision: 1.1 $
 */
public class InstantSnapshotManager extends SnapshotManager
{
   static Logger log = Logger.getLogger(InstantSnapshotManager.class);

   public InstantSnapshotManager(AbstractJBossManager manager, String path)
   {
      super(manager, path);
   }

   /**
    * Instant replication of the modified session
    */
   public void snapshot(String id)
   {
      try
      {
         // find the session that has been modified
         Session session = manager.findSession(id);
         manager.storeSession(session);
      }
      catch (Exception e)
      {
         log.warn("Failed to replicate sessionID:" + id, e);
      }
   }

   public void start()
   {
   }

   public void stop()
   {
   }
}
