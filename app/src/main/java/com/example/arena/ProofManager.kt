package com.example.arena

// Bu bizning "Global Seyfimiz". Kamera bunga soladi, Home bundan oladi.
object ProofManager {
    private var result: Pair<String, String>? = null

    var capturedData: Pair<String, String>? = null

    fun setResult(challengeId: String, url: String) {
        result = Pair(challengeId, url)
    }

    fun getResult(): Pair<String, String>? {
        val temp = result
        result = null // Olgandan keyin o'chirib tashlaymiz (bir marta ishlatish uchun)
        return temp
    }
}