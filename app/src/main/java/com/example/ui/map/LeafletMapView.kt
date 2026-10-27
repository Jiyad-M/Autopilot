package com.example.ui.map

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.NavigationViewModel

class LeafletBridge(
  private val onReady: () -> Unit,
  private val onMapClick: (Double, Double) -> Unit,
  private val onObjectClick: (Long, String, String, Double, Double) -> Unit
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun onMapReady() {
    mainHandler.post { onReady() }
  }

  @JavascriptInterface
  fun onMapClick(lat: Double, lng: Double) {
    mainHandler.post { onMapClick(lat, lng) }
  }

  @JavascriptInterface
  fun onMapObjectClick(id: Long, title: String, type: String, lat: Double, lng: Double) {
    mainHandler.post { onObjectClick(id, title, type, lat, lng) }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
  viewModel: NavigationViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val webView = remember {
    WebView(context).apply {
      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        cacheMode = WebSettings.LOAD_DEFAULT
        allowFileAccess = true
        allowContentAccess = true
      }
      webViewClient = WebViewClient()
      webChromeClient = WebChromeClient()

      val bridge = LeafletBridge(
        onReady = { viewModel.onMapReady() },
        onMapClick = { lat, lng -> viewModel.onMapClick(lat, lng) },
        onObjectClick = { id, title, type, lat, lng ->
          viewModel.onMapObjectClick(id, title, type, lat, lng)
        }
      )
      addJavascriptInterface(bridge, "AndroidBridge")

      loadUrl("file:///android_asset/leaflet/map.html")
    }
  }

  DisposableEffect(webView) {
    viewModel.onMapCommand = { script ->
      webView.post {
        webView.evaluateJavascript(script, null)
      }
    }
    onDispose {
      viewModel.onMapCommand = null
    }
  }

  AndroidView(
    factory = { webView },
    modifier = modifier
      .fillMaxSize()
      .testTag("leaflet_webview")
  )
}
