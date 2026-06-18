package com.example.diplom

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testParseNotification() {
        val json = """
        {
          "notifications": [
            {
              "id": 779,
              "user_id": 2,
              "actor_id": null,
              "type": "SYSTEM",
              "recipe_id": null,
              "comment_id": null,
              "message": "Системное уведомление: Проверка",
              "is_read": false,
              "createdAt": "2026-06-18T15:27:59.566Z",
              "updatedAt": "2026-06-18T15:27:59.566Z",
              "Actor": {
                "id": null,
                "username": "admin",
                "name": "Администратор",
                "avatar_url": null,
                "Role": {
                  "name": "Admin"
                },
                "role": "admin"
              },
              "Recipe": null,
              "Comment": null
            }
          ],
          "pagination": {
            "total": 27,
            "page": 1,
            "limit": 20,
            "totalPages": 2
          }
        }
        """.trimIndent()
        val gson = com.google.gson.Gson()
        val response = gson.fromJson(json, com.example.diplom.data.NotificationResponse::class.java)
        println("Parsed response: " + response)
        val actor = response.notifications[0].actor
        println("Actor: " + actor)
        if (actor != null) {
            println("Actor Username: " + actor.username)
            try {
                val actorIdStr = actor.id
                println("Actor ID (String): " + actorIdStr)
            } catch (e: Exception) {
                println("Exception when accessing actor.id: " + e)
                throw e
            }
            try {
                val actorHashCode = actor.hashCode()
                println("Actor HashCode: " + actorHashCode)
            } catch (e: Exception) {
                println("Exception when calling actor.hashCode(): " + e)
            }
        }
        try {
            val notificationHashCode = response.notifications[0].hashCode()
            println("Notification HashCode: " + notificationHashCode)
        } catch (e: Exception) {
            println("Exception when calling notification.hashCode(): " + e)
        }
    }
}