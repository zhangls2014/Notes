package me.zhangls.data.impl.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseNotesSettingsTest {
  @Test
  fun legacySettingsAndAcknowledgementSurviveRepositoryRecreation() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-release-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    prefs.edit { it[stringPreferencesKey("settings")] = """{"dynamicColor":true}""" }
    val repository = SettingsRepositoryImpl(prefs)
    assertNull(repository.settingsFlow.first().lastReadReleaseVersion)
    repository.updateLastReadReleaseVersion("1.1.0")
    val restored = SettingsRepositoryImpl(prefs).settingsFlow.first()
    assertEquals("1.1.0", restored.lastReadReleaseVersion)
    assertEquals(true, restored.dynamicColor)
    repository.updateDynamicColor(false)
    assertEquals("1.1.0", repository.settingsFlow.first().lastReadReleaseVersion)
    repository.updateLastReadReleaseVersion("1.2.0")
    assertEquals("1.2.0", repository.settingsFlow.first().lastReadReleaseVersion)
  }
}
