package com.usermanagement.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDaoTest {

    @Test
    void likePatternTreatsWildcardsAndInjectionTextAsLiterals() {
        assertEquals("%ada%", UserDao.likePattern("ada"));
        assertEquals("%100\\%%", UserDao.likePattern("100%"));
        assertEquals("%a\\_b%", UserDao.likePattern("a_b"));
        assertEquals("%' OR 1=1 --%", UserDao.likePattern("' OR 1=1 --"));
    }
}
