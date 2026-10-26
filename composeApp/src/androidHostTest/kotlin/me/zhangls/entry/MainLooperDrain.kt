package me.zhangls.entry

import android.os.Looper
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.robolectric.Shadows.shadowOf

/** Drain cancelled UI work after Compose disposal, before Robolectric clears the main queue. */
internal fun drainMainLooperAfter(rule: TestRule): TestRule = RuleChain.outerRule(
  TestRule { base, _ -> object : Statement() {
    override fun evaluate() {
      try {
        base.evaluate()
      } finally {
        // AndroidUiDispatcher.Main retains its scheduling flag across tests. Clearing the
        // queue before its callback runs strands Paging collection and animation work.
        shadowOf(Looper.getMainLooper()).idle()
      }
    }
  } },
).around(rule)
