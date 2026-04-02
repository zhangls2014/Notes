package me.zhangls.main.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.main.EmailViewModel

@OptIn(
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
expect fun HomeScreen(isBottomNavigationBar: Boolean, viewmodel: EmailViewModel)
