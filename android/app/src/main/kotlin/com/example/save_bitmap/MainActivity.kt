package com.example.save_bitmap

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.flutter.Log
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.sql.Timestamp
import java.util.*

class MainActivity: FlutterFragmentActivity() {
    private val CHANNEL = "saveBitmap/test"
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            // This method is invoked on the main thread.
                call, result ->
            if (call.method == "saveImage") {
                var workResult = false;
                try {
                    workResult = saveImage(
                        image = call.argument<ByteArray>("bitmap")!!,
                        folderName = call.argument<String>("album")!!
                    )

                } catch (e: Exception) {

                    e.printStackTrace()
                    Log.e("Android Native Fail", "$e")

                }

                if (workResult) {
                    result.success(workResult)
                } else {
                    result.error("Fail", "Android Native Fail.", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    @Throws(FileNotFoundException::class, NullPointerException::class)
    private fun saveImage(
        image: ByteArray,
        folderName: String
    ): Boolean {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            values.put(MediaStore.Images.Media.IS_PENDING, true)

            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? = this.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                saveImageToStream(bitmap, this.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                this.contentResolver.update(uri, values, null, null)
                return true
            }
            return false
        } else {

            val dir = File(
                this.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                folderName
            )

            // getExternalStorageDirectory is deprecated in API 29

            if (!dir.exists()) {

                dir.mkdirs()

            }

            val date = Date()

            val fullFileName = "myFileName.png"

            val fileName = fullFileName.substring(0, fullFileName.lastIndexOf("."))
            val extension = fullFileName.substring(fullFileName.lastIndexOf("."))

            val imageFile = File(
                dir.absolutePath
                    .toString() + File.separator
                        + fileName + "_" + Timestamp(date.time).toString()
                        + extension
            )
            try {
                saveImageToStream(bitmap, FileOutputStream(imageFile))

            } catch (e: Exception) {


                e.printStackTrace()
                Log.e("Android Native Fail", "$e")
                return false
            }

            val values = ContentValues()

            values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)

            // .DATA is deprecated in API 29

            this.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return true
        }
    }

    private fun contentValues(): ContentValues {
        val values = ContentValues()

        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

        return values

    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}
