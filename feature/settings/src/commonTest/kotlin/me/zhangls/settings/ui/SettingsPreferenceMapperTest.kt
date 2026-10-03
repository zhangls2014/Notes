package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.FontSizePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.PreferenceSpec
import me.zhangls.preference.ui.PreferenceUiModel
import me.zhangls.settings.mvi.SettingsIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsPreferenceMapperTest {
  @Test
  fun capabilityControlsOnlyDynamicColorAndKeysRemainUnique() {
    for (supported in listOf(false, true)) {
      val models = SettingsModel().toPreferenceUiModels({}, supported)
      val expected = buildList {
        add("profile")
        if (supported) add("dynamicColor")
        addAll(listOf("darkTheme", "fontSize", "appLanguage", "about", "logout"))
      }
      val keys = models.map { it.spec.key }
      assertEquals(expected, keys)
      assertEquals(keys.size, keys.toSet().size)
    }
  }

  @Test
  fun defaultCapabilityUsesPlatformValue() {
    assertEquals(
      SettingsModel().toPreferenceUiModels({}, supportsDynamicColor).map { it.spec.key },
      SettingsModel().toPreferenceUiModels({}).map { it.spec.key },
    )
  }

  @Test
  fun currentSettingsValuesAreMapped() {
    for (darkTheme in DarkThemeConfig.entries) {
      for (fontSize in FontSizeConfig.entries) {
        for (language in AppLanguage.entries) {
          for (dynamicColor in listOf(false, true)) {
            val settings = SettingsModel(dynamicColor, darkTheme, fontSize, language)
            val models = settings.toPreferenceUiModels({}, true)
            assertEquals(dynamicColor, assertIs<PreferenceUiModel.Toggle>(models[1]).value)
            assertEquals(darkTheme, models.select(DarkThemePreference.spec).value)
            assertEquals(fontSize, models.select(FontSizePreference.spec).value)
            assertEquals(language, models.select(LanguagePreference.spec).value)
          }
        }
      }
    }
  }

  @Test
  fun actionsSendTheirCorrespondingIntents() {
    for (supported in listOf(false, true)) {
      val sent = mutableListOf<SettingsIntent>()
      val models = SettingsModel().toPreferenceUiModels({ sent.add(it) }, supported)
      for (key in listOf("profile", "about", "logout")) {
        assertIs<PreferenceUiModel.Action>(models.single { it.spec.key == key }).onClick()
      }
      assertEquals(
        listOf(SettingsIntent.OpenProfile, SettingsIntent.OpenAbout, SettingsIntent.ClickLogout),
        sent,
      )
    }
  }

  @Test
  fun valueChangesSendNewValuesWithoutDispatchDuringMapping() {
    val sent = mutableListOf<SettingsIntent>()
    val models = SettingsModel().toPreferenceUiModels({ sent.add(it) }, true)
    assertEquals(emptyList(), sent)
    val toggle = assertIs<PreferenceUiModel.Toggle>(models.single { it.spec.key == "dynamicColor" })
    for (value in listOf(true, false)) toggle.onValueChange(value)
    for (value in DarkThemeConfig.entries) models.select(DarkThemePreference.spec).onValueChange(value)
    for (value in FontSizeConfig.entries) models.select(FontSizePreference.spec).onValueChange(value)
    for (value in AppLanguage.entries) models.select(LanguagePreference.spec).onValueChange(value)
    val expected = buildList {
      add(SettingsIntent.UpdateDynamicColor(true))
      add(SettingsIntent.UpdateDynamicColor(false))
      addAll(DarkThemeConfig.entries.map { SettingsIntent.UpdateDarkTheme(it) })
      addAll(FontSizeConfig.entries.map { SettingsIntent.UpdateFontSize(it) })
      addAll(AppLanguage.entries.map { SettingsIntent.UpdateAppLanguage(it) })
    }
    assertEquals(expected, sent)
  }

  // spec 的类型参数与模型在装配时一致；先核对 spec，再集中处理擦除后的类型转换。
  @Suppress("UNCHECKED_CAST")
  private fun <T> List<PreferenceUiModel>.select(spec: PreferenceSpec.Select<T>): PreferenceUiModel.Select<T> =
    assertIs<PreferenceUiModel.Select<*>>(single { it.spec == spec }) as PreferenceUiModel.Select<T>
}
