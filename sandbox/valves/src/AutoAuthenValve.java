/*
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 * @version $Revision: 420067 $, $Date: 2006-07-08 09:16:58 +0200 (sub, 08 srp 2006) $
 */

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.authenticator.Constants;

import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;

/*
 * Valve to prevent the Form of a Form based application to
 * be displayed.
 */

public class AutoAuthenValve
    extends ValveBase {
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        Principal principal = request.getPrincipal();
        if ( principal != null) {
            System.out.println("Principal is : " + principal);
            getNext().invoke(request, response);
            return;
        }

        Session sess = request.getSessionInternal(false);
        if (sess != null) {
          System.out.println("session already exists: " + sess);
          principal = sess.getPrincipal();
          if (principal != null) {
            System.out.println("principal already exists: " + principal);
            getNext().invoke(request, response);
            return;
          }
        }

        String path = request.getContextPath();
        System.out.println("ContextPath: " + path);

        String username = request.getParameter(Constants.FORM_USERNAME);
        String password = request.getParameter(Constants.FORM_PASSWORD);

        Context context =  request.getContext();
        Realm realm = context.getRealm();
        principal = realm.authenticate(username, password);
        System.out.println("New Principal is : " + principal);
        if (principal == null) {
            getNext().invoke(request, response);
            return;
        }
        request.setAuthType("BASIC");
        request.setUserPrincipal(principal);

        if (sess == null) {
            sess = request.getSessionInternal(true);
        }
        System.out.println("session is : " + sess);
        sess.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
        sess.setNote(Constants.SESS_USERNAME_NOTE, username);
        sess.setNote(Constants.SESS_PASSWORD_NOTE, password);

        getNext().invoke(request, response);
    }
}
