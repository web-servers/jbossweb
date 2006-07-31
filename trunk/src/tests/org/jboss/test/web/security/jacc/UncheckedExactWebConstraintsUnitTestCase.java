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

/**
 * Test 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 37459 $
 */
public class UncheckedExactWebConstraintsUnitTestCase extends TestCase
{
   private PolicyConfiguration pc;

   public void testUncheckedExact() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = null;
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      WebResourcePermission wrp = new WebResourcePermission("/protected/exact/get/roleA", "GET");
      assertFalse("/protected/exact/get/roleA GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/protected/exact/get/roleA", "POST");
      assertFalse("/protected/exact/get/roleA POST", p.implies(pd, wrp));

      caller = new SimplePrincipal[]{new SimplePrincipal("RoleA")};
      wrp = new WebResourcePermission("/protected/exact/get/roleA", "GET");
      assertFalse("/protected/exact/get/roleA GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/protected/exact/get/roleA", "POST");
      assertFalse("/protected/exact/get/roleA POST", p.implies(pd, wrp));

      caller = new SimplePrincipal[]{new SimplePrincipal("RoleB")};
      pd = new ProtectionDomain(null, null, null, caller);
      wrp = new WebResourcePermission("/protected/exact/get/roleA", "GET");
      assertFalse("/protected/exact/get/roleA GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/protected/exact/get/roleA", "POST");
      assertTrue("/protected/exact/get/roleA POST", p.implies(pd, wrp));
   }

   protected void setUp() throws Exception
   {
      WebMetaData metaData = new WebMetaData();
      ArrayList securityContraints = new ArrayList();
      addProtectedASC(securityContraints);
      addProtectedBSC(securityContraints);
      metaData.setSecurityConstraints(securityContraints);

      DelegatingPolicy policy = new DelegatingPolicy();
      Policy.setPolicy(policy);
      JBossPolicyConfigurationFactory pcf = new JBossPolicyConfigurationFactory();
      pc = pcf.getPolicyConfiguration("UncheckedWebConstraintsUnitTestCase", true);
      WebPermissionMapping.createPermissions(metaData, pc);
      pc.commit();
      System.out.println(policy.listContextPolicies());
      PolicyContext.setContextID("UncheckedWebConstraintsUnitTestCase");
   }

   /*
   <security-constraint>
       <web-resource-collection>
           <web-resource-name>exact, get method, roleA</web-resource-name>
           <url-pattern>/protected/exact/get/roleA</url-pattern>
           <http-method>GET</http-method>
       </web-resource-collection>
       <auth-constraint>
           <role-name>RoleA</role-name>
       </auth-constraint>
       <user-data-constraint>
           <transport-guarantee>NONE</transport-guarantee>
       </user-data-constraint>
   </security-constraint>
   */
   private void addProtectedASC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = exact, get method, roleA
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("exact, get method, roleA");
      wrc.addPattern("/protected/exact/get/roleA");
      wrc.addHttpMethod("GET");

      // auth-constraint/role-name = RoleA
      wsmd.addRole("RoleA");

      // user-data-constraint/transport-guarantee
      wsmd.setTransportGuarantee("NONE");      
   }

   /*
   <security-constraint>
       <web-resource-collection>
           <web-resource-name>exact, get method, roleA verifier</web-resource-name>
           <url-pattern>/protected/exact/get/roleA</url-pattern>
           <http-method>POST</http-method>
           <http-method>PUT</http-method>
           <http-method>HEAD</http-method>
           <http-method>TRACE</http-method>
           <http-method>OPTIONS</http-method>
           <http-method>DELETE</http-method>
       </web-resource-collection>
       <auth-constraint>
           <role-name>RoleB</role-name>
       </auth-constraint>
   </security-constraint> 
   */
   private void addProtectedBSC(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = exact, get method, roleA verifier
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("exact, get method, roleA verifier");
      wrc.addPattern("/protected/exact/get/roleA");
      wrc.addHttpMethod("POST");
      wrc.addHttpMethod("PUT");
      wrc.addHttpMethod("HEAD");
      wrc.addHttpMethod("TRACE");
      wrc.addHttpMethod("OPTIONS");
      wrc.addHttpMethod("DELETE");

      // auth-constraint/role-name = RoleB
      wsmd.addRole("RoleB");
   }
}
