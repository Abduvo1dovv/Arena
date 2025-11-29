package com.example.arena


object ProofManager {
    private var result: Pair<String, String>? = null

    var capturedData: Pair<String, String>? = null

    fun setResult(challengeId: String, url: String) {
        result = Pair(challengeId, url)
    }

    fun getResult(): Pair<String, String>? {
        val temp = result
        result = null
        return temp
    }
}