package me.zhangls.email.component

import androidx.compose.runtime.Composable
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_label_empty_subject
import org.jetbrains.compose.resources.stringResource


@Composable
fun String.toDisplaySubject(): String = ifBlank { stringResource(Res.string.email_label_empty_subject) }
