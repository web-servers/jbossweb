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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a clustered session for the JBossCacheManager. The replication granularity
 * level is attribute based; that is, we replicate only the dirty attributes.
 * We use JBossCache for our internal, deplicated data store.
 * The internal structure is like in JBossCache:
 * <pre>
 * /JSESSION
 *    /hostname
 *       /web_app_path    (path + session id is unique)
 *           /id   Map(id, session)
 *                    (VERSION_KEY, version)  // Used for version tracking. version is an Integer.
 *              /ATTRIBUTE    Map(attr_key, value)
 * </pre>
 * <p/>
 * Note that the isolation level of the cache dictates the
 * concurrency behavior. Also note that session and its associated attribtues are stored in different nodes.
 * This will be ok since cache will take care of concurrency. When replicating, we will need to replicate both
 * session and its attributes.</p>
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * 
 * @version $Revision: 1.2 $
 */
class AttributeBasedClusteredSession
   extends JBossCacheClusteredSession
{
   static final long serialVersionUID = -5625209785550936713L;
   /**
    * Descriptive information describing this Session implementation.
    */
   protected static final String info = "AttributeBasedClusteredSession/1.0";

   // Transient map to store attr changes for replication.
   private transient Map attrModifiedMap_ = new HashMap();
   // Note that the removed attr is intentionally stored in a map 
   // instead of a Set so it is faster to lookup and remove.
   private transient Map attrRemovedMap_ = new HashMap();
   private static final int REMOVE = 0;   // Used to track attribute changes
   private static final int MODIFY = 1;
   // TODO why isn't the superclass field sufficient?
   private transient Map attributes_ = Collections.synchronizedMap(new HashMap());

   public AttributeBasedClusteredSession(JBossCacheManager manager)
   {
      super(manager);
   }
   
   /**
    * Used only for externalization.
    */
   private AttributeBasedClusteredSession() 
   {
      super();
   }

   // ----------------------------------------------- Overridden Public Methods


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

      attributes_.clear();
      clearAttrChangedMaps();
   }

   /**
    * Return a string representation of this object.
    */
   public String toString()
   {

      StringBuffer sb = new StringBuffer();
      sb.append("AttributeBasedClusteredSession[");
      sb.append(super.toString());
      sb.append("]");
      return (sb.toString());

   }

   /**
    * Overrides the superclass version to read in the attributes.
    */
   public synchronized void processSessionRepl()
   {
      // Replicate the metadata first. Note this will be lightweight since many  
      // of the fields are transient and the attribute map isn't included.
      if (log.isTraceEnabled())
      {
         log.trace("processSessionRepl(): session is dirty. Will increment " +
                "version from: " + getVersion() + " and replicate.");
      }
      this.incrementVersion();
      proxy_.putSession(realId, this);

      // Go thru the attribute change list
      
      if (getSessionAttributesDirty())
      {
         // Go thru the modified attr list first
         Set set = attrModifiedMap_.keySet();
         Iterator it = set.iterator();
         while (it.hasNext())
         {
            Object key = it.next();
            proxy_.putAttribute(realId, (String) key, attrModifiedMap_.get(key));
         }
   
         // Go thru the remove attr list
         set = attrRemovedMap_.keySet();
         it = set.iterator();
         while (it.hasNext())
         {
            Object key = it.next();
            proxy_.removeAttribute(realId, (String) key);
         }
   
         clearAttrChangedMaps();
      }
      
      sessionAttributesDirty = false;
      sessionMetadataDirty = false;
      
      updateLastReplicated();
   }

   public void removeMyself()
   {
      // This is a shortcut to remove session and it's child attributes.
      proxy_.removeSession(realId);
   }

   public void removeMyselfLocal()
   {
      // Need to evict attribute first before session to clean up everything.
      // BRIAN -- the attributes *are* already evicted, but we leave the
      // removeAttributesLocal call here in order to evict the ATTRIBUTE node.  
      // Otherwise empty nodes for the session root and child ATTRIBUTE will 
      // remain in the tree and screw up our list of session names.
      proxy_.removeAttributesLocal(realId);
      proxy_.removeSessionLocal(realId);
   }

   // ------------------------------------------------ JBoss internal abstract method

   /**
    * Populate the attributes stored in the distributed store to local transient ones.
    */
   protected void populateAttributes()
   {
      Map map = proxy_.getAttributes(realId);
      
      // Preserve any local attributes that were excluded from replication
      Map excluded = removeExcludedAttributes(attributes_);
      if (excluded != null)
         map.putAll(excluded);
      
      attributes_ = Collections.synchronizedMap(map);
      attrModifiedMap_.clear();
      attrRemovedMap_.clear();
   }

   protected Object getJBossInternalAttribute(String name)
   {
      Object result = attributes_.get(name);

      // Do dirty check even if result is null, as w/ SET_AND_GET null
      // still makes us dirty (ensures timely replication w/o using ACCESS)
      if (isGetDirty(result) && !replicationExcludes.contains(name))
      {
         attributeChanged(name, result, MODIFY);
      }

      return result;
   }

   protected Object removeJBossInternalAttribute(String name, 
                                                 boolean localCall, 
                                                 boolean localOnly)
   {
      Object result = attributes_.remove(name);
      if (localCall && !replicationExcludes.contains(name))
         attributeChanged(name, result, REMOVE);
      return result;
   }

   protected Map getJBossInternalAttributes()
   {
      return attributes_;
   }

   protected Set getJBossInternalKeys()
   {
      return attributes_.keySet();
   }

   /**
    * Method inherited from Tomcat. Return zero-length based string if not found.
    */
   protected String[] keys()
   {
      return ((String[]) getJBossInternalKeys().toArray(EMPTY_ARRAY));
   }

   protected Object setJBossInternalAttribute(String key, Object value)
   {
      Object old = attributes_.put(key, value);
      if (!replicationExcludes.contains(key))
         attributeChanged(key, value, MODIFY);
      return old;
   }

   protected synchronized void attributeChanged(Object key, Object value, int op)
   {
      if (op == MODIFY)
      {
         if (attrRemovedMap_.containsKey(key))
         {
            attrRemovedMap_.remove(key);
         }
         attrModifiedMap_.put(key, value);
      }
      else if (op == REMOVE)
      {
         if (attrModifiedMap_.containsKey(key))
         {
            attrModifiedMap_.remove(key);
         }
         attrRemovedMap_.put(key, value);
      }
      sessionAttributesDirty();
   }

   protected synchronized void clearAttrChangedMaps()
   {
      attrRemovedMap_.clear();
      attrModifiedMap_.clear();
   }
}
