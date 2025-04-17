package com.dalsegnosolutions.ij.swrf.editor

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class FoldByWheelTest : BasePlatformTestCase() {

    fun testLogic() {
        assertTrue(true)
    }

    override fun getTestDataPath() = "src/test/testData/fold"
}
