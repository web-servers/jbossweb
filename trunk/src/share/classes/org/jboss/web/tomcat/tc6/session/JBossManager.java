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

import org.apache.catalina.*;
import org.jboss.mx.util.MBeanServerLocator;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.beans.PropertyChangeEvent;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.jboss.logging.Logger;
import org.jboss.metadata.WebMetaData;
import org.jboss.web.tomcat.statistics.ReplicationStatistics;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * Base abstract implementation of Tomcat manager without the concept of
 * session operations, e.g., add, remove, etc.
 *
 * @author Ben Wang
 * @version $Revision: 1.1 $
 */
public abstract class JBossManager
   implements AbstractJBossManager, Lifecycle,
      JBossManagerMBean, PropertyChangeListener
{

   // -- Constants ----------------------------------------
   /**
    * Informational name for this Catalina component
    */
   private static final String info_ = "JBossManager/1.0";

   // -- Class attributes ---------------------------------
   protected ReplicationStatistics stats_ = new ReplicationStatistics();

   /**
    * Policy to determine if a session is dirty
    */
   protected int invalidateSessionPolicy_ = WebMetaData.SESSION_INVALIDATE_SET_AND_NON_PRIMITIVE_GET;
   /**
    * Replication granulairty.
    */
   protected int replicationGranularity_ = WebMetaData.REPLICATION_GRANULARITY_SESSION;
   /**
    * The lifecycle_ event support for this component.
    */
   protected LifecycleSupport lifecycle_ = new LifecycleSupport(this);
   /**
    * Has this component been started_ yet?
    */
   protected boolean started_ = false;
   /**
    * The objectname this Manager is associated with
    */
   protected ObjectName objectName_;
   /**
    * The Log-object for this class
    */
   protected Logger log_ = Logger.getLogger(this.getClass().getName());
   /**
    * The Container with which this Manager is associated.
    */
   protected Container container_;
  /**
   /**
    * The distributable flag for Sessions created by this Manager.  If this
    * flag is set to <code>true</code>, any user attributes added to a
    * session controlled by this Manager must be Serializable.
    */
   protected boolean distributable_ = true;
   /**
    * The default maximum inactive interval for Sessions created by
    * this Manager.
    */
   protected int maxInactiveInterval_ = 60;
   /**
    * The session id length of Sessions created by this Manager.
    */
   protected int sessionIdLength_ = 16;

   // Maximum of ative sessions allowed. -1 is unlimited.
   protected int maxActive_ = -1;

   // Number of sessions created by this manager
   protected int createdCounter_ = 0;

   // number of rejected sessions because the number active sessions exceeds maxActive
   protected int rejectedCounter_ = 0;

   // Number of active sessions
   protected int activeCounter_ = 0;

   // Maximum number of active sessions seen so far
   protected int maxActiveCounter_ = 0;

   // number of expired session ids. Not sure what exactly does it mean in our clustered case.
   protected int expiredCounter_ = 0;

   protected long timeSinceLastReset_ = 0;

   // Cumulative time spent in backgroundProcess
   protected long processingTime_ = 0;
   
   /**
    * Map<String,ClusteredSession>. Store the local sessions.
    */
   protected final Map sessions_ = new ConcurrentHashMap();

   /**
    * If set to true, it will not replicate the access time stamp unless attributes are dirty.
    */
   protected boolean useLocalCache_ = false;

   /**
    * The property change support for this component.
    */
   protected PropertyChangeSupport support_ = new PropertyChangeSupport(this);

   protected SessionIDGenerator sessionIDGenerator_;

   protected String jvmRoute_;
   
   // TODO Need a string manager to handle exception localization

   public JBossManager()
   {
      sessionIDGenerator_ = SessionIDGenerator.getInstance();

   }

   public void init(String name, WebMetaData webMetaData, boolean useJK, boolean useLocalCache)
           throws ClusteringNotSupportedException
   {
      replicationGranularity_ = webMetaData.getReplicationGranularity();
      invalidateSessionPolicy_ = webMetaData.getInvalidateSessionPolicy();
      useLocalCache_ = useLocalCache;
      log_.info("init(): replicationGranularity_ is " + replicationGranularity_ +
         " and invaldateSessionPolicy is " + invalidateSessionPolicy_);

      try
      {
         // Give this manager a name
         objectName_ = new ObjectName("jboss.web:service=ClusterManager,WebModule=" + name);
      }
      catch (Throwable e)
      {
         log_.error("Could not create ObjectName", e);
         throw new ClusteringNotSupportedException(e.toString());
      }
   }

   public int getInvalidateSessionPolicy()
   {
      return this.invalidateSessionPolicy_;
   }

   /**
    * Retrieve the enclosing Engine for this Manager.
    *
    * @return an Engine object (or null).
    */
   public Engine getEngine()
   {
      Engine e = null;
      for (Container c = getContainer(); e == null && c != null; c = c.getParent())
      {
         if (c != null && c instanceof Engine)
         {
            e = (Engine) c;
         }
      }
      return e;
   }

   /**
    * Retrieve the JvmRoute for the enclosing Engine.
    *
    * @return the JvmRoute or null.
    */
   public String getJvmRoute()
   {
      if (jvmRoute_ == null)
      {
         Engine e = getEngine();
         jvmRoute_= (e == null ? null : e.getJvmRoute());
      }
      return jvmRoute_;
   }

   /**
    * Get a new session-id from the distributed store
    *
    * @return new session-id
    */
   protected String getNextId()
   {
      return sessionIDGenerator_.getSessionId();
   }

   /**
    * Gets the JMX <code>ObjectName</code> under
    * which our <code>TreeCache</code> is registered. 
    */
   public ObjectName getObjectName()
   {
      return objectName_;
   }

   public boolean isUseLocalCache()
   {
      return useLocalCache_;
   }

   /**
    * Sets a new cookie for the given session id and response
    *
    * @param sessionId The session id
    */
   public void setSessionCookie(String sessionId)
   {
      HttpServletResponse response = (HttpServletResponse) ClusteredSessionValve.responseThreadLocal.get();
      setNewSessionCookie(sessionId, response);
   }

   public void setNewSessionCookie(String sessionId, HttpServletResponse response)
   {
      if (response != null)
      {
         Context context = (Context) container_;
         Connector connector = ((Response)response).getConnector();
         if (context.getCookies())
         {
            // set a new session cookie
            Cookie newCookie = new Cookie(Globals.SESSION_COOKIE_NAME, sessionId);
            if (log_.isDebugEnabled())
            {
               log_.debug("Setting cookie with session id:" + sessionId + " & name:" + Globals.SESSION_COOKIE_NAME);
            }

            String contextPath = null;
            if (!connector.getEmptySessionPath() && (context != null)) {
                contextPath = context.getEncodedPath();
            }

            if ((contextPath != null) && (contextPath.length() > 0)) {
                newCookie.setPath(contextPath);
            } else {
                newCookie.setPath("/");
            }

            if (connector.getSecure()) {
                newCookie.setSecure(true);
            }

            response.addCookie(newCookie);
         }
      }
   }

   // JBossManagerMBean-methods -------------------------------------

   // A better property name for the MBean API
   public int getMaxActiveAllowed()
   {
      return getMaxActive();
   }
   
   // A better property name for the MBean API
   public void setMaxActiveAllowed(int maxActive)
   {
      setMaxActive(maxActive);
   }
   
   public long getMaxActiveSessionCount()
   {
      return this.maxActiveCounter_;
   }

   public ReplicationStatistics getReplicationStatistics()
   {
      return stats_;
   }

   public void resetStats()
   {
      stats_.resetStats();
      activeCounter_ = 0;
      maxActiveCounter_ = 0;
      rejectedCounter_ = 0;
      createdCounter_ = 0;
      expiredCounter_ = 0;
      processingTime_ = 0;
      timeSinceLastReset_ = System.currentTimeMillis();
   }

   public long timeInSecondsSinceLastReset()
   {
      return (System.currentTimeMillis() - timeSinceLastReset_) / (1000L);
   }

   public long getActiveSessionCount()
   {
      return getActiveSessions();
   }

   public long getCreatedSessionCount()
   {
      return createdCounter_;
   }

   public long getExpiredSessionCount()
   {
      return expiredCounter_;
   }

   public long getRejectedSessionCount()
   {
      return rejectedCounter_;
   }

   public int getSessionMaxAliveTime()
   {
       return 0;
   }

   public void setSessionMaxAliveTime(int sessionMaxAliveTime)
   {
   }

   public int getSessionAverageAliveTime()
   {
       return 0;
   }

   public void setSessionAverageAliveTime(int sessionAverageAliveTime)
   {
   }

   public String reportReplicationStatistics()
   {
      StringBuffer tmp = new StringBuffer();
      HashMap copy = new HashMap(stats_.getStats());
      Iterator iter = copy.entrySet().iterator();
      tmp.append("<table><tr>");
      tmp.append("<th>sessionID</th>");
      tmp.append("<th>replicationCount</th>");
      tmp.append("<th>minPassivationTime</th>");
      tmp.append("<th>maxPassivationTime</th>");
      tmp.append("<th>totalPassivationTime</th>");
      tmp.append("<th>minReplicationTime</th>");
      tmp.append("<th>maxReplicationTime</th>");
      tmp.append("<th>totalReplicationlTime</th>");
      tmp.append("<th>loadCount</th>");
      tmp.append("<th>minLoadTime</th>");
      tmp.append("<th>maxLoadTime</th>");
      tmp.append("<th>totalLoadTime</th>");
      while (iter.hasNext())
      {
         Map.Entry entry = (Map.Entry) iter.next();
         ReplicationStatistics.TimeStatistic stat = (ReplicationStatistics.TimeStatistic) entry.getValue();
         if (stat != null)
         {
            tmp.append("<tr><td>");
            tmp.append(entry.getKey());
            tmp.append("</td><td>");
            tmp.append(stat.replicationCount);
            tmp.append("</td><td>");
            tmp.append(stat.minPassivationTime);
            tmp.append("</td><td>");
            tmp.append(stat.maxPassivationTime);
            tmp.append("</td><td>");
            tmp.append(stat.totalPassivationTime);
            tmp.append("</td><td>");
            tmp.append(stat.minReplicationTime);
            tmp.append("</td><td>");
            tmp.append(stat.maxReplicationTime);
            tmp.append("</td><td>");
            tmp.append(stat.totalReplicationlTime);
            tmp.append("</td><td>");
            tmp.append(stat.loadCount);
            tmp.append("</td><td>");
            tmp.append(stat.minLoadTime);
            tmp.append("</td><td>");
            tmp.append(stat.maxLoadTime);
            tmp.append("</td><td>");
            tmp.append(stat.totalLoadlTime);
            tmp.append("</td></tr>");
         }
      }
      tmp.append("</table>");
      copy.clear();
      return tmp.toString();

   }
   
   public String reportReplicationStatisticsCSV()
   {
      StringBuffer tmp = createCSVHeader();
      HashMap copy = new HashMap(stats_.getStats());
      Iterator iter = copy.entrySet().iterator();
      while (iter.hasNext())
      {
         Map.Entry entry = (Map.Entry) iter.next();
         ReplicationStatistics.TimeStatistic stat = (ReplicationStatistics.TimeStatistic) entry.getValue();
         if (stat != null)
         {
            tmp.append("\n");
            tmp.append(entry.getKey());
            tmp.append(",");
            tmp.append(stat.replicationCount);
            tmp.append(",");
            tmp.append(stat.minPassivationTime);
            tmp.append(",");
            tmp.append(stat.maxPassivationTime);
            tmp.append(",");
            tmp.append(stat.totalPassivationTime);
            tmp.append(",");
            tmp.append(stat.minReplicationTime);
            tmp.append(",");
            tmp.append(stat.maxReplicationTime);
            tmp.append(",");
            tmp.append(stat.totalReplicationlTime);
            tmp.append(",");
            tmp.append(stat.loadCount);
            tmp.append(",");
            tmp.append(stat.minLoadTime);
            tmp.append(",");
            tmp.append(stat.maxLoadTime);
            tmp.append(",");
            tmp.append(stat.totalLoadlTime);
         }
      }
      copy.clear();
      return tmp.toString();

   }
   
   public String reportReplicationStatisticsCSV(String sessionId)
   {
      StringBuffer tmp = createCSVHeader();
      Map stats = stats_.getStats();
      ReplicationStatistics.TimeStatistic stat = 
         (ReplicationStatistics.TimeStatistic) stats.get(sessionId);
      if (stat != null)
      {
         tmp.append("\n");
         tmp.append(sessionId);
         tmp.append(",");
         tmp.append(stat.replicationCount);
         tmp.append(",");
         tmp.append(stat.minPassivationTime);
         tmp.append(",");
         tmp.append(stat.maxPassivationTime);
         tmp.append(",");
         tmp.append(stat.totalPassivationTime);
         tmp.append(",");
         tmp.append(stat.minReplicationTime);
         tmp.append(",");
         tmp.append(stat.maxReplicationTime);
         tmp.append(",");
         tmp.append(stat.totalReplicationlTime);
         tmp.append(",");
         tmp.append(stat.loadCount);
         tmp.append(",");
         tmp.append(stat.minLoadTime);
         tmp.append(",");
         tmp.append(stat.maxLoadTime);
         tmp.append(",");
         tmp.append(stat.totalLoadlTime);
      }
      return tmp.toString();
   }
   
   private StringBuffer createCSVHeader()
   {
      StringBuffer tmp = new StringBuffer();
      tmp.append("sessionID,");
      tmp.append("replicationCount,");
      tmp.append("minPassivationTime,");
      tmp.append("maxPassivationTime,");
      tmp.append("totalPassivationTime,");
      tmp.append("minReplicationTime,");
      tmp.append("maxReplicationTime,");
      tmp.append("totalReplicationlTime,");
      tmp.append("loadCount,");
      tmp.append("minLoadTime,");
      tmp.append("maxLoadTime,");
      tmp.append("totalLoadTime");
      
      return tmp;
   }

   // Lifecycle-methods -------------------------------------

   public void addLifecycleListener(LifecycleListener listener)
   {
      lifecycle_.addLifecycleListener(listener);
   }

   public LifecycleListener[] findLifecycleListeners()
   {
      return lifecycle_.findLifecycleListeners();
   }

   public void removeLifecycleListener(LifecycleListener listener)
   {
      lifecycle_.removeLifecycleListener(listener);
   }

   /**
    * Start this Manager
    *
    * @throws org.apache.catalina.LifecycleException
    *
    */
   public void start() throws LifecycleException
   {
      startManager();
   }

   /**
    * Stop this Manager
    *
    * @throws org.apache.catalina.LifecycleException
    *
    */
   public void stop() throws LifecycleException
   {
      resetStats();
      stopManager();
   }

   /**
    * Prepare for the beginning of active use of the public methods of this
    * component.  This method should be called after <code>configure()</code>,
    * and before any of the public methods of the component are utilized.
    *
    * @throws IllegalStateException if this component has already been
    *                               started_
    * @throws org.apache.catalina.LifecycleException
    *                               if this component detects a fatal error
    *                               that prevents this component from being used
    */
   protected void startManager() throws LifecycleException
   {
      log_.info("Starting JBossManager");

      // Validate and update our current component state
      if (started_)
         throw new LifecycleException
            ("JBossManager alreadyStarted");
      lifecycle_.fireLifecycleEvent(START_EVENT, null);
      started_ = true;

      // register ClusterManagerMBean to the MBeanServer
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         server.registerMBean(this, objectName_);
      }
      catch (Exception e)
      {
         log_.error("Could not register ClusterManagerMBean to MBeanServer", e);
      }
   }

   /**
    * Gracefully terminate the active use of the public methods of this
    * component.  This method should be the last one called on a given
    * instance of this component.
    *
    * @throws IllegalStateException if this component has not been started_
    * @throws org.apache.catalina.LifecycleException
    *                               if this component detects a fatal error
    *                               that needs to be reported
    */
   protected void stopManager() throws LifecycleException
   {
      log_.info("Stopping JBossManager");

      // Validate and update our current component state
      if (!started_)
         throw new LifecycleException
            ("JBossManager notStarted");
      lifecycle_.fireLifecycleEvent(STOP_EVENT, null);
      started_ = false;

      // unregister ClusterManagerMBean from the MBeanServer
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         server.unregisterMBean(objectName_);
      }
      catch (Exception e)
      {
         log_.error("Could not unregister ClusterManagerMBean from MBeanServer", e);
      }
   }

   // Manager-methods -------------------------------------
   public Container getContainer()
   {
      return container_;
   }

   public void setContainer(Container container)
   {

      // De-register from the old Container (if any)
      if ((this.container_ != null) && (this.container_ instanceof Context))
         this.container_.removePropertyChangeListener(this);

      // Default processing provided by our superclass
      this.container_ = container;

      // Register with the new Container (if any)
      if ((this.container_ != null) && (this.container_ instanceof Context))
      {
         setMaxInactiveInterval
            (((Context) this.container_).getSessionTimeout() * 60);
         this.container_.addPropertyChangeListener(this);
      }
   }

   public boolean getDistributable()
   {
      return distributable_;
   }

   public void setDistributable(boolean distributable)
   {
      this.distributable_ = distributable;
   }

   public String getInfo()
   {
      return info_;
   }

   public int getMaxInactiveInterval()
   {
      return maxInactiveInterval_;
   }

   public void setMaxInactiveInterval(int interval)
   {
      this.maxInactiveInterval_ = interval;
   }

   public int getSessionIdLength()
   {
      return sessionIdLength_;
   }

   public void setSessionIdLength(int idLength)
   {
      this.sessionIdLength_ = idLength;
   }

   public int getSessionCounter()
   {
      return createdCounter_;
   }

   public void setSessionCounter(int sessionCounter)
   {
      this.createdCounter_ = sessionCounter;
   }

   public int getMaxActive()
   {
      return maxActive_;
   }

   public void setMaxActive(int maxActive)
   {
      this.maxActive_ = maxActive;
   }

   public int getExpiredSessions()
   {
      return expiredCounter_;
   }

   public void setExpiredSessions(int expiredSessions)
   {
      this.expiredCounter_ = expiredSessions;
   }

   public int getRejectedSessions()
   {
      return rejectedCounter_;
   }

   public void setRejectedSessions(int rejectedSessions)
   {
      this.rejectedCounter_ = rejectedSessions;
   }
   
   public long getProcessingTime()
   {
      return this.processingTime_;
   }

   public void addPropertyChangeListener(PropertyChangeListener listener)
   {
      support_.addPropertyChangeListener(listener);
   }

   /**
    * Remove the active session locally from the manager without replicating to the cluster. This can be
    * useful when the session is exipred, for example, where there is not need to propagate the expiration.
    *
    * @param session
    */
   public abstract void removeLocal(Session session);

   /**
    * Store the modified session.
    *
    * @param session
    */
   public abstract boolean storeSession(Session session);

   public int getActiveSessions()
   {
      return activeCounter_;
   }

