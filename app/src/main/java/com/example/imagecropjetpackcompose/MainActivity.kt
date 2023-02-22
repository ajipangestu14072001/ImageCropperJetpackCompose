package com.example.imagecropjetpackcompose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.imagecropjetpackcompose.ui.theme.ImageCropJetpackComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCropJetpackComposeTheme {
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                val bitmap = remember { mutableStateOf<ImageBitmap?>(null) }
                val cropLauncher =
                    rememberLauncherForActivityResult(CropImageContract()) { result ->
                        result.uriContent?.let { imageUri = it }
                        result.error?.let { println(it.toString()) }
                    }
                val cameraLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmapImage ->
                        bitmapImage?.let {
                            bitmap.value = it.asImageBitmap()
                            val tempUri = getTempUri(applicationContext)
                            applicationContext.contentResolver.openOutputStream(tempUri)
                                .use { outputStream ->
                                    it.compress(Bitmap.CompressFormat.PNG,100, outputStream)
                                }
                            imageUri = tempUri
                            cropLauncher.launch(
                                CropImageContractOptions(
                                    imageUri,
                                    CropImageOptions()
                                )
                            )
                        }
                    }

                val galleryLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        uri?.let {
                            cropLauncher.launch(
                                CropImageContractOptions(
                                    uri,
                                    CropImageOptions()
                                )
                            )
                        }
                    }

                imageUri.takeIf { it != null }?.let { uri ->
                    bitmap.value = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri)
                            .asImageBitmap()
                    } else {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(applicationContext.contentResolver, uri)
                        ).asImageBitmap()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    bitmap.value?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier.size(400.dp)
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            galleryLauncher.launch("image/*")
                        }) {
                        Text(text = "Galery")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            cameraLauncher.launch(null)
                        }) {
                        Text(text = "Camera")
                    }
                }
            }
        }
    }
}

fun getTempUri(context: Context): Uri {
    val fileName = "temp_image.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )!!
}