package com.example.apigateway.model

enum class UserRole {
    STUDENT,   // std - 인증 + 학생 인가 필요
    TEACHER,   // tch - 인증 + 교사 인가 필요
    ANONYMOUS, // ara - 인증/인가 둘다 불필요
    BOTH       // 권한 없음 - 인증만 필요 (인가 불필요)
}
