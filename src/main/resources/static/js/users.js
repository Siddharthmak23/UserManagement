(function ($) {
    "use strict";

    var contextPath = document.body.dataset.contextPath || "";
    var currentUserId = Number(document.body.dataset.userId);
    var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute("content");
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute("content");
    var pageSize = 10;
    var currentPage = 1;
    var searchTerm = "";
    var userModal;
    var deleteModal;
    var deleteUserId = null;
    var saving = false;
    var searchTimer;

    var NAME_PATTERN = /^[\p{L}][\p{L} .'\-]{1,99}$/u;
    var EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    var PHONE_PATTERN = /^[0-9+()\- ]{7,20}$/;
    var PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/;

    $(function () {
        userModal = new bootstrap.Modal(document.getElementById("userModal"));
        deleteModal = new bootstrap.Modal(document.getElementById("deleteModal"));
        $("#addUserButton").on("click", openCreate);
        $("#userForm").on("submit", submitUser);
        $("#confirmDeleteButton").on("click", submitDelete);
        $("#searchForm").on("submit", function (event) {
            event.preventDefault();
            searchTerm = $("#searchInput").val().trim();
            loadUsers(1);
        });
        $("#searchInput").on("input", function () {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(function () {
                searchTerm = $("#searchInput").val().trim();
                loadUsers(1);
            }, 350);
        });
        $("#clearSearch").on("click", function () {
            $("#searchInput").val("");
            searchTerm = "";
            loadUsers(1);
        });
        $("#userTableBody").on("click", ".edit-user", function () {
            openEdit($(this).attr("data-id"));
        });
        $("#userTableBody").on("click", ".delete-user", function () {
            openDelete($(this).attr("data-id"), $(this).attr("data-name"));
        });
        window.addEventListener("pageshow", function (event) {
            if (event.persisted) {
                window.location.reload();
            }
        });
        loadStats();
        loadUsers(1);
    });

    function openCreate() {
        $("#userModalTitle").text("Add user");
        $("#saveUserLabel").text("Save user");
        $("#userId").val("");
        $("#userForm")[0].reset();
        $("#userRole").val("USER");
        $("#passwordHelp").text("At least 8 characters, with uppercase, lowercase, and a number.");
        clearFieldErrors();
        userModal.show();
    }

    function openEdit(id) {
        clearFieldErrors();
        setTableLoading(true);
        api("GET", "/api/users/" + encodeURIComponent(id))
            .done(function (user) {
                $("#userModalTitle").text("Edit user");
                $("#saveUserLabel").text("Save changes");
                $("#userId").val(user.id);
                $("#userName").val(user.name);
                $("#userEmail").val(user.email);
                $("#userPhone").val(user.phone);
                $("#userPassword").val("");
                $("#userRole").val(user.role);
                $("#passwordHelp").text("Leave blank to keep the current password.");
                userModal.show();
            })
            .fail(handleFailure)
            .always(function () {
                setTableLoading(false);
            });
    }

    function submitUser(event) {
        event.preventDefault();
        if (saving) {
            return;
        }
        var id = $("#userId").val();
        var creating = id === "";
        var payload = {
            name: $("#userName").val().trim(),
            email: $("#userEmail").val().trim(),
            phone: $("#userPhone").val().trim(),
            password: $("#userPassword").val(),
            role: $("#userRole").val()
        };
        var errors = validate(payload, creating);
        if (Object.keys(errors).length) {
            showFieldErrors(errors);
            return;
        }
        setSaving(true);
        var request = creating
            ? api("POST", "/api/users", payload)
            : api("PUT", "/api/users/" + encodeURIComponent(id), payload);
        request.done(function (user) {
            userModal.hide();
            showToast(creating ? "User created." : "User updated.", "success");
            if (Number(user.id) === currentUserId) {
                $("#welcomeName").text(user.name);
            }
            loadStats();
            loadUsers(currentPage);
        }).fail(function (xhr) {
            handleFailure(xhr, true);
        }).always(function () {
            setSaving(false);
        });
    }

    function openDelete(id, name) {
        deleteUserId = id;
        $("#deleteUserName").text(name);
        deleteModal.show();
    }

    function submitDelete() {
        if (saving || !deleteUserId) {
            return;
        }
        setDeleting(true);
        api("DELETE", "/api/users/" + encodeURIComponent(deleteUserId))
            .done(function () {
                deleteModal.hide();
                showToast("User deleted.", "success");
                loadStats();
                loadUsers(currentPage);
            })
            .fail(function (xhr) {
                deleteModal.hide();
                handleFailure(xhr, false);
            })
            .always(function () {
                setDeleting(false);
                deleteUserId = null;
            });
    }

    function loadUsers(page) {
        currentPage = page;
        setTableLoading(true);
        var query = "/api/users?page=" + page + "&size=" + pageSize + "&search=" + encodeURIComponent(searchTerm);
        api("GET", query)
            .done(function (data) {
                if (data.totalPages > 0 && page > data.totalPages) {
                    loadUsers(data.totalPages);
                    return;
                }
                renderUsers(data.users || []);
                renderPagination(data.page, data.totalPages);
                renderSummary(data);
            })
            .fail(function (xhr) {
                handleFailure(xhr, false);
            })
            .always(function () {
                setTableLoading(false);
            });
    }

    function loadStats() {
        api("GET", "/api/stats")
            .done(function (stats) {
                $("#statTotal").text(stats.totalUsers);
                $("#statAdmins").text(stats.adminCount);
                $("#statUsers").text(stats.standardUserCount);
            })
            .fail(function (xhr) {
                handleFailure(xhr, false);
            });
    }

    function renderUsers(users) {
        var body = $("#userTableBody");
        body.empty();
        if (!users.length) {
            var message = searchTerm
                ? "No users match your search."
                : "No users yet. Add the first user to get started.";
            body.append($("<tr>").append(
                $("<td>", { colspan: 7, class: "text-center text-secondary py-5" }).text(message)
            ));
            return;
        }
        users.forEach(function (user) {
            var role = $("<span>", { class: "badge" }).text(user.role);
            role.addClass(user.role === "ADMIN" ? "text-bg-primary" : "text-bg-secondary");
            var edit = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-primary edit-user me-2",
                "data-id": user.id
            }).text("Edit");
            var remove = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-danger delete-user",
                "data-id": user.id,
                "data-name": user.name
            }).text("Delete");
            if (Number(user.id) === currentUserId) {
                remove.prop("disabled", true).attr("title", "You cannot delete your own account");
            }
            var actions = $("<td>").append(edit, remove);
            body.append($("<tr>")
                .append($("<td>").text(user.id))
                .append($("<td>").text(user.name))
                .append($("<td>").text(user.email))
                .append($("<td>").text(user.phone))
                .append($("<td>").append(role))
                .append($("<td>").text(formatDate(user.createdAt)))
                .append(actions));
        });
    }

    function renderSummary(data) {
        if (!data.total) {
            $("#resultSummary").text("Showing 0 users");
            return;
        }
        var start = ((data.page - 1) * data.size) + 1;
        var end = Math.min(data.page * data.size, data.total);
        $("#resultSummary").text("Showing " + start + "–" + end + " of " + data.total);
    }

    function renderPagination(page, totalPages) {
        var list = $("#pagination");
        list.empty();
        if (!totalPages || totalPages < 2) {
            return;
        }
        list.append(pageItem("Previous", page - 1, page === 1, false));
        pageWindow(page, totalPages).forEach(function (entry) {
            if (entry === "…") {
                list.append($("<li>", { class: "page-item disabled" }).append(
                    $("<span>", { class: "page-link" }).text("…")
                ));
                return;
            }
            list.append(pageItem(String(entry), entry, false, entry === page));
        });
        list.append(pageItem("Next", page + 1, page === totalPages, false));
    }

    function pageItem(label, page, disabled, active) {
        var item = $("<li>", { class: "page-item" });
        if (disabled) {
            item.addClass("disabled");
        }
        if (active) {
            item.addClass("active");
        }
        var link = $("<button>", { type: "button", class: "page-link" }).text(label);
        if (active) {
            link.attr("aria-current", "page");
        }
        if (!disabled && !active) {
            link.on("click", function () {
                loadUsers(page);
            });
        }
        return item.append(link);
    }

    function pageWindow(current, total) {
        var start = Math.max(1, current - 2);
        var end = Math.min(total, current + 2);
        var pages = [];
        if (start > 1) {
            pages.push(1);
        }
        if (start > 2) {
            pages.push("…");
        }
        for (var i = start; i <= end; i += 1) {
            pages.push(i);
        }
        if (end < total - 1) {
            pages.push("…");
        }
        if (end < total) {
            pages.push(total);
        }
        return pages;
    }

    function validate(payload, passwordRequired) {
        var errors = {};
        if (!payload.name) {
            errors.name = "Name is required";
        } else if (payload.name.length < 2 || payload.name.length > 100 || !NAME_PATTERN.test(payload.name)) {
            errors.name = payload.name.length < 2 || payload.name.length > 100
                ? "Name must be between 2 and 100 characters"
                : "Name can contain only letters, spaces, apostrophes, periods, and hyphens";
        }
        if (!payload.email) {
            errors.email = "Email is required";
        } else if (payload.email.length > 150 || !EMAIL_PATTERN.test(payload.email)) {
            errors.email = "Enter a valid email address";
        }
        var digits = (payload.phone.match(/\d/g) || []).length;
        if (!payload.phone || !PHONE_PATTERN.test(payload.phone) || digits < 7 || digits > 15) {
            errors.phone = "Enter a valid phone number with 7 to 15 digits";
        }
        if (passwordRequired && !payload.password) {
            errors.password = "Password is required";
        } else if (payload.password && !PASSWORD_PATTERN.test(payload.password)) {
            errors.password = "Password must be 8 to 72 characters and include uppercase, lowercase, and a number";
        }
        if (payload.role !== "ADMIN" && payload.role !== "USER") {
            errors.role = "Role must be ADMIN or USER";
        }
        return errors;
    }

    function showFieldErrors(errors) {
        clearFieldErrors();
        ["name", "email", "phone", "password", "role"].forEach(function (field) {
            if (!errors[field]) {
                return;
            }
            var input = document.querySelector('#userForm [data-field="' + field + '"]');
            if (!input) {
                return;
            }
            input.classList.add("is-invalid");
            var feedback = input.parentElement.querySelector(".invalid-feedback");
            if (feedback) {
                feedback.textContent = errors[field];
            }
        });
    }

    function clearFieldErrors() {
        $("#userForm .is-invalid").removeClass("is-invalid");
        $("#userForm .invalid-feedback").text("");
    }

    function api(method, url, body) {
        var headers = { Accept: "application/json" };
        if (method !== "GET" && method !== "HEAD") {
            headers[csrfHeader] = csrfToken;
        }
        return $.ajax({
            url: contextPath + url,
            method: method,
            data: body == null ? undefined : JSON.stringify(body),
            contentType: body == null ? undefined : "application/json; charset=utf-8",
            dataType: "text",
            headers: headers
        }).then(function (text, _status, xhr) {
            if (!text || xhr.status === 204) {
                return null;
            }
            return JSON.parse(text);
        }, function (xhr) {
            return $.Deferred().reject(xhr).promise();
        });
    }

    function handleFailure(xhr, showFields) {
        if (xhr && xhr.status === 401) {
            window.location.href = contextPath + "/login?expired";
            return;
        }
        var body = parseError(xhr);
        if (showFields && body.errors) {
            showFieldErrors(body.errors);
        }
        showToast(body.message || "Request failed", "danger");
    }

    function parseError(xhr) {
        try {
            return JSON.parse(xhr.responseText);
        } catch (error) {
            return { message: "Request failed" };
        }
    }

    function showToast(message, type) {
        var toast = $("<div>", {
            class: "toast align-items-center border-0 text-bg-" + (type === "success" ? "success" : "danger"),
            role: "status",
            "aria-live": "polite",
            "aria-atomic": "true"
        });
        var body = $("<div>", { class: "d-flex" });
        body.append($("<div>", { class: "toast-body" }).text(message));
        body.append($("<button>", {
            type: "button",
            class: "btn-close btn-close-white me-2 m-auto",
            "data-bs-dismiss": "toast",
            "aria-label": "Close"
        }));
        toast.append(body);
        $("#toastContainer").append(toast);
        var instance = new bootstrap.Toast(toast.get(0), { delay: 4000 });
        toast.on("hidden.bs.toast", function () {
            toast.remove();
        });
        instance.show();
    }

    function setTableLoading(loading) {
        $("#tableLoading").toggleClass("d-none", !loading);
    }

    function setSaving(busy) {
        saving = busy;
        $("#saveUserButton").prop("disabled", busy);
        $("#saveSpinner").toggleClass("d-none", !busy);
    }

    function setDeleting(busy) {
        saving = busy;
        $("#confirmDeleteButton").prop("disabled", busy);
        $("#deleteSpinner").toggleClass("d-none", !busy);
    }

    function formatDate(value) {
        if (!value) {
            return "";
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }
        return date.toLocaleString();
    }
})(jQuery);
