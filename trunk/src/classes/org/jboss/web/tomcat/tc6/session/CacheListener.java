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

import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.TreeCacheListener;
import org.jboss.logging.Logger;
import org.jboss.metadata.WebMetaData;
import org.jgroups.View;


public class CacheListener implements TreeCacheListener
{
   // Element within an FQN that is the session id
   private static final int SESSION_ID_FQN_INDEX = 3;
   // Size of an Fqn that points to the root of a session
   private static final int SESSION_FQN_SIZE = SESSION_ID_FQN_INDEX + 1;
   // Element within an FQN that is the root of a Pojo attribute map
   private static final int POJO_ATTRIBUTE_FQN_INDEX = SESSION_ID_FQN_INDEX + 1;
   // Element within an FQN that is the root of an individual Pojo attribute
   private static final int POJO_KEY_FQN_INDEX = POJO_ATTRIBUTE_FQN_INDEX + 1;
   // Size of an Fqn that points to the root of a session
   private static final int POJO_KEY_FQN_SIZE = POJO_KEY_FQN_INDEX + 1;
   // The root of the buddy backup subtree
   private static final String BUDDY_BACKUP = "_BUDDY_BACKUP_";
   
   protected static Logger log_ = Logger.getLogger(CacheListener.class);
   protected JBossCacheWrapper cacheWrapper_;
   protected JBossCacheManager manager_;
   protected Fqn subtreeRoot_;
   protected boolean fieldBased_;

   CacheListener(JBossCacheWrapper wrapper, JBossCacheManager manager, Fqn subtreeRoot)
   {
      cacheWrapper_ = wrapper;
      manager_ = manager;
      subtreeRoot_ = subtreeRoot;
      fieldBased_ = manager_.getReplicationGranularity() == WebMetaData.REPLICATION_GRANULARITY_FIELD;
   }

   /**
    * If this event is emitted by myself or is not for the web app
    * I'm managing, then we can skip.
    * @param fqn
    * @return
    */
   protected boolean needToHandle(Fqn fqn)
   {
      return (LocalSessionActivity.isLocallyActive(getIdFromFqn(fqn)) == false 
              && fqn.isChildOf(subtreeRoot_));
   }

   // --------------- TreeCacheListener methods ------------------------------------

   public void nodeCreated(Fqn fqn)
   {
      // No-op
   }

   public void nodeRemoved(Fqn fqn)
   {
      ParsedBuddyFqn pfqn = new ParsedBuddyFqn(fqn);
      fqn = pfqn.noBuddy;
      if (fqn == null)
         return;
      
      if (fqn.size() == POJO_KEY_FQN_SIZE)
      {
         // Potential removal of a Pojo where we need to unregister
         // as an Observer.
         if (fieldBased_
               && needToHandle(fqn)
               && JBossCacheService.ATTRIBUTE.equals(fqn.get(POJO_ATTRIBUTE_FQN_INDEX))
               && fqn.size() > POJO_KEY_FQN_INDEX)
         {
            String sessId = getIdFromFqn(fqn);
            String attrKey = (String) fqn.get(POJO_KEY_FQN_INDEX);
            
            manager_.processRemoteAttributeRemoval(sessId, attrKey);
         }
      }
      else if(fqn.size() == SESSION_FQN_SIZE && needToHandle(fqn))
      {
         // A session has been invalidated from another node;
         // need to inform manager
         String sessId = getIdFromFqn(fqn);
         manager_.processRemoteInvalidation(sessId);
      }
   }

   /**
    * Called when a node is loaded into memory via the CacheLoader. This is not the same
    * as {@link #nodeCreated(Fqn)}.
    */
   public void nodeLoaded(Fqn fqn)
   {
   }

   public void nodeModified(Fqn fqn)
   {
      nodeDirty(fqn);
   }

   protected void nodeDirty(Fqn fqn)
   {
      // Parse the Fqn so we if it has a buddy backup region in it
      // we can just deal with the part below that
      ParsedBuddyFqn pfqn = new ParsedBuddyFqn(fqn);
      Fqn noBuddy = pfqn.noBuddy;
      
      // Check if we need to handle this event. If this is from myself or not for
      // my webapp, then I should skip it.
      // We only deal with events for the root node of a session,
      // so skip all others
      if(noBuddy == null || noBuddy.size() != SESSION_FQN_SIZE || !needToHandle(noBuddy)) return;

      // Query if we have version value. Use the full Fqn, not just the
      // "no buddy" part.
      // If we have a version value, we compare the version. Invalidate if necessary.
      Integer version = (Integer)cacheWrapper_.get(fqn, JBossCacheService.VERSION_KEY);
      if(version != null)
      {
         String realId = getIdFromFqn(noBuddy);

         ClusteredSession session = manager_.findLocalSession(realId);
         if (session == null)
         {
            // Notify the manager that an unloaded session has been updated
            manager_.unloadedSessionChanged(realId, pfqn.owner);
         }
         else if (session.isNewData(version.intValue()))
         {
            // Need to invalidate the loaded session
            session.setOutdatedVersion(version.intValue());
            if(log_.isTraceEnabled())
            {
               log_.trace("nodeDirty(): session in-memory data is " +
                          "invalidated with id: " + realId + " and version: " +
                          version.intValue());
            }
         }
         else
         {
            log_.warn("Possible concurrency problem: Replicated version id " + 
                      version + " matches in-memory version for session " + realId); 
         }
      }
      else
      {
         log_.warn("No VERSION_KEY attribute found in " + fqn);
      }
   }

   public void nodeVisited(Fqn fqn)
   {
      // no-op
   }

   public void cacheStarted(TreeCache cache)
   {
      // TODO will need to synchronize this with local sessions
   }

   public void cacheStopped(TreeCache cache)
   {
      // TODO will need to synchronize this with local sessions
   }

   public void viewChange(View new_view)
   {
      // We don't care for this event.
   }

   public void nodeEvicted(Fqn fqn)
   {
      // We don't care for this event.
   }

   protected String getIdFromFqn(Fqn fqn)
   {
      return (String)fqn.get(SESSION_ID_FQN_INDEX);
   }
   
   private class ParsedBuddyFqn
   {
      Fqn raw;
      Fqn noBuddy;
      String owner;
      
      ParsedBuddyFqn(Fqn raw)
      {
         this.raw = raw;
         if (raw != null)
         {
            if (BUDDY_BACKUP.equals(raw.get(0)))
            {
               if (raw.size() > 2)
               {
                  owner = (String) raw.get(1);
                  noBuddy = raw.getFqnChild(2, raw.size());
                  if (log_.isTraceEnabled())
                     log_.trace(raw + " parsed to " + noBuddy + " with owner " + owner);
               }
            }
            else
            {
               noBuddy = raw;
            }
         }
      }
   }
}
