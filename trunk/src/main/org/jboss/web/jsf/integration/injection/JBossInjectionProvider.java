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

package org.jboss.web.jsf.integration.injection;

import com.sun.faces.spi.InjectionProvider;
import com.sun.faces.spi.InjectionProviderException;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.catalina.util.AnnotationProcessor;
import org.apache.log4j.Logger;

/**
 * Provides interface between JSF RI and Tomcat Catalina for injection of managed beans as
 * per JSF 1.2 Spec section 5.4.
 *
 * @author Stan Silvert
 */
public class JBossInjectionProvider implements InjectionProvider {
    private static final Logger LOG = Logger.getLogger(JBossInjectionProvider.class);
    private static final String NAMING_DISABLED = "Injection of naming resources into JSF managed beans disabled.";

    private Context namingContext;
    
    /**
     * Uses the default naming context for injection of resources into managed beans.
     */
    public JBossInjectionProvider() {
        try {
            this.namingContext = new InitialContext();
        } catch (Exception e) {
            LOG.warn(NAMING_DISABLED, e);
        }
    }
    
    /**
     * This constructor allows a subclass to override the default naming 
     * context.
     *
     * @param namingContext The naming context to use for injection of managed beans.
     *                      If this param is null then injection of resources will be
     *                      disabled and JBoss will only call @PostConstruct and
     *                      @PreDestroy methods.
     */
    protected JBossInjectionProvider(Context namingContext) {
        if (namingContext == null) {
            LOG.warn(NAMING_DISABLED);
        }
        
        this.namingContext = namingContext;
    }
    
    /**
     * Call methods on a managed bean that are annotated with @PreDestroy.
     */
    public void invokePreDestroy(Object managedBean) throws InjectionProviderException {
        try {
            AnnotationProcessor.preDestroy(managedBean);
        } catch (Exception e) {
            LOG.error("PreDestroy failed on managed bean.", e);
        }
    }

    /**
     * Inject naming resources into a managed bean and then call methods
     * annotated with @PostConstruct.
     */
    public void inject(Object managedBean) throws InjectionProviderException {
        if (this.namingContext != null) {
            try {
                AnnotationProcessor.injectNamingResources(this.namingContext, managedBean);
            } catch (Exception e) {
                LOG.error("Injection failed on managed bean.", e);
            }
        }
        
        try {
            AnnotationProcessor.postConstruct(managedBean);
        } catch (Exception e) {
            LOG.error("PostConstruct failed on managed bean.", e);
        }
    }
    
}
