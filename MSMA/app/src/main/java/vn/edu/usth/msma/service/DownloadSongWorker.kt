package vn.edu.usth.msma.service

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadSongWorker(
    appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val songUrl = inputData.getString("song_url") ?: return Result.failure()
            val songTitle = inputData.getString("song_title") ?: "Unknown"
            val songId = inputData.getString("song_id") ?: "0"

            // Create directory in Downloads/MusicHub
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MusicHub"
            )
            downloadsDir.mkdirs() // Create directory if it doesn't exist

            // Create file with name format: songTitle_songId.mp3
            val fileName = "${songTitle}.mp3"
            val file = File(downloadsDir, fileName)

            // Download file
            downloadFile(songUrl, file)

            // Broadcast download completion
            val intent = Intent("MUSIC_EVENT").apply {
                putExtra("ACTION", "DOWNLOAD_COMPLETE")
                putExtra("SONG_ID", songId)
                putExtra("FILE_PATH", file.absolutePath)
            }
            applicationContext.sendBroadcast(intent)

            Log.d("DownloadSongWorker", "Tải xong: ${file.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadSongWorker", "Lỗi tải bài hát: ${e.message}")
            Result.retry() // Retry if there's an error
        }
    }

    private fun downloadFile(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()

        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            outputStream.write(buffer, 0, len)
        }

        outputStream.close()
        inputStream.close()
    }
}