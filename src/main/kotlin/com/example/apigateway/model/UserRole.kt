package com.example.apigateway.model

enum class UserRole {
    STUDENT,   // std - 인증 + 학생 인가 필요
    TEACHER,   // tch - 인증 + 교사 인가 필요
    ANONYMOUS, // ara - 인증/인가 둘다 불필요
    STUDENT_OR_TEACHER // 프리픽스 없음 -> 인증 + 학생 또는 교사 권한 필요
}
