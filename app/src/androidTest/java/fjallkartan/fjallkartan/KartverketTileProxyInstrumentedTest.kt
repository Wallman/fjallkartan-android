package fjallkartan.fjallkartan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import fjallkartan.fjallkartan.map.KartverketTileProxy
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KartverketTileProxyInstrumentedTest {
    @Test
    fun rewritesCreamPixelsToTransparent() {
        val source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.argb(255, 255, 255, 230))
        source.setPixel(1, 0, Color.argb(255, 20, 40, 60))
        val encoded = ByteArrayOutputStream().also {
            source.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()

        val rewritten = KartverketTileProxy.NoDataFill.rewrite(encoded)
        val result = BitmapFactory.decodeByteArray(rewritten, 0, rewritten.size)

        assertEquals(0, Color.alpha(result.getPixel(0, 0)))
        assertEquals(Color.rgb(20, 40, 60), result.getPixel(1, 0))
    }
}
