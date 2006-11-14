/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.protocol.ajp;

import java.nio.ByteBuffer;
import java.lang.IndexOutOfBoundsException;
import java.lang.NoSuchMethodException;
import java.util.Hashtable;
import java.util.Locale;

/**
 * AJP Headers
 *
 * @author Mladen Turk
 */
public final class Headers
{

    private Headers()
    {
        // Private to prevent creation.
    }

    private static final String[] requestHeaders = {
        "Accept",
        "Accept-Charset",
        "Accept-Encoding",
        "Accept-Language",
        "Authorization",
        "Connection",
        "Content-Type",
        "Content-Length",
        "Cookie",
        "Cookie2",
        "Host",
        "Pragma",
        "Referer",
        "User-Agent",

        /* AJP14 Headers */
        "Allow",
        "Cache-Control",
        "Content-Encoding",
        "Content-Language",
        "Content-Location",
        "Content-MD5",
        "Content-Range",
        "Date",
        "Expect",
        "Expires",
        "From",
        "If-Match",
        "If-Modified-Since",
        "If-None-Match",
        "If-Range",
        "If-Unmodified-Since",
        "Keep-Alive",
        "Last-Modified",
        "Max-Forwards",
        "Proxy-Authorization",
        "Range",
        "Trailer",
        "Transfer-Encoding",
        "TE",
        "Upgrade",
        "Via",
        "Warning"
    };


    private static final String[] responseHeaders = {
        "Content-Type",
        "Content-Language",
        "Content-Length",
        "Date",
        "Last-Modified",
        "Location",
        "Set-Cookie",
        "Set-Cookie2",
        "Servlet-Engine",
        "Status",
        "WWW-Authenticate",

        /* AJP14 Headers */
        "Accept-Ranges",
        "Age",
        "Allow",
        "Cache-Control",
        "Connection",
        "Content-Encoding",
        "Content-Location",
        "Content-MD5",
        "Content-Range",
        "ETag",
        "Expires",
        "Keep-Alive",
        "Pragma",
        "Proxy-Authenticate",
        "Retry-After",
        "Server",
        "Trailer",
        "Transfer-Encoding",
        "Upgrade",
        "Via",
        "Vary",
        "Warning"
    };

    private static final String[] httpMethods = {
        "OPTIONS",
        "GET",
        "HEAD",
        "POST",
        "PUT",
        "DELETE",
        "TRACE",
        "PROPFIND",
        "PROPPATCH",
        "MKCOL",
        "COPY",
        "MOVE",
        "LOCK",
        "UNLOCK",
        "ACL",
        "REPORT",
        "VERSION-CONTROL",
        "CHECKIN",
        "CHECKOUT",
        "UNCHECKOUT",
        "SEARCH",
        "MKWORKSPACE",
        "UPDATE",
        "LABEL",
        "MERGE",
        "BASELINE-CONTROL",
        "MKACTIVITY"
    };
    
    public static final int SC_RR_OFFSET                = 0xA0000;

    /*
     * Frequent request headers, these headers are coded as numbers
     * instead of strings.
     */
    public static final int SC_REQ_ACCEPT               = 1;
    public static final int SC_REQ_ACCEPT_CHARSET       = 2;
    public static final int SC_REQ_ACCEPT_ENCODING      = 3;
    public static final int SC_REQ_ACCEPT_LANGUAGE      = 4;
    public static final int SC_REQ_AUTHORIZATION        = 5;
    public static final int SC_REQ_CONNECTION           = 6;
    public static final int SC_REQ_CONTENT_TYPE         = 7;
    public static final int SC_REQ_CONTENT_LENGTH       = 8;
    public static final int SC_REQ_COOKIE               = 9;
    public static final int SC_REQ_COOKIE2              = 10;
    public static final int SC_REQ_HOST                 = 11;
    public static final int SC_REQ_PRAGMA               = 12;
    public static final int SC_REQ_REFERER              = 13;
    public static final int SC_REQ_USER_AGENT           = 14;
    public static final int SC_REQ_AJP13_MAX            = 14;

    public static final int SC_REQ_ALLOW                = 15;
    public static final int SC_REQ_CACHE_CONTROL        = 16;
    public static final int SC_REQ_CONTENT_ENCODING     = 17;
    public static final int SC_REQ_CONTENT_LANGUAGE     = 18;
    public static final int SC_REQ_CONTENT_LOCATION     = 19;
    public static final int SC_REQ_CONTENT_MD5          = 20;
    public static final int SC_REQ_CONTENT_RANGE        = 21;
    public static final int SC_REQ_DATE                 = 22;
    public static final int SC_REQ_EXPECT               = 23;
    public static final int SC_REQ_EXPIRES              = 24;
    public static final int SC_REQ_FROM                 = 25;
    public static final int SC_REQ_IF_MATCH             = 26;
    public static final int SC_REQ_IF_MODIFIED_SINCE    = 27;
    public static final int SC_REQ_IF_NONE_MATCH        = 28;
    public static final int SC_REQ_IF_RANGE             = 29;
    public static final int SC_REQ_IF_UNMODIFIED_SINCE  = 30;
    public static final int SC_REQ_KEEP_ALIVE           = 31;
    public static final int SC_REQ_LAST_MODIFIED        = 32;
    public static final int SC_REQ_MAX_FORWARDS         = 33;
    public static final int SC_REQ_PROXY_AUTHORIZATION  = 34;
    public static final int SC_REQ_RANGE                = 35;
    public static final int SC_REQ_TRAILER              = 36;
    public static final int SC_REQ_TRANSFER_ENCODING    = 37;
    public static final int SC_REQ_TE                   = 38;
    public static final int SC_REQ_UPGRADE              = 39;
    public static final int SC_REQ_VIA                  = 40;
    public static final int SC_REQ_WARNING              = 41;
    public static final int SC_REQ_AJP14_MAX            = 41;

