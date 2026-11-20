package me.zhangls.entry

import androidx.compose.material3.Text
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeTestDiagnosticsTest {
  @Test fun failurePreservesOriginalErrorAndWritesScreenshotAndSemantics() {
    val compose = createLegacyDiagnosedComposeRule()
    val original = AssertionError("diagnostic probe")
    val description = Description.createTestDescription(javaClass, "probe-${System.nanoTime()}")
    val statement = object : Statement() {
      override fun evaluate() {
        compose.setContent { Text("Diagnostic sample") }
        compose.waitForIdle()
        throw original
      }
    }
    try {
      drainMainLooperAfter(compose).apply(statement, description).evaluate()
      error("Expected failure")
    } catch (failure: AssertionError) {
      assertSame(original, failure)
    }
    val name = "${description.className}-${description.methodName}".replace(Regex("[^a-zA-Z0-9_.-]"), "_")
    val directory = File("build/compose-test-failures", name)
    assertTrue(File(directory, "failure.txt").readText().contains("diagnostic probe"))
    assertTrue(File(directory, "root-0.png").length() > 0)
    assertTrue(File(directory, "root-0.txt").readText().contains("Diagnostic sample"))
  }
}
