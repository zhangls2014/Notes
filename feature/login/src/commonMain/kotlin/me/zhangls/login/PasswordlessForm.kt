package me.zhangls.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zhangls.login.api.LocalPasskeyProvider
import me.zhangls.login.api.LocalPasswordlessAuth
import me.zhangls.login.mvi.LoginIntent
import me.zhangls.login.mvi.LoginState
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Passkey
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_passkey
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PasswordlessForm(state: LoginState, onIntent: (LoginIntent) -> Unit) {
  val auth = LocalPasswordlessAuth.current ?: return
  val passkey = LocalPasskeyProvider.current?.takeIf { it.isSupported } ?: return
  TextButton(
    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
    enabled = !state.isLoading,
    onClick = { onIntent(LoginIntent.SignInWithPasskey(auth, passkey)) },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(imageVector = Icons.Rounded.Passkey, contentDescription = null)
      Text(stringResource(Res.string.login_passkey), textAlign = TextAlign.Center)
    }
  }
}
