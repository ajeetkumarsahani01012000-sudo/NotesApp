package com.example.noteapp.data

import androidx.room.*

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE noteId = :noteId ORDER BY pageIndex ASC")
    suspend fun getPagesForNote(noteId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getPageById(pageId: Long): PageEntity?

    @Insert
    suspend fun insertPage(page: PageEntity): Long

    @Insert
    suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: PageEntity)

    @Update
    suspend fun updatePages(pages: List<PageEntity>)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("SELECT COUNT(*) FROM pages WHERE noteId = :noteId")
    suspend fun getPageCount(noteId: Long): Int

    @Query("DELETE FROM pages WHERE noteId = :noteId")
    suspend fun deleteAllPagesForNote(noteId: Long)
}
