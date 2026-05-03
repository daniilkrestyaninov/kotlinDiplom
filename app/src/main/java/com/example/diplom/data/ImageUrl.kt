package com.example.diplom.data

private const val PUBLIC_HOST = "188.233.238.70"

fun normalizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val normalized = url
        .replace("http://10.8.0.40:9000", "http://$PUBLIC_HOST:9000")
        .replace("https://10.8.0.40:9000", "http://$PUBLIC_HOST:9000")
        .replace("http://10.8.0.40:9001", "http://$PUBLIC_HOST:9000")
        .replace("https://10.8.0.40:9001", "http://$PUBLIC_HOST:9000")
        .replace("http://$PUBLIC_HOST:9001", "http://$PUBLIC_HOST:9000")

    return if (normalized.startsWith("/vkusno/")) {
        "http://$PUBLIC_HOST:9000$normalized"
    } else {
        normalized
    }
}
