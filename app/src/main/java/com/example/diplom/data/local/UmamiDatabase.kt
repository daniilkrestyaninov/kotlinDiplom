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
    val likesCount: Int? = 0,
    val isLiked: Boolean? = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_user_account")
data class LocalUserAccount(
    @PrimaryKey val id: String,
    val userJson: String,
    val profileJson: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_favorites")
data class CachedFavoriteRecipe(
    @PrimaryKey val id: Long,
    val recipeJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_my_recipes")
data class CachedMyRecipe(
    @PrimaryKey val id: Long,
    val recipeJson: String,
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

    // User Account
    @Query("SELECT * FROM local_user_account LIMIT 1")
    suspend fun getUserAccount(): LocalUserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserAccount(account: LocalUserAccount): Long

    @Query("DELETE FROM local_user_account")
    suspend fun clearUserAccount(): Int

    // Favorites Cache
    @Query("SELECT * FROM cached_favorites ORDER BY createdAt DESC")
    suspend fun getCachedFavorites(): List<CachedFavoriteRecipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(recipes: List<CachedFavoriteRecipe>): List<Long>

    @Query("DELETE FROM cached_favorites")
    suspend fun clearFavoritesCache(): Int

    @Query("DELETE FROM cached_favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long): Int

    @Query("SELECT * FROM cached_favorites WHERE id = :id")
    suspend fun getCachedFavoriteById(id: Long): CachedFavoriteRecipe?

    // My Recipes Cache
    @Query("SELECT * FROM cached_my_recipes ORDER BY createdAt DESC")
    suspend fun getCachedMyRecipes(): List<CachedMyRecipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyRecipes(recipes: List<CachedMyRecipe>): List<Long>

    @Query("DELETE FROM cached_my_recipes")
    suspend fun clearMyRecipesCache(): Int

    @Query("SELECT * FROM cached_my_recipes WHERE id = :id")
    suspend fun getCachedMyRecipeById(id: Long): CachedMyRecipe?
}

@Database(entities = [LocalChatMessage::class, LocalRecipeDraft::class, CachedRecipe::class, LocalUserAccount::class, CachedFavoriteRecipe::class, CachedMyRecipe::class], version = 4)
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
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
