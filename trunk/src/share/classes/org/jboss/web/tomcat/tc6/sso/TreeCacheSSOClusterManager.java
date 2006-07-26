/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

import java.io.Serializable;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.util.LifecycleSupport;
import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.TreeCacheListener;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.util.NestedRuntimeException;
import org.jboss.web.tomcat.tc6.Tomcat6;
import org.jgroups.View;

/**
 * An implementation of SSOClusterManager that uses a TreeCache
 * to share SSO information between cluster nodes.
 *
 * @author Brian E. Stansberry
 * @version $Revision: 1.1 $ $Date: 2006/06/21 19:39:00 $
 */
public final class TreeCacheSSOClusterManager
   implements SSOClusterManager, TreeCacheListener
{
   // -------------------------------------------------------------  Constants

   /**
    * Final segment of any FQN that names a TreeCache node storing
    * SSO credential information.
    */
   private static final String CREDENTIALS = "credentials";

   /**
    * First segment of any FQN that names a TreeCache node associated
    * with an SSO
    */
   private static final String SSO = "SSO";

   /**
    * Final segment of any FQN that names a TreeCache node storing
    * the set of Sessions associated with an SSO.
    */
   private static final String SESSIONS = "sessions";

   /**
    * Key under which data is stored to the TreeCache.
    */
   private static final String KEY = "key";

   /**
    * Default global value for the cacheName property
    */
   public static final String DEFAULT_GLOBAL_CACHE_NAME =
      Tomcat6.DEFAULT_CACHE_NAME;

   /**
    * Parameter signature used for TreeCache.get calls over JMX
    */
   private static final String[] GET_SIGNATURE =
      {Fqn.class.getName(), Object.class.getName()};

   /**
    * Parameter signature used for TreeCache.put calls over JMX
    */
   private static final String[] PUT_SIGNATURE =
      {Fqn.class.getName(), Object.class.getName(), Object.class.getName()};

   /**
    * Parameter signature used for TreeCache.remove calls over JMX
    */
   private static final String[] REMOVE_SIGNATURE = {Fqn.class.getName()};
   
   // -------------------------------------------------------  Instance Fields
   
   /**
    * SSO id which the thread is currently storing to the cache
    */
   private ThreadLocal beingLocallyAdded = new ThreadLocal();

   /**
    * SSO id which a thread is currently removing from the cache
    */
   private ThreadLocal beingLocallyRemoved = new ThreadLocal();

   /**
    * SSO id which the thread is deregistering due to removal on another node
    */
   private ThreadLocal beingRemotelyRemoved = new ThreadLocal();

   /**
    * ObjectName of the TreeCache
    */
   private ObjectName cacheObjectName = null;

   /**
    * String version of the object name to use to access the TreeCache
    */
   private String cacheName = null;

   /**
    * Transaction Manager
    */
   private TransactionManager tm = null;
   
   /**
    * The lifecycle event support for this component.
    */
   private LifecycleSupport lifecycle = new LifecycleSupport(this);

   /**
    * The Log-object for this class
    */
   private Logger log = Logger.getLogger(getClass().getName());;

   /**
    * Whether we are registered as a TreeCacheListener anywhere
    */
   private boolean registeredAsListener = false;

   /**
    * The MBean server we use to access our TreeCache
    */
   private MBeanServer server = null;

   /**
    * The SingleSignOn for which we are providing cluster support
    */
   private ClusteredSingleSignOn ssoValve = null;

   /**
    * Whether we have been started
    */
   private boolean started = false;

   /**
    * Whether a valid TreeCache is available for use
    */
   private boolean treeCacheAvailable = false;

   /**
    * Whether we have logged an error due to not having a valid cache
    */
   private boolean missingCacheErrorLogged = false;
   
   /**
    * Our node's address in the cluster.
    */
   private Serializable localAddress = null;
   
   // ----------------------------------------------------------  Constructors

   
   /**
    * Creates a new TreeCacheSSOClusterManager
    */
   public TreeCacheSSOClusterManager()
   {
      // Find our MBeanServer
      server = MBeanServerLocator.locateJBoss();
      if (server == null)
         server = MBeanServerLocator.locate();
   }

   
   /**
    * Creates a new TreeCacheSSOClusterManager that works with the given
    * MBeanServer.  This constructor is only intended for use in unit testing.
    */
   public TreeCacheSSOClusterManager(MBeanServer server)
   {
      this.server = server;
   }
   
   
   // ------------------------------------------------------------  Properties
  
   public String getCacheName()
   {
      return cacheName;
   }

   public void setCacheName(String objectName)
      throws Exception
   {
      if (objectName == null)
      {
         setCacheObjectName(null);
      }
      else if (objectName.equals(cacheName) == false)
      {
         setCacheObjectName(new ObjectName(objectName));
      }
   }

   public ObjectName getCacheObjectName()
   {
      return cacheObjectName;
   }

   public void setCacheObjectName(ObjectName objectName)
      throws Exception
   {
      // If no change, do nothing
      if ((objectName != null && objectName.equals(cacheObjectName))
         || (cacheObjectName != null && cacheObjectName.equals(objectName))
         || (objectName == null && cacheObjectName == null))
      {
         return;
      }

      removeAsTreeCacheListener();
      this.tm = null;
      
      this.cacheObjectName = objectName;
      this.cacheName = (objectName == null
         ? null
         : objectName.getCanonicalName());

      if (false == isTreeCacheAvailable(true))
      {
         if (started)
         {
            logMissingCacheError();
         }
         else
         {
            // Just put an advice in the log
            log.info("Cannot find TreeCache using " + cacheName + " -- tree" +
               "CacheName must be set to point to a running TreeCache " +
               "before ClusteredSingleSignOn can handle requests");
         }
      }
   }
   
   // -----------------------------------------------------  SSOClusterManager

   /**
    * Notify the cluster of the addition of a Session to an SSO session.
    *
    * @param ssoId   the id of the SSO session
    * @param session the Session that has been added
    */
   public void addSession(String ssoId, Session session)
   {
      if (ssoId == null || session == null)
      {
         return;
      }

      if (!checkTreeCacheAvailable())
      {
         return;
      }

      if (log.isTraceEnabled())
      {
         log.trace("addSession(): adding Session " + session.getId() +
            " to cached session set for SSO " + ssoId);
      }

      Fqn fqn = getSessionsFqn(ssoId);
      boolean doTx = false;
      try
      {
         // Confirm we have a transaction manager; if not get it from TreeCache
         // failure to find will throw an IllegalStateException
         if (tm == null)
            configureFromCache();
         
         // Don't do anything if there is already a transaction 
         // context associated with this thread.
         if(tm.getTransaction() == null)
            doTx = true;

         if(doTx)
            tm.begin();
         
         Set sessions = getSessionSet(fqn, true);
         sessions.add(new SessionAddress(session.getId(), localAddress));
         putInTreeCache(fqn, sessions);
      }
      catch (Exception e)
      {
         try
         {
            if(doTx)
               tm.setRollbackOnly();
         }
         catch (Exception ignored)
         {
         }
         String sessId = (session == null ? "NULL" : session.getId());
         log.error("caught exception adding session " + sessId +
            " to SSO id " + ssoId, e);
      }
      finally
      {
         if (doTx)
            endTransaction();
      }
   }


   /**
    * Gets the SingleSignOn valve for which this object is handling
    * cluster communications.
    *
    * @return the <code>SingleSignOn</code> valve.
    */
   public ClusteredSingleSignOn getSingleSignOnValve()
   {
      return ssoValve;
   }


   /**
    * Sets the SingleSignOn valve for which this object is handling
    * cluster communications.
    * <p><b>NOTE:</b> This method must be called before calls can be
    * made to the other methods of this interface.
    *
    * @param valve a <code>SingleSignOn</code> valve.
    */
   public void setSingleSignOnValve(ClusteredSingleSignOn valve)
   {
      ssoValve = valve;
   }


   /**
    * Notifies the cluster that a single sign on session has been terminated
    * due to a user logout.
    *
    * @param ssoId
    */
   public void logout(String ssoId)
   {
      if (!checkTreeCacheAvailable())
      {
         return;
      }
      
      // Check whether we are already handling this removal 
      if (ssoId.equals(beingLocallyRemoved.get()))
      {
         return;
      }         
      
      // Add this SSO to our list of in-process local removals so
      // this.nodeRemoved() will ignore the removal
      beingLocallyRemoved.set(ssoId);

      if (log.isTraceEnabled())
      {
         log.trace("Registering logout of SSO " + ssoId +
            " in clustered cache");
      }

      Fqn fqn = getSingleSignOnFqn(ssoId);
      
      try
      {
         removeFromTreeCache(fqn);
      }
      catch (Exception e)
      {
         log.error("Exception attempting to remove node " +
            fqn.toString() + " from TreeCache", e);
      }
      finally
      {
         beingLocallyRemoved.set(null);
      }
   }


   /**
    * Queries the cluster for the existence of an SSO session with the given
    * id, returning a <code>SingleSignOnEntry</code> if one is found.
    *
    * @param ssoId the id of the SSO session
    * @return a <code>SingleSignOnEntry</code> created using information
    *         found on another cluster node, or <code>null</code> if no
    *         entry could be found.
    */
   public SingleSignOnEntry lookup(String ssoId)
   {
      if (!checkTreeCacheAvailable())
      {
         return null;
      }

      SingleSignOnEntry entry = null;
      // Find the latest credential info from the cluster
      Fqn fqn = getCredentialsFqn(ssoId);
      try
      {
         SSOCredentials data = (SSOCredentials) getFromTreeCache(fqn);
         if (data != null)
         {
            entry = new SingleSignOnEntry(null,
               data.getAuthType(),
               data.getUsername(),
               data.getPassword());
         }
      }
      catch (Exception e)
      {
         log.error("caught exception looking up SSOCredentials for SSO id " +
            ssoId, e);
      }
      return entry;
   }


   /**
    * Notifies the cluster of the creation of a new SSO entry.
    *
    * @param ssoId    the id of the SSO session
    * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST
    *                 or FORM) used to authenticate the SSO.
    * @param username the username (if any) used for the authentication
    * @param password the password (if any) used for the authentication
    */
   public void register(String ssoId, String authType,
      String username, String password)
   {
      if (!checkTreeCacheAvailable())
      {
         return;
      }

      if (log.isTraceEnabled())
      {
         log.trace("Registering SSO " + ssoId + " in clustered cache");
      }

      storeSSOData(ssoId, authType, username, password);
   }


   /**
    * Notify the cluster of the removal of a Session from an SSO session.
    *
    * @param ssoId   the id of the SSO session
    * @param session the Session that has been removed
    */
   public void removeSession(String ssoId, Session session)
   {
      if (ssoId == null || session == null)
      {
         return;
      }
      
      if (!checkTreeCacheAvailable())
      {
         return;
      }
      
      // Check that this session removal is not due to our own deregistration
      // of an SSO following receipt of a nodeRemoved() call
      if (ssoId.equals(beingRemotelyRemoved.get()))
      {
         return;
      }

      if (log.isTraceEnabled())
      {
         log.trace("removeSession(): removing Session " + session.getId() +
            " from cached session set for SSO " + ssoId);
      }

      Fqn fqn = getSessionsFqn(ssoId);
      boolean doTx = false;
      boolean removing = false;
      try
      {
         // Confirm we have a transaction manager; if not get it from TreeCache
         // failure to find will throw an IllegalStateException
         if (tm == null)
            configureFromCache();

         // Don't do anything if there is already a transaction 
         // context associated with this thread.
         if(tm.getTransaction() == null)
            doTx = true;

         if(doTx)
            tm.begin();
         
         Set sessions = getSessionSet(fqn, false);
         if (sessions != null)
         {
            sessions.remove(new SessionAddress(session.getId(), localAddress));
            if (sessions.size() == 0)
            {               
               // No sessions left; remove node
               
               // Add this SSO to our list of in-process local removals so
               // this.nodeRemoved() will ignore the removal
               removing = true;
               beingLocallyRemoved.set(ssoId);
               removeFromTreeCache(getSingleSignOnFqn(ssoId));
            }
            else
            {
               putInTreeCache(fqn, sessions);
            }
         }
      }
      catch (Exception e)
      {
         try
         {
            if(doTx)
               tm.setRollbackOnly();
         }
         catch (Exception x)
         {
         }
         
         String sessId = (session == null ? "NULL" : session.getId());
         log.error("caught exception removing session " + sessId +
            " from SSO id " + ssoId, e);
      }
      finally
      {
         try
         {
            if (removing)
            {
               beingLocallyRemoved.set(null);
            }
         }
         finally
         {
            if (doTx)
               endTransaction();
         }
      }
   }


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
   public void updateCredentials(String ssoId, String authType,
      String username, String password)
   {
      if (!checkTreeCacheAvailable())
      {
         return;
      }

      if (log.isTraceEnabled())
      {
         log.trace("Updating credentials for SSO " + ssoId +
            " in clustered cache");
      }

      storeSSOData(ssoId, authType, username, password);
   }

   
   // ------------------------------------------------------  TreeCacheListener

   /**
    * Does nothing
    */
   public void nodeCreated(Fqn fqn)
   {
      ; // do nothing
   }

   /**
    * Does nothing
    */
   public void nodeLoaded(Fqn fqn)
   {
      ; // do nothing
   }


   /**
    * Does nothing
    */
   public void nodeVisited(Fqn fqn)
   {
      ; // do nothing
   }


   /**
    * Does nothing
    */
   public void cacheStarted(TreeCache cache)
   {
      ; // do nothing
   }


   /**
    * Does nothing
    */
   public void cacheStopped(TreeCache cache)
   {
      ; // do nothing
   }


   /**
    * Extracts an SSO session id from the Fqn and uses it in an invocation of
    * {@link ClusteredSingleSignOn#deregister(String) ClusteredSingleSignOn.deregister(String)}.
    * <p/>
    * Ignores invocations resulting from TreeCache changes originated by
    * this object.
    *
    * @param fqn the fully-qualified name of the node that was removed
    */
   public void nodeRemoved(Fqn fqn)
   {
      String ssoId = getIdFromFqn(fqn);
      
      if (ssoId == null)
         return;

      // Ignore messages generated by our own activity
      if (ssoId.equals(beingLocallyRemoved.get()))
      {
         return;
      }
      
      beingRemotelyRemoved.set(ssoId);

      try
      {
         if (log.isTraceEnabled())
         {
            log.trace("received a node removed message for SSO " + ssoId);
         }

         ssoValve.deregister(ssoId);
      }
      finally
      {
         beingRemotelyRemoved.set(null);
      }

   }


   /**
    * Extracts an SSO session id from the Fqn and uses it in an invocation of
    * {@link ClusteredSingleSignOn#update ClusteredSingleSignOn.update()}.
    * <p/>
    * Only responds to modifications of nodes whose FQN's final segment is
    * "credentials".
    * <p/>
    * Ignores invocations resulting from TreeCache changes originated by
    * this object.
    * <p/>
    * Ignores invocations for SSO session id's that are not registered
    * with the local SingleSignOn valve.
    *
    * @param fqn the fully-qualified name of the node that was modified
    */
   public void nodeModified(Fqn fqn)
   {
      // We are only interested in changes to the CREDENTIALS node
      if (CREDENTIALS.equals(getTypeFromFqn(fqn)) == false)
      {
         return;
      }

      String ssoId = getIdFromFqn(fqn); // won't be null or above check fails

      // Ignore invocations that come as a result of our additions
      if (ssoId.equals(beingLocallyAdded.get()))
      {
         return;
      }

      SingleSignOnEntry sso = ssoValve.localLookup(ssoId);
      if (sso == null || sso.getCanReauthenticate())
      {
         // No reason to update
         return;
      }

      if (log.isTraceEnabled())
      {
         log.trace("received a credentials modified message for SSO " + ssoId);
      }

      // Put this SSO in the queue of those to be updated
//      credentialUpdater.enqueue(sso, ssoId);
      try
      {
         SSOCredentials data = (SSOCredentials) getFromTreeCache(fqn);
         if (data != null)
         {
            // We want to release our read lock quickly, so get the needed
            // data from the cache, commit the tx, and then use the data
            String authType = data.getAuthType();
            String username = data.getUsername();
            String password = data.getPassword();

            if (log.isTraceEnabled())
            {
               log.trace("CredentialUpdater: Updating credentials for SSO " + sso);
            }

            synchronized (sso)
            {
               // Use the existing principal
               Principal p = sso.getPrincipal();
               sso.updateCredentials(p, authType, username, password);
            }
         }
      }
      catch (Exception e)
      {
         log.error("failed to update credentials for SSO " + ssoId, e);
      }
   }


   /**
    * Does nothing
    */
   public void viewChange(View new_view)
   {
      ; // do nothing
   }


   /**
    * Does nothing. Called when a node is evicted (not the same as remove()).
    *
    * @param fqn
    */
   public void nodeEvicted(Fqn fqn)
   {
      // TODO do we need to handle this?
      ; // do nothing
   }

   
   // -------------------------------------------------------------  Lifecycle


   /**
    * Add a lifecycle event listener to this component.
    *
    * @param listener The listener to add
    */
   public void addLifecycleListener(LifecycleListener listener)
   {
      lifecycle.addLifecycleListener(listener);
   }


   /**
    * Get the lifecycle listeners associated with this lifecycle. If this
    * Lifecycle has no listeners registered, a zero-length array is returned.
    */
   public LifecycleListener[] findLifecycleListeners()
   {
      return lifecycle.findLifecycleListeners();
   }


   /**
    * Remove a lifecycle event listener from this component.
    *
    * @param listener The listener to remove
    */
   public void removeLifecycleListener(LifecycleListener listener)
   {
      lifecycle.removeLifecycleListener(listener);
   }

   /**
    * Prepare for the beginning of active use of the public methods of this
    * component.  This method should be called before any of the public
    * methods of this component are utilized.  It should also send a
    * LifecycleEvent of type START_EVENT to any registered listeners.
    *
    * @throws LifecycleException if this component detects a fatal error
    *                            that prevents this component from being used
    */
   public void start() throws LifecycleException
   {
      // Validate and update our current component state
      if (started)
      {
         throw new LifecycleException
            ("TreeCacheSSOClusterManager already Started");
      }

      try 
      {
         if (isTreeCacheAvailable(true))
         {
            integrateWithCache();
         }
      }
      catch (Exception e)
      {
         throw new LifecycleException("Caught exception looking up " +
                                      "TransactionManager from TreeCache", e);
      }
      
      started = true;

      // Notify our interested LifecycleListeners
      lifecycle.fireLifecycleEvent(START_EVENT, null);
   }


   /**
    * Gracefully terminate the active use of the public methods of this
    * component.  This method should be the last one called on a given
    * instance of this component.  It should also send a LifecycleEvent
    * of type STOP_EVENT to any registered listeners.
    *
    * @throws LifecycleException if this component detects a fatal error
    *                            that needs to be reported
    */
   public void stop() throws LifecycleException
   {
      // Validate and update our current component state
      if (!started)
      {
         throw new LifecycleException
            ("TreeCacheSSOClusterManager not Started");
      }
      
      started = false;

      // Notify our interested LifecycleListeners
      lifecycle.fireLifecycleEvent(STOP_EVENT, null);
   }

   
   // -------------------------------------------------------  Private Methods

   private Object getFromTreeCache(Fqn fqn) throws Exception
   {
      Object[] args = new Object[]{fqn, KEY};
      return server.invoke(getCacheObjectName(), "get", args, GET_SIGNATURE);
   }

   private Fqn getCredentialsFqn(String ssoid)
   {
      Object[] objs = new Object[]{SSO, ssoid, CREDENTIALS};
      return new Fqn(objs);
   }

   private Fqn getSessionsFqn(String ssoid)
   {
      Object[] objs = new Object[]{SSO, ssoid, SESSIONS};
      return new Fqn(objs);
   }

   private Fqn getSingleSignOnFqn(String ssoid)
   {
      Object[] objs = new Object[]{SSO, ssoid};
      return new Fqn(objs);
   }

   /**
    * Extracts an SSO session id from a fully qualified name object.
    *
    * @param fqn the Fully Qualified Name used by TreeCache
    * @return the second element in the Fqn -- the SSO session id
    */
   private String getIdFromFqn(Fqn fqn)
   {
      String id = null;
      if (fqn.size() > 1 && SSO.equals(fqn.get(0)))
      {
         id = (String) fqn.get(1);
      }
      return id;
   }

   private Set getSessionSet(Fqn fqn, boolean create)
      throws Exception
   {
      Set sessions = (Set) getFromTreeCache(fqn);
      if (create && sessions == null)
      {
         sessions = new HashSet();
      }
      return sessions;
   }

   /**
    * Extracts the SSO tree cache node type from a fully qualified name
    * object.
    *
    * @param fqn the Fully Qualified Name used by TreeCache
    * @return the 3rd in the Fqn -- either
    *         {@link #CREDENTIALS CREDENTIALS} or {@link #SESSIONS SESSIONS},
    *         or <code>null</code> if <code>fqn</code> is not for an SSO.
    */
   private String getTypeFromFqn(Fqn fqn)
   {
      String type = null;
      if (fqn.size() > 2 && SSO.equals(fqn.get(0)))
         type = (String) fqn.get(2);
      return type;
   }
   
   /**
    * Obtains needed configuration information from the tree cache.
    * Invokes "getTransactionManager" on the tree cache, caching the
    * result or throwing an IllegalStateException if one is not found.
    * Also get our cluster-wide unique local address from the cache.
    * 
    * @throws Exception
    */
   private void configureFromCache() throws Exception
   {  
      tm = (TransactionManager) server.getAttribute(getCacheObjectName(), 
                                                    "TransactionManager");

      if (tm == null) 
      {
         throw new IllegalStateException("TreeCache does not have a " +
                                         "transaction manager; please " +
                                         "configure a valid " +
                                         "TransactionManagerLookupClass");
      }
      
      // Find out our address
      Object address = server.getAttribute(cacheObjectName, "LocalAddress");
      // In reality this is a JGroups IpAddress, but the API says
      // "Object" so we have to be sure its Serializable
      if (address instanceof Serializable)
         localAddress = (Serializable) address;
      else
         localAddress = address.toString();
   }
   

   private void endTransaction()
   {
      try 
      {
         if(tm.getTransaction().getStatus() != Status.STATUS_MARKED_ROLLBACK)
         {
            tm.commit();
         } 
         else
         {
            tm.rollback();
         }
      } 
      catch (Exception e) 
      {
         log.error(e);
         throw new NestedRuntimeException("TreeCacheSSOClusterManager.endTransaction(): ", e);
      }
   }

   /**
    * Checks whether an MBean is registered under the value of property
    * "cacheObjectName".
    *
    * @param forceCheck check for availability whether or not it has already
    *                   been positively established
    * @return <code>true</code> if property <code>cacheName</code> has been
    *         set and points to a registered MBean.
    */
   private synchronized boolean isTreeCacheAvailable(boolean forceCheck)
   {
      if (forceCheck || treeCacheAvailable == false)
      {
         boolean available = (cacheObjectName != null);
         if (available)
         {
            Set s = server.queryMBeans(cacheObjectName, null);
            available = s.size() > 0;
            if (available)
            {
               try
               {
                  // If Tomcat6 overrides the default cache name, it will do so
                  // after we are started. So we need to configure ourself here
                  // and throw an exception if there is a problem. Having this
                  // here also allows us to recover if our cache is started
                  // after we are
                  if (started)
                     integrateWithCache();
                  setMissingCacheErrorLogged(false);
               }
               catch (Exception e)
               {
                  log.error("Caught exception configuring from cache " +
                            cacheObjectName, e);
                  available = false;
               }
            }
         }
         treeCacheAvailable = available;
      }
      return treeCacheAvailable;
   }
   
   private boolean checkTreeCacheAvailable()
   {
      boolean avail = isTreeCacheAvailable(false);
      if (!avail)
         logMissingCacheError();
      return avail;
   }

   private void putInTreeCache(Fqn fqn, Object data) throws Exception
   {
      Object[] args = new Object[]{fqn, KEY, data};
      server.invoke(getCacheObjectName(), "put", args, PUT_SIGNATURE);
   }

   private void integrateWithCache() throws Exception
   {
      // Ensure we have a transaction manager and a cluster-wide unique address
      configureFromCache();
      
      // If the SSO region is inactive, activate it
      activateCacheRegion();
      
      registerAsTreeCacheListener();
      
      log.debug("Successfully integrated with cache service " + cacheObjectName);
   }


   /**
    * If we are sharing a cache with HttpSession replication, the SSO
    * region may not be active, so here we ensure it is.
    * 
    * @throws Exception
    */
   private void activateCacheRegion() throws Exception
   {
      // NOTE: to be compatible with 1.2.3, no direct use of the 1.2.4
      // region activation API can be used here even if the rest of this
      // class is converted to using MBean proxies
      try
      {
         Boolean inactive = (Boolean) server.getAttribute(cacheObjectName, "InactiveOnStartup");
         if (inactive.booleanValue())
         {
            Boolean useMarshalling = (Boolean) server.getAttribute(cacheObjectName, "UseMarshalling");
            if (useMarshalling.booleanValue())
            {
               // TODO replace this try/catch with a call to an isRegionActive API
               try
               {
                  server.invoke(cacheObjectName, "activateRegion",
                                new Object[]{ "/" + SSO },
                                new String[]{ "java.lang.String"});
               }
               catch (MBeanException e)
               {
                  Exception cause = e.getTargetException();
                  if (cause != null 
                        && "org.jboss.cache.RegionNotEmptyException".equals(cause.getClass().getName()))
                  {
                     log.debug(SSO + " region already active", cause);
                  }
                  else
                     throw e;
               }
            }
         }
      }
      catch (AttributeNotFoundException ignore)
      {
         log.debug("Attribute InactiveOnStartup not available; " +
                   "must be using JBossCache 1.2.3 or earlier");
      }
   }

   /**
    * Invokes an operation on the JMX server to register ourself as a
    * listener on the TreeCache service.
    *
    * @throws Exception
    */
   private void registerAsTreeCacheListener() throws Exception
   {
      server.invoke(cacheObjectName, "addTreeCacheListener",
         new Object[]{this},
         new String[]{TreeCacheListener.class.getName()});
      registeredAsListener = true;
   }


   /**
    * Invokes an operation on the JMX server to register ourself as a
    * listener on the TreeCache service.
    *
    * @throws Exception
    */
   private void removeAsTreeCacheListener() throws Exception
   {
      if (registeredAsListener && cacheObjectName != null)
      {
         server.invoke(cacheObjectName, "removeTreeCacheListener",
            new Object[]{this},
            new String[]{TreeCacheListener.class.getName()});
      }
   }

   private void removeFromTreeCache(Fqn fqn) throws Exception
   {
      server.invoke(getCacheObjectName(), "remove",
         new Object[]{fqn},
         REMOVE_SIGNATURE);
   }

   /**
    * Stores the given data to the clustered cache in a tree branch whose FQN
    * is the given SSO id.  Stores the given credential data in a child node
    * named "credentials".  If parameter <code>storeSessions</code> is
    * <code>true</code>, also stores an empty HashSet in a sibling node
    * named "sessions".  This HashSet will later be used to hold session ids
    * associated with the SSO.
    * <p/>
    * Any items stored are stored under the key "key".
    *
    * @param ssoId    the id of the SSO session
    * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST
    *                 or FORM) used to authenticate the SSO.
    * @param username the username (if any) used for the authentication
    * @param password the password (if any) used for the authentication
    */
   private void storeSSOData(String ssoId, String authType, String username,
      String password)
   {
      SSOCredentials data = new SSOCredentials(authType, username, password);
      
      // Add this SSO to our list of in-process local adds so
      // this.nodeModified() will ignore the addition
      beingLocallyAdded.set(ssoId);
      
      try
      {
         putInTreeCache(getCredentialsFqn(ssoId), data);
      }
      catch (Exception e)
      {
         log.error("Exception attempting to add TreeCache nodes for SSO " +
            ssoId, e);
      }
      finally
      {
         beingLocallyAdded.set(null);
      }
   }

   private boolean isMissingCacheErrorLogged()
   {
      return missingCacheErrorLogged;
   }

   private void setMissingCacheErrorLogged(boolean missingCacheErrorLogged)
   {
      this.missingCacheErrorLogged = missingCacheErrorLogged;
   }

   private void logMissingCacheError()
   {
      StringBuffer msg = new StringBuffer("Cannot find TreeCache using ");
      msg.append(getCacheName());
      msg.append(" -- TreeCache must be started before ClusteredSingleSignOn ");
      msg.append("can handle requests");

      if (isMissingCacheErrorLogged())
      {
         // Just log it as a warning
         log.warn(msg);
      }
      else
      {
         log.error(msg);
         // Set a flag so we don't relog this error over and over
         setMissingCacheErrorLogged(true);
      }
   }

   // ---------------------------------------------------------  Outer Classes

   /**
    * Private class used to store authentication credentials in the TreeCache.
    * <p/>
    * For security, password accessor is private.
    */
   public static class SSOCredentials
      implements Serializable
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 5704877226920571663L;
      
      private String authType = null;
      private String password = null;
      private String username = null;

      /**
       * Creates a new SSOCredentials.
       *
       * @param authType The authorization method used to authorize the
       *                 SSO (BASIC, CLIENT-CERT, DIGEST, FORM or NONE).
       * @param username The username of the user associated with the SSO
       * @param password The password of the user associated with the SSO
       */
      private SSOCredentials(String authType, String username, String password)
      {
         this.authType = authType;
         this.username = username;
         this.password = password;
      }

      /**
       * Gets the username of the user associated with the SSO.
       *
       * @return the username
       */
      public String getUsername()
      {
         return username;
      }

      /**
       * Gets the authorization method used to authorize the SSO.
       *
       * @return "BASIC", "CLIENT-CERT", "DIGEST" or "FORM"
       */
      public String getAuthType()
      {
         return authType;
      }

      /**
       * Gets the password of the user associated with the SSO.
       *
       * @return the password, or <code>null</code> if the authorization
       *         type was DIGEST or CLIENT-CERT.
       */
      private String getPassword()
      {
         return password;
      }

   } // end SSOCredentials
   
   static class SessionAddress implements Serializable
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = -3702932999380140004L;
      
      Serializable address;
      String sessionId;
      
      SessionAddress(String sessionId, Serializable address)
      {
         this.sessionId = sessionId;
         this.address   = address;
      }

      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         
         if (!(obj instanceof SessionAddress))
            return false;
         
         SessionAddress other = (SessionAddress) obj;
         
         return (sessionId.equals(other.sessionId) 
                 && address.equals(other.address));
      }

      public int hashCode()
      {
         int total = (19 * 43) + sessionId.hashCode();
         return ((total * 43) + address.hashCode());
      }
      
      
   }

} // end TreeCacheSSOClusterManager

