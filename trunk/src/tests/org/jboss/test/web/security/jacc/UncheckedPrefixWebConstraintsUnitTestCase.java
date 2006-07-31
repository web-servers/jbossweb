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
package org.jboss.test.web.security.jacc;

import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.WebResourcePermission;

import junit.framework.TestCase;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.WebSecurityMetaData;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.jacc.DelegatingPolicy;
import org.jboss.security.jacc.JBossPolicyConfigurationFactory;
import org.jboss.web.WebPermissionMapping;

/** Test of the unchecked permission
 
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.4"
   xmlns="http://java.sun.com/xml/ns/j2ee"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee
   http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">

   <description>Tests of various security-constraints</description>

   <servlet>
      <servlet-name>ConstraintsServlet</servlet-name>
      <servlet-class>org.jboss.test.security.servlets.ConstraintsServlet</servlet-class>
   </servlet>

   <servlet-mapping>
      <servlet-name>ConstraintsServlet</servlet-name>
      <url-pattern>/*</url-pattern>
   </servlet-mapping>

   <security-constraint>
      <web-resource-collection>
         <web-resource-name>Excluded</web-resource-name>
         <url-pattern>/restricted/post-only/excluded/*</url-pattern>
         <url-pattern>/*</url-pattern>
      </web-resource-collection>
      <auth-constraint />
      <user-data-constraint>
         <transport-guarantee>NONE</transport-guarantee>
      </user-data-constraint>
   </security-constraint>

   <security-constraint>
      <web-resource-collection>
         <web-resource-name>Restricted POST</web-resource-name>
         <url-pattern>/restricted/post-only/*</url-pattern>
         <http-method>POST</http-method>
      </web-resource-collection>
      <auth-constraint>
         <role-name>PostRole</role-name>
      </auth-constraint>
      <user-data-constraint>
         <transport-guarantee>NONE</transport-guarantee>
      </user-data-constraint>
   </security-constraint>
   <security-constraint>
      <web-resource-collection>
         <web-resource-name>Excluded POST</web-resource-name>
         <url-pattern>/restricted/post-only/*</url-pattern>
         <http-method>DELETE</http-method>
         <http-method>PUT</http-method>
         <http-method>HEAD</http-method>
         <http-method>OPTIONS</http-method>
         <http-method>TRACE</http-method>
         <http-method>GET</http-method>
      </web-resource-collection>
      <auth-constraint />
      <user-data-constraint>
         <transport-guarantee>NONE</transport-guarantee>
      </user-data-constraint>
   </security-constraint>

   <security-role>
      <role-name>PostRole</role-name>
   </security-role>

   <login-config>
      <auth-method>BASIC</auth-method>
      <realm-name>WebConstraintsUnitTestCase</realm-name>
   </login-config>
</web-app>

 @author Scott.Stark@jboss.org
 @version $Revision: 37459 $
 */
public class UncheckedPrefixWebConstraintsUnitTestCase extends TestCase
{
   private PolicyConfiguration pc;

   public void testUncheckedPrefix() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = null;
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      // There should be no 
      WebResourcePermission wrp = new WebResourcePermission("/restricted/post-only/x", "GET");
      assertFalse("/restricted/post-only/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/x", "POST");
      assertFalse("/restricted/post-only/x POST", p.implies(pd, wrp));

      caller = new SimplePrincipal[]{new SimplePrincipal("PostRole")};
      pd = new ProtectionDomain(null, null, null, caller);
      wrp = new WebResourcePermission("/restricted/post-only/x", "GET");
      assertFalse("/restricted/post-only/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/x", "POST");
      assertTrue("/restricted/post-only/x POST", p.implies(pd, wrp));

   }

   protected void setUp() throws Exception
   {
      WebMetaData metaData = new WebMetaData();
      ArrayList securityContraints = new ArrayList();
      addSC(securityContraints);
      metaData.setSecurityConstraints(securityContraints);

      DelegatingPolicy policy = new DelegatingPolicy();
      Policy.setPolicy(policy);
      JBossPolicyConfigurationFactory pcf = new JBossPolicyConfigurationFactory();
      pc = pcf.getPolicyConfiguration("UncheckedPrefixWebConstraintsUnitTestCase", true);
      WebPermissionMapping.createPermissions(metaData, pc);
      pc.commit();
      System.out.println(policy.listContextPolicies());
      PolicyContext.setContextID("UncheckedPrefixWebConstraintsUnitTestCase");
   }

   private void addSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = Excluded
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Excluded");
      wrc.addPattern("/restricted/post-only/excluded/*");
      wrc.addPattern("/*");

      // <auth-constraint />
      wsmd.setExcluded(true);

      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");

      wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = Restricted POST
      wrc = wsmd.addWebResource("Restricted POST");
      wrc.addPattern("/restricted/post-only/*");
      wrc.addHttpMethod("POST");
      wsmd.addRole("PostRole");
      wsmd.setTransportGuarantee("NONE");

      wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = Excluded POST
      wrc = wsmd.addWebResource("Excluded POST");
      wrc.addPattern("/restricted/post-only/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");
      wsmd.setExcluded(true);
      wsmd.setTransportGuarantee("NONE");
   }

}
