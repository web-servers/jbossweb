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
package org.jboss.web.tomcat.tc6;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.catalina.Loader;
import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.Util;
import org.jboss.security.authorization.PolicyRegistration;
import org.jboss.security.plugins.AuthorizationManagerServiceMBean;
import org.jboss.web.AbstractWebContainer;
import org.jboss.web.AbstractWebDeployer;
import org.jboss.web.WebApplication;
import org.jboss.web.tomcat.security.JaccContextValve;
import org.jboss.web.tomcat.security.RunAsListener;
import org.jboss.web.tomcat.security.SecurityAssociationValve;
import org.jboss.web.tomcat.tc6.session.AbstractJBossManager;
import org.jboss.web.tomcat.tc6.session.ClusteringNotSupportedException;
import org.jboss.web.tomcat.tc6.session.JBossCacheManager;

/**
 * The tomcat web application deployer
 *
 * @author Scott.Stark@jboss.org
 * @author Costin Manolache
 * @version $Revision: 1.3 $
 */
public class TomcatDeployer extends AbstractWebDeployer
{
   /**
    * The name of the war level context configuration descriptor
    */
   private static final String CONTEXT_CONFIG_FILE = "WEB-INF/context.xml"; 
   
   /**
    * Optional XACML Policy File
    */
   private static final String XACML_POLICY_FILE = "WEB-INF/jboss-xacml-policy.xml";

   private DeployerConfig config;
   private String[] javaVMs =
      {" jboss.management.local:J2EEServer=Local,j2eeType=JVM,name=localhost"};
   private String serverName = "jboss";
   private HashMap vhostToHostNames = new HashMap();

   public void init(Object containerConfig) throws Exception
   {
      this.config = (DeployerConfig)containerConfig;
      super.setJava2ClassLoadingCompliance(config.isJava2ClassLoadingCompliance());
      super.setUnpackWars(config.isUnpackWars());
      super.setLenientEjbLink(config.isLenientEjbLink());
      super.setDefaultSecurityDomain(config.getDefaultSecurityDomain());
   }

   /**
    * Perform the tomcat specific deployment steps.
    */
   protected void performDeploy(WebApplication appInfo, String warUrl,
      AbstractWebContainer.WebDescriptorParser webAppParser)
      throws Exception
   {
      WebMetaData metaData = appInfo.getMetaData();
      String hostName = null;
      // Get any jboss-web/virtual-hosts
      Iterator vhostNames = metaData.getVirtualHosts();
      // Map the virtual hosts onto the configured hosts
      Iterator hostNames = mapVirtualHosts(vhostNames);
      if (hostNames.hasNext())
      {
         hostName = hostNames.next().toString();
      }
      performDeployInternal(hostName, appInfo, warUrl, webAppParser);
      while (hostNames.hasNext())
      {
         String additionalHostName = hostNames.next().toString();
         performDeployInternal(additionalHostName, appInfo, warUrl, webAppParser);
      }
   }

