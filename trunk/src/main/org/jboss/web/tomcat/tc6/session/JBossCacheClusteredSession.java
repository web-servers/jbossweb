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

package org.jboss.web.tomcat.tc6.session;

/**
 * Common superclass of ClusteredSession types that use JBossCache
 * as their distributed cache.
 * 
 * @author Brian Stansberry 
 * 
 * @version $Revision: 1.2 $
 */
public abstract class JBossCacheClusteredSession extends ClusteredSession
{
   /**
    * Our proxy to the cache.
    */
   protected transient JBossCacheService proxy_;
   
   /**
    * Create a new JBossCacheClusteredSession.
    * 
    * @param manager
    * @param useJK
    */
   public JBossCacheClusteredSession(JBossCacheManager manager)
   {
      super(manager, manager.getUseJK());
      establishProxy();
   }
   
   /**
    * Used only for externalization.
    */
   protected JBossCacheClusteredSession() 
   {
      super();
   }

   /**
    * Initialize fields marked as transient after loading this session
    * from the distributed store
    *
    * @param manager the manager for this session
    */
   public void initAfterLoad(AbstractJBossManager manager)
   {
      // Our manager and proxy may have been lost if we were recycled,
      // so reestablish them
      setManager(manager);
      establishProxy();

      // Since attribute map may be transient, we may need to populate it 
      // from the underlying store.
      populateAttributes();
      
      // Notify all attributes of type HttpSessionActivationListener (SRV 7.7.2)
      this.activate();
      
      // We are no longer outdated vis a vis distributed cache
      clearOutdated();
   }
   
   /**
    * Gets a reference to the JBossCacheService.
    */
   protected void establishProxy()
   {
      if (proxy_ == null)
      {
         proxy_ = ((JBossCacheManager) manager).getCacheService();

         // still null???
         if (proxy_ == null)
         {
            throw new RuntimeException("JBossCacheClusteredSession: Cache service is null.");
         }
      }
   }
   
   protected abstract void populateAttributes();

   /**
    * Override the superclass to additionally reset this class' fields.
    * <p>
    * <strong>NOTE:</strong> It is not anticipated that this method will be
    * called on a ClusteredSession, but we are overriding the method to be
    * thorough.
    * </p>
    */
   public void recycle()
   {
      super.recycle();
      
      proxy_ = null;
   }

   /**
    * Increment our version and place ourself in the cache.
    */
   public synchronized void processSessionRepl()
   {
      // Replicate the session.
      if (log.isDebugEnabled())
      {
         log.debug("processSessionRepl(): session is dirty. Will increment " +
                   "version from: " + getVersion() + " and replicate.");
      }
      this.incrementVersion();
      proxy_.putSession(realId, this);
      
      sessionAttributesDirty = false;
      sessionMetadataDirty = false;
      
      updateLastReplicated();
   }

   /**
    * Overrides the superclass impl by doing nothing if <code>localCall</code>
    * is <code>false</code>.  The JBossCacheManager will already be aware of
    * a remote invalidation and will handle removal itself.
    */
   protected void removeFromManager(boolean localCall, boolean localOnly)
   {
      if (localCall)
      {
         super.removeFromManager(localCall, localOnly);
      }
   }
   
   
   protected Object removeAttributeInternal(String name, boolean localCall, boolean localOnly)
   {
      return removeJBossInternalAttribute(name, localCall, localOnly);
   }

   protected Object removeJBossInternalAttribute(String name)
   {
      throw new UnsupportedOperationException("removeJBossInternalAttribute(String) " +
            "is not supported by JBossCacheClusteredSession; use " +
            "removeJBossInternalAttribute(String, boolean, boolean");
   }

   protected abstract Object removeJBossInternalAttribute(String name, 
                                                          boolean localCall, 
                                                          boolean localOnly);

}
