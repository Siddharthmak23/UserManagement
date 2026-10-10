package com.usermanagement.dto;

public record StatsResponse(long totalUsers, long adminCount, long standardUserCount) {
}
