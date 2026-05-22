package com.ingray.deadlock.domain.model

enum class FocusMode(
    val displayName: String,
    val description: String,
    val allowedCategories: List<String> = emptyList()
) {
    SOFT_FOCUS(
        displayName = "Soft Focus",
        description = "Blocks selected apps only"
    ),
    DEEP_WORK(
        displayName = "Deep Work",
        description = "Only productivity apps allowed",
        allowedCategories = listOf("productivity", "office", "notes")
    ),
    MONK_MODE(
        displayName = "Monk Mode",
        description = "Only calls and notes",
        allowedCategories = listOf("phone", "dialer", "notes")
    ),
    DOPAMINE_DETOX(
        displayName = "Dopamine Detox",
        description = "Blocks social media and all video content",
        allowedCategories = listOf("phone", "productivity", "health")
    ),
    EXAM_MODE(
        displayName = "Exam Mode",
        description = "Only study and reference apps allowed",
        allowedCategories = listOf("education", "productivity", "notes")
    )
}
