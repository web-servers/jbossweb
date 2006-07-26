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
import org.jboss.cache.aop.PojoCacheMBean;
import org.jboss.cache.config.Option;
import org.jboss.cache.lock.TimeoutException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JBossCacheWrapper 
{
   static final Option GRAVITATE_OPTION = new Option();
   
   static
   {
      GRAVITATE_OPTION.setForceDataGravitation(true);
   }
   
   private static final int RETRY = 3;
   private PojoCacheMBean proxy_;

   JBossCacheWrapper(PojoCacheMBean cache)
   {
      proxy_ = cache;
   }

   /**
    * Wrapper to embed retyr logic.
    *
    * @param fqn
    * @param id
    * @return
    */
   Object get(Fqn fqn, String id)
   {
      return get(fqn, id, false);
   }

   /**
    * Wrapper to embed retyr logic.
    *
    * @param fqn
    * @param id
    * @return
    */
   Object get(Fqn fqn, String id, boolean gravitate)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            Object value = null;
            if (gravitate)
               value = proxy_.get(fqn, id, GRAVITATE_OPTION);
            else
               value = proxy_.get(fqn, id);
            return value;
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            if (e instanceof RuntimeException)
               throw (RuntimeException) e;
            throw new RuntimeException("JBossCacheService: exception occurred in cache get ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache get after retry ... ", ex);
   }

   /**
    * Wrapper to embed retry logic.
    *
    * @param fqn
    * @param id
    * @param value
    * @return
    */
   void put(Fqn fqn, String id, Object value)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            proxy_.put(fqn, id, value);
            return;
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache put ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache put after retry ... ", ex);
   }


   /**
    * Wrapper to embed retry logic.
    *
    * @param fqn
    * @param map
    */
   void put(Fqn fqn, Map map)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            proxy_.put(fqn, map);
            return;
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache put ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache put after retry ... ", ex);
   }

   /**
    * Wrapper to embed retyr logic.
    *
    * @param fqn
    * @param id
    * @return
    */
   Object remove(Fqn fqn, String id)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            return proxy_.remove(fqn, id);
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache remove ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache remove after retry ... ", ex);
   }

   /**
    * Wrapper to embed retry logic.
    *
    * @param fqn
    */
   void remove(Fqn fqn)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            proxy_.remove(fqn);
            return;
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache remove ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache remove after retry ... ", ex);
   }

   /**
    * Wrapper to embed retry logic.
    *
    * @param fqn
    */
   void evict(Fqn fqn)
   {
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            proxy_.evict(fqn);
            return;
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache evict ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache evict after retry ... ", ex);
   }
   
   void evictSubtree(Fqn fqn)
   {
      
      Exception ex = null;
      for (int i = 0; i < RETRY; i++)
      {
         try
         {
            // Evict the node itself first, since if it stores a Pojo
            // that will do everything
            proxy_.evict(fqn);
            
            // next do a depth first removal; this ensure all nodes
            // are removed, not just their data map
            Set children = proxy_.getChildrenNames(fqn);
            if (children != null)
            {
               for (Iterator it = children.iterator(); it.hasNext(); )
               {
                  Fqn child = new Fqn(fqn, it.next());
                  proxy_.evict(child);
               }
               
               proxy_.evict(fqn);
            }
            return;
            
         }
         catch (TimeoutException e)
         {
            ex = e;
         }
         catch (Exception e)
         {
            throw new RuntimeException("JBossCacheService: exception occurred in cache evict ... ", e);
         }
      }
      throw new RuntimeException("JBossCacheService: exception occurred in cache evictSubtree after retry ... ", ex);
      
      
   }

}
