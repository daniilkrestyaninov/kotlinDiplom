package com.example.diplom.data

private const val STORAGE_BASE = "https://umami-recipes.ru/storage"

fun normalizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    
    // 0. If it's a local URI (gallery or camera), don't touch it
    if (url.startsWith("content://") || url.startsWith("file://")) {
        return url
    }
    
    // 1. If it's already a correct public URL, return it
    if (url.startsWith(STORAGE_BASE)) {
        return url
    }

    // 2. Clean up ALL possible local/internal prefixes and old hosts
    var path = url
        .replace("http://188.233.238.70:9000", "")
        .replace("http://188.233.238.70:5000", "")
        .replace("http://localhost:9000", "")
        .replace("http://127.0.0.1:9000", "")
        .replace("http://10.0.2.2:9000", "")
        .replace("http://10.8.0.40:9000", "")
        .replace("https://10.8.0.40:9000", "")
        .replace("http://10.8.0.40:9001", "")
        .replace("http://10.0.2.2:9001", "")
        .replace("http://localhost:5000", "")
        .replace("http://127.0.0.1:5000", "")
        .replace("http://10.0.2.2:5000", "")
        .replace("/api/", "/")
        .replace("/storage/", "/")

    // 3. Extract the clean path if it's still a full URL (external)
    if (path.startsWith("http")) {
        // If it's from our old IP or local addresses, extract the path
        if (path.contains("188.233.238.70") || path.contains("localhost") || 
            path.contains("127.0.0.1") || path.contains("10.0.2.2") || path.contains("10.8.0.40")) {
            
            val lastSlashAfterHost = path.indexOf("/", path.indexOf("//") + 2)
            if (lastSlashAfterHost != -1) {
                path = path.substring(lastSlashAfterHost)
            }
        } else {
            // It's a completely external internet URL, leave it as is
            return path
        }
    }

    // 4. Build the final URL pointing to our new storage
    val cleanPath = path.trimStart('/')
    
    // Ensure the path starts with the bucket name 'vkusno/' if it's not there
    val finalPath = if (cleanPath.startsWith("vkusno/")) cleanPath else "vkusno/$cleanPath"
    
    return "$STORAGE_BASE/$finalPath"
}
