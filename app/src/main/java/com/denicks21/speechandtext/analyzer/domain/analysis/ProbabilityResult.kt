package com.denicks21.speechandtext.analyzer.domain.analysis

/**
 * Representa el resultado de un análisis de imagen,
 * con un valor de probabilidad de detección.
 *
 * @param probability Valor entre 0.0 (mínima confianza) y 1.0 (máxima).
 */
data class ProbabilityResult(
    val probability: Float
)
