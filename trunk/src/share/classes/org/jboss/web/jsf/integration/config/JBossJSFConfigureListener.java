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

import com.sun.faces.config.ConfigureListener;
import com.sun.faces.util.Util;
import java.util.logging.Filter;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspFactory;
import org.apache.log4j.Logger;

/**
 * This ServletContextListener sets up a JBoss-specific environment for JSF
 * and then delegates the rest of the setup to the JSF RI.
 *
 * @author Stan Silvert
 */
public class JBossJSFConfigureListener extends ConfigureListener {
    
    private static Logger LOG = Logger.getLogger(JBossJSFConfigureListener.class);
    
    public static final String SHOULD_LOG_CONFIG_MESSAGES = "org.jboss.faces.logConfigParams";
    
    private ServletContext servletContext;
    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        
        // If the pluginClass is not set, assume Log4J
        if (System.getProperty("org.jboss.logging.Logger.pluginClass") == null) {
            setLog4J();
        }

        // TODO:  This will be removed when Tomcat fixes a bug.  For now,
        //        if we don't preload JspRuntimeContext then the JSF RI will
        //        get a NullPointerException when it tries to register a
        //        ELResolver with JSP.
        try {
            Class.forName("org.apache.jasper.compiler.JspRuntimeContext");
            JspFactory.getDefaultFactory();  //make sure we can get the factory
        } catch (Exception e) {
            LOG.error("Unable to initialize Jasper JspRuntimeContext", e);
        }

        super.contextInitialized(event);
    }
    
    /**
     * If Log4J is being used, set a filter that converts JSF RI java.util.logger
     * messages to Log4J messages.
     */
    private void setLog4J() {
        Filter conversionFilter = new Log4JConversionFilter(logConfigMessages());
        
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.APPLICATION_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.CONFIG_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.CONTEXT_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.LIFECYCLE_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.RENDERKIT_LOGGER)
                                .setFilter(conversionFilter);
        java.util.logging.Logger.getLogger(Util.FACES_LOGGER + Util.TAGLIB_LOGGER)
                                .setFilter(conversionFilter);
    }
    
    // should we log the configuration messages?
    private boolean logConfigMessages() {
        String shouldLogConfigParam = this.servletContext.getInitParameter(SHOULD_LOG_CONFIG_MESSAGES);
        return (shouldLogConfigParam != null) && (shouldLogConfigParam.equalsIgnoreCase("true"));
    }
    
}
