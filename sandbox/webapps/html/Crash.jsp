<body>
<h1> Test 1 computing </h1>
<%

 double b = 0;
 for(int i=1; i<200000; i++)
 {
   for(int j=1; j<2000000; j++)
   {
     for(int k=1; k<2000000; k++)
     {
       double a = k*100000;
       b +=a;
     }
   }
%>x<%
 }

%>
</body>
</html>
