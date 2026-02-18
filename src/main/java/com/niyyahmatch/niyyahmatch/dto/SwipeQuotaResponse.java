package com.niyyahmatch.niyyahmatch.dto;

public class SwipeQuotaResponse {

    private final int remaining;
    private final int limit;

    public SwipeQuotaResponse(int remaining, int limit) {
        this.remaining = remaining;
        this.limit = limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public int getLimit() {
        return limit;
    }
}
