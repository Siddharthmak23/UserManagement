<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in | User Management</title>
    <%@ include file="/WEB-INF/jsp/fragments/assets.jsp" %>
</head>
<body class="auth-body">
<main class="auth-card">
    <div class="d-flex align-items-center gap-3 mb-4">
        <div class="brand-mark" aria-hidden="true">UM</div>
        <div>
            <div class="fw-semibold">User Management</div>
            <div class="text-secondary small">Administrator sign in</div>
        </div>
    </div>
    <h1 class="h3 mb-2">Welcome back</h1>
    <p class="text-secondary mb-4">Sign in with your administrator account to manage users.</p>

    <c:if test="${param.error != null}">
        <div class="alert alert-danger" role="alert">Invalid email or password.</div>
    </c:if>
    <c:if test="${param.logout != null}">
        <div class="alert alert-success" role="status">You have been logged out.</div>
    </c:if>
    <c:if test="${param.expired != null}">
        <div class="alert alert-warning" role="status">Your session has expired. Please sign in again.</div>
    </c:if>

    <form id="loginForm" method="post" action="${pageContext.request.contextPath}/login" novalidate>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <div class="mb-3">
            <label class="form-label" for="email">Email</label>
            <input class="form-control" id="email" name="email" type="email" autocomplete="username"
                   maxlength="150" required autofocus>
        </div>
        <div class="mb-4">
            <label class="form-label" for="password">Password</label>
            <div class="input-group">
                <input class="form-control" id="password" name="password" type="password"
                       autocomplete="current-password" maxlength="72" required>
                <button class="btn btn-outline-secondary" id="togglePassword" type="button">Show</button>
            </div>
        </div>
        <button class="btn btn-primary w-100" id="loginButton" type="submit">Sign in</button>
    </form>
</main>
<script src="${pageContext.request.contextPath}/webjars/jquery/3.7.1/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/js/login.js"></script>
</body>
</html>
