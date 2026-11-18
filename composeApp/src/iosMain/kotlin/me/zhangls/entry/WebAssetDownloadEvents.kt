package me.zhangls.entry

import me.zhangls.about.handleWebAssetDownloadEvents

fun onWebAssetDownloadEvents(identifier: String, completion: () -> Unit) =
  handleWebAssetDownloadEvents(identifier, completion)
