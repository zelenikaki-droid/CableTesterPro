
package com.cabletester.pro.model

data class BackupData(
    val schemaVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val profiles: List<CableProfile> = emptyList(),
    val testRecords: List<TestRecord> = emptyList()
)