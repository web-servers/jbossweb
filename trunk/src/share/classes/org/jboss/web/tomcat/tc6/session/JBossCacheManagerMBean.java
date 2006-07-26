package org.jboss.web.tomcat.tc6.session;

public interface JBossCacheManagerMBean extends JBossManagerMBean
{
   /**
    * Gets the value of the attribute with the given key from the given
    * session.  If the session is in the distributed store but hasn't been
    * loaded on this node, invoking this method will cause it to be loaded.
    * 
    * @param sessionId the id of the session
    * @param key       the attribute key
    * @return          the value, or <code>null</code> if the session or
    *                  key does not exist.
    */
   Object getSessionAttribute(String sessionId, String key);
   
   /**
    * Same as <code>getSessionAttribute(sessionId, key).toString()</code>.
    * 
    */
   String getSessionAttributeString(String sessionId, String key);
   
   /**
    * Expires the given session. If the session is in the distributed store 
    * but hasn't been loaded on this node, invoking this method will cause it 
    * to be loaded.
    * 
    * @param sessionId the id of the session
    */
   void expireSession(String sessionId);
   
   /**
    * Gets the last time the given session was accessed on this node.
    * Information about sessions stored in the distributed store but never
    * accessed on this node will not be made available.
    * 
    * @param sessionId
    * @return the last accessed time, or <code>null</code> if the session
    *         has expired or has never been accessed on this node.
    */
   String getLastAccessedTime(String sessionId);
   
   /**
    * Gets the JMX ObjectName of the distributed session cache as a string.
    */
   String getCacheObjectNameString();
   
   /**
    * Gets the replication granularity.
    * 
    * @return SESSION, ATTRIBUTE or FIELD, or <code>null</code> if this
    *         has not yet been configured.
    */
   String getReplicationGranularityString();

   /**
    * Gets the replication trigger.
    * 
    * @return SET, SET_AND_GET, SET_AND_NON_PRIMITIVE_GET or <code>null</code> 
    *         if this has not yet been configured.
    */
   String getReplicationTriggerString();
   
   /**
    * Gets whether batching of field granularity changes will be done.  Only
    * relevant with replication granularity FIELD.
    * 
    * @return <code>true</code> if per-request batching will be done, 
    *         <code>false</code> if not, <code>null</code> if not configured
    */
   Boolean isReplicationFieldBatchMode();
   
   /**
    * Gets whether JK is being used and special handling of a jvmRoute
    * portion of session ids is needed.
    */
   boolean getUseJK();
   
   /**
    * Gets the snapshot mode.
    * 
    * @return "instant" or "interval"
    */
   String getSnapshotMode();
   
   /**
    * Gets the number of milliseconds between replications if "interval" mode
    * is used.
    */
   int getSnapshotInterval();
   
   /**
    * Lists all session ids known to this manager, including those in the 
    * distributed store that have not been accessed on this node.
    * 
    * @return a comma-separated list of session ids
    */
   String listSessionIds();
   
   /**
    * Lists all session ids known to this manager, excluding those in the 
    * distributed store that have not been accessed on this node.
    * 
    * @return a comma-separated list of session ids
    */
   String listLocalSessionIds();
}
