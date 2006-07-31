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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.transaction.TransactionManager;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;

/**
 * This Valve handles batch replication mode. It uses the cache tm to performa batch replication.
 *
 * @author Ben Wang
 * @version $Revision: 1.2 $
 */
public class BatchReplicationClusteredSessionValve extends ValveBase implements Lifecycle
{
   private static Logger log_ = Logger.getLogger(BatchReplicationClusteredSessionValve.class);

   // The info string for this Valve
   private static final String info = "BatchReplicationClusteredSessionValve/1.0";

   // Valve-lifecycle_ helper object
   protected LifecycleSupport support = new LifecycleSupport(this);

   // store the request and response object for parts of the clustering code that
   // have no direct access to this objects
   protected static ThreadLocal requestThreadLocal = new ThreadLocal();
   protected static ThreadLocal responseThreadLocal = new ThreadLocal();

   protected JBossCacheManager manager_;

   /**
    * Create a new Valve.
    *
    */
   public BatchReplicationClusteredSessionValve(AbstractJBossManager manager)
   {
      super();
      manager_ = (JBossCacheManager)manager;
   }

   /**
    * Get information about this Valve.
    */
   public String getInfo()
   {
      return info;
   }

   /**
    * Valve-chain handler method.
    * This method gets called when the request goes through the Valve-chain. Our session replication mechanism replicates the
    * session after request got through the servlet code.
    *
    * @param request  The request object associated with this request.
    * @param response The response object associated with this request.
    */
   public void invoke(Request request, Response response) throws IOException, ServletException
   {
      // Note: we use specfically the tm in cache.
      TransactionManager tm = manager_.getCacheService().getTransactionManager();
      if(tm == null)
      {
         throw new RuntimeException("BatchReplicationClusteredSessionValve.invoke(): Obtain null tm");
      }

      // Before we start a tx, get the session.  If this is a failover
      // situation, this will cause data gravitation, which will occur 
      // thus outside of the scope of the tx we are about to start.  
      // JBossCacheManager will ensure the gravitation is in its own tx
      request.getSession(false);
      
      // Start a new transaction, we need transaction so all the replication are sent in batch.
      try
      {
         tm.begin();
         // let the servlet invocation go through
         getNext().invoke(request, response);
         if(log_.isDebugEnabled())
         {
            log_.debug("Ready to commit batch replication for field level granularity");
         }
         tm.commit();
      }
      catch (Exception e)
      {
         try
         {
            tm.rollback();
         }
         catch (Exception exn)
         {
            exn.printStackTrace();
         }
         // We will need to alert Tomcat of this exception.
         throw new RuntimeException("JBossCacheManager.processSessionRepl(): failed to replicate session.", e);
      }
   }

   // Lifecylce-interface
   public void addLifecycleListener(LifecycleListener listener)
   {
      support.addLifecycleListener(listener);
   }

   public void removeLifecycleListener(LifecycleListener listener)
   {
      support.removeLifecycleListener(listener);
   }

   public LifecycleListener[] findLifecycleListeners()
   {
      return support.findLifecycleListeners();
   }

   public void start() throws LifecycleException
   {
      support.fireLifecycleEvent(START_EVENT, this);
   }

   public void stop() throws LifecycleException
   {
      support.fireLifecycleEvent(STOP_EVENT, this);
   }

}
