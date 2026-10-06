package com.cabletester.pro.model

data class TestResult(
    val passed: Boolean,
    val message: String,
    val connections: List<PinConnection> = emptyList()
)