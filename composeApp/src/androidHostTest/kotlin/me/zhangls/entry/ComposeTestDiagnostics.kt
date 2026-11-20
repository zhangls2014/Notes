package me.zhangls.entry

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.TestFailureHandler
import androidx.compose.ui.test.TestFailurePolicy
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.printToString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.AssumptionViolatedException
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

internal fun createLegacyDiagnosedComposeRule(): ComposeContentTestRule = createDiagnosedComposeRule(false)

/** Host roots must be captured before teardown; UiAutomation screenshots require a device. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun createDiagnosedComposeRule(useStandardDispatcher: Boolean = true): ComposeContentTestRule {
  var directory: File? = null
  val compose = androidx.compose.ui.test.junit4.v2.createComposeRule(
    ComposeUiTestConfig(
      effectContext = if (useStandardDispatcher) EmptyCoroutineContext else UnconfinedTestDispatcher(),
      testTimeout = Duration.INFINITE,
      failurePolicy = TestFailurePolicy(
        screenshotCaptureMode = TestFailurePolicy.CaptureMode.Disabled,
        uiHierarchyCaptureMode = TestFailurePolicy.CaptureMode.Disabled,
        failureHandlers = listOf(
          TestFailureHandler { context ->
            directory?.let { File(it, "failure.txt").writeText(context.error.stackTraceToString()) }
          },
        ),
      ),
    ),
  )
  return object : ComposeContentTestRule by compose {
    override fun apply(base: Statement, description: Description): Statement {
      val diagnosed = object : Statement() {
        override fun evaluate() {
          runCatching { base.evaluate() }.onFailure { failure ->
            if (failure !is AssumptionViolatedException) {
              directory = captureFailure(compose, description, failure)
            }
          }.getOrThrow()
        }
      }
      return compose.apply(diagnosed, description)
    }
  }
}

private fun captureFailure(compose: ComposeContentTestRule, description: Description, failure: Throwable): File {
  val name = "${description.className}-${description.methodName}".replace(Regex("[^a-zA-Z0-9_.-]"), "_")
  val directory = File("build/compose-test-failures", name)
  runCatching {
    check(directory.isDirectory || directory.mkdirs())
    val roots = compose.onAllNodes(isRoot())
    roots.fetchSemanticsNodes().indices.forEach { index ->
      File(directory, "root-$index.txt").writeText(roots[index].printToString())
      File(directory, "root-$index.png").outputStream().use { stream ->
        roots[index].captureToImage().asAndroidBitmap().compress(
          android.graphics.Bitmap.CompressFormat.PNG,
          100,
          stream,
        )
      }
    }
  }.exceptionOrNull()?.let(failure::addSuppressed)
  return directory
}
