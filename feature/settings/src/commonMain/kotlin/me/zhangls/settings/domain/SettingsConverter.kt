package me.zhangls.settings.domain

import me.zhangls.data.model.SettingsModel

/**
 * @author zhangls
 */
expect fun SettingsModel.mapToPreferences(): List<Preference<*>>