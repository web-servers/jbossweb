<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Arrays" %>
<%@ page language="java" session="false"  pageEncoding="UTF-8" contentType="text/html" %>
<html>
<head>
    <title>Login page for UTF-8 URL-param decoding problem</title>

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta http-equiv="Expires" content="0">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Cache-Control" content="no-cache">
</head>
<body>
<form method="POST" action="j_security_check">
   <input type="text" name="j_username">
   <input type="text" name="j_password">
   <input type="submit" value="Log in">
</form>
</body>
</html>
