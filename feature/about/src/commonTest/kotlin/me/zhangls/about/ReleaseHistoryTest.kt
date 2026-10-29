package me.zhangls.about

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReleaseHistoryTest {
  private fun release(version: String, date: String? = "2026-10-30", platform: String = "android") =
    AppRelease(version, date, listOf(ReleaseChange("added", listOf(platform), mapOf("en" to "Added", "zh" to "新增"))))

  @Test fun sortsNumericallyAndFiltersPlatform() {
    val releases = listOf(release("1.2.0"), release("1.10.0"), release("2.0.0", platform = "ios"))
    assertEquals(listOf("1.10.0", "1.2.0"), releases.forPlatform("android").map { it.version })
  }

  @Test fun filtersChangesAndFallsBackToEnglish() {
    val android = release("1.0.0").changes.single()
    val ios = release("1.0.0", platform = "ios").changes.single()
    val filtered = listOf(AppRelease("1.0.0", null, listOf(android, ios))).forPlatform("ios").single()
    assertEquals(listOf(ios), filtered.changes)
    assertEquals("Added", ios.localizedText("fr"))
    assertEquals("新增", ios.localizedText("zh"))
  }

  @Test fun parsesTheStoredFormatAndRejectsMissingFields() {
    val json = """
      [{"version":"1.0.0","date":null,"changes":[
        {"type":"added","platforms":["android","ios"],"text":{"en":"Added","zh":"新增"}}
      ]}]
    """.trimIndent()
    assertEquals("1.0.0", parseReleases(json).single().version)
    assertFailsWith<IllegalArgumentException> { parseReleases("[{\"version\":\"1.0.0\"}]") }
  }

  @Test fun rejectsInvalidRecords() {
    assertFailsWith<IllegalArgumentException> { validateReleases(listOf(release("1.0.0"), release("1.0.0"))) }
    for (version in listOf("1.0", "01.0.0", "-1.0.0")) {
      assertFailsWith<IllegalArgumentException> { validateReleases(listOf(release(version))) }
    }
    assertFailsWith<IllegalArgumentException> { validateReleases(listOf(release("1.0.0", "today"))) }
    assertFailsWith<IllegalArgumentException> { validateReleases(listOf(release("1.0.0", platform = "web"))) }
    assertFailsWith<IllegalArgumentException> { validateReleases(listOf(release("1.0.0", null), release("2.0.0"))) }
  }
}
