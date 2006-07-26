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
package org.jboss.web.tomcat.security;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.realm.Constants;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.jboss.security.CertificatePrincipal;
import org.jboss.security.RealmMapping;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.SubjectSecurityManager;
import org.jboss.security.auth.certs.SubjectDNMapping;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;

/**
 * An implementation of the catelinz Realm and Valve interfaces. The Realm
 * implementation handles authentication and authorization using the JBossSX
 * security framework. It relieas on the JNDI ENC namespace setup by the
 * AbstractWebContainer. In particular, it uses the java:comp/env/security
 * subcontext to access the security manager interfaces for authorization and
 * authenticaton. <p/> The Valve interface is used to associated the
 * authenticated user with the SecurityAssociation class when a request begins
 * so that web components may call EJBs and have the principal propagated. The
 * security association is removed when the request completes.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.23 $
 * @see org.jboss.security.AuthenticationManager
 * @see org.jboss.security.CertificatePrincipal
 * @see org.jboss.security.RealmMapping
 * @see org.jboss.security.SimplePrincipal
 * @see org.jboss.security.SecurityAssociation
 * @see org.jboss.security.SubjectSecurityManager
 */
public class JBossSecurityMgrRealm extends RealmBase implements Realm
{
   static Logger log = Logger.getLogger(JBossSecurityMgrRealm.class);
   /**
    * The converter from X509 cert chain to Princpal
    */
   private CertificatePrincipal certMapping = new SubjectDNMapping();
   /**
    * The JBossSecurityMgrRealm category trace flag
    */
   private boolean trace;
   /** The mode for handling the all roles mode of role-name=* */
   private AllRolesMode allRolesMode = AllRolesMode.AUTH_ONLY_MODE;

