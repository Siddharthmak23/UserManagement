package com.usermanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack checks against the local MySQL server on localhost:3306.
 * Requires the {@code user_management} database from {@code database/schema.sql}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserManagementIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@usermanagement.local";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @LocalServerPort
    private int port;

    @Test
    void unauthenticatedVisitorsCannotOpenTheDashboardOrTheApi() throws Exception {
        SessionClient client = new SessionClient();

        HttpResponse<String> home = client.get("/home");
        HttpResponse<String> users = client.get("/api/users");

        assertEquals(302, home.statusCode());
        assertTrue(home.headers().firstValue("location").orElse("").contains("/login"));
        assertEquals(401, users.statusCode());
        assertTrue(users.body().contains("Authentication required"));
        assertFalse(users.body().contains("password_hash"));
    }

    @Test
    void loginFailureIsGenericAndDoesNotTreatSqlInjectionAsSuccess() throws Exception {
        SessionClient client = new SessionClient();
        String token = csrfFromLogin(client);

        HttpResponse<String> badPassword = client.postForm("/login", Map.of(
                "email", ADMIN_EMAIL,
                "password", "wrong-password",
                "_csrf", token));
        assertEquals(302, badPassword.statusCode());
        assertTrue(badPassword.headers().firstValue("location").orElse("").contains("/login?error"));

        SessionClient injected = new SessionClient();
        String injectedToken = csrfFromLogin(injected);
        HttpResponse<String> injection = injected.postForm("/login", Map.of(
                "email", ADMIN_EMAIL + "' OR '1'='1",
                "password", "' OR '1'='1",
                "_csrf", injectedToken));
        assertEquals(302, injection.statusCode());
        assertTrue(injection.headers().firstValue("location").orElse("").contains("error"));
        assertFalse(injection.headers().firstValue("location").orElse("").contains("/home"));
    }

    @Test
    void loginPageDoesNotReflectTheErrorQuery() throws Exception {
        SessionClient client = new SessionClient();
        HttpResponse<String> response = client.get("/login?error=%3Cscript%3Ealert(1)%3C/script%3E");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Invalid email or password."));
        assertFalse(response.body().contains("<script>alert(1)</script>"));
    }

    @Test
    void administratorCanManageUsersThroughTheSharedSession() throws Exception {
        SessionClient client = loginAsAdmin();
        HttpResponse<String> home = client.get("/home");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("System Administrator"));
        assertTrue(home.headers().firstValue("cache-control").orElse("").toLowerCase().contains("no-store"));

        String csrf = meta(home.body(), "_csrf");
        String header = meta(home.body(), "_csrf_header");
        SessionClient anonymous = new SessionClient();
        HttpResponse<String> noSession = anonymous.send("POST", "/api/users", "{}",
                Map.of("Content-Type", "application/json"), true);
        assertEquals(401, noSession.statusCode());

        HttpResponse<String> badCsrf = client.send("POST", "/api/users", userJson(
                "Bad Token", "bad@example.com", "5550100199", "Secret123", "USER"),
                Map.of("Content-Type", "application/json", header, "not-the-token"), true);
        assertEquals(403, badCsrf.statusCode());
        assertTrue(badCsrf.body().contains("CSRF"));

        String email = "ada." + UUID.randomUUID() + "@example.com";
        HttpResponse<String> created = client.send("POST", "/api/users", userJson(
                "Ada Lovelace", email, "5550100123", "Secret123", "USER"), jsonHeaders(header, csrf), true);
        assertEquals(201, created.statusCode());
        assertFalse(created.body().contains("password"));
        long id = Long.parseLong(jsonField(created.body(), "id"));

        HttpResponse<String> duplicate = client.send("POST", "/api/users", userJson(
                "Ada Two", email, "5550100124", "Secret123", "USER"), jsonHeaders(header, csrf), true);
        assertEquals(409, duplicate.statusCode());

        HttpResponse<String> invalid = client.send("POST", "/api/users",
                "{\"name\":\"\",\"email\":\"bad\",\"phone\":\"1\",\"password\":\"x\",\"role\":\"GUEST\"}",
                jsonHeaders(header, csrf), true);
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("\"errors\""));

        HttpResponse<String> badJson = client.send("POST", "/api/users", "{", jsonHeaders(header, csrf), true);
        assertEquals(400, badJson.statusCode());

        HttpResponse<String> listed = client.get("/api/users?page=1&size=10&search="
                + URLEncoder.encode("' OR 1=1 --", StandardCharsets.UTF_8));
        assertEquals(200, listed.statusCode());
        assertFalse(listed.body().contains(email));

        HttpResponse<String> found = client.get("/api/users?search="
                + URLEncoder.encode("Ada Lovelace", StandardCharsets.UTF_8));
        assertEquals(200, found.statusCode());
        assertTrue(found.body().contains(email));

        HttpResponse<String> updated = client.send("PUT", "/api/users/" + id, userJson(
                "Ada Byron", email, "5550100123", "", "USER"), jsonHeaders(header, csrf), true);
        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("Ada Byron"));

        HttpResponse<String> missing = client.get("/api/users/999999999");
        assertEquals(404, missing.statusCode());
        HttpResponse<String> malformed = client.get("/api/users/abc");
        assertEquals(400, malformed.statusCode());

        HttpResponse<String> deleted = client.send("DELETE", "/api/users/" + id, null, jsonHeaders(header, csrf), true);
        assertEquals(204, deleted.statusCode());
        assertEquals(404, client.get("/api/users/" + id).statusCode());

        HttpResponse<String> stats = client.get("/api/stats");
        assertEquals(200, stats.statusCode());
        assertTrue(stats.body().contains("totalUsers"));

        long adminId = Long.parseLong(home.body().replaceAll("(?s).*data-user-id=\"(\\d+)\".*", "$1"));
        HttpResponse<String> selfDelete = client.send("DELETE", "/api/users/" + adminId, null,
                jsonHeaders(header, csrf), true);
        assertEquals(409, selfDelete.statusCode());
        assertTrue(selfDelete.body().contains("logged-in"));

        HttpResponse<String> loggedOut = client.postForm("/logout", Map.of("_csrf", csrf));
        assertEquals(302, loggedOut.statusCode());
        assertTrue(loggedOut.headers().firstValue("location").orElse("").contains("/login?logout"));
        assertEquals(401, client.get("/api/users").statusCode());
        assertEquals(302, client.get("/home").statusCode());
    }

    @Test
    void standardUserIsAuthenticatedButForbiddenFromAdminFeatures() throws Exception {
        SessionClient admin = loginAsAdmin();
        HttpResponse<String> home = admin.get("/home");
        String header = meta(home.body(), "_csrf_header");
        String csrf = meta(home.body(), "_csrf");
        String email = "user." + UUID.randomUUID() + "@example.com";
        HttpResponse<String> created = admin.send("POST", "/api/users", userJson(
                "Regular User", email, "5550100188", "Secret123", "USER"), jsonHeaders(header, csrf), true);
        assertEquals(201, created.statusCode());

        SessionClient user = new SessionClient();
        String loginToken = csrfFromLogin(user);
        String before = user.cookie("JSESSIONID");
        HttpResponse<String> login = user.postForm("/login", Map.of(
                "email", email,
                "password", "Secret123",
                "_csrf", loginToken));
        assertEquals(302, login.statusCode());
        assertTrue(login.headers().firstValue("location").orElse("").endsWith("/home"));
        assertNotEquals(before, user.cookie("JSESSIONID"));

        HttpResponse<String> dashboard = user.get("/home");
        assertEquals(302, dashboard.statusCode());
        assertTrue(dashboard.headers().firstValue("location").orElse("").contains("/forbidden"));
        assertEquals(403, user.get("/api/users").statusCode());
        assertEquals(200, user.get("/forbidden").statusCode());
    }

    private SessionClient loginAsAdmin() throws Exception {
        SessionClient client = new SessionClient();
        String before = null;
        String token = csrfFromLogin(client);
        before = client.cookie("JSESSIONID");
        HttpResponse<String> login = client.postForm("/login", Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD,
                "_csrf", token));
        assertEquals(302, login.statusCode(), login.headers().firstValue("location").orElse(login.body()));
        assertTrue(login.headers().firstValue("location").orElse("").endsWith("/home"));
        assertNotEquals(before, client.cookie("JSESSIONID"));
        return client;
    }

    private String csrfFromLogin(SessionClient client) throws Exception {
        HttpResponse<String> page = client.get("/login");
        assertEquals(200, page.statusCode(), page.body());
        Matcher matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(page.body());
        assertTrue(matcher.find(), page.body());
        return matcher.group(1);
    }

    private static String meta(String html, String name) {
        Matcher matcher = Pattern.compile("name=\"" + name + "\"\\s+content=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), html);
        return matcher.group(1);
    }

    private static Map<String, String> jsonHeaders(String headerName, String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put(headerName, token);
        return headers;
    }

    private static String userJson(String name, String email, String phone, String password, String role) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone
                + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}";
    }

    private static String jsonField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?").matcher(json);
        assertTrue(matcher.find(), json);
        return matcher.group(1);
    }

    private final class SessionClient {
        private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        private final Map<String, String> cookies = new LinkedHashMap<>();

        private HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, Map.of(), true);
        }

        private HttpResponse<String> postForm(String path, Map<String, String> form) throws Exception {
            String body = form.entrySet().stream()
                    .map(entry -> url(entry.getKey()) + "=" + url(entry.getValue()))
                    .reduce((left, right) -> left + "&" + right)
                    .orElse("");
            return send("POST", path, body, Map.of("Content-Type", "application/x-www-form-urlencoded"), true);
        }

        private HttpResponse<String> send(String method, String path, String body, Map<String, String> headers,
                                          boolean includeCookies) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            headers.forEach(builder::header);
            if (includeCookies && !cookies.isEmpty()) {
                builder.header("Cookie", cookies.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((left, right) -> left + "; " + right)
                        .orElse(""));
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            response.headers().allValues("set-cookie").forEach(this::storeCookie);
            return response;
        }

        private void storeCookie(String setCookie) {
            String pair = setCookie.split(";", 2)[0];
            int separator = pair.indexOf('=');
            if (separator < 1) {
                return;
            }
            String name = pair.substring(0, separator).trim();
            String value = pair.substring(separator + 1).trim();
            if (value.isEmpty() || setCookie.toLowerCase().contains("max-age=0")) {
                cookies.remove(name);
                return;
            }
            cookies.put(name, value);
        }

        private String cookie(String name) {
            return cookies.get(name);
        }

        private String url(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }
}
