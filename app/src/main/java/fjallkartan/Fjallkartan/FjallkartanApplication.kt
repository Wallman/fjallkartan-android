package fjallkartan.fjallkartan

import android.app.Application
import android.util.Log
import fjallkartan.fjallkartan.map.KartverketTileProxy
import fjallkartan.fjallkartan.settings.RemoteSettings
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineManager

class FjallkartanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        OfflineManager.getInstance(this).setMaximumAmbientCacheSize(
            500L * 1024 * 1024,
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit

                override fun onError(message: String) {
                    Log.e("Fjallkartan", "Could not size the tile cache: $message")
                }
            },
        )
        RemoteSettings.initialize(this)
        KartverketTileProxy.start()
    }
}
