package com.cabletester.pro.model

data class CableProfile(

    val articleCode: String,

    val articleName: String,

    val description: String,

    val image: String? = null,

    val created: String,

    val lastTest: String? = null,

    val hasReference: Boolean = false
)