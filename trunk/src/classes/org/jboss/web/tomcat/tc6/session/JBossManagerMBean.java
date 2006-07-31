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

import org.jboss.web.tomcat.statistics.ReplicationStatistics;

/**
 * The MBean-interface for the JBossManager
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @author Brian Stansberry
 * 
 * @version $Revision: 1.1 $
 */
public interface JBossManagerMBean
{
   /**
    * Gets the replication statistics for the sessions managed
    * by this manager.
    * 
    * @return the statistics
    */
   ReplicationStatistics getReplicationStatistics();

   /**
    * Resets all statistics.
    */
   void resetStats();

   /**
    * Gets the elapsed time since this manager was instantiated or the 
    * last call to resetStats()
    */
   long timeInSecondsSinceLastReset();

   /**
    * Gets the number of sessions active on this node.  Does not include
    * replicated sessions that have not been accessed on this node.
    */
   long getActiveSessionCount();

   /**
    * Gets the number of times session creation has failed because the
    * number of active sessions exceeds 
    * {@link #getMaxActiveAllowed() maxActiveAllowed}
    */
   long getRejectedSessionCount();

   /**
    * Gets the number of sessions created on this node. Does not include
    * sessions initially created on other nodes, even if those sessions
    * were accessed on this node.
    */
   long getCreatedSessionCount();

   /**
    * Gets the number of sessions that have been expired on this node.
    */
   long getExpiredSessionCount();
   
   /**
    * Gets the highest number of sessions concurrently active on this node.   
    * Does not include replicated sessions that have not been accessed on 
    * this node.
    */
   long getMaxActiveSessionCount();
   
   /**
    * Gets the maximum number of active sessions that will concurrently be
    * allowed on this node.  Does not include replicated sessions that have 
    * not been accessed on this node.
    */
   int getMaxActiveAllowed();
   
   /**
    * Sets the maximum number of active sessions that will concurrently be
    * allowed on this node.  Does not include replicated sessions that have 
    * not been accessed on this node.
    * 
    * <p>
    * Note that if sessions fail over to this node from other nodes, the max
    * number of active sessions may exceed this value.
    * </p>
    * 
    * @param max the max number of sessions, or <code>-1</code> if there is
    *            no limit.
    */
   void setMaxActiveAllowed(int max);
   
   /**
    * Gets the maximum time interval, in seconds, between client requests
    * after which sessions created by this manager should be expired.  A 
    * negative time indicates that the session should never time out.
    */
   int getMaxInactiveInterval();
   
   /**
    * Sets the maximum time interval, in seconds, between client requests
    * after which sessions created by this manager should be expired.  A 
    * negative time indicates that the session should never time out.
    *
    * @param interval The new maximum interval
    */
   void setMaxInactiveInterval(int minutes);
   
   /**
    * Gets whether this manager's sessions are distributable.
    */
   boolean getDistributable();
   
   /**
    * Gets the cumulative number of milliseconds spent in the 
    * <code>Manager.backgroundProcess()</code> method.
    */
   long getProcessingTime();
   
   /**
    * Outputs the replication statistics as an HTML table, with one row
    * per session.
    */
   String reportReplicationStatistics();
   
   /**
    * Outputs the replication statistics as a comma-separated-values, with one 
    * row per session.  First row is a header listing field names.
    */
   String reportReplicationStatisticsCSV();

   /**
    * Outputs the replication statistics for the given session as a set of 
    * comma-separated-values.  First row is a header listing field names.
    */
   String reportReplicationStatisticsCSV(String sessionId);
   
   /**
    * Gets the number of characters used in creating a session id.  Excludes
    * any jvmRoute.
    */
   int getSessionIdLength();
}