    /*
     * Frequent response headers, these headers are coded as numbers
     * instead of strings.
     */
    public static final int SC_RESP_CONTENT_TYPE        = 1;
    public static final int SC_RESP_CONTENT_LANGUAGE    = 2;
    public static final int SC_RESP_CONTENT_LENGTH      = 3;
    public static final int SC_RESP_DATE                = 4;
    public static final int SC_RESP_LAST_MODIFIED       = 5;
    public static final int SC_RESP_LOCATION            = 6;
    public static final int SC_RESP_SET_COOKIE          = 7;
    public static final int SC_RESP_SET_COOKIE2         = 8;
    public static final int SC_RESP_SERVLET_ENGINE      = 9;
    public static final int SC_RESP_STATUS              = 10;
    public static final int SC_RESP_WWW_AUTHENTICATE    = 11;
    public static final int SC_RESP_AJP13_MAX           = 11;


    public static final int SC_RESP_ACCEPT_RANGES       = 12;
    public static final int SC_RESP_AGE                 = 13;
    public static final int SC_RESP_ALLOW               = 14;
    public static final int SC_RESP_CACHE_CONTROL       = 15;
    public static final int SC_RESP_CONNECTION          = 16;
    public static final int SC_RESP_CONTENT_ENCODING    = 17;
    public static final int SC_RESP_CONTENT_LOCATION    = 18;
    public static final int SC_RESP_CONTENT_MD5         = 19;
    public static final int SC_RESP_CONTENT_RANGE       = 20;
    public static final int SC_RESP_ETAG                = 21;
    public static final int SC_RESP_EXPIRES             = 22;
    public static final int SC_RESP_KEEP_ALIVE          = 23;
    public static final int SC_RESP_PRAGMA              = 24;
    public static final int SC_RESP_PROXY_AUTHENTICATE  = 25;
    public static final int SC_RESP_RETRY_AFTER         = 26;
    public static final int SC_RESP_SERVER              = 27;
    public static final int SC_RESP_TRAILER             = 28;
    public static final int SC_RESP_TRANSFER_ENCODING   = 29;
    public static final int SC_RESP_UPGRADE             = 30;
    public static final int SC_RESP_VIA                 = 31;
    public static final int SC_RESP_VARY                = 32;
    public static final int SC_RESP_WARNING             = 33;
    public static final int SC_RESP_AJP14_MAX           = 33;

    /*
     * Request methods, coded as numbers instead of strings.
     * The list of methods was taken from Section 5.1.1 of RFC 2616,
     * RFC 2518, the ACL IETF draft, and the DeltaV IESG Proposed Standard.
     */
    public static final byte SC_M_OPTIONS               = 1;
    public static final byte SC_M_GET                   = 2;
    public static final byte SC_M_HEAD                  = 3;
    public static final byte SC_M_POST                  = 4;
    public static final byte SC_M_PUT                   = 5;
    public static final byte SC_M_DELETE                = 6;
    public static final byte SC_M_TRACE                 = 7;
    public static final byte SC_M_PROPFIND              = 8;
    public static final byte SC_M_PROPPATCH             = 9;
    public static final byte SC_M_MKCOL                 = 10;
    public static final byte SC_M_COPY                  = 11;
    public static final byte SC_M_MOVE                  = 12;
    public static final byte SC_M_LOCK                  = 13;
    public static final byte SC_M_UNLOCK                = 14;
    public static final byte SC_M_ACL                   = 15;
    public static final byte SC_M_REPORT                = 16;
    public static final byte SC_M_VERSION_CONTROL       = 17;
    public static final byte SC_M_CHECKIN               = 18;
    public static final byte SC_M_CHECKOUT              = 19;
    public static final byte SC_M_UNCHECKOUT            = 20;
    public static final byte SC_M_SEARCH                = 21;
    public static final byte SC_M_MKWORKSPACE           = 22;
    public static final byte SC_M_UPDATE                = 23;
    public static final byte SC_M_LABEL                 = 24;
    public static final byte SC_M_MERGE                 = 25;
    public static final byte SC_M_BASELINE_CONTROL      = 26;
    public static final byte SC_M_MKACTIVITY            = 27;
    public static final byte SC_M_MAX                   = 27;

