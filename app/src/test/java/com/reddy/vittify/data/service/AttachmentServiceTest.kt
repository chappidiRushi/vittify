package com.reddy.vittify.data.service

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AttachmentServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFilesDir: File
    private lateinit var testContext: Context
    private lateinit var service: AttachmentService

    @Before
    fun setup() {
        testFilesDir = tempFolder.newFolder("files")
        testContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = testFilesDir
            override fun getPackageName(): String = "com.reddy.vittify"
        }
        service = AttachmentService(testContext)
    }

    @Test
    fun testParseAttachments() {
        assertEquals(emptyList<String>(), service.parseAttachments(""))
        assertEquals(emptyList<String>(), service.parseAttachments("   "))
        assertEquals(
            listOf("attachments/1.jpg", "attachments/2.pdf"),
            service.parseAttachments("attachments/1.jpg,attachments/2.pdf")
        )
        assertEquals(
            listOf("attachments/1.jpg"),
            service.parseAttachments("attachments/1.jpg,")
        )
    }

    @Test
    fun testJoinAttachments() {
        assertEquals("", service.joinAttachments(emptyList()))
        assertEquals(
            "attachments/1.jpg,attachments/2.pdf",
            service.joinAttachments(listOf("attachments/1.jpg", "attachments/2.pdf"))
        )
    }

    @Test
    fun testIsImage() {
        assertTrue(service.isImage("attachments/receipt.jpg"))
        assertTrue(service.isImage("attachments/receipt.jpeg"))
        assertTrue(service.isImage("attachments/receipt.png"))
        assertTrue(service.isImage("attachments/receipt.webp"))
        assertFalse(service.isImage("attachments/receipt.pdf"))
        assertFalse(service.isImage("attachments/document.docx"))
        assertFalse(service.isImage("attachments/spreadsheet.csv"))
    }

    @Test
    fun testGetAttachmentMimeType() {
        assertEquals("application/pdf", service.getAttachmentMimeType("attachments/invoice.pdf"))
        assertEquals("image/jpeg", service.getAttachmentMimeType("attachments/invoice.jpg"))
        assertEquals("text/csv", service.getAttachmentMimeType("attachments/sheet.csv"))
        assertEquals("application/octet-stream", service.getAttachmentMimeType("attachments/unknown.unknown_ext"))
    }

    @Test
    fun testDeleteAttachment() {
        val attachmentsDir = File(testFilesDir, "attachments").apply { mkdirs() }
        val testFile = File(attachmentsDir, "test.jpg").apply { writeText("dummy content") }
        assertTrue(testFile.exists())

        val result = service.deleteAttachment("attachments/test.jpg")
        assertTrue(result)
        assertFalse(testFile.exists())
    }

    @Test
    fun testDeleteNonExistentAttachmentReturnsTrue() {
        val result = service.deleteAttachment("attachments/non_existent.jpg")
        assertTrue(result)
    }

    @Test
    fun testGetAllAttachmentFiles() {
        val attachmentsDir = File(testFilesDir, "attachments").apply { mkdirs() }
        File(attachmentsDir, "receipt1.jpg").writeText("1")
        File(attachmentsDir, "receipt2.jpg").writeText("2")

        val files = service.getAllAttachmentFiles()
        assertEquals(2, files.size)
        assertTrue(files.any { it.name == "receipt1.jpg" })
        assertTrue(files.any { it.name == "receipt2.jpg" })
    }

    @Test
    fun testGetAbsolutePath() {
        val path = "attachments/test.jpg"
        val expected = File(testFilesDir, path).absolutePath
        assertEquals(expected, service.getAbsolutePath(path))
    }
}
