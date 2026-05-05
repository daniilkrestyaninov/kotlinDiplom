package com.example.diplom.data

private const val PUBLIC_HOST = "188.233.238.70"

fun normalizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    
    // 0. If it's a local URI (gallery or camera), don't touch it
    if (url.startsWith("content://") || url.startsWith("file://")) {
        return url
    }
    
    val publicHost = "188.233.238.70"
    val publicPort = "9000"
    
    // 1. If it's already a correct public URL, return it
    if (url.startsWith("http://$publicHost:$publicPort")) {
        return url
    }

    // 2. Clean up ALL possible local/internal prefixes and wrong ports
    var path = url
        .replace("http://localhost:9000", "")
        .replace("http://127.0.0.1:9000", "")
        .replace("http://10.8.0.40:9000", "")
        .replace("https://10.8.0.40:9000", "")
        .replace("http://10.8.0.40:9001", "")
        .replace("http://$publicHost:9001", "")
        .replace("http://$publicHost:5000", "")
        .replace("http://localhost:5000", "")
        .replace("http://127.0.0.1:5000", "")

    // 3. Extract the clean path (handle cases where full URLs were partially replaced)
    if (path.startsWith("http")) {
        // If it's an external internet URL (not our server), leave it
        if (!path.contains(publicHost) && !path.contains("localhost") && !path.contains("127.0.0.1")) {
            return path
        }
        // Otherwise, try to extract the path after the last slash of the host:port part
        val lastSlashAfterHost = path.indexOf("/", 8) // skip http://
        if (lastSlashAfterHost != -1) {
            path = path.substring(lastSlashAfterHost)
        }
    }

    // 4. Build the final URL pointing to our MinIO
    val cleanPath = path.trimStart('/')
    
    // Ensure the path starts with the bucket name 'vkusno/' if it's not there
    val finalPath = if (cleanPath.startsWith("vkusno/")) cleanPath else "vkusno/$cleanPath"
    
    return "http://$publicHost:$publicPort/$finalPath"
}
