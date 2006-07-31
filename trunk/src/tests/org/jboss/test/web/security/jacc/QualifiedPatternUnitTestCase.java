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
import javax.security.jacc.WebUserDataPermission;

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
public class QualifiedPatternUnitTestCase extends TestCase
{
   private PolicyConfiguration pc;

   public void testUnchecked() throws Exception
   {
      Policy p = Policy.getPolicy();
      SimplePrincipal[] caller = null;
      ProtectionDomain pd = new ProtectionDomain(null, null, null, caller);

      WebResourcePermission wrp = new WebResourcePermission("/a", "GET");
      assertTrue("/a GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/a", "POST");
      assertTrue("/a POST", p.implies(pd, wrp));

      caller = new SimplePrincipal[]{new SimplePrincipal("R1")};
      pd = new ProtectionDomain(null, null, null, caller);
      wrp = new WebResourcePermission("/a/x", "GET");
      assertTrue("/a/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/a/x", "POST");
      boolean implied = p.implies(pd, wrp);
      assertTrue("/a/x POST", implied);
      wrp = new WebResourcePermission("/b/x", "GET");
      assertTrue("/b/x GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/b/x", "POST");
      assertTrue("/b/x POST", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/b/x", "DELETE");
      assertFalse("/b/x DELETE", p.implies(pd, wrp));

      wrp = new WebResourcePermission("/a/x.asp", "GET");
      assertTrue("/a/x.asp GET", p.implies(pd, wrp));
      wrp = new WebResourcePermission("/a/x.asp", "POST");
      assertTrue("/a/x.asp POST", p.implies(pd, wrp));

      WebUserDataPermission wudp = new WebUserDataPermission("/a/*:/a", "GET:CONFIDENTIAL");
      assertTrue("/a/*:/a GET:CONFIDENTIAL", p.implies(pd, wudp));
      wudp = new WebUserDataPermission("/a/*:/a", "GET:CONFIDENTIAL");
      assertTrue("/b/*:/b GET,POST:CONFIDENTIAL", p.implies(pd, wudp));
      
   }

   protected void setUp() throws Exception
   {
      WebMetaData metaData = new WebMetaData();
      ArrayList securityContraints = new ArrayList();
      addSC1(securityContraints);
      addSC2(securityContraints);
      metaData.setSecurityConstraints(securityContraints);

      DelegatingPolicy policy = new DelegatingPolicy();
      Policy.setPolicy(policy);
      JBossPolicyConfigurationFactory pcf = new JBossPolicyConfigurationFactory();
      pc = pcf.getPolicyConfiguration("QualifiedPatternUnitTestCase", true);
      WebPermissionMapping.createPermissions(metaData, pc);
      pc.commit();
      System.out.println(policy.listContextPolicies());
      PolicyContext.setContextID("QualifiedPatternUnitTestCase");
   }

   /*
   <security-constraint>
      <web-resource-collection>
         <web-resource-name>sc1.c1</web-resource-name>
         <url-pattern>/a/*</url-pattern>
         <url-pattern>/b/*</url-pattern>
         <url-pattern>/a</url-pattern>
         <url-pattern>/b</url-pattern>
         <http-method>DELETE</http-method>
         <http-method>PUT</http-method>
      </web-resource-collection>
      <web-resource-collection>
      <web-resource-name>sc1.c2</web-resource-name>
         <url-pattern>*.asp</url-pattern>
      </web-resource-collection>
      <auth-constraint/>
   </security-constraint>
   */
   private void addSC1(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = sc1.c1
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("sc1.c1");
      wrc.addPattern("/a/*");
      wrc.addPattern("/b/*");
      wrc.addPattern("/a");
      wrc.addPattern("/b");
      wrc.addHttpMethod("DELETE");
      wrc.addHttpMethod("PUT");

      wrc = wsmd.addWebResource("sc1.c2");
      wrc.addPattern("*.asp");

      wsmd.setExcluded(true);      
   }

   /*
   <security-constraint>
      <web-resource-collection>
         <web-resource-name>sc2.c1</web-resource-name>
         <url-pattern>/a/*</url-pattern>
         <url-pattern>/b/*</url-pattern>
         <http-method>GET</http-method>
      </web-resource-collection>
      <web-resource-collection>
         <web-resource-name>sc2.c2</web-resource-name>
         <url-pattern>/b/*</url-pattern>
         <http-method>POST</http-method>
      </web-resource-collection>
      <auth-constraint>
         <role-name>R1</role-name>
      </auth-constraint>
      <user-data-constraint>
         <transport-guarantee>CONFIDENTIAL</transport-guarantee>
      </user-data-constraint>
   </security-constraint>
   */
   private void addSC2(List securityContraints)
   {
      WebSecurityMetaData wsmd = new WebSecurityMetaData();
      securityContraints.add(wsmd);
      // web-resource-collection/web-resource-name = sc1.c1
      WebSecurityMetaData.WebResourceCollection wrc = wsmd.addWebResource("sc2.c1");
      wrc.addPattern("/a/*");
      wrc.addPattern("/b/*");
      wrc.addHttpMethod("GET");

      wrc = wsmd.addWebResource("sc2.c2");
      wrc.addPattern("/b/*");
      wrc.addHttpMethod("POST");

      wsmd.addRole("R1");
      wsmd.setTransportGuarantee("CONFIDENTIAL");
   }
}
