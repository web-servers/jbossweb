/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.connector;

import static org.jboss.web.CatalinaMessages.MESSAGES;

import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class AsyncContextImpl implements AsyncContext {

    protected Request request;

    protected ServletRequest servletRequest = null;
    protected ServletResponse servletResponse = null;
    
    protected ServletContext servletContext = null;
    protected String path = null;
    protected Runnable runnable = null;
    protected Throwable error = null;
    protected boolean useAttributes = false;
    protected boolean original = true;
    protected boolean ready = true;

    public AsyncContextImpl(Request request) {
        this.request = request;
    }

    public void complete() {
        if (request == null) {
            return;
        }
        request.setEventMode(false);
        request.wakeup();
    }

    public void dispatch() {
        if (request == null) {
            return;
        }
        this.servletContext = null;
        if (servletRequest == request.getRequestFacade()) {
            // Get the path directly
            path = request.getRequestPathMB().toString();
        } else if (servletRequest instanceof HttpServletRequest) {
            // Remap the path to the target context
            String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
            this.servletContext = request.getServletContext0().getContext(requestURI);
            if (servletContext != null) {
                path = requestURI.substring(servletContext.getContextPath().length());
            } else {
                throw MESSAGES.cannotFindDispatchContext(requestURI);
            }
        }
        request.wakeup();
    }

    public void dispatch(String path) {
        if (request == null) {
            return;
        }
        this.servletContext = null;
        this.path = path;
        useAttributes = true;
        request.wakeup();
    }

    public void dispatch(ServletContext servletContext, String path) {
        if (request == null) {
            return;
        }
        this.servletContext = servletContext;
        this.path = path;
        useAttributes = true;
        request.wakeup();
    }

    public ServletRequest getRequest() {
        if (servletRequest != null) {
            return servletRequest;
        } else {
            return request.getRequestFacade();
        }
    }

    public ServletResponse getResponse() {
        if (servletResponse != null) {
            return servletResponse;
        } else {
            return request.getResponseFacade();
        }
    }

    public boolean hasOriginalRequestAndResponse() {
        return (servletRequest == request.getRequestFacade() && servletResponse == request.getResponseFacade());
    }

    public void start(Runnable runnable) {
        if (request == null) {
            return;
        }
        this.runnable = runnable;
        request.wakeup();
    }

    public boolean isReady() {
        return ready;
    }

    public void done() {
        ready = false;
    }

    public void setRequestAndResponse(ServletRequest servletRequest, ServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getPath() {
        return path;
    }

    public boolean getUseAttributes() {
        return useAttributes;
    }

    public Runnable getRunnable() {
        return runnable;
    }
    
    public Runnable runRunnable() {
        Runnable result = runnable;
        runnable = null;
        return result;
    }
    
    public void reset() {
        servletContext = null;
        path = null;
        runnable = null;
        useAttributes = false;
        ready = true;
        error = null;
    }
    
    public Map<AsyncListener, AsyncListenerRegistration> getAsyncListeners() {
        return request.getAsyncListeners();
    }

    public void addListener(AsyncListener listener,
            ServletRequest servletRequest, ServletResponse servletResponse) {
        getAsyncListeners().put(listener, 
                new AsyncListenerRegistration(listener, servletRequest, servletResponse));
    }

    public void addListener(AsyncListener listener) {
        addListener(listener, getRequest(), request.getResponse().getResponse());
    }

    public long getTimeout() {
        return request.getAsyncTimeout();
    }

    public void setTimeout(long timeout) {
        if (request == null) {
            return;
        }
        request.setAsyncTimeout(timeout);
        int realTimeout = (timeout > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) timeout;
        if (realTimeout <= 0) {
            realTimeout = Integer.MAX_VALUE;
        }
        request.setTimeout0(realTimeout);
    }

    public <T extends AsyncListener> T createListener(Class<T> clazz)
            throws ServletException {
        T listenerInstance = null;
        try {
            listenerInstance = (T) request.getContext().getInstanceManager().newInstance(clazz);
        } catch (Exception e) {
            throw new ServletException(MESSAGES.listenerCreationFailed(clazz.getName()), e);
        }
        request.getAsyncListenerInstances().add(listenerInstance);
        return listenerInstance;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        ready = true;
        this.error = error;
    }

    public void clear() {
        reset();
        request = null;
    }

    public class AsyncListenerRegistration {
        protected ServletRequest request;
        protected ServletResponse response;
        protected AsyncListener listener;
        protected AsyncListenerRegistration(AsyncListener listener, 
                ServletRequest request, ServletResponse response)
        {
            this.listener = listener;
            this.request = request;
            this.response = response;
        }
        public ServletRequest getRequest() {
            return request;
        }
        public ServletResponse getResponse() {
            return response;
        }
        public AsyncListener getListener() {
            return listener;
        }
    }


}

