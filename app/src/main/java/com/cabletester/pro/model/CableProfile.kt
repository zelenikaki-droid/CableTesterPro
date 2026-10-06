package com.cabletester.pro.model


data class CableProfile(

    val id: Long = 0,

    val name: String,

    val articleCode: String,

    val articleName: String,

    val notes: String = "",

    val created: String,

    val lastTest: String = "",

    val referenceLearned: Boolean = false,

    val imagePath: String = "",


    // REZULTAT TESTA

    val lastResult: Boolean? = null,


    // ŠTEVILO TESTOV

    val passedTests: Int = 0,

    val failedTests: Int = 0,


    // NAPAKE PRI TESTU

    val testErrors: List<String> = emptyList(),

    //naučene povezave kabla
    val connections: List<PinConnection> = emptyList(),

    // datum zadnjega SELF LEARN
    val learnedDate: String = ""

)