package com.cabletester.pro.model

data class TestRecord(
    val id: Long = 0,

    // Povezava s profilom
    val profileId: Long,

    // Podatki o artiklu ob testiranju
    val articleCode: String,
    val articleName: String,

    // Datum in ura testa
    val date: String,

    // true = PASS, false = FAIL
    val passed: Boolean,

    // Napake, ugotovljene pri testu
    val errors: List<String> = emptyList()
)