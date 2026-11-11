package com.sabrigur.dto;

public record MemberActivityResponse(Long memberId, String memberName, long totalLoans, long activeLoans,
                                     long overdueLoans) {
}
