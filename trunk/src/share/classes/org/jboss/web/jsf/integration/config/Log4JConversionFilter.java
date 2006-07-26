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

package org.jboss.web.jsf.integration.config;

import com.sun.faces.util.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * This filter is used to convert java.util.logging messages from the JSF RI
 * to Log4J messages.
 *
 * @author Stan Silvert
 */
public class Log4JConversionFilter implements Filter {
    
    // cache Logger instances.  Logger.getLogger() is known to be slow.
    // See http://www.qos.ch/logging/thinkAgain.jsp
    private Map<String, Logger> loggerCache = new HashMap<String, Logger>();
    
    private Formatter formatter = new SimpleFormatter();
    
    // should we log the INFO level config messages that tell about
    // servlet context params?
    private boolean logConfigMessages;
    
    private static final String FACES_CONFIG_LOGGER = Util.FACES_LOGGER + Util.CONFIG_LOGGER;
    
    public Log4JConversionFilter(boolean logConfigMessages) {
        this.logConfigMessages = logConfigMessages;
    }
    
    private boolean isConfigInfoMessage(LogRecord record) {
        return record.getLevel().equals(Level.INFO) &&
               record.getLoggerName().equals(FACES_CONFIG_LOGGER);
    }
    
    /**
     * If the JSF RI message should be logged, convert the JDK 1.4 
     * LogRecord to a Log4J message.
     *
     * @return <code>false</code> because JDK 1.4 logging should not happen
     *         for JSF if this filter is active.
     */
    public boolean isLoggable(LogRecord record) {
        if (!logConfigMessages && isConfigInfoMessage(record)) return false;
        
        Logger logger = getLogger(record);
        
        if (record.getThrown() != null) {
            logWithThrowable(logger, record);
        } else {
            logWithoutThrowable(logger, record);
        }
        
        return false;
    }
    
    private void logWithThrowable(Logger logger, LogRecord record) {
        int loggedLevel = record.getLevel().intValue();
        Object message = formatter.formatMessage(record);
        Throwable throwable = record.getThrown();
        
        if (loggedLevel == Level.INFO.intValue()) {
            logger.info(message, throwable);
            return;
        }
        
        if (loggedLevel == Level.WARNING.intValue()) {
            logger.warn(message, throwable);
            return;
        }
        
        if (loggedLevel == Level.SEVERE.intValue()) {
            logger.fatal(message, throwable);
            return;
        }
    
        logger.info(message, throwable);
    }
    
    private void logWithoutThrowable(Logger logger, LogRecord record) {
        int loggedLevel = record.getLevel().intValue();
        Object message = formatter.formatMessage(record);
        
        if (loggedLevel == Level.INFO.intValue()) {
            logger.info(message);
            return;
        }
        
        if (loggedLevel == Level.WARNING.intValue()) {
            logger.warn(message);
            return;
        }
        
        if (loggedLevel == Level.SEVERE.intValue()) {
            logger.fatal(message);
            return;
        }
    
        logger.info(message);
    }
    
    // get the Log4J logger corresponding to the java.util.logger.LogRecord
    private Logger getLogger(LogRecord record) {
        String loggerName = record.getLoggerName();
        Logger logger = loggerCache.get(loggerName);
        if (logger == null) {
            logger = Logger.getLogger(loggerName);
            loggerCache.put(loggerName, logger);
        }
        
        return logger;
    }
    
}
