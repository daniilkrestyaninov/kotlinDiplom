package com.example.diplom.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class LocalChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isRecipeSuggestion: Boolean = false,
    val recipeJson: String? = null // Store AiRecipeSuggestion as JSON
)

@Entity(tableName = "recipe_drafts")
data class LocalRecipeDraft(
    @PrimaryKey val id: String = "current_draft",
    val title: String = "",
    val description: String = "",
    val ingredientsJson: String = "[]",
    val stepsJson: String = "[]",
    val mainImageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_recipes")
data class CachedRecipe(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val authorName: String?,
    val cookingTime: Int?,
    val difficulty: String?,
    val calorific: Int?,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface UmamiDao {
    // Chat
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<LocalChatMessage>>

    @Insert
    suspend fun insertMessage(message: LocalChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat(): Int

    // Drafts
    @Query("SELECT * FROM recipe_drafts WHERE id = :id")
    suspend fun getDraft(id: String = "current_draft"): LocalRecipeDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: LocalRecipeDraft): Long

    @Query("DELETE FROM recipe_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String = "current_draft"): Int

    // Feed Cache
    @Query("SELECT * FROM cached_recipes ORDER BY createdAt DESC")
    fun getCachedFeed(): Flow<List<CachedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<CachedRecipe>): List<Long>

    @Query("DELETE FROM cached_recipes")
    suspend fun clearFeedCache(): Int
}

@Database(entities = [LocalChatMessage::class, LocalRecipeDraft::class, CachedRecipe::class], version = 1)
abstract class UmamiDatabase : RoomDatabase() {
    abstract fun dao(): UmamiDao

    companion object {
        @Volatile
        private var INSTANCE: UmamiDatabase? = null

        fun getDatabase(context: android.content.Context): UmamiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UmamiDatabase::class.java,
                    "umami_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
