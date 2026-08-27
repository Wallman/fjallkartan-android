package fjallkartan.fjallkartan

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fjallkartan.fjallkartan.product.ReviewPrompter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewPrompterInstrumentedTest {
    @Test
    fun requiresEngagementAndSuccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("review-prompter", Context.MODE_PRIVATE).edit().clear().commit()
        val prompter = ReviewPrompter(context, version = "test")
        repeat(2) { prompter.noteAppOpen() }
        repeat(2) { assertFalse(prompter.recordMeasurement(600.0)) }
        prompter.noteAppOpen()
        assertTrue(prompter.recordMeasurement(600.0))
        assertTrue(prompter.consume())
        assertFalse(prompter.consume())
    }
}
