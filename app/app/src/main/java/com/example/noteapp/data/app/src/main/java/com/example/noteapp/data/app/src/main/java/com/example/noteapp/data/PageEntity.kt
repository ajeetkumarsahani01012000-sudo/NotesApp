package com.example.noteapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var noteId: Long = 0,
    var pageIndex: Int = 0,
    var strokesJson: String = "[]",
    var backgroundImagePath: String? = null
)
