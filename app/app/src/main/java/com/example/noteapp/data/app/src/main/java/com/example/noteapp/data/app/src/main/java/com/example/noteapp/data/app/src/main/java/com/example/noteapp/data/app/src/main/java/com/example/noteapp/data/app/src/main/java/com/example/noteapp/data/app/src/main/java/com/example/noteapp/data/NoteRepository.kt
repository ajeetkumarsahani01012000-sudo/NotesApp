package com.example.noteapp.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.noteapp.ui.StrokeRenderer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class NoteRepository(private val context: Context) {

    private val noteDao = NoteDatabase.getInstance(context).noteDao()
    private val pageDao = NoteDatabase.getInstance(context).pageDao()
    private val gson = Gson()

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun createNote(): Long {
        val noteId = noteDao.insertNote(NoteEntity())
        pageDao.insertPage(PageEntity(noteId = noteId, pageIndex = 0))
        return noteId
    }

    suspend fun renameNote(noteId: Long, newTitle: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        note.title = newTitle
        noteDao.updateNote(note)
    }

    suspend fun touchNote(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        note.updatedAt = System.currentTimeMillis()
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        val pages = pageDao.getPagesForNote(note.id)
        pages.forEach { it.backgroundImagePath?.let { path -> File(path).delete() } }
        pageDao.deleteAllPagesForNote(note.id)
        noteDao.deleteNote(note)
    }

    suspend fun getPagesForNote(noteId: Long): List<PageEntity> = pageDao.getPagesForNote(noteId)

    suspend fun savePageStrokes(pageId: Long, strokes: List<Stroke>) {
        val page = pageDao.getPageById(pageId) ?: return
        page.strokesJson = gson.toJson(strokes)
        pageDao.updatePage(page)
    }

    suspend fun addBlankPage(noteId: Long): PageEntity {
        val count = pageDao.getPageCount(noteId)
        val page = PageEntity(noteId = noteId, pageIndex = count)
        val id = pageDao.insertPage(page)
        return page.copy(id = id)
    }

    suspend fun deletePage(page: PageEntity, noteId: Long) {
        page.backgroundImagePath?.let { File(it).delete() }
        pageDao.deletePage(page)
        val remaining = pageDao.getPagesForNote(noteId)
        remaining.forEachIndexed { index, p ->
            if (p.pageIndex != index) {
                p.pageIndex = index
                pageDao.updatePage(p)
            }
        }
    }

    fun deserializeStrokes(json: String): MutableList<Stroke> {
        val type = object : TypeToken<MutableList<Stroke>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    suspend fun importPdfIntoNote(noteId: Long, uri: Uri): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        val renderer = PdfRenderer(pfd)
        val startIndex = pageDao.getPageCount(noteId)
        val importedCount = renderer.pageCount

        for (i in 0 until renderer.pageCount) {
            val pdfPage = renderer.openPage(i)
            val scale = 2
            val bitmap = Bitmap.createBitmap(
                pdfPage.width * scale,
                pdfPage.height * scale,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val matrix = Matrix().apply { setScale(scale.toFloat(), scale.toFloat()) }
            pdfPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()

            val imageFile = File(context.filesDir, "note_${noteId}_pdfpage_${System.currentTimeMillis()}_$i.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            pageDao.insertPage(
                PageEntity(
                    noteId = noteId,
                    pageIndex = startIndex + i,
                    backgroundImagePath = imageFile.absolutePath
                )
            )
        }
        renderer.close()
        pfd.close()
        return importedCount
    }

    suspend fun exportNoteToPdf(noteId: Long, pageWidth: Int, pageHeight: Int): String? {
        val note = noteDao.getNoteById(noteId) ?: return null
        val pages = pageDao.getPagesForNote(noteId)
        if (pages.isEmpty() || pageWidth <= 0 || pageHeight <= 0) return null

        val pdfDocument = PdfDocument()

        pages.forEachIndexed { index, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            canvas.drawColor(android.graphics.Color.WHITE)

            page.backgroundImagePath?.let { path ->
                val bmp = android.graphics.BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    val dest = android.graphics.RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    canvas.drawBitmap(bmp, null, dest, null)
                    bmp.recycle()
                }
            }

            val strokes = deserializeStrokes(page.strokesJson)
            StrokeRenderer.renderStrokes(canvas, strokes)

            pdfDocument.finishPage(pdfPage)
        }

        val fileName = "${note.title.ifBlank { "Note" }}_${System.currentTimeMillis()}.pdf"
        val saved = writePdfToDownloads(pdfDocument, fileName)
        pdfDocument.close()
        return if (saved) fileName else null
    }

    private fun writePdfToDownloads(pdfDocument: PdfDocument, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out -> pdfDocument.writeTo(out) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
