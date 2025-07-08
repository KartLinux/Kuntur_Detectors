package com.denicks21.speechandtext.analyzer.data

import android.content.Context

/**
 * Carga vocabulario WordPiece desde assets/vocab.txt
 * y devuelve un Map[token -> id].
 */
object VocabularyLoader {
    fun load(context: Context): Map<String, Int> {
        val assetManager = context.assets
        val lines = assetManager.open("vocab.txt").bufferedReader().use { it.readLines() }
        return lines.mapIndexed { index, token -> token to index }.toMap()
    }
}