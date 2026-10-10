<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Cache-Control" content="no-store">
    <title>Access denied | User Management</title>
    <%@ include file="/WEB-INF/jsp/fragments/assets.jsp" %>
</head>
<body class="auth-body">
<main class="auth-card">
    <div class="brand-mark mb-3" aria-hidden="true">UM</div>
    <h1 class="h3">Access denied</h1>
    <p class="text-secondary">
        <c:out value="${adminName}"/> is signed in, but this account does not have administrator access.
    </p>
    <form method="post" action="${pageContext.request.contextPath}/logout">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <button class="btn btn-primary" type="submit">Log out</button>
    </form>
</main>
</body>
</html>
