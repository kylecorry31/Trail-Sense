package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelManagerTest {

    @TempDir
    lateinit var tempDir: File

    private var selectedModelId = ModelManager.DEFAULT_MODEL_ID

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

    @Test
    fun `selectedModel changes the active model`() {
        val manager = createManager()
        val e4b = manager.models.first { it.id == "gemma-4-e4b-it" }

        manager.selectedModel = e4b

        assertEquals(e4b, manager.selectedModel)
    }

    @Test
    fun `getModelPath uses selected model`() {
        val manager = createManager()
        val e4b = manager.models.first { it.id == "gemma-4-e4b-it" }
        manager.selectedModel = e4b
        val modelFile = File(tempDir, e4b.fileName)
        modelFile.createNewFile()

        assertEquals(modelFile.absolutePath, manager.getModelPath())
    }

    @Test
    fun `deleteModel removes only the selected model`() {
        val manager = createManager()
        val e2b = manager.models.first { it.id == ModelManager.DEFAULT_MODEL_ID }
        val e4b = manager.models.first { it.id == "gemma-4-e4b-it" }
        val e2bFile = File(tempDir, e2b.fileName)
        val e4bFile = File(tempDir, e4b.fileName)
        e2bFile.createNewFile()
        e4bFile.createNewFile()
        manager.selectedModel = e4b

        manager.deleteModel()

        assertTrue(e2bFile.exists())
        assertFalse(e4bFile.exists())
    }

    private fun createManager(): ModelManager {
        return ModelManager(
            tempDir,
            getSelectedModelId = { selectedModelId },
            setSelectedModelId = { selectedModelId = it }
        )
    }
}
