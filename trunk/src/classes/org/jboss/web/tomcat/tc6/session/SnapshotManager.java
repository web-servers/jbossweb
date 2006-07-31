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

import org.apache.catalina.Context;

/**
 * Abstract base class for a session snapshot manager.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @version $Revision: 1.1 $
 */
public abstract class SnapshotManager
{
   // The manager the snapshot manager should use
   protected AbstractJBossManager manager;

   // The context-path
   protected String contextPath;

   public SnapshotManager(AbstractJBossManager manager, String path)
   {
      this.manager = manager;
      contextPath = path;
   }

   /**
    * Tell the snapshot manager which session was modified and
    * must be replicated
    */
   public abstract void snapshot(String id);

   /**
    * Start the snapshot manager
    */
   public abstract void start();

   /**
    * Stop the snapshot manager
    */
   public abstract void stop();
}