   protected void performDeployInternal(String hostName,
      WebApplication appInfo, String warUrl,
      AbstractWebContainer.WebDescriptorParser webAppParser)
      throws Exception
   {

      WebMetaData metaData = appInfo.getMetaData();
      String ctxPath = metaData.getContextRoot();
      if (ctxPath.equals("/") || ctxPath.equals("/ROOT") || ctxPath.equals(""))
      {
         log.debug("deploy root context=" + ctxPath);
         ctxPath = "/";
         metaData.setContextRoot(ctxPath);
      }

      log.info("deploy, ctxPath=" + ctxPath + ", warUrl=" + shortWarUrlFromServerHome(warUrl));

      URL url = new URL(warUrl);

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      /* If we are using the jboss class loader we need to augment its path
      to include the WEB-INF/{lib,classes} dirs or else scoped class loading
      does not see the war level overrides. The call to setWarURL adds these
      paths to the deployment UCL.
      */
      Loader webLoader = null;
      if (config.isUseJBossWebLoader())
      {
         WebCtxLoader jbossLoader = new WebCtxLoader(loader);
         jbossLoader.setWarURL(url);
         webLoader = jbossLoader;
      }
      else
      {
         String[] pkgs = config.getFilteredPackages();
         WebAppLoader jbossLoader = new WebAppLoader(loader, pkgs);
         jbossLoader.setDelegate(getJava2ClassLoadingCompliance());
         webLoader = jbossLoader;
      }

      // We need to establish the JNDI ENC prior to the start 
      // of the web container so that init on startup servlets are able 
      // to interact with their ENC. We hook into the context lifecycle 
      // events to be notified of the start of the
      // context as this occurs before the servlets are started.
      if (appInfo.getAppData() == null)
         webAppParser.parseWebAppDescriptors(loader, appInfo.getMetaData());

      appInfo.setName(url.getPath());
      appInfo.setClassLoader(loader);
      appInfo.setURL(url);

      String objectNameS = config.getCatalinaDomain()
         + ":j2eeType=WebModule,name=//" +
         ((hostName == null) ? "localhost" : hostName)
         + ctxPath + ",J2EEApplication=none,J2EEServer=none";

      ObjectName objectName = new ObjectName(objectNameS);

      if (server.isRegistered(objectName))
      {
         log.debug("Already exists, destroying " + objectName);
         server.invoke(objectName, "destroy", new Object[]{},
            new String[]{});
      }  

      server.createMBean("org.apache.commons.modeler.BaseModelMBean",
         objectName, new Object[]{config.getContextClassName()},
         new String[]{"java.lang.String"}); 
      
      
      // Find and set config file on the context
      // If WAR is packed, expand config file to temp folder
      String ctxConfig = null;
      try
      {
         ctxConfig = findConfig(url);
      }
      catch (IOException e)
      {
         log.debug("No " + CONTEXT_CONFIG_FILE + " in " + url, e);
      }

      server.setAttribute(objectName, new Attribute("docBase", url.getFile()));

      server.setAttribute(objectName, new Attribute("configFile", ctxConfig));

      server.setAttribute(objectName, new Attribute
         ("defaultContextXml", "context.xml"));
      server.setAttribute(objectName, new Attribute
         ("defaultWebXml", "conf/web.xml"));

      server.setAttribute(objectName, new Attribute("javaVMs", javaVMs));

      server.setAttribute(objectName, new Attribute("server", serverName));

      server.setAttribute(objectName, new Attribute
         ("saveConfig", Boolean.FALSE));

      if (webLoader != null)
      {
         server.setAttribute(objectName, new Attribute
            ("loader", webLoader));
      }
      else
      {
         server.setAttribute(objectName, new Attribute
            ("parentClassLoader", loader));
      }

      server.setAttribute(objectName, new Attribute
         ("delegate", new Boolean(getJava2ClassLoadingCompliance())));

      String[] jspCP = getCompileClasspath(loader);
      StringBuffer classpath = new StringBuffer();
      for (int u = 0; u < jspCP.length; u++)
      {
         String repository = jspCP[u];
         if (repository == null)
            continue;
         if (repository.startsWith("file://"))
            repository = repository.substring(7);
         else if (repository.startsWith("file:"))
            repository = repository.substring(5);
         else
            continue;
         if (repository == null)
            continue;
         // ok it is a file.  Make sure that is is a directory or jar file
         File fp = new File(repository);
         if (!fp.isDirectory())
         {
            // if it is not a directory, try to open it as a zipfile.
            try
            {
               // avoid opening .xml files
               if (fp.getName().toLowerCase().endsWith(".xml"))
                  continue;
               
               ZipFile zip = new ZipFile(fp);
               zip.close();
            }
            catch (IOException e)
            {
               continue;
            }

         }
         if (u > 0)
            classpath.append(File.pathSeparator);
         classpath.append(repository);
      }

      server.setAttribute(objectName, new Attribute
         ("compilerClasspath", classpath.toString()));

      // Set the session cookies flag according to metadata
      switch (metaData.getSessionCookies())
      {
         case WebMetaData.SESSION_COOKIES_ENABLED:
            server.setAttribute(objectName, new Attribute
               ("cookies", new Boolean(true)));
            log.debug("Enabling session cookies");
            break;
         case WebMetaData.SESSION_COOKIES_DISABLED:
            server.setAttribute(objectName, new Attribute
               ("cookies", new Boolean(false)));
            log.debug("Disabling session cookies");
            break;
         default:
            log.debug("Using session cookies default setting");
      }

      // Add a valve to estalish the JACC context before authorization valves
      Certificate[] certs = null;
      CodeSource cs = new CodeSource(url, certs);
      JaccContextValve jaccValve = new JaccContextValve(metaData.getJaccContextID(), cs);
      server.invoke(objectName, "addValve",
         new Object[]{jaccValve},
         new String[]{"org.apache.catalina.Valve"}
      );

      // Pass the metadata to the RunAsListener via a thread local
      RunAsListener.metaDataLocal.set(metaData);
      try
      {
         // Init the container; this will also start it
         server.invoke(objectName, "init", new Object[]{}, new String[]{});
      }
      finally
      {
         RunAsListener.metaDataLocal.set(null);
      }
      
      // make the context class loader known to the WebMetaData, ws4ee needs it
      // to instanciate service endpoint pojos that live in this webapp
      Loader ctxLoader = (Loader)server.getAttribute(objectName, "loader");
      metaData.setContextLoader(ctxLoader.getClassLoader());

      // Clustering
      if (metaData.getDistributable())
      {
         // Try to initate clustering, fallback to standard if no clustering is available
         try
         {
            AbstractJBossManager manager = null;
            String managerClassName = config.getManagerClass();
            Class managerClass = Thread.currentThread().getContextClassLoader().loadClass(managerClassName);
            manager = (AbstractJBossManager) managerClass.newInstance();
            String name = "//" + ((hostName == null) ? "localhost" : hostName) + ctxPath;
            manager.init(name, metaData, config.isUseJK(), config.isUseLocalCache());

            if (manager instanceof JBossCacheManager)
            {              
               String snapshotMode = config.getSnapshotMode();
               int snapshotInterval = config.getSnapshotInterval();
               JBossCacheManager jbcm = (JBossCacheManager) manager;
               jbcm.setSnapshotMode(snapshotMode);
               jbcm.setSnapshotInterval(snapshotInterval);
            }
            
            server.setAttribute(objectName, new Attribute("manager", manager));

            log.debug("Enabled clustering support for ctxPath=" + ctxPath);
         }
         catch (ClusteringNotSupportedException e)
         {
            log.error("Failed to setup clustering, clustering disabled", e);
         }
         catch(Throwable t)
         {
            log.error("Failed to setup clustering, clustering disabled. Exception: ", t);
         }
      }

      /* Add security association valve after the authorization
      valves so that the authenticated user may be associated with the
      request thread/session.
      */
      SecurityAssociationValve valve = new SecurityAssociationValve(metaData,
         config.getSecurityManagerService());
      valve.setSubjectAttributeName(config.getSubjectAttributeName());
      server.invoke(objectName, "addValve",
         new Object[]{valve},
         new String[]{"org.apache.catalina.Valve"});

      // Retrieve the state, and throw an exception in case of a failure
      Integer state = (Integer) server.getAttribute(objectName, "state");
      if (state.intValue() != 1)
      {
         throw new DeploymentException("URL " + warUrl + " deployment failed");
      }

      appInfo.setAppData(objectName);

      // Create mbeans for the servlets
      DeploymentInfo di = webAppParser.getDeploymentInfo();
      di.deployedObject = objectName;
      ObjectName servletQuery = new ObjectName
         (config.getCatalinaDomain() + ":j2eeType=Servlet,WebModule="
         + objectName.getKeyProperty("name") + ",*");
      Iterator iterator = server.queryMBeans(servletQuery, null).iterator();
      while (iterator.hasNext())
      {
         di.mbeans.add(((ObjectInstance)iterator.next()).getObjectName());
      }  
      
      if(metaData.getSecurityDomain() != null)
      {
         String secDomain = Util.unprefixSecurityDomain(metaData.getSecurityDomain());
         //Associate the Context Id with the Security Domain
         String contextID = metaData.getJaccContextID();
         mapSecurityDomain(secDomain, contextID); 
         
         //Check if xacml policy is available 
         URL xacmlPolicyFile = null;
         try
         {
            xacmlPolicyFile = this.findXACMLFile(url);
            if(xacmlPolicyFile != null)
            {
               AuthorizationManagerServiceMBean authzmgrService = config.getAuthorizationManagerService();
               if(authzmgrService == null)
                  throw new IllegalStateException("AuthorizationManagerService not configured in Tomcat5");
               AuthorizationManager authzmgr= authzmgrService.getAuthorizationManager(secDomain);
               if(authzmgr instanceof PolicyRegistration)
               {
                  PolicyRegistration xam = (PolicyRegistration)authzmgr;
                  xam.registerPolicy(contextID,xacmlPolicyFile);
               } 
            } 
         }
         catch(IOException ioe)
         {
            //Ignore
         }  
      }
      
      log.debug("Initialized: " + appInfo + " " + objectName);
   }


