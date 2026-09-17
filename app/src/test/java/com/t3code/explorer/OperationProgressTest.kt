package com.t3code.explorer

import com.t3code.explorer.domain.model.OperationProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationProgressTest {
    @Test fun `completed zero byte operation reports one hundred percent`() {
        assertEquals(100, OperationProgress("copy", "empty", 0, 0, 1, 1, isComplete = true).percent)
    }
}