/*
   public void add(Session session)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public Session createEmptySession()
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Session createSession()
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Session findSession(String id) throws IOException
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Session[] findSessions()
   {
      return new Session[0];  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void remove(Session session)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }
*/

   public void load() throws ClassNotFoundException, IOException
   {
      // TODO. Implement persistence layer.
      throw new RuntimeException("JBossManager.load(): Method not implemented.");
   }

   public void removePropertyChangeListener(PropertyChangeListener listener)
   {
      support_.removePropertyChangeListener(listener);
   }

   public void unload() throws IOException
   {
      // TODO. Implement persistence layer.
      throw new RuntimeException("JBossManager.load(): Method not implemented.");
   }


   public void backgroundProcess()
   {
      // Called from Catalina StandardEngine for every 60 seconds.
      
      long start = System.currentTimeMillis();
      
      processExpires();
      
      long elapsed = System.currentTimeMillis() - start;
      
      processingTime_ += elapsed;
   }

   /**
    * Go through all sessions and look if they have expired
    */
   protected void processExpires()
   {
      // What's the time?
//      long timeNow = System.currentTimeMillis();

      // Get all sessions
      Session sessions[] = findSessions();
      if (log_.isDebugEnabled())
      {
         log_.debug("Looking for sessions that have expired ...");
      }

      for (int i = 0; i < sessions.length; ++i)
      {
         ClusteredSession session = (ClusteredSession) sessions[i];

         // We only look at valid sessions. This will remove session if not valid already.
         if (!session.isValid())
         {
            continue;
         }

         /* I don't think it is right to check idle time based on lastAccessedTime since it may
         // remove some request that is currently in progress!!!
         // How long are they allowed to be idle?
         int maxInactiveInterval = session.getMaxInactiveInterval();

         // Negative values = never expire
         if( maxInactiveInterval < 0 )
         {
            continue;
         }

         // How long has this session been idle?
         int timeIdle =
            (int) ((timeNow - session.getLastAccessedTime()) / 1000L);

         // Too long?
         if( timeIdle >= maxInactiveInterval )
         {
            try
            {
               log_.debug("Session with id = " + session.getId() + " has expired on local node");
               remove(session);
            }
            catch(Throwable t)
            {
               log_.error("Problems while expiring session with id = " + session.getId(), t);
            }
         }
         */
      }
   }

   public void propertyChange(PropertyChangeEvent evt)
   {
      // TODO Need to handle it here.
   }

   /**
    * Find in-memory sessions, if any.
    * @return local session found. Sessions of size 0, if not found.
    */
   abstract public ClusteredSession[] findLocalSessions();

   /**
    * Find in-memory sessions, if any.
    * @param realId the Session id without JvmRoute tag.
    * @return local session found. Null if not found.
    */
   abstract public ClusteredSession findLocalSession(String realId);

}
