// src/main/java/com/denicks21/speechandtext/analyzer/data/OnnxTextAnalyzer.kt
package com.denicks21.speechandtext.analyzer.data

import android.content.Context
import android.util.Log
import com.denicks21.speechandtext.analyzer.domain.ITextAnalyzer
import com.denicks21.speechandtext.analyzer.domain.AnalysisResult
import com.denicks21.speechandtext.analyzer.domain.LabelMapping
import com.denicks21.speechandtext.viewmodel.WordPieceTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.io.FileOutputStream

/**
 * Implementación de ITextAnalyzer usando ONNX Runtime y WordPiece.
 */
class OnnxTextAnalyzer(private val context: Context) : ITextAnalyzer {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    // Carga vocabulario y tokenizer
    private val vocab = VocabularyLoader.load(context)
    private val tokenizer = WordPieceTokenizer(vocab)

    init {
        // Copia el modelo assets → filesDir
        val modelName = "modelo_detectar_robo_violencia.onnx"
        val modelFile = File(context.filesDir, modelName).apply {
            if (!exists()) {
                context.assets.open(modelName).use { input ->
                    FileOutputStream(this).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        session = env.createSession(modelFile.absolutePath)

        // DEBUG: listar formas de I/O
        session.inputInfo.forEach { (name, info) ->
            (info.info as? TensorInfo)?.let {
                Log.d("ONNX-DEBUG", "INPUT  $name shape=${it.shape.contentToString()}")
            }
        }
        session.outputInfo.forEach { (name, info) ->
            (info.info as? TensorInfo)?.let {
                Log.d("ONNX-DEBUG", "OUTPUT $name shape=${it.shape.contentToString()}")
            }
        }
    }

    override suspend fun analyze(text: String): AnalysisResult = withContext(Dispatchers.IO) {
        // 1) Tokenización WordPiece
        val tokens = tokenizer.tokenize(text)
        val seqLen = 19
        val inputIds = LongArray(seqLen) { idx ->
            vocab[tokens.getOrNull(idx)]?.toLong() ?: vocab["[PAD]"]!!.toLong()
        }
        val attentionMask = LongArray(seqLen) { idx ->
            if (idx < tokens.size) 1L else 0L
        }

        // 2) Tensores de entrada
        val tensorIds  = OnnxTensor.createTensor(env, arrayOf(inputIds))
        val tensorMask = OnnxTensor.createTensor(env, arrayOf(attentionMask))

        try {
            // 3) Inferencia
            val inputs = mapOf(
                session.inputNames.elementAt(0) to tensorIds,
                session.inputNames.elementAt(1) to tensorMask
            )
            session.run(inputs).use { outputs ->
                @Suppress("UNCHECKED_CAST")
                val rawScores = (outputs[0].value as Array<FloatArray>)[0]

                // 4) Softmax → probabilidad 0..1
                val maxLogit = rawScores.maxOrNull() ?: 0f
                val exps     = rawScores.map { kotlin.math.exp(it - maxLogit) }
                val sumExps  = exps.sum()
                val probs    = exps.map { it / sumExps }.toFloatArray()

                // 5) Top-1
                val maxIdx            = probs.indices.maxByOrNull { probs[it] } ?: 0
                val label             = LabelMapping.id2label[maxIdx] ?: maxIdx.toString()
                val confidencePercent = probs[maxIdx] * 100f

                // 6) Retorna solo label+score
                return@withContext AnalysisResult(
                    label = label,
                    score = confidencePercent
                )
            }
        } finally {
            tensorIds.close()
            tensorMask.close()
        }
    }
}