   /**
    * Set the class name of the CertificatePrincipal used for mapping X509 cert
    * chains to a Princpal.
    *
    * @param className the CertificatePrincipal implementation class that must
    *                  have a no-arg ctor.
    * @see org.jboss.security.CertificatePrincipal
    */
   public void setCertificatePrincipal(String className)
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class cpClass = loader.loadClass(className);
         certMapping = (CertificatePrincipal) cpClass.newInstance();
      }
      catch (Exception e)
      {
         log.error("Failed to load CertificatePrincipal: " + className, e);
         certMapping = new SubjectDNMapping();
      }
   }

   private Context getSecurityContext()
   {
      Context securityCtx = null;
      // Get the JBoss security manager from the ENC context
      try
      {
         InitialContext iniCtx = new InitialContext();
         securityCtx = (Context) iniCtx.lookup("java:comp/env/security");
      }
      catch (NamingException e)
      {
         // Apparently there is no security context?
      }
      return securityCtx;
   }

   /**
    * Override to allow a single realm to be shared as a realm and valve
    */
   public void start() throws LifecycleException
   {
      if (super.started == true)
      {
         return;
      }
      super.start();
      trace = log.isTraceEnabled();
   }

   /**
    * Override to allow a single realm to be shared as a realm and valve
    */
   public void stop() throws LifecycleException
   {
      if (super.started == false)
      {
         return;
      }
      super.stop();
   }

   public boolean hasResourcePermission(Request request, Response response,
      SecurityConstraint[] constraints, org.apache.catalina.Context context)
      throws IOException
   {
      if (constraints == null || constraints.length == 0)
      {
         return (true);
      }

      boolean hasPermission = false;
      // Specifically allow access to the form login and form error pages
      // and the "j_security_check" action
      LoginConfig config = context.getLoginConfig();
      if ((config != null) &&
         (Constants.FORM_METHOD.equals(config.getAuthMethod())))
      {
         String requestURI = request.getRequestPathMB().toString();
         String loginPage = config.getLoginPage();
         if (loginPage.equals(requestURI))
         {
            if( trace )
               log.trace("Allow access to login page " + loginPage);
            return (true);
         }
         String errorPage = config.getErrorPage();
         if (errorPage.equals(requestURI))
         {
            if( trace )
               log.trace("Allow access to error page " + errorPage);
            return (true);
         }
         if (requestURI.endsWith(Constants.FORM_ACTION))
         {
            if( trace )
               log.trace("Allow access to username/password submission");
            return (true);
         }
      }

      // Which user principal have we already authenticated?
      Principal principal = request.getPrincipal();
      boolean denyfromall = false;
      for (int i = 0; i < constraints.length; i++)
      {
         SecurityConstraint constraint = constraints[i];

         String roles[];
         if (constraint.getAllRoles())
         {
            // * means all roles defined in web.xml
            roles = request.getContext().findSecurityRoles();
         }
         else
         {
            roles = constraint.findAuthRoles();
         }

         if (roles == null)
         {
            roles = new String[0];
         }

         if( trace )
            log.trace("Checking roles " + principal);

         if (roles.length == 0 && !constraint.getAllRoles())
         {
            if (constraint.getAuthConstraint())
            {
               if( trace )
                  log.trace("No roles");
               hasPermission = false; // No listed roles means no access at all
               denyfromall = true;
            }
            else
            {
               if( trace )
                  log.trace("Passing all access");
               return (true);
            }
         }
         else if (principal == null)
         {
            if( trace )
               log.trace("No user authenticated, cannot grant access");
            hasPermission = false;
         }
         else if (!denyfromall)
         {
            for (int j = 0; j < roles.length; j++)
            {
               if (hasRole(principal, roles[j]))
               {
                  hasPermission = true;
               }
               if( trace )
                  log.trace("No role found:  " + roles[j]);
            }
         }
      }

      if (allRolesMode != AllRolesMode.STRICT_MODE
         && hasPermission == false
         && principal != null)
      {
         if (trace)
         {
            log.trace("Checking for all roles mode: " + allRolesMode);
         }
         // Check for an all roles(role-name="*")
         for (int i = 0; i < constraints.length; i++)
         {
            SecurityConstraint constraint = constraints[i];
            String roles[];
            // If the all roles mode exists, sets
            if (constraint.getAllRoles())
            {
               if (allRolesMode == AllRolesMode.AUTH_ONLY_MODE)
               {
                  if (trace)
                  {
                     log.trace("Granting access for role-name=*, auth-only");
                  }
                  hasPermission = true;
                  break;
               }

               // For AllRolesMode.STRICT_AUTH_ONLY_MODE there must be zero roles
               roles = request.getContext().findSecurityRoles();
               if (roles.length == 0 && allRolesMode == AllRolesMode.STRICT_AUTH_ONLY_MODE)
               {
                  if (trace)
                  {
                     log.trace("Granting access for role-name=*, strict auth-only");
                  }
                  hasPermission = true;
                  break;
               }
            }
         }
      }

      // Return a "Forbidden" message denying access to this resource
      if (!hasPermission)
      {
         response.sendError
            (HttpServletResponse.SC_FORBIDDEN,
               sm.getString("realmBase.forbidden"));
      }
      return hasPermission;
   }

   /**
    * Return the Principal associated with the specified chain of X509 client
    * certificates.  If there is none, return <code>null</code>.
    *
    * @param certs Array of client certificates, with the first one in the array
    *              being the certificate of the client itself.
    */
   public Principal authenticate(X509Certificate[] certs)
   {
      Principal principal = null;
      Context securityCtx = getSecurityContext();
      if (securityCtx == null)
      {
         if (trace)
         {
            log.trace("No security context for authenticate(X509Certificate[])");
         }
         return null;
      }

      try
      {
         // Get the JBoss security manager from the ENC context
         SubjectSecurityManager securityMgr = (SubjectSecurityManager) securityCtx.lookup("securityMgr");
         Subject subject = new Subject();
         principal = certMapping.toPrinicipal(certs);
         if (securityMgr.isValid(principal, certs, subject))
         {
            if (trace)
            {
               log.trace("User: " + principal + " is authenticated");
            }
            SecurityAssociationActions.setPrincipalInfo(principal, certs, subject);
            // Get the CallerPrincipal mapping
            RealmMapping realmMapping = (RealmMapping) securityCtx.lookup("realmMapping");
            Principal oldPrincipal = principal;
            principal = realmMapping.getPrincipal(oldPrincipal);
            if (trace)
            {
               log.trace("Mapped from input principal: " + oldPrincipal
                  + "to: " + principal);
            }
            // Get the caching principal
            principal = getCachingPrincpal(realmMapping, oldPrincipal,
               principal, certs, subject);
         }
         else
         {
            if (trace)
            {
               log.trace("User: " + principal + " is NOT authenticated");
            }
            principal = null;
         }
      }
      catch (NamingException e)
      {
         log.error("Error during authenticate", e);
      }
      return principal;
   }

   /**
    * Return the Principal associated with the specified username, which matches
    * the digest calculated using the given parameters using the method
    * described in RFC 2069; otherwise return <code>null</code>.
    *
    * @param username Username of the Principal to look up
    * @param digest   Digest which has been submitted by the client
    * @param nonce    Unique (or supposedly unique) token which has been used for
    *                 this request
    * @param nc       client nonce reuse count
    * @param cnonce   client token
    * @param qop      quality of protection
    * @param realm    Realm name
    * @param md5a2    Second MD5 digest used to calculate the digest : MD5(Method +
    *                 ":" + uri)
    */
   public Principal authenticate(String username, String digest, String nonce,
      String nc, String cnonce, String qop, String realm, String md5a2)
   {
      Principal principal = null;
      Context securityCtx = getSecurityContext();
      if (securityCtx == null)
      {
         if (trace)
         {
            log.trace("No security context for authenticate(String, String)");
         }
         return null;
      }

      Principal caller = (Principal) SecurityAssociationValve.userPrincipal.get();
      if (caller == null && username == null && digest == null)
      {
         return null;
      }

      try
      {
         DigestCallbackHandler handler = new DigestCallbackHandler(username, nonce,
            nc, cnonce, qop, realm, md5a2);
         CallbackHandlerPolicyContextHandler.setCallbackHandler(handler);

         // Get the JBoss security manager from the ENC context
         SubjectSecurityManager securityMgr = (SubjectSecurityManager) securityCtx.lookup("securityMgr");
         principal = new SimplePrincipal(username);
         Subject subject = new Subject();
         if (securityMgr.isValid(principal, digest, subject))
         {
            log.trace("User: " + username + " is authenticated");
            SecurityAssociationActions.setPrincipalInfo(principal, digest, subject);
            // Get the CallerPrincipal mapping
            RealmMapping realmMapping = (RealmMapping) securityCtx.lookup("realmMapping");
            Principal oldPrincipal = principal;
            principal = realmMapping.getPrincipal(oldPrincipal);
            if (trace)
            {
               log.trace("Mapped from input principal: " + oldPrincipal
                  + "to: " + principal);
            }
            // Get the caching principal
            principal = getCachingPrincpal(realmMapping, oldPrincipal,
               principal, digest, subject);
         }
         else
         {
            principal = null;
            if (trace)
            {
               log.trace("User: " + username + " is NOT authenticated");
            }
         }
      }
      catch (NamingException e)
      {
         principal = null;
         log.error("Error during authenticate", e);
      }
      finally
      {
         CallbackHandlerPolicyContextHandler.setCallbackHandler(null);
      }
      if (trace)
      {
         log.trace("End authenticate, principal=" + principal);
      }
      return principal;
   }

   /**
    * Return the Principal associated with the specified username and
    * credentials, if there is one; otherwise return <code>null</code>.
    *
    * @param username    Username of the Principal to look up
    * @param credentials Password or other credentials to use in authenticating
    *                    this username
    */
   public Principal authenticate(String username, String credentials)
   {
      if (trace)
      {
         log.trace("Begin authenticate, username=" + username);
      }
      Principal principal = null;
      Context securityCtx = getSecurityContext();
      if (securityCtx == null)
      {
         if (trace)
         {
            log.trace("No security context for authenticate(String, String)");
         }
         return null;
      }

      Principal caller = (Principal) SecurityAssociationValve.userPrincipal.get();
      if (caller == null && username == null && credentials == null)
      {
         return null;
      }

      try
      {
         // Get the JBoss security manager from the ENC context
         SubjectSecurityManager securityMgr = (SubjectSecurityManager) securityCtx.lookup("securityMgr");
         principal = new SimplePrincipal(username);
         Subject subject = new Subject();
         if (securityMgr.isValid(principal, credentials, subject))
         {
            log.trace("User: " + username + " is authenticated");
            SecurityAssociationActions.setPrincipalInfo(principal, credentials, subject);
            // Get the CallerPrincipal mapping
            RealmMapping realmMapping = (RealmMapping) securityCtx.lookup("realmMapping");
            Principal oldPrincipal = principal;
            principal = realmMapping.getPrincipal(oldPrincipal);
            if (trace)
            {
               log.trace("Mapped from input principal: " + oldPrincipal
                  + "to: " + principal);
            }
            // Get the caching principal
            principal = getCachingPrincpal(realmMapping, oldPrincipal,
               principal, credentials, subject);
         }
         else
         {
            principal = null;
            if (trace)
            {
               log.trace("User: " + username + " is NOT authenticated");
            }
         }
      }
      catch (NamingException e)
      {
         principal = null;
         log.error("Error during authenticate", e);
      }
      if (trace)
      {
         log.trace("End authenticate, principal=" + principal);
      }
      return principal;
   }

   /**
    * Returns <code>true</code> if the specified user <code>Principal</code> has
    * the specified security role, within the context of this
    * <code>Realm</code>; otherwise return <code>false</code>. This will be true
    * when an associated role <code>Principal</code> can be found whose
    * <code>getName</code> method returns a <code>String</code> equalling the
    * specified role.
    *
    * @param principal <code>Principal</code> for whom the role is to be
    *                  checked
    * @param role      Security role to be checked
    */
   public boolean hasRole(Principal principal, String role)
   {
      return super.hasRole(principal, role);
      /*
      if ((principal == null) || (role == null))
      {
         return false;
      }
      if (principal instanceof JBossGenericPrincipal)
      {
         return super.hasRole(principal, role);
      }
      JBossGenericPrincipal gp = (JBossGenericPrincipal) roleMap.get(principal);
      Set userRoles = gp.getUserRoles();
      if (userRoles != null)
      {
         Iterator iter = userRoles.iterator();
         while (iter.hasNext())
         {
            Principal p = (Principal) iter.next();
            if (role.equals(p.getName()))
            {
               return true;
            }
         }
      }
      return false;
      */
   }

   /**
    * Return the Principal associated with the specified username and
    * credentials, if there is one; otherwise return <code>null</code>.
    *
    * @param username    Username of the Principal to look up
    * @param credentials Password or other credentials to use in authenticating
    *                    this username
    */
   public Principal authenticate(String username, byte[] credentials)
   {
      return authenticate(username, new String(credentials));
   }

   /**
    * Return a short name for this Realm implementation, for use in log
    * messages.
    */
   protected String getName()
   {
      return getClass().getName();
   }

   /**
    * Return the password associated with the given principal's user name.
    */
   protected String getPassword(String username)
   {
      String password = null;
      return password;
   }

   /**
    * Return the Principal associated with the given user name.
    */
   protected Principal getPrincipal(String username)
   {
      return new SimplePrincipal(username);
   }

   /**
    * Access the set of role Princpals associated with the given caller princpal.
    *
    * @param principal - the Principal mapped from the authentication principal
    *                  and visible from the HttpServletRequest.getUserPrincipal
    * @return a possible null Set<Principal> for the caller roles
    */
   protected Set getPrincipalRoles(Principal principal)
   {
      if( (principal instanceof GenericPrincipal) == false )
         throw new IllegalStateException("Expected GenericPrincipal, but saw: "+principal.getClass());
      GenericPrincipal gp = (GenericPrincipal) principal;
      String[] roleNames = gp.getRoles();
      Set userRoles = new HashSet();
      if( roleNames != null )
      {
         for(int n = 0; n < roleNames.length; n ++)
         {
            SimplePrincipal sp = new SimplePrincipal(roleNames[n]);
            userRoles.add(sp);
         }
      }
      return userRoles;
   }

   /**
    * Create the session principal tomcat will cache to avoid callouts to this
    * Realm.
    *
    * @param realmMapping    - the role mapping security manager
    * @param authPrincipal   - the principal used for authentication and stored in
    *                        the security manager cache
    * @param callerPrincipal - the possibly different caller principal
    *                        representation of the authenticated principal
    * @param credential      - the credential used for authentication
    * @return the tomcat session principal wrapper
    */
   protected Principal getCachingPrincpal(RealmMapping realmMapping,
      Principal authPrincipal, Principal callerPrincipal, Object credential,
      Subject subject)
   {
      // Cache the user roles in the principal
      Set userRoles = realmMapping.getUserRoles(authPrincipal);
      ArrayList roles = new ArrayList();
      if (userRoles != null)
      {
         Iterator iterator = userRoles.iterator();
         while (iterator.hasNext())
         {
            Principal role = (Principal) iterator.next();
            roles.add(role.getName());
         }
      }
      JBossGenericPrincipal gp = new JBossGenericPrincipal(this, subject,
         authPrincipal, callerPrincipal, credential, roles, userRoles);
      return gp;
   }
}
