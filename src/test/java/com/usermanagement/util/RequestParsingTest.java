package com.usermanagement.util;

import com.usermanagement.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestParsingTest {

    @Test
    void pageAndSizeUseDefaultsAndRejectGarbage() {
        assertEquals(1, QueryParameters.page(null));
        assertEquals(10, QueryParameters.size(" "));
        assertEquals(3, QueryParameters.page("3"));
        assertNull(QueryParameters.search("  "));
        assertEquals("ada", QueryParameters.search(" ada "));

        assertEquals(400, assertThrows(ApiException.class, () -> QueryParameters.page("0")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> QueryParameters.size("500")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> QueryParameters.page("1;drop")).getStatus());
        assertEquals(400, assertThrows(ApiException.class,
                () -> QueryParameters.search("x".repeat(101))).getStatus());
    }

    @Test
    void userIdsMustBePositiveNumbers() {
        assertEquals(15L, PathIds.parseUserId("15"));
        assertEquals(400, assertThrows(ApiException.class, () -> PathIds.parseUserId("abc")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> PathIds.parseUserId("0")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> PathIds.parseUserId("-4")).getStatus());
    }
}
