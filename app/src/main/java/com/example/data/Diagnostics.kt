package com.example.data

data class SubsystemProbe(
    val id: String,
    val name: String,
    val isHealthy: Boolean,
    val statusBadge: String,
    val latencyMs: Long,
    val details: String
)

data class DiagnosticsReport(
    val timestamp: Long = System.currentTimeMillis(),
    val probes: List<SubsystemProbe>,
    val allPassing: Boolean,
    val totalLatencyMs: Long
)