    /**
    * Called as part of the undeploy() method template to ask the
    * subclass for perform the web container specific undeployment steps.
    */
   protected void performUndeploy(String warUrl, WebApplication appInfo)
      throws Exception
   {
      if (appInfo == null)
      {
         log.debug("performUndeploy, no WebApplication found for URL "
            + warUrl);
         return;
      }

      log.info("undeploy, ctxPath=" + appInfo.getMetaData().getContextRoot()
         + ", warUrl=" + shortWarUrlFromServerHome(warUrl));

      WebMetaData metaData = appInfo.getMetaData();
      String hostName = null;
      Iterator hostNames = metaData.getVirtualHosts();
      if (hostNames.hasNext())
      {
         hostName = hostNames.next().toString();
      }
      performUndeployInternal(hostName, warUrl, appInfo);
      while (hostNames.hasNext())
      {
         String additionalHostName = hostNames.next().toString();
         performUndeployInternal(additionalHostName, warUrl, appInfo);
      }

   }

   protected void performUndeployInternal(String hostName, String warUrl,
      WebApplication appInfo)
      throws Exception
   {

      WebMetaData metaData = appInfo.getMetaData();
      String ctxPath = metaData.getContextRoot();

      // If the server is gone, all apps were stopped already
      if (server == null)
         return;

      ObjectName objectName = new ObjectName(config.getCatalinaDomain()
         + ":j2eeType=WebModule,name=//" +
         ((hostName == null) ? "localhost" : hostName)
         + ctxPath + ",J2EEApplication=none,J2EEServer=none");

      if (server.isRegistered(objectName))
      {
         // Contexts should be stopped by the host already
         server.invoke(objectName, "destroy", new Object[]{},
            new String[]{});
      }

   }

