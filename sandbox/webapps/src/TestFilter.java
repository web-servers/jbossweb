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

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;



/*
 * Test filter use do=option as parameter to test the behaviour.
 */
public class TestFilter implements Filter {

    protected FilterConfig filterConfig;

/**
* init() : init() method called when the filter is instantiated.
*/
public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
}


/**
* destroy() : destroy() method called when the filter is taken
* out of service.
*/
public void destroy() {
    this.filterConfig = null;
}

/**
* doFilter() : doFilter() method called before the servlet to
* which this filter is mapped is invoked.
*/
public void doFilter(ServletRequest request, ServletResponse response,
                     FilterChain chain) throws java.io.IOException,
                                               ServletException {


    HttpServletRequest req = (HttpServletRequest)request;
    HttpServletResponse res = (HttpServletResponse)response;

    // get action
    String action = req.getParameter("do");
    if (action == null) {
        chain.doFilter(request, response);
        return;
    }

    // and process it.
    if (action.equalsIgnoreCase("Nothing")) {
        return;
    } else if (action.equalsIgnoreCase("Error")) {
        res.sendError(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }

    // call next filter in the chain.
    chain.doFilter(request, response);
}
}
