<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Arrays" %>
<%@ page language="java" session="false"  pageEncoding="UTF-8" contentType="text/html" %>
<html>
<head>
    <title>Test page for UTF-8 URL-param decoding problem</title>

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta http-equiv="Expires" content="0">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Cache-Control" content="no-cache">
</head>
<body>
Character encoding of the request: <%=request.getCharacterEncoding()%> <br/>
Query Parameters: <%=request.getQueryString()%><br/>
Decoded Parameters
<table>
    <thead>
        <tr><tr><td>param name</td><td>value</td></tr>
    </thead>
    <tbody>
<%
    Enumeration parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
        String paramName = (String) parameterNames.nextElement();
        String[] paramValues = request.getParameterValues(paramName);
        String paramValuesString = Arrays.asList(paramValues).toString();
%>
    <tr>
        <td><%=paramName%></td><td><%=paramValuesString%></td>
    </tr>
<%
        }
%>
    </tbody>
</table>
</body>
</html>
