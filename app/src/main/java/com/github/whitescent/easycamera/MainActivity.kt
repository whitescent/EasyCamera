package com.github.whitescent.easycamera

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.whitescent.easycamera.ui.theme.EasycameraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import com.github.whitescent.R

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContent {
      EasycameraTheme {
        val systemUiController = rememberSystemUiController()
        val useDarkIcons = !isSystemInDarkTheme()
        systemUiController.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        SideEffect {
          systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
          )
          systemUiController.isSystemBarsVisible = false
        }
        MainContent()
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MainContent() {
  val cameraPermissionState = rememberPermissionState(
    android.Manifest.permission.CAMERA
  )
  Box(
    modifier = Modifier.fillMaxSize().background(AppTheme.colorScheme.background)
  ) {
    if (cameraPermissionState.status.isGranted) {
      CameraPreview()
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
          buildAnnotatedString {
            append(stringResource(id = R.string.rationale))
          }
        } else {
          buildAnnotatedString {
            append(stringResource(id = R.string.request_rationale))
            append("\n")
            withStyle(style = SpanStyle(fontSize = 16.sp, color = Color.Gray)) {
              append(stringResource(id = R.string.tips))
            }
          }
        }
        Text(
          text = textToShow,
          style = AppTheme.typography.titleLarge,
          color = AppTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.padding(vertical = 10.dp))
        Button(
          onClick = { cameraPermissionState.launchPermissionRequest() }
        ) {
          Text(stringResource(id = R.string.request_permissions))
        }
      }
    }
  }
}

@Composable
fun CameraPreview(
  modifier: Modifier = Modifier,
  scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
  cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
  val coroutineScope = rememberCoroutineScope()
  val lifecycleOwner = LocalLifecycleOwner.current
  AndroidView(
    modifier = modifier,
    factory = { context ->
      val previewView = PreviewView(context).apply {
        this.scaleType = scaleType
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
      val previewUseCase = Preview.Builder()
        .build()
        .also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }
      coroutineScope.launch {
        val cameraProvider = context.getCameraProvider()
        try {
          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, previewUseCase
          )
        } catch (ex: Exception) {
          Log.e("CameraPreview", "Use case binding failed", ex)
        }
      }
      previewView
    }
  )
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
  ProcessCameraProvider.getInstance(this).also { future ->
    future.addListener({
      continuation.resume(future.get())
    }, executor)
  }
}

val Context.executor: Executor
  get() = ContextCompat.getMainExecutor(this)

typealias AppTheme = MaterialTheme