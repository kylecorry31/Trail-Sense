package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `isModelDownloaded returns false when model file does not exist`() {
        val manager = ModelManager(tempDir)
        assertFalse(manager.isModelDownloaded())
    }

    @Test
    fun `isModelDownloaded returns true when model file exists`() {
        val manager = ModelManager(tempDir)
        File(tempDir, ModelManager.MODEL_FILE_NAME).createNewFile()
        assertTrue(manager.isModelDownloaded())
    }

    @Test
    fun `getModelPath returns null when model not downloaded`() {
        val manager = ModelManager(tempDir)
        assertNull(manager.getModelPath())
    }

    @Test
    fun `getModelPath returns path when model exists`() {
        val manager = ModelManager(tempDir)
        val modelFile = File(tempDir, ModelManager.MODEL_FILE_NAME)
        modelFile.createNewFile()
        assertEquals(modelFile.absolutePath, manager.getModelPath())
    }

    @Test
    fun `deleteModel removes the model file`() {
        val manager = ModelManager(tempDir)
        val modelFile = File(tempDir, ModelManager.MODEL_FILE_NAME)
        modelFile.writeText("fake model data")
        assertTrue(modelFile.exists())
        manager.deleteModel()
        assertFalse(modelFile.exists())
    }

    @Test
    fun `deleteModel does nothing when no model exists`() {
        val manager = ModelManager(tempDir)
        manager.deleteModel()
        assertFalse(manager.isModelDownloaded())
    }
}
