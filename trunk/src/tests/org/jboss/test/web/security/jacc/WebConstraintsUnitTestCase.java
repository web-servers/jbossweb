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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.security.Policy;
import java.security.ProtectionDomain;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.PolicyContext;

import junit.framework.TestCase;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.WebSecurityMetaData;
import org.jboss.metadata.SecurityRoleMetaData;
import org.jboss.web.WebPermissionMapping;
import org.jboss.security.jacc.DelegatingPolicy;
import org.jboss.security.jacc.JBossPolicyConfigurationFactory;
import org.jboss.security.SimplePrincipal;

/** Test

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
       <display-name>excluded</display-name>
       <web-resource-collection>
          <web-resource-name>No Access</web-resource-name>
          <url-pattern>/excluded/*</url-pattern>
          <url-pattern>/restricted/get-only/excluded/*</url-pattern>
          <url-pattern>/restricted/post-only/excluded/*</url-pattern>
          <url-pattern>/restricted/any/excluded/*</url-pattern>
       </web-resource-collection>
       <web-resource-collection>
          <web-resource-name>No Access</web-resource-name>
          <url-pattern>/restricted/*</url-pattern>
          <http-method>DELETE</http-method>
          <http-method>PUT</http-method>
          <http-method>HEAD</http-method>
          <http-method>OPTIONS</http-method>
          <http-method>TRACE</http-method>
          <http-method>GET</http-method>
          <http-method>POST</http-method>
       </web-resource-collection>
       <auth-constraint />
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>

    <security-constraint>
       <display-name>unchecked</display-name>
       <web-resource-collection>
          <web-resource-name>All Access</web-resource-name>
          <url-pattern>/unchecked/*</url-pattern>
          <http-method>DELETE</http-method>
          <http-method>PUT</http-method>
          <http-method>HEAD</http-method>
          <http-method>OPTIONS</http-method>
          <http-method>TRACE</http-method>
          <http-method>GET</http-method>
          <http-method>POST</http-method>
       </web-resource-collection>
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>

    <security-constraint>
       <display-name>Restricted GET</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Get Only</web-resource-name>
          <url-pattern>/restricted/get-only/*</url-pattern>
          <http-method>GET</http-method>
       </web-resource-collection>
       <auth-constraint>
          <role-name>GetRole</role-name>
       </auth-constraint>
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>
    <security-constraint>
       <display-name>Excluded GET</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Get Only</web-resource-name>
          <url-pattern>/restricted/get-only/*</url-pattern>
          <http-method>DELETE</http-method>
          <http-method>PUT</http-method>
          <http-method>HEAD</http-method>
          <http-method>OPTIONS</http-method>
          <http-method>TRACE</http-method>
          <http-method>POST</http-method>
       </web-resource-collection>
       <auth-constraint />
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>

    <security-constraint>
       <display-name>Restricted POST</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Post Only</web-resource-name>
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
       <display-name>Excluded POST</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Post Only</web-resource-name>
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

    <security-constraint>
       <display-name>Restricted ANY</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Any</web-resource-name>
          <url-pattern>/restricted/any/*</url-pattern>
          <http-method>DELETE</http-method>
          <http-method>PUT</http-method>
          <http-method>HEAD</http-method>
          <http-method>OPTIONS</http-method>
          <http-method>TRACE</http-method>
          <http-method>GET</http-method>
          <http-method>POST</http-method>
       </web-resource-collection>
       <auth-constraint>
          <role-name>*</role-name>
       </auth-constraint>
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>

    <security-constraint>
       <display-name>Unrestricted</display-name>
       <web-resource-collection>
          <web-resource-name>Restricted Access - Any</web-resource-name>
          <url-pattern>/restricted/not/*</url-pattern>
          <http-method>DELETE</http-method>
          <http-method>PUT</http-method>
          <http-method>HEAD</http-method>
          <http-method>OPTIONS</http-method>
          <http-method>TRACE</http-method>
          <http-method>GET</http-method>
          <http-method>POST</http-method>
       </web-resource-collection>
       <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
    </security-constraint>

    <security-role>
       <role-name>GetRole</role-name>
    </security-role>
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
public class WebConstraintsUnitTestCase extends TestCase
{
   private PolicyConfiguration pc;

   public void testUnchecked() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = null;
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);
      // Test /unchecked
      WebResourcePermission wrp = new WebResourcePermission("/unchecked", "GET");
      assertTrue("/unchecked GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/unchecked/x", "GET");
      assertTrue("/unchecked/x GET", p.implies(pd, wrp));

      // Test the Unrestricted security-constraint
      wrp = new WebResourcePermission("/restricted/not", "GET");
      assertTrue("/restricted/not GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/not/x", "GET");
      assertTrue("/restricted/not/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/not/x", "HEAD");
      assertTrue("/restricted/not/x HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/not/x", "POST");
      assertTrue("/restricted/not/x POST", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/", "GET");
      assertTrue("/ GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/other", "GET");
      assertTrue("/other GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/other", "HEAD");
      assertTrue("/other HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/other", "POST");
      assertTrue("/other POST", p.implies(pd, wrp));
   }

   public void testGetAccess() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = {new SimplePrincipal("GetRole")};
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      // Test the Restricted GET security-constraint
      WebResourcePermission wrp = new WebResourcePermission("/restricted/get-only", "GET");
      assertTrue("/restricted/get-only GET", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/restricted/get-only/x", "GET");
      assertTrue("/restricted/get-only/x GET", p.implies(pd, wrp));

      // Test the Restricted ANY security-constraint
      wrp = new WebResourcePermission("/restricted/any/x", "GET");
      assertTrue("/restricted/any/x GET", p.implies(pd, wrp));

      // Test that a POST to the Restricted GET security-constraint fails
      wrp = new WebResourcePermission("/restricted/get-only/x", "POST");
      assertFalse("/restricted/get-only/x POST", p.implies(pd, wrp));

      // Test that Restricted POST security-constraint fails
      wrp = new WebResourcePermission("/restricted/post-only/x", "GET");
      assertFalse("/restricted/post-only/x GET", p.implies(pd, wrp));

      // Validate that the excluded subcontext if not accessible
      wrp = new WebResourcePermission("/restricted/get-only/excluded/x", "GET");
      assertFalse("/restricted/get-only/excluded/x GET", p.implies(pd, wrp));

      caller = new SimplePrincipal[]{new SimplePrincipal("OtherRole")};
      pd = new ProtectionDomain(null, null, null, caller);
      // Test the Restricted GET security-constraint 
      wrp = new WebResourcePermission("/restricted/get-only", "GET");
      assertFalse("/restricted/get-only GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/get-only/x", "GET");
      assertFalse("/restricted/get-only/x GET", p.implies(pd, wrp));

      /* Test the Restricted ANY security-constraint. Note that this would be
      allowed by the non-JACC and standalone tomcat as they interpret the "*"
      role-name to mean any role while the JACC mapping simply replaces "*" with
      the web.xml security-role/role-name values.
      */
      wrp = new WebResourcePermission("/restricted/any/x", "GET");
      assertFalse("/restricted/any/x GET", p.implies(pd, wrp));
   }

   /** Test that the excluded paths are not accessible by anyone
    */
   public void testExcludedAccess() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = {new SimplePrincipal("GetRole")};
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      WebResourcePermission wrp = new WebResourcePermission("/excluded/x", "GET");
      assertFalse("/excluded/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/excluded/x", "OPTIONS");
      assertFalse("/excluded/x OPTIONS", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/excluded/x", "HEAD");
      assertFalse("/excluded/x HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/excluded/x", "POST");
      assertFalse("/excluded/x POST", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/restricted/", "GET");
      assertFalse("/restricted/ GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/", "OPTIONS");
      assertFalse("/restricted/ OPTIONS", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/", "HEAD");
      assertFalse("/restricted/ HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/", "POST");
      assertFalse("/restricted/ POST", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/restricted/get-only/excluded/x", "GET");
      assertFalse("/restricted/get-only/excluded/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/get-only/excluded/x", "OPTIONS");
      assertFalse("/restricted/get-only/excluded/x OPTIONS", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/get-only/excluded/x", "HEAD");
      assertFalse("/restricted/get-only/excluded/x HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/get-only/excluded/x", "POST");
      assertFalse("/restricted/get-only/excluded/x POST", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/restricted/post-only/excluded/x", "GET");
      assertFalse("/restricted/post-only/excluded/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/excluded/x", "OPTIONS");
      assertFalse("/restricted/post-only/excluded/x OPTIONS", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/excluded/x", "HEAD");
      assertFalse("/restricted/post-only/excluded/x HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/excluded/x", "POST");
      assertFalse("/restricted/post-only/excluded/x POST", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/restricted/any/excluded/x", "GET");
      assertFalse("/restricted/any/excluded/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/any/excluded/x", "OPTIONS");
      assertFalse("/restricted/any/excluded/x OPTIONS", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/any/excluded/x", "HEAD");
      assertFalse("/restricted/any/excluded/x HEAD", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/any/excluded/x", "POST");
      assertFalse("/restricted/any/excluded/x POST", p.implies(pd, wrp));
   }

   /** Test POSTs against URLs that only allows the POST method and required
    * the PostRole role
    */
   public void testPostAccess() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = {new SimplePrincipal("PostRole")};
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      WebResourcePermission wrp = new WebResourcePermission("/restricted/post-only/", "POST");
      assertTrue("/restricted/post-only/ POST", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/x", "POST");
      assertTrue("/restricted/post-only/x POST", p.implies(pd, wrp));

      // Test the Restricted ANY security-constraint
      wrp = new WebResourcePermission("/restricted/any/x", "POST");
      assertTrue("/restricted/any/x POST", p.implies(pd, wrp));

      // Validate that the excluded subcontext if not accessible
      wrp = new WebResourcePermission("/restricted/post-only/excluded/x", "POST");
      assertFalse("/restricted/post-only/excluded/x POST", p.implies(pd, wrp));

      // Test that a GET to the Restricted POST security-constraint fails
      wrp = new WebResourcePermission("/restricted/post-only/x", "GET");
      assertFalse("/restricted/post-only/excluded/x GET", p.implies(pd, wrp));
      // Test that Restricted POST security-constraint fails
      wrp = new WebResourcePermission("/restricted/get-only/x", "POST");
      assertFalse("/restricted/get-only/x POST", p.implies(pd, wrp));

      // Change to otherUser to test failure
      caller = new SimplePrincipal[]{new SimplePrincipal("OtherRole")};
      pd = new ProtectionDomain(null, null, null, caller);

      // Test the Restricted Post security-constraint 
      wrp = new WebResourcePermission("/restricted/post-only", "POST");
      assertFalse("/restricted/post-only POST", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/restricted/post-only/x", "POST");
      assertFalse("/restricted/post-only/x POST", p.implies(pd, wrp));

   }

   protected void setUp() throws Exception
   {
      WebMetaData metaData = new WebMetaData();
      ArrayList securityContraints = new ArrayList();
      addExcluded(securityContraints);
      addAllAccessSC(securityContraints);
      addRestrictedGetSC(securityContraints);
      addExcludedGetSC(securityContraints);
      addRestrictedPostSC(securityContraints);
      addExcludedPostSC(securityContraints);
      addRestrictedAnySC(metaData, securityContraints);
      addUnrestrictedSC(securityContraints);
      metaData.setSecurityConstraints(securityContraints);

      DelegatingPolicy policy = new DelegatingPolicy();
      Policy.setPolicy(policy);
      JBossPolicyConfigurationFactory pcf = new JBossPolicyConfigurationFactory();
      pc = pcf.getPolicyConfiguration("WebConstraintsUnitTestCase", true);
      WebPermissionMapping.createPermissions(metaData, pc);
      pc.commit();
      System.out.println(policy.listContextPolicies());
      PolicyContext.setContextID("WebConstraintsUnitTestCase");
   }

   private void addExcluded(List securityContraints)
   {
      // security-constraint/ display-name = excluded
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = No Access
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("No Access");
      wrc.addPattern("/excluded/*");
      wrc.addPattern("/restricted/get-only/excluded/*");
      wrc.addPattern("/restricted/post-only/excluded/*");
      wrc.addPattern("/restricted/any/excluded/*");
      wrc.addPattern("/excluded/*");

      // web-resource-collection/web-resource-name = No Access
      wrc = wsmd.addWebResource("No Access");
      wrc.addPattern("/restricted/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");
      wrc.addHttpMethod("POST");

      // empty auth-constraint
      wsmd.setExcluded(true);

      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addAllAccessSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // All Access
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("All Access");
      wrc.addPattern("/unchecked/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");
      wrc.addHttpMethod("POST");

      // No auth-constraint
      wsmd.setUnchecked(true);
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");
   }

   private void addRestrictedGetSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Get Only
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Get Only");
      wrc.addPattern("/restricted/get-only/*");
      wrc.addHttpMethod("GET");

      // auth-constraint/role-name = GetRole
      wsmd.addRole("GetRole");
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addExcludedGetSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Get Only
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Get Only");
      wrc.addPattern("/restricted/get-only/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("POST");

      // empty auth-constraint
      wsmd.setExcluded(true);
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addRestrictedPostSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Post Only
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Post Only");
      wrc.addPattern("/restricted/post-only/*");
      wrc.addHttpMethod("POST");

      // auth-constraint/role-name = PostRole
      wsmd.addRole("PostRole");
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addExcludedPostSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Post Only
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Post Only");
      wrc.addPattern("/restricted/post-only/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");

      // empty auth-constraint
      wsmd.setExcluded(true);
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addRestrictedAnySC(WebMetaData wmd, List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Any
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Any");
      wrc.addPattern("/restricted/any/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");
      wrc.addHttpMethod("POST");

      // auth-constraint/role-name = *
      wsmd.addRole("*");
      // Add the security-role/role-name values * would map to
      HashMap roles = new HashMap();
      roles.put("GetRole", new SecurityRoleMetaData("GetRole"));
      roles.put("PostRole", new SecurityRoleMetaData("PostRole"));
      wmd.setSecurityRoles(roles);
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   private void addUnrestrictedSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);

      // web-resource-name = Restricted Access - Any
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("Restricted Access - Any");
      wrc.addPattern("/restricted/not/*");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("GET");
      wrc.addHttpMethod("POST");

      // no auth-constraint
      wsmd.setUnchecked(true);
      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");            
   }
}