   /**
    * Resolve the input virtual host names to the names of the configured Hosts
    * @param vhostNames Iterator<String> for the jboss-web/virtual-host elements 
    * @return Iterator<String> of the unique Host names
    * @throws Exception
    */
   protected synchronized Iterator mapVirtualHosts(Iterator vhostNames)
      throws Exception
   {
      if (vhostToHostNames.size() == 0)
      {
         // Query the configured Host mbeans
         String hostQuery = config.getCatalinaDomain() + ":type=Host,*";
         ObjectName query = new ObjectName(hostQuery);
         Set hosts = server.queryNames(query, null);
         Iterator iter = hosts.iterator();
         while (iter.hasNext())
         {
            ObjectName host = (ObjectName)iter.next();
            String name = host.getKeyProperty("host");
            if (name != null)
            {
               vhostToHostNames.put(name, name);
               String[] aliases = (String[])
                  server.invoke(host, "findAliases", null, null);
               int count = aliases != null ? aliases.length : 0;
               for (int n = 0; n < count; n++)
               {
                  vhostToHostNames.put(aliases[n], name);
               }
            }
         }
      }

      // Map the virtual host names to the hosts
      HashSet hosts = new HashSet();
      while (vhostNames.hasNext())
      {
         String vhost = (String)vhostNames.next();
         String host = (String)vhostToHostNames.get(vhost);
         if (host == null)
         {
            log.warn("Failed to map vhost: " + vhost);
            // This will cause a new host to be created
            host = vhost;
         }
         hosts.add(host);
      }
      return hosts.iterator();
   }

   private String findConfig(URL warURL) throws IOException
   {
      String result = null;
      // See if the warUrl is a dir or a file
      File warFile = new File(warURL.getFile());
      if (warURL.getProtocol().equals("file") && warFile.isDirectory() == true)
      {
         File webDD = new File(warFile, CONTEXT_CONFIG_FILE);
         if (webDD.exists() == true) result = webDD.getAbsolutePath();
      }
      else
      {
         ZipFile zipFile = new ZipFile(warFile);
         ZipEntry entry = zipFile.getEntry(CONTEXT_CONFIG_FILE);
         if (entry != null)
         {
            InputStream zipIS = zipFile.getInputStream(entry);
            byte[] buffer = new byte[512];
            int bytes;
            result = warFile.getAbsolutePath() + "-context.xml";
            FileOutputStream fos = new FileOutputStream(result);
            while ((bytes = zipIS.read(buffer)) > 0)
            {
               fos.write(buffer, 0, bytes);
            }
            zipIS.close();
            fos.close();
         }
         zipFile.close();
      }
      return result;
   }
   
   /**
    * Locate a XACMl Policy file packaged in the war
    * @param warURL
    * @return
    * @throws IOException
    */
   private URL findXACMLFile(URL warURL) throws IOException
   {
      URL result = null;
      // See if the warUrl is a dir or a file
      File warFile = new File(warURL.getFile());
      if (warURL.getProtocol().equals("file") && warFile.isDirectory() == true)
      {
         File webDD = new File(warFile, XACML_POLICY_FILE);
         if (webDD.exists() == true) 
            result = webDD.toURL();
      } 
      return result;
   }

    private String shortWarUrlFromServerHome(String warUrl)
    {
        String serverHomeUrl =  System.getProperty(org.jboss.system.server.ServerConfig.SERVER_HOME_URL);

        if (warUrl == null || serverHomeUrl == null)
            return warUrl;

        if (warUrl.startsWith(serverHomeUrl))
          return ".../" + warUrl.substring(serverHomeUrl.length());
        else
          return warUrl;
    } 
    
    protected void mapSecurityDomain(String securityDomain, String contextId)
    {
       //Register the context id with the authentication service 
       ObjectName oname = null;
       try
       {
          oname = new ObjectName("jboss.security:service=JASPISecurityManager");
          server.invoke(oname,"registerSecurityDomain", new Object[]{securityDomain,contextId},
                new String[]{"java.lang.String", "java.lang.String"} );
       }catch(JMException me)
       {
          log.error("mapSecurityDomain::" + me.getLocalizedMessage());
       } 
    } 
}
