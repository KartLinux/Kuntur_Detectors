package com.denicks21.speechandtext.analyzer.domain.analysis

import androidx.camera.core.ImageProxy

interface CameraAnalyzer {
    fun analyze(image: ImageProxy): ProbabilityResult
}