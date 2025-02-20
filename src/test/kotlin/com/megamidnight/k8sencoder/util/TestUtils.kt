package com.megamidnight.k8sencoder.util

import io.mockk.clearAllMocks
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Utility functions and extensions for testing
 */
object TestUtils {
    /**
     * Creates a temporary file with the given content
     * @param content Content to write to the file
     * @return The created temporary file
     */
    fun createTempFileWithContent(content: String): String {
        val tempFile = createTempFile()
        tempFile.writeText(content)
        tempFile.deleteOnExit()
        return tempFile.absolutePath
    }

    /**
     * Creates a temporary directory
     * @return The created temporary directory
     */
    fun createTempDir(): String {
        val tempDir = kotlin.io.createTempDir()
        tempDir.deleteOnExit()
        return tempDir.absolutePath
    }
}

/**
 * JUnit extension to clear all mocks before each test
 */
class MockClearingExtension : BeforeEachCallback {
    override fun beforeEach(context: ExtensionContext?) {
        clearAllMocks()
    }
}