<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Cache-Control" content="no-store">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <title>Dashboard | User Management</title>
    <%@ include file="/WEB-INF/jsp/fragments/assets.jsp" %>
</head>
<body data-user-id="${currentUserId}" data-context-path="${pageContext.request.contextPath}">
<aside class="sidebar">
    <div class="d-flex align-items-center gap-2 mb-4">
        <div class="brand-mark" aria-hidden="true">UM</div>
        <div>
            <div class="fw-semibold">User Management</div>
            <div class="small text-white-50">Admin console</div>
        </div>
    </div>
    <nav aria-label="Primary">
        <a class="nav-link active" href="${pageContext.request.contextPath}/home">
            <i class="bi bi-grid me-2" aria-hidden="true"></i>Dashboard
        </a>
    </nav>
    <div class="sidebar-user">
        <div class="fw-semibold text-white"><c:out value="${adminName}"/></div>
        <div class="small"><c:out value="${adminEmail}"/></div>
    </div>
</aside>

<div class="offcanvas offcanvas-start text-bg-dark" tabindex="-1" id="mobileNav" aria-labelledby="mobileNavLabel">
    <div class="offcanvas-header">
        <h2 class="offcanvas-title h5" id="mobileNavLabel">User Management</h2>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="offcanvas" aria-label="Close"></button>
    </div>
    <div class="offcanvas-body">
        <a class="nav-link text-white" href="${pageContext.request.contextPath}/home">Dashboard</a>
        <div class="mt-4 small text-white-50">
            <div><c:out value="${adminName}"/></div>
            <div><c:out value="${adminEmail}"/></div>
        </div>
    </div>
</div>

<div class="app-shell">
    <header class="topbar">
        <div class="d-flex align-items-center gap-2 min-w-0">
            <button class="btn btn-outline-secondary d-lg-none" type="button" data-bs-toggle="offcanvas"
                    data-bs-target="#mobileNav" aria-controls="mobileNav" aria-label="Open navigation">
                <i class="bi bi-list" aria-hidden="true"></i>
            </button>
            <div class="min-w-0">
                <div class="fw-semibold d-lg-none">User Management</div>
                <div class="text-secondary small text-truncate">Signed in as <c:out value="${adminEmail}"/></div>
            </div>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button class="btn btn-outline-secondary" type="submit">
                <i class="bi bi-box-arrow-right me-1" aria-hidden="true"></i>Log out
            </button>
        </form>
    </header>

    <main class="content">
        <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
            <div>
                <h1 class="h3 page-title mb-1">Welcome back, <span id="welcomeName"><c:out value="${adminName}"/></span></h1>
                <p class="text-secondary mb-0">Review accounts, then add, update, or remove users.</p>
            </div>
            <button class="btn btn-primary" id="addUserButton" type="button">
                <i class="bi bi-person-plus me-1" aria-hidden="true"></i>Add user
            </button>
        </div>

        <section class="row g-3 mb-4" aria-label="Summary">
            <div class="col-md-4">
                <article class="stat-card">
                    <div class="label">Total users</div>
                    <p class="value" id="statTotal">—</p>
                </article>
            </div>
            <div class="col-md-4">
                <article class="stat-card">
                    <div class="label">Administrators</div>
                    <p class="value" id="statAdmins">—</p>
                </article>
            </div>
            <div class="col-md-4">
                <article class="stat-card">
                    <div class="label">Standard users</div>
                    <p class="value" id="statUsers">—</p>
                </article>
            </div>
        </section>

        <section class="panel" aria-labelledby="directoryHeading">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 p-3">
                <h2 class="h5 mb-0" id="directoryHeading">User directory</h2>
                <form id="searchForm" class="d-flex flex-wrap gap-2" role="search">
                    <label class="visually-hidden" for="searchInput">Search by name or email</label>
                    <input class="form-control" id="searchInput" maxlength="100" placeholder="Search by name or email">
                    <button class="btn btn-outline-secondary" type="submit">Search</button>
                    <button class="btn btn-link text-decoration-none" id="clearSearch" type="button">Clear</button>
                </form>
            </div>
            <div class="table-wrap">
                <div class="loading-overlay d-none" id="tableLoading">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading users</span>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table user-table mb-0">
                        <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Name</th>
                            <th scope="col">Email</th>
                            <th scope="col">Phone</th>
                            <th scope="col">Role</th>
                            <th scope="col">Created</th>
                            <th scope="col">Actions</th>
                        </tr>
                        </thead>
                        <tbody id="userTableBody">
                        <tr>
                            <td colspan="7" class="text-center text-secondary py-5">Loading users…</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 p-3">
                <p class="text-secondary small mb-0" id="resultSummary" aria-live="polite"></p>
                <nav aria-label="User pages">
                    <ul class="pagination pagination-sm mb-0" id="pagination"></ul>
                </nav>
            </div>
        </section>
    </main>
</div>

<div class="modal fade" id="userModal" tabindex="-1" aria-labelledby="userModalTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form id="userForm" novalidate>
                <div class="modal-header">
                    <h2 class="modal-title h5" id="userModalTitle">Add user</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="userId">
                    <div class="mb-3">
                        <label class="form-label" for="userName">Full name</label>
                        <input class="form-control" id="userName" data-field="name" maxlength="100" required autocomplete="name">
                        <div class="invalid-feedback"></div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="userEmail">Email</label>
                        <input class="form-control" id="userEmail" data-field="email" type="email" maxlength="150" required autocomplete="email">
                        <div class="invalid-feedback"></div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="userPhone">Phone</label>
                        <input class="form-control" id="userPhone" data-field="phone" type="tel" maxlength="20" required autocomplete="tel">
                        <div class="form-text">7 to 15 digits. Spaces, plus signs, and dashes are allowed.</div>
                        <div class="invalid-feedback"></div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="userPassword">Password</label>
                        <input class="form-control" id="userPassword" data-field="password" type="password" maxlength="72" autocomplete="new-password">
                        <div class="form-text" id="passwordHelp">At least 8 characters, with uppercase, lowercase, and a number.</div>
                        <div class="invalid-feedback"></div>
                    </div>
                    <div class="mb-1">
                        <label class="form-label" for="userRole">Role</label>
                        <select class="form-select" id="userRole" data-field="role" required>
                            <option value="USER">USER</option>
                            <option value="ADMIN">ADMIN</option>
                        </select>
                        <div class="invalid-feedback"></div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-outline-secondary" type="button" data-bs-dismiss="modal">Cancel</button>
                    <button class="btn btn-primary" id="saveUserButton" type="submit">
                        <span class="spinner-border spinner-border-sm me-1 d-none" id="saveSpinner" aria-hidden="true"></span>
                        <span id="saveUserLabel">Save user</span>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title h5" id="deleteModalTitle">Delete user</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="mb-0">Delete <strong id="deleteUserName"></strong>? This action cannot be undone.</p>
            </div>
            <div class="modal-footer">
                <button class="btn btn-outline-secondary" type="button" data-bs-dismiss="modal">Cancel</button>
                <button class="btn btn-danger" id="confirmDeleteButton" type="button">
                    <span class="spinner-border spinner-border-sm me-1 d-none" id="deleteSpinner" aria-hidden="true"></span>
                    Delete user
                </button>
            </div>
        </div>
    </div>
</div>

<div class="toast-container position-fixed bottom-0 end-0 p-3" id="toastContainer"></div>

<script src="${pageContext.request.contextPath}/webjars/jquery/3.7.1/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/js/users.js"></script>
</body>
</html>