    /*
     * The method name is provided via SC_A_STORED_METHOD
     * attribute.
     */
    public static final byte SC_M_STORED                = (byte)0xFF;


    /*
     * Conditional request attributes
     */
    public static final byte SC_A_CONTEXT               = 1;
    public static final byte SC_A_SERVLET_PATH          = 2;
    public static final byte SC_A_REMOTE_USER           = 3;
    public static final byte SC_A_AUTH_TYPE             = 4;
    public static final byte SC_A_QUERY_STRING          = 5;
    public static final byte SC_A_JVM_ROUTE             = 6;
    public static final byte SC_A_SSL_CERT              = 7;
    public static final byte SC_A_SSL_CIPHER            = 8;
    public static final byte SC_A_SSL_SESSION           = 9;
    public static final byte SC_A_REQ_ATTRIBUTE         = 10;
    public static final byte SC_A_SSL_KEYSIZE           = 11;
    public static final byte SC_A_SECRET                = 12;
    public static final byte SC_A_STORED_METHOD         = 13;


    /* Terminates list of attributes
     */
    public static final byte SC_A_ARE_DONE              = (byte)0xFF;

    // /*  Java 1.4 declarations
    private static final Hashtable  requestHeadersHash  = new Hashtable(50);
    private static final Hashtable  responseHeadersHash = new Hashtable(40);    
    private static final Hashtable  httpMethodsHash     = new Hashtable(30);
    // */
    /* Java 1.5 declarations
    private static final Hashtable<String,Integer>  requestHeadersHash  = new Hashtable<String,Integer>(50);
    private static final Hashtable<String,Integer>  responseHeadersHash = new Hashtable<String,Integer>(40);
    private static final Hashtable<String,Integer>  httpMethodsHash     = new Hashtable<String,Integer>(30);
    */

    static {
        try {
            int i;
            for (i = 0; i < SC_REQ_AJP14_MAX; i++) {
                requestHeadersHash.put(requestHeaders[i].toLowerCase(Locale.US),
                                       new Integer(i));
            }
            for (i = 0; i < SC_RESP_AJP14_MAX; i++) {
                responseHeadersHash.put(responseHeaders[i].toLowerCase(Locale.US),
                                        new Integer(i));
            }
            for (i = 0; i < SC_M_MAX; i++) {
                httpMethodsHash.put(httpMethods[i], new Integer(i));
            }
        }
        catch (Exception e) {
            // Do nothing
        }
    }    

    public static int getRequestHeaderIndex(String header)
    {
        Integer i = (Integer)requestHeadersHash.get(header.toLowerCase(Locale.US));
        if (i == null)
            return 0;
        else
            return i.intValue();
    }

    public static int getAjp13RequestHeaderIndex(String header)
    {
        int i = getRequestHeaderIndex(header);
        if (i > SC_REQ_AJP13_MAX)
            return 0;
        else
            return i;
    }

    public static int getResponseHeaderIndex(String header)
    {
        Integer i = (Integer)responseHeadersHash.get(header.toLowerCase(Locale.US));
        if (i == null)
            return 0;
        else
            return i.intValue();
    }

    public static int getAjp13ResponseHeaderIndex(String header)
    {
        int i = getResponseHeaderIndex(header);
        if (i > SC_RESP_AJP13_MAX)
            return 0;
        else
            return i;
    }

    public static String getRequestHeaderName(int index)
                         throws IndexOutOfBoundsException
    {
        if (index < 1 || index > SC_REQ_AJP14_MAX) {
            throw new IndexOutOfBoundsException();
        }
        else {
            return requestHeaders[index];
        }
    }

    public static String getResponseHeaderName(int index)
                         throws IndexOutOfBoundsException
    {
        if (index < 1 || index > SC_RESP_AJP14_MAX) {
            throw new IndexOutOfBoundsException();
        }
        else {
            return responseHeaders[index];
        }
    }

    public static byte getHttpMethodIndex(String method)
    {
        Integer i = (Integer)httpMethodsHash.get(method.toUpperCase(Locale.US));
        if (i == null)
            return 0;
        else
            return i.byteValue();
    }

    public static String getHttpMethodName(int index)
                         throws NoSuchMethodException
    {
        if (index < 1 || index > SC_M_MAX) {
            if (index == SC_M_STORED)
                return null;
            else
                throw new NoSuchMethodException();
        }
        else {
            return httpMethods[index];
        }
    }

    public static String getStatusMessage(int code)
    {
        switch (code) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 102: return "Processing";
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            case 207: return "Multi-Status";
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            case 305: return "Use Proxy";
            case 307: return "Temporary Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Request Entity Too Large";
            case 414: return "Request-Uri Too Long";
            case 415: return "Unsupported Media Type";
            case 416: return "Requested Range Not Satisfiable";
            case 417: return "Expectation Failed";
            case 422: return "Unprocessable Entity";
            case 423: return "Locked";
            case 424: return "Failed Dependency";
            case 426: return "Upgrade Required";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "Http Version Not Supported";
            case 507: return "Insufficient Storage";
            case 510: return "Not Extended";
            default: return "";
        }
    }

}
