package fjallkartan.fjallkartan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fjallkartan.fjallkartan.map.MapScreen
import fjallkartan.fjallkartan.ui.theme.FjallkartanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FjallkartanTheme {
                MapScreen()
            }
        }
    }
}
