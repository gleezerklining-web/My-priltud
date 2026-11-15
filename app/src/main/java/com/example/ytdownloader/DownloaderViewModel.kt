package com.example.ytdownloader

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DownloaderViewModel(app: Application) : AndroidViewModel(app) {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    fun download(url: String, format: String) {
        if (!isYouTubeUrl(url)) {
            _status.value = "❌ Это не похоже на ссылку YouTube"
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            _progress.value = 0f
            _status.value = "Подготовка..."

            try {
                val result = withContext(Dispatchers.IO) {
                    downloadInternal(url.trim(), format)
                }
                _status.value = "✅ Готово: ${result.name}\nПуть: ${result.parent}"
                _progress.value = 1f
            } catch (e: Exception) {
                Log.e("Downloader", "Ошибка", e)
                _status.value = "❌ Ошибка: ${e.message}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private fun downloadInternal(url: String, format: String): File {
        val app = getApplication<Application>()
        val tempDir = File(app.cacheDir, "ytdl").apply { mkdirs() }

        val request = YoutubeDLRequest(url)
        request.addOption("-o", "${tempDir.absolutePath}/%(title)s.%(ext)s")

        when (format) {
            "mp3" -> {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            }
            "m4a" -> {
                request.addOption("-x")
                request.addOption("--audio-format", "m4a")
            }
            "wav" -> {
                request.addOption("-x")
                request.addOption("--audio-format", "wav")
            }
            "mp4" -> {
                request.addOption("-f", "bestvideo[height<=1080]+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
            }
        }

        YoutubeDL.getInstance().execute(request) { p, _, _ ->
            _progress.value = (p / 100f).coerceIn(0f, 1f)
        }

        val downloaded = tempDir.listFiles()
            ?.filter { it.isFile }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("Файл не найден после скачивания")

        val publicDir = File(
            app.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
            "YouTubeDownloader"
        ).apply { mkdirs() }

        val target = File(publicDir, downloaded.name)
        FileInputStream(downloaded).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
        downloaded.delete()
        return target
    }

    private fun isYouTubeUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.contains("youtube.com/watch") ||
                trimmed.contains("youtu.be/") ||
                trimmed.contains("youtube.com/shorts/")
    }
}
