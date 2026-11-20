package me.zhangls.about

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DeviceViewportPublisherTest {
  @Test fun publisherOnlyTargetsCurrentTrustedMainDocumentAndDeduplicates() {
    val publisher = DeviceViewportPublisher("https://example.com/")
    publisher.bindDocument("test-document")
    assertNull(publisher.publish("http://example.com/", "[]"))
    assertNull(publisher.publish("https://other.example/", "[]"))
    val first = requireNotNull(publisher.publish("https://example.com/releases/", "[]"))
    assertContains(first, "location.origin")
    assertContains(first, "documentId:bridge.documentId")
    assertContains(first, "\"sequence\":1")
    assertNull(publisher.publish("https://example.com/releases/", "[]"))
    publisher.reset()
    val next = requireNotNull(publisher.publish("https://example.com/releases/", "[]"))
    assertContains(next, "\"sequence\":2")
    assertFalse(first.contains("addJavascriptInterface"))
  }
}
