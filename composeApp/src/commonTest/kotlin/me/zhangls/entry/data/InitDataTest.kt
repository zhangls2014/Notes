package me.zhangls.entry.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.zhangls.data.model.CommonModel
import me.zhangls.data.repository.CommonRepository
import me.zhangls.framework.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class InitDataTest {
  @Test
  fun firstLaunchRecordsCountAndVersion() = runTest {
    val common = FakeCommonRepository(CommonModel())
    val initData = InitData(common, FakeAppInfo(42))

    initData.run()

    assertEquals(1L, common.commonFlow.value.launchCount)
    assertEquals(42L, common.commonFlow.value.lastVersionCode)
  }

  @Test
  fun laterLaunchRecordsNewVersionAndIncrementsCount() = runTest {
    val common = FakeCommonRepository(CommonModel(launchCount = 3, lastVersionCode = 42))

    InitData(common, FakeAppInfo(43)).run()

    assertEquals(4L, common.commonFlow.value.launchCount)
    assertEquals(43L, common.commonFlow.value.lastVersionCode)
  }
}

private class FakeAppInfo(private val version: Long) : AppInfo {
  override fun getVersionCode(): Long = version
  override fun getVersionName(): String = "test"
  override fun getBuildNumber(): String = version.toString()
}

private class FakeCommonRepository(initial: CommonModel) : CommonRepository {
  override val commonFlow = MutableStateFlow(initial)

  override suspend fun increaseLaunchCount() {
    commonFlow.value = commonFlow.value.copy(launchCount = commonFlow.value.launchCount + 1)
  }

  override suspend fun updateVersionCode(versionCode: Long) {
    commonFlow.value = commonFlow.value.copy(lastVersionCode = versionCode)
  }
}
