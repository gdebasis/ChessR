<%-- 
    Document   : index
    Created on : 08-Jan-2014, 14:30:54
    Author     : Debasis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<HTML>
<HEAD>
<META NAME="author" content="Debasis Ganguly">
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=iso-8859-1">
<title>Chess Position Searcher</title>

<%
    String displayAlert = request.getParameter("displayAlert");
    if (displayAlert != null) {
%>

    <script type="text/javascript">
        alert("Thank you for providing the relevance assessments. You can enter a new query now!");
    </script>
<%        
    }
%>
</HEAD>

<FRAMESET cols="*" border=1 frameborder=0 framespacing=0>
<frame src="pgnedit.htm?AllowRecording=true" name="board" scrolling=auto>
</FRAMESET>

</HTML>

