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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.BatchModeTransactionManagerLookup;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.aop.PojoCache;
import org.jboss.cache.aop.PojoCacheMBean;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.web.tomcat.tc6.Tomcat6;

/**
 * A Tomcat <code>Cluster</code> implementation that uses a JBoss
 * <code>TreeCache</code> to support intra-cluster session replication.
 * <p>
 * This class registers a <code>TreeCache</code> in JMX, making it 
 * available to other users who wish to replicate data within the cluster.
 * </p>
 *  
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class JBossCacheCluster
   implements JBossCacheClusterMBean, Lifecycle 
{
   //  -------------------------------------------------------  Static Fields
   
   protected static final String info = "JBossCacheCluster/2.1";

   public static Log log = LogFactory.getLog(JBossCacheCluster.class);

   public static final String DEFAULT_CLUSTER_NAME = "Tomcat-Cluster";

   /** TreeCache's isolation level */
   public static final String DEFAULT_ISOLATION_LEVEL = "REPEATABLE_READ";
   
   /** TreeCache's cache mode */
   public static final String DEFAULT_CACHE_MODE = "REPL_ASYNC";
   
   /** TreeCache's lock aquisition timeout */
   public static final long DEFAULT_LOCK_TIMEOUT = 15000;
   
   /** TransactionManagerLookup implementation that the TreeCache should use. */
   public static final String DEFAULT_TM_LOOKUP = 
      BatchModeTransactionManagerLookup.class.getName();

   public static final String DEFAULT_CACHE_CONFIG_PATH = "conf/cluster-cache.xml";
   
   //  -------------------------------------------------------  Instance Fields

   /** Parent container of this cluster. */
   private Container container = null;
   
   /** Our JMX Server. */
   private MBeanServer mserver = null;
   
   /** Name under which we are registered in JMX */
   private ObjectName objectName = null;
   
   /** Are we started? */
   private boolean started = false;

   /** The lifecycle event support for this component. */
   private LifecycleSupport lifecycle = new LifecycleSupport(this);

   /** Our tree cache */
   private PojoCacheMBean treeCache = null;
   
   /** Name under which our TreeCache is registered in JMX */
   private String treeCacheObjectName = Tomcat6.DEFAULT_CACHE_NAME;
   
   /** Did we create the tree cache, or was it already registered in JMX? */
   private boolean treeCacheLocal = false;
   
   /** Name of the tree cache's JGroups channel */
   private String clusterName = null;
   
   /** File name, URL or String to use to configure JGroups. */
   private String cacheConfigPath = null;
   
   /**
    * Implementation of Manager to instantiate when 
    * createManager() is called.
    */
   private String managerClassName = JBossCacheManager.class.getName();

   /** Does the Engine in which we are running use mod_jk? */
   private boolean useJK = false;
   
   /** Whether our Managers should use a local cache. */
   private boolean useLocalCache = false;

   /**
    * Default replication trigger to assign to our 
    * Managers that haven't had this property set.
    */
   private String defaultReplicationTrigger = null;

   /**
    * Default replication granularity to assign to our Managers
    * that haven't had this property set.
    */
   private String defaultReplicationGranularity = null;
   
   /**
    * JBossCacheManager's snapshot mode.
    */
   private String snapshotMode = null;
   
   /**
    * JBossCacheManager's snapshot interval.
    */
   private int snapshotInterval = 0;
   
   /** Whether we use batch mode replication for field level granularity */
   private boolean replicationFieldBatchMode;
   
   //  ----------------------------------------------------------  Constructors
   
   /**
    * Default constructor.
    */
   public JBossCacheCluster()
   {
      super();
   }

   //  ------------------------------------------------------------  Properties 

   /**
    * Gets a String representation of the JMX <code>ObjectName</code> under
    * which our <code>TreeCache</code> is registered.
    * <p>
    * If this property is not explicitly set, the <code>TreeCache</code> will
    * be registered under 
    * @{@link Tomcat6.DEFAULT_CACHE_NAME the default name used in 
    * embedded JBoss/Tomcat}.
    * </p>
    * 
    * @jmx.managed-attribute
    */
   public String getCacheObjectName()
   {
      return treeCacheObjectName;
   }

   /**
    * Sets the JMX <code>ObjectName</code> under which our 
    * <code>TreeCache</code> is registered, if already created, or under
    * which it should be registered if this object creates it.
    * 
    * @jmx.managed-attribute
    */
   public void setCacheObjectName(String objectName)
   {
      this.treeCacheObjectName = objectName;
   }

   /**
    * Sets the name of the <code>TreeCache</code>'s JGroups channel.
    * <p>
    * This property is ignored if a <code>TreeCache</code> is already
    * registered under the provided 
    * {@link #setCacheObjectName cache object name}.
    * </p>
    * 
    * @jmx.managed-attribute
    */
   public void setClusterName(String clusterName)
   {
      this.clusterName = clusterName;
   }

   /**
    * Gets the filesystem path, which can either be absolute or a path 
    * relative to <code>$CATALINA_BASE</code>, where a
    * a JBossCache configuration file can be found.
    * 
    * @return  a path, either absolute or relative to 
    *          <code>$CATALINA_BASE</code>.  Will return 
    *          <code>null</code> if no such path was configured.
    * 
    * @jmx.managed-attribute
    */
   public String getCacheConfigPath()
   {
      return cacheConfigPath;
   }

   /** 
    * Sets the filesystem path, which can either be absolute or a path 
    * relative to <code>$CATALINA_BASE</code>, where a
    * a JBossCache configuration file can be found.
    * <p>
    * This property is ignored if a <code>TreeCache</code> is already
    * registered under the provided 
    * {@link #setCacheObjectName cache object name}.
    * </p>
    * 
    * @param cacheConfigPath   a path, absolute or relative to 
    *                          <code>$CATALINA_BASE</code>,
    *                          pointing to a JBossCache configuration file.
    *                            
    * @jmx.managed-attribute
    */
   public void setCacheConfigPath(String cacheConfigPath)
   {
      this.cacheConfigPath = cacheConfigPath;
   }

   /**
    * Gets the name of the implementation of Manager to instantiate when 
    * createManager() is called.
    * 
    * @jmx.managed-attribute
    */
   public String getManagerClassName()
   {
      return managerClassName;
   }

   /**
    * Sets the name of the implementation of Manager to instantiate when 
    * createManager() is called.
    * <p>
    * This should be {@link JBossCacheManager} (the default) or a subclass
    * of it.
    * </p>
    * 
    * @jmx.managed-attribute
    */
   public void setManagerClassName(String managerClassName)
   {
      this.managerClassName = managerClassName;
   }

   /**
    * Gets whether the <code>Engine</code> in which we are running
    * uses <code>mod_jk</code>.
    * 
    * @jmx.managed-attribute
    */
   public boolean isUseJK()
   {
      return useJK;
   }

   /**
    * Sets whether the <code>Engine</code> in which we are running
    * uses <code>mod_jk</code>.
    * 
    * @jmx.managed-attribute
    */
   public void setUseJK(boolean useJK)
   {
      this.useJK = useJK;
   }

   /**
    * Gets the <code>JBossCacheManager</code>'s <code>useLocalCache</code>
    * property.
    * 
    * @jmx.managed-attribute
    */
   public boolean isUseLocalCache()
   {
      return useLocalCache;
   }

   /**
    * Sets the <code>JBossCacheManager</code>'s <code>useLocalCache</code>
    * property.
    * 
    * @jmx.managed-attribute
    */
   public void setUseLocalCache(boolean useLocalCache)
   {
      this.useLocalCache = useLocalCache;
   }

   /**
    * Gets the default granularity of session data replicated across the 
    * cluster; i.e. whether the entire session should be replicated when 
    * replication is triggered, or only modified attributes.
    * <p>
    * The value of this property is applied to <code>Manager</code> instances
    * that did not have an equivalent property explicitly set in 
    * <code>context.xml</code> or <code>server.xml</code>.
    * </p>
    * 
    * @jmx.managed-attribute
    */
   public String getDefaultReplicationGranularity()
   {
      return defaultReplicationGranularity;
   }

   /**
    * Sets the granularity of session data replicated across the cluster.
    * Valid values are:
    * <ul>
    * <li>SESSION</li>
    * <li>ATTRIBUTE</li>
    * <li>FIELD</li>
    * </ul>
    * @jmx.managed-attribute
    */
   public void setDefaultReplicationGranularity(
         String defaultReplicationGranularity)
   {
      this.defaultReplicationGranularity = defaultReplicationGranularity;
   }

   /**
    * Gets the type of operations on a <code>HttpSession</code> that 
    * trigger replication.
    * <p>
    * The value of this property is applied to <code>Manager</code> instances
    * that did not have an equivalent property explicitly set in 
    * <code>context.xml</code> or <code>server.xml</code>.
    * </p>
    *  
    * @jmx.managed-attribute
    */
   public String getDefaultReplicationTrigger()
   {
      return defaultReplicationTrigger;
   }

   /**
    * Sets the type of operations on a <code>HttpSession</code> that 
    * trigger replication.  Valid values are:
    * <ul>
    * <li>SET_AND_GET</li>
    * <li>SET_AND_NON_PRIMITIVE_GET</li>
    * <li>SET</li>
    * </ul>
    * 
    * @jmx.managed-attribute
    */
   public void setDefaultReplicationTrigger(String defaultTrigger)
   {
      this.defaultReplicationTrigger = defaultTrigger;
   }
   
   /**
    * Gets whether Managers should use batch mode replication.
    * Only meaningful if replication granularity is set to <code>FIELD</code>.
    * 
    * @jmx.managed-attribute
    */
   public boolean getDefaultReplicationFieldBatchMode()
   {
      return replicationFieldBatchMode;
   }
   
   /**
    * Sets whether Managers should use batch mode replication.
    * Only meaningful if replication granularity is set to <code>FIELD</code>.
    * 
    * @jmx.managed-attribute
    */
   public void setDefaultReplicationFieldBatchMode(boolean replicationFieldBatchMode)
   {
      this.replicationFieldBatchMode = replicationFieldBatchMode;
   }

   /**
    * Gets when sessions are replicated to the other nodes.
    * The default value, "instant", synchronously replicates changes
    * to the other nodes. In this case, the "SnapshotInterval" attribute
    * is not used.
    * The "interval" mode, in association with the "SnapshotInterval"
    * attribute, indicates that Tomcat will only replicate modified
    * sessions every "SnapshotInterval" miliseconds at most.
    * 
    * @see #getSnapshotInterval()
    * 
    * @jmx.managed-attribute
    */
   public String getSnapshotMode()
   {
      return snapshotMode;
   }

   /**
    * Sets when sessions are replicated to the other nodes. Valid values are:
    * <ul>
    * <li>instant</li>
    * <li>interval</li> 
    * </ul>
    * 
    * @jmx.managed-attribute
    */
   public void setSnapshotMode(String snapshotMode)
   {
      this.snapshotMode = snapshotMode;
   }

   /**
    * Gets how often session changes should be replicated to other nodes.
    * Only relevant if property {@link #getSnapshotMode() snapshotMode} is 
    * set to <code>interval</code>.
    * 
    * @return the number of milliseconds between session replications.
    * 
    * @jmx.managed-attribute
    */
   public int getSnapshotInterval()
   {
      return snapshotInterval;
   }

   /**
    * Sets how often session changes should be replicated to other nodes.
    * 
    * @param snapshotInterval the number of milliseconds between 
    *                         session replications.
    * @jmx.managed-attribute
    */
   public void setSnapshotInterval(int snapshotInterval)
   {
      this.snapshotInterval = snapshotInterval;
   }
   
   // ----------------------------------------------------------------  Cluster

   /**
    * Gets the name of the <code>TreeCache</code>'s JGroups channel.
    * 
    * @see org.apache.catalina.Cluster#getClusterName()
    */
   public String getClusterName()
   {
      return clusterName;
   }
   
   /* (non-javadoc)
    * @see org.apache.catalina.Cluster#getContainer()
    */
   public Container getContainer()
   {
      return container;
   }
   
   /* (non-javadoc)
    * @see org.apache.catalina.Cluster#setContainer()
    */
   public void setContainer(Container container)
   {
      this.container = container;
   }

   /**
    * @see org.apache.catalina.Cluster#getInfo()
    * 
    * @jmx.managed-attribute access="read-only"
    */
   public String getInfo()
   {
      return info;
   }

   /**
    * @see org.apache.catalina.Cluster#createManager(java.lang.String)
    */
   public Manager createManager(String name)
   {
      if (log.isDebugEnabled())
         log.debug("Creating ClusterManager for context " + name
               + " using class " + getManagerClassName());
      Manager manager = null;
      try
      {
         manager = (Manager) getClass().getClassLoader().loadClass(
               getManagerClassName()).newInstance();
      } 
      catch (Exception x)
      {
         log.error("Unable to load class for replication manager", x);
         manager = new JBossCacheManager();
      } 
      finally
      {
         manager.setDistributable(true);
      }
      
      if (manager instanceof JBossCacheManager)
      {
         configureManager((JBossCacheManager) manager);
      }
      
      return manager;
   }

   /**
    * Does nothing; tracking the status of other members of the cluster is
    * provided by the JGroups layer. 
    * 
    * @see org.apache.catalina.Cluster#backgroundProcess()
    */
   public void backgroundProcess()
   {
      ; // no-op
   }

   // ---------------------------------------------  Deprecated Cluster Methods
   
   /**
    * Returns <code>null</code>; method is deprecated.
    * 
    * @return <code>null</code>, always.
    * 
    * @see org.apache.catalina.Cluster#getProtocol()
    */
   public String getProtocol()
   {
      return null;
   }
   
   /**
    * Does nothing; method is deprecated.
    * 
    * @see org.apache.catalina.Cluster#setProtocol(java.lang.String)
    */
   public void setProtocol(String protocol)
   {
      ; // no-op
   }
   
   /**
    * Does nothing; method is deprecated.
    * 
    * @see org.apache.catalina.Cluster#startContext(java.lang.String)
    */
   public void startContext(String contextPath) throws IOException
   {
      ; // no-op
   }
   
   /**
    * Does nothing; method is deprecated.
    * 
    * @see org.apache.catalina.Cluster#installContext(java.lang.String, java.net.URL)
    */
   public void installContext(String contextPath, URL war)
   {
      ; // no-op
   }
   
   /**
    * Does nothing; method is deprecated.
    * 
    * @see org.apache.catalina.Cluster#stop(java.lang.String)
    */
   public void stop(String contextPath) throws IOException 
   {
      ; // no-op
   }

   // ---------------------------------------------------------  Public Methods
   
   /**
    * Sets the cluster-wide properties of a <code>Manager</code> to
    * match those of this cluster.  Does not override 
    * <code>Manager</code>-specific properties with cluster-wide defaults 
    * if the <code>Manager</code>-specfic properties have already been set.
    */
   public void configureManager(JBossCacheManager manager)
   {
      manager.setSnapshotMode(snapshotMode);
      manager.setSnapshotInterval(snapshotInterval);
      manager.setUseJK(useJK);
      manager.setUseLocalCache(useLocalCache);
      manager.setCacheObjectNameString(treeCacheObjectName);
      
      // Only set replication attributes if they were not
      // already set via a <Manager> element in an XML config file
      
      if (manager.getReplicationGranularityString() == null) 
      {
         manager.setReplicationGranularityString(defaultReplicationGranularity);
      }
      
      if (manager.getReplicationTriggerString() == null) 
      {
         manager.setReplicationTriggerString(defaultReplicationTrigger);
      }
      
      if (manager.isReplicationFieldBatchMode() == null)
      {
         manager.setReplicationFieldBatchMode(replicationFieldBatchMode);
      }
   }

   // ---------------------------------------------------------------  Lifecyle

   /**
    * Finds or creates a {@link TreeCache}; if created, starts the 
    * cache and registers it with our JMX server.
    * 
    * @see org.apache.catalina.Lifecycle#start()
    */
   public void start() throws LifecycleException
   {
      if (started)
      {
         throw new LifecycleException("Cluster already started");
      }

      // Notify our interested LifecycleListeners
      lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, this);
      
      try
      {         
         // Tell the JBoss MBeanServerLocator utility 
         // that Tomcat's MBean server is 'jboss'
         MBeanServerLocator.setJBoss(getMBeanServer());
         
         // Initialize the tree cache
         PojoCacheMBean cache = getTreeCache();
         
         if (treeCacheLocal)
         {
            cache.createService();
            cache.startService();
         }

         registerMBeans();

         started = true;
         
         // Notify our interested LifecycleListeners
         lifecycle.fireLifecycleEvent(AFTER_START_EVENT, this);

      }
      catch (LifecycleException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         log.error("Unable to start cluster.", e);
         throw new LifecycleException(e);
      } 
   }

   /**
    * If this object created its own {@link TreeCache}, stops it 
    * and unregisters it with JMX.
    * 
    * @see org.apache.catalina.Lifecycle#stop()
    */
   public void stop() throws LifecycleException
   {
      if (!started)
      {
         throw new IllegalStateException("Cluster not started");
      }
      
      // Notify our interested LifecycleListeners
      lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, this);
      
      if (treeCacheLocal)
      {
         treeCache.stopService();
         treeCache.destroyService();
      }

      started = false;
      // Notify our interested LifecycleListeners
      lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, this);

      unregisterMBeans();
   }

   /* (non-javadoc)           
    * @see org.apache.catalina.Lifecycle#addLifecycleListener()
    */
   public void addLifecycleListener(LifecycleListener listener)
   {
      lifecycle.addLifecycleListener(listener);
   }

   /* (non-javadoc)           
    * @see org.apache.catalina.Lifecycle#findLifecycleListeners()
    */
   public LifecycleListener[] findLifecycleListeners()
   {
      return lifecycle.findLifecycleListeners();
   }

   /* (non-javadoc)           
    * @see org.apache.catalina.Lifecycle#removeLifecycleListener()
    */
   public void removeLifecycleListener(LifecycleListener listener)
   {
      lifecycle.removeLifecycleListener(listener);
   }
   
   // -------------------------------------------------------- Private Methods
 
   /**
    * Gets our TreeCache, either from a local reference or the JMX
    * server.  If one is not found, creates and configures it.
    */
   private PojoCacheMBean getTreeCache() throws Exception
   {
      if (treeCache == null) {
         
         MBeanServer server = getMBeanServer();
         ObjectName objName = new ObjectName(treeCacheObjectName);
         if (server.isRegistered(objName))
         {
            // Get a proxy to the existing TreeCache
            treeCache = (PojoCacheMBean) 
                  MBeanProxyExt.create(PojoCacheMBean.class, objName);
         }
         else
         {
            // Create our own tree cache
            treeCache = new PojoCache();
            
            // See if there is an XML descriptor file to configure the cache
            InputStream configIS = getCacheConfigStream();
            
            if (configIS != null)
            {
               PropertyConfigurator config = new PropertyConfigurator();
               config.configure(treeCache, configIS);
               try 
               {
                  configIS.close();
               }
               catch (IOException io)
               {
                  // ignore
               }
               
               if (clusterName != null)
               {
                  // Override the XML config with the name provided in
                  // server.xml.  Method setClusterName is specified in the
                  // Cluster interface, otherwise we would not do this
                  treeCache.setClusterName(clusterName);
               }
            }
            else
            {
               // User did not try to configure the cache.
               // Configure it using defaults.  Only exception
               // is the clusterName, which user can specify in server.xml.
               String channelName = (clusterName == null) ? DEFAULT_CLUSTER_NAME
                                                          : clusterName;
               treeCache.setClusterName(channelName);
               treeCache.setIsolationLevel(DEFAULT_ISOLATION_LEVEL);
               treeCache.setCacheMode(DEFAULT_CACHE_MODE);
               treeCache.setLockAcquisitionTimeout(DEFAULT_LOCK_TIMEOUT);
               treeCache.setTransactionManagerLookupClass(DEFAULT_TM_LOOKUP);
            }
            
            treeCacheLocal = true;
         }
      }
      return treeCache;
   }   
   
   

   private InputStream getCacheConfigStream() throws FileNotFoundException
   {
      boolean useDefault = (this.cacheConfigPath == null);
      String path = (useDefault) ? DEFAULT_CACHE_CONFIG_PATH : cacheConfigPath;
      // See if clusterProperties points to a file relative
      // to $CATALINA_BASE
      File file = new File(path);
      if (!file.isAbsolute())
      {
         file = new File(System.getProperty("catalina.base"), path);
      }
      
      try
      {
         return new FileInputStream(file);
      }
      catch (FileNotFoundException fnf)
      {
         if (useDefault)
         {
            // Not a problem, just means user did not try to
            // configure the cache.  Return null and let the cache
            // be configured from defaults.
            return null;
         }
         else
         {
            // User provided config was invalid; throw the exception
            log.error("No tree cache config file found at " + 
                      file.getAbsolutePath());
            throw fnf;
         }
      }      
   }

   /**
    * Registers this object and the tree cache (if we created it) with JMX.
    */
   private void registerMBeans()
   {
      try
      {
         MBeanServer server = getMBeanServer();
         
         String domain;
         if (container instanceof ContainerBase)
         {
            domain = ((ContainerBase) container).getDomain();
         }
         else
         {
            domain = server.getDefaultDomain();
         }
         
         String name = ":type=Cluster";
         if (container instanceof Host) {
            name += ",host=" + container.getName();
         }
         else if (container instanceof Engine)
         {            
            name += ",engine=" + container.getName();
         }
         
         ObjectName clusterName = new ObjectName(domain + name);

         if (server.isRegistered(clusterName))
         {
            log.warn("MBean " + clusterName + " already registered");
         }
         else
         {
            this.objectName = clusterName;
            server.registerMBean(this, objectName);
         }

         if (treeCacheLocal)
         {
            // Register the treeCache
            ObjectName treeCacheName = new ObjectName(treeCacheObjectName);
            server.registerMBean(getTreeCache(), treeCacheName);
         }

      }
      catch (Exception ex)
      {
         log.error(ex.getMessage(), ex);
      }
   }   

   /**
    * Unregisters this object and the tree cache (if we created it) with JMX.
    */
   private void unregisterMBeans()
   {
      if (mserver != null)
      {
         try
         {
            if (objectName != null) {
               mserver.unregisterMBean(objectName);
            }
            if (treeCacheLocal)
            {
               mserver.unregisterMBean(new ObjectName(treeCacheObjectName));
            }
         }
         catch (Exception e)
         {
            log.error(e);
         }
      }
   }

   /**
    * Get the current Catalina MBean Server.
    * 
    * @return
    * @throws Exception
    */
   private MBeanServer getMBeanServer() throws Exception
   {
      if (mserver == null)
      {
         ArrayList servers = MBeanServerFactory.findMBeanServer(null);
         if (servers.size() > 0)
         {
            mserver = (MBeanServer) servers.get(0);
         }
         else
         {
            mserver = MBeanServerFactory.createMBeanServer();
         }
      }
      return mserver;
   }

}
