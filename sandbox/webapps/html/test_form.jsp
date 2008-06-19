<html>
<head>
    <title>Form test Example</title>
</head>
<body>

<%
String value = request.getParameter("name");
if (value != null) {
%>
    <h1>Data Received at the Server</h1>

Name <%= value %><br/>
<%
} else {
%>
<b> Nothing received</b>
<form name="myform" action="test_form.jsp" method="post">
    Specify your name:<br />
      <input type="text" name="name" size="15"/><br/>
      <input type="submit" name="Submit" value="Submit your files"/>
</form>
<%
}
%>

</body>
</html>
