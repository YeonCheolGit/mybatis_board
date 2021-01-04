<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="contextPath"  value="${pageContext.request.contextPath}"  />
<script src="//cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
<html>
<head>
    <script type="text/javascript">
        $(document).ready(function(){
            $("#logoutBtn").on("click", function(){
                location.href="${contextPath}/member/logout";
            })
        })
    </script>
    <title>Title</title>
</head>
<body>
    <form name='homeForm' method="post" action="${contextPath}/member/login">
        <div>
            <label for="id"></label>
            <input type="text" id="id" name="id">
        </div>
        <div>
            <label for="pwd"></label>
            <input type="password" id="pwd" name="pwd">
        </div>
        <div>
            <button type="submit">로그인</button>
        </div>
            <c:if test="${msg == false}">
                <p style="color: #ff0000;">아이디와 비밀번호를 다시 확인해주세요.</p>
            </c:if>
    </form>
</body>
</html>