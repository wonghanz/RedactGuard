package dev.capriguard.redactguard.redact

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.capriguard.redactguard.core.AiClient
import dev.capriguard.redactguard.core.AiConfig
import dev.capriguard.redactguard.core.SettingsRepository
import dev.capriguard.redactguard.core.encodeJpegBase64
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Mode { Photo, Text }

/**
 * Everything this app touches lives in memory until you press Save or Share.
 *
 * The picked picture is copied into the cache only because the content stream a
 * share sheet hands over cannot be opened twice — once for the metadata pass and
 * once for the decode — and that copy is deleted the moment decoding finishes. The
 * original in your gallery is never modified, and no history, queue or thumbnail
 * database is kept anywhere.
 */
class RedactViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val ai = AiClient()

    var config by mutableStateOf(
        AiConfig(SettingsRepository.DEFAULT_BASE_URL, "", SettingsRepository.DEFAULT_MODEL, false),
    )
        private set

    init {
        viewModelScope.launch { settings.config.collect { config = it } }
    }

    fun setBaseUrl(v: String) { viewModelScope.launch { settings.setBaseUrl(v) } }
    fun setApiKey(v: String) { viewModelScope.launch { settings.setApiKey(v) } }
    fun setModel(v: String) { viewModelScope.launch { settings.setModel(v) } }
    fun setSecondReadOn(v: Boolean) { viewModelScope.launch { settings.setVisionOn(v) } }

    var mode by mutableStateOf(Mode.Photo)
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var note by mutableStateOf<String?>(null)
        private set

    fun pickMode(m: Mode) {
        mode = m
        error = null
    }

    fun dismissNote() { note = null }
    fun dismissError() { error = null }

    /* ------------------------------------------------------------------ text */

    var text by mutableStateOf("")
        private set

    var findings by mutableStateOf<List<Finding>>(emptyList())
        private set

    /** Indices into [findings] the person has switched on. Firm shapes start on. */
    var chosen by mutableStateOf<Set<Int>>(emptySet())
        private set

    var redacted by mutableStateOf("")
        private set

    /** True while the text on screen came in through the share sheet. */
    var fromShare by mutableStateOf(false)
        private set

    fun typeText(v: String) {
        text = v
        val f = TextScanner.scan(v)
        findings = f
        chosen = f.indices.filter { f[it].autoApply }.toSet()
        recompute()
    }

    fun receiveSharedText(v: String) {
        fromShare = true
        mode = Mode.Text
        typeText(v)
    }

    fun toggle(i: Int) {
        chosen = if (i in chosen) chosen - i else chosen + i
        recompute()
    }

    fun selectAllFirm() {
        chosen = findings.indices.filter { findings[it].autoApply }.toSet()
        recompute()
    }

    fun selectNone() {
        chosen = emptySet()
        recompute()
    }

    private fun recompute() {
        redacted = TextScanner.apply(text, findings.filterIndexed { i, _ -> i in chosen })
    }

    val enabledFindings: List<Finding> get() = findings.filterIndexed { i, _ -> i in chosen }

    /* ----------------------------------------------------------------- photo */

    var loaded by mutableStateOf<Loaded?>(null)
        private set

    /** Rectangles drawn but not yet written into the pixels. */
    var pending by mutableStateOf<List<BoxPx>>(emptyList())
        private set

    var drag by mutableStateOf<BoxPx?>(null)
        private set

    var fill by mutableStateOf(Fill.Solid)
        private set

    var block by mutableStateOf(24)
        private set

    /** Measured after the last apply: how much of each region really changed. */
    var checks by mutableStateOf<List<RegionCheck>>(emptyList())
        private set

    var rounds by mutableStateOf(0)
        private set

    /** Bytes and surviving tags of the last encoding this app produced. */
    var outCheck by mutableStateOf<OutCheck?>(null)
        private set

    data class OutCheck(val bytes: Int, val survived: List<Given>, val moment: String)

    fun pickFill(f: Fill) { fill = f }
    fun pickBlock(n: Int) { block = n.coerceIn(4, 60) }

    fun dragStart(b: BoxPx) { drag = b }
    fun dragTo(b: BoxPx) { drag = b }

    fun dragEnd(b: BoxPx?) {
        drag = null
        if (b != null && !b.degenerate()) {
            pending = pending + b
            error = null
        }
    }

    fun undoLast() {
        if (pending.isNotEmpty()) pending = pending.dropLast(1)
    }

    fun clearBoxes() {
        pending = emptyList()
        drag = null
    }

    /**
     * Copies the incoming stream, decodes it, reads what it gives away, then deletes
     * the copy. Two passes over one stream is not possible, and re-opening the
     * share-sheet URI is not guaranteed.
     */
    fun takeImage(uri: Uri) {
        busy = true
        error = null
        note = null
        outCheck = null
        viewModelScope.launch {
            runCatching {
                val ctx = getApplication<Application>()
                val tmp = File(ctx.cacheDir, "incoming-copy.tmp")
                val input = ctx.contentResolver.openInputStream(uri)
                    ?: throw IOException("The other app would not hand the picture over.")
                input.use { src -> tmp.outputStream().use { out -> src.copyTo(out) } }
                val loaded = withContext(Dispatchers.Default) { ImageAudit.load(tmp) }
                tmp.delete()
                loaded
            }.onSuccess { l ->
                loaded = l
                pending = emptyList()
                checks = emptyList()
                rounds = 0
                mode = Mode.Photo
                fromShare = false
                busy = false
            }.onFailure {
                error = it.message ?: "That file could not be read."
                busy = false
            }
        }
    }

    fun applyRedaction() {
        val l = loaded ?: return
        val boxes = pending
        if (boxes.isEmpty()) {
            error = "Nothing to apply. Drag a rectangle over the part you want gone first."
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) { ImageAudit.redact(l.bitmap, boxes, fill, block) }
            }.onSuccess { (bmp, cs) ->
                loaded = l.copy(bitmap = bmp)
                checks = cs
                rounds += 1
                pending = emptyList()
                outCheck = null
                busy = false
            }.onFailure {
                error = it.message ?: "The fill could not be written."
                busy = false
            }
        }
    }

    fun suggestedName(): String =
        "redacted-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".jpg"

    /**
     * Encodes what is on screen right now and re-reads the result, so the claim
     * "no metadata left" is measured on the bytes that would actually leave the
     * phone rather than asserted from the code path.
     */
    fun verifyOutput() {
        val l = loaded ?: return
        busy = true
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val bytes = ImageAudit.encode(l.bitmap)
                    OutCheck(bytes.size, ImageAudit.survivingTags(bytes), "checked just now")
                }
            }.onSuccess { outCheck = it; busy = false; note = null }
                .onFailure { error = it.message ?: "The check could not run."; busy = false }
        }
    }

    fun writeCopyTo(uri: Uri) {
        val l = loaded ?: return
        busy = true
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.Default) { ImageAudit.encode(l.bitmap) }
                withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: throw IOException("The place you chose could not be opened for writing.")
                    stream.use { it.write(bytes) }
                }
                OutCheck(bytes.size, ImageAudit.survivingTags(bytes), "written to the file you chose")
            }.onSuccess {
                outCheck = it
                note = "Saved. The redacted copy is a new JPEG; your original is untouched in your gallery."
                busy = false
            }.onFailure {
                error = it.message ?: "The copy could not be written."
                busy = false
            }
        }
    }

    /**
     * Hands the redacted copy to another app through a URI the receiver can read.
     * The file goes into this app's own private folder, so no storage permission is
     * asked for and the gallery never indexes a half-redacted picture.
     */
    fun shareRedacted() {
        val l = loaded ?: return
        busy = true
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>()
                    val dir = File(ctx.cacheDir, "out").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val bytes = withContext(Dispatchers.Default) { ImageAudit.encode(l.bitmap) }
                    val f = File(dir, suggestedName())
                    f.outputStream().use { it.write(bytes) }
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("image/jpeg")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(
                        Intent.createChooser(send, "Share the redacted copy")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    OutCheck(bytes.size, ImageAudit.survivingTags(bytes), "handed to the app you chose")
                }
            }.onSuccess { outCheck = it; busy = false }
                .onFailure { error = it.message ?: "Sharing was refused."; busy = false }
        }
    }

    fun shareText() {
        if (redacted.isBlank()) return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, redacted)
        runCatching {
            getApplication<Application>().startActivity(
                Intent.createChooser(send, "Share the redacted text").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { error = it.message ?: "Sharing was refused." }
    }

    fun discard() {
        loaded = null
        pending = emptyList()
        drag = null
        checks = emptyList()
        rounds = 0
        outCheck = null
        secondRead = null
        text = ""
        findings = emptyList()
        chosen = emptySet()
        redacted = ""
        fromShare = false
        note = null
        error = null
        busyRemote = false
    }

    /* ----------------------------------------------------------- second read */

    var secondRead by mutableStateOf<String?>(null)
        private set

    var busyRemote by mutableStateOf(false)
        private set

    /**
     * The optional pass, and the reason the app can ask for nothing. What goes out
     * is what the local checks already failed to remove: the machine-redacted text,
     * or the already-filled picture. A value the shapes caught never reaches an
     * endpoint, because it is no longer in what gets sent.
     */
    fun askSecondRead() {
        if (!config.usable) {
            error = "Switch the second read on in Settings and add your own base URL, model and key first."
            return
        }
        if (mode == Mode.Photo && loaded == null) {
            error = "Pick a picture first — there is nothing for a model to look at."
            return
        }
        if (mode == Mode.Text && redacted.isBlank()) {
            error = "Nothing to send — the text box is empty."
            return
        }
        val photo = if (mode == Mode.Photo) loaded else null
        if (photo == null && redacted.isBlank()) return
        busyRemote = true
        error = null
        secondRead = null
        viewModelScope.launch {
            runCatching {
                val cfg = settings.current()
                if (!cfg.usable) throw IllegalArgumentException("Add a base URL, model and key in Settings first.")
                val encoded = if (photo != null) {
                    withContext(Dispatchers.Default) {
                        ImageAudit.encode(downscaleForModel(photo.bitmap), MODEL_JPEG_QUALITY)
                    }
                } else {
                    null
                }
                withContext(Dispatchers.IO) {
                    ai.complete(
                        endpoint = cfg.endpoint,
                        apiKey = cfg.apiKey,
                        model = cfg.model,
                        prompt = if (photo != null) photoPrompt else textPrompt,
                        jpegBase64 = encoded?.let { encodeJpegBase64(it) },
                    )
                }
            }.onSuccess { secondRead = it.trim(); busyRemote = false }
                .onFailure { error = it.message ?: "The request failed."; busyRemote = false }
        }
    }

    private fun downscaleForModel(src: Bitmap): Bitmap {
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= MODEL_MAX_EDGE) return src
        val ratio = MODEL_MAX_EDGE.toFloat() / longEdge
        val w = maxOf(1, (src.width * ratio).toInt())
        val h = maxOf(1, (src.height * ratio).toInt())
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private val textPrompt: String
        get() = buildString {
        appendLine("You are the optional second read inside RedactGuard, an open-source Android app that removes")
        appendLine("identifying detail from text and pictures on the device. The text below has already been run")
        appendLine("through the app's shape table on the device; every [REDACTED:…] marker is something the local")
        appendLine("checks already destroyed. You are looking only for what the shapes missed.")
        appendLine()
        appendLine("Text as it would be sent:")
        appendLine(redacted)
        appendLine()
        appendLine("Task: name, in up to five short bullets, any detail still standing here that could identify a")
        appendLine("person, a place, a device or a routine — a name, a job title, a relationship, an unusual time,")
        appendLine("a street, a venue, a filename, a handle written without its @.")
        appendLine("Rules, all binding:")
        appendLine("- Do not attempt to reconstruct, guess or expand anything behind a [REDACTED:…] marker.")
        appendLine("- Do not say the text is safe or clean; you are looking for residue, not giving clearance.")
        appendLine("- Do not output a percentage, a score or a confidence number.")
        appendLine("- Do not invent a detail that is not present in the text above.")
        appendLine("- Do not advise who to contact, or rewrite the text for them.")
        appendLine("- If nothing stands out, reply exactly: no residue I can point at.")
        appendLine("- Format: the bullets only, or that one line. No heading, no preamble.")
    }

    private val photoPrompt: String
        get() = buildString {
        appendLine("You are the optional second read inside RedactGuard, an open-source Android app that removes")
        appendLine("identifying detail from pictures on the device. This image has already had every region the")
        appendLine("person drew filled in on the device, and its metadata was dropped by re-encoding. You are")
        appendLine("looking only for what is still visible.")
        appendLine()
        appendLine("Task: name, in up to five short bullets, anything still visible that identifies a person or a")
        appendLine("place — a face, a badge, a sign, a storefront, a reflection, a document, a view, a number plate,")
        appendLine("a screen of text.")
        appendLine("Rules, all binding:")
        appendLine("- Do not describe or guess at what is under a filled or blocked-out region.")
        appendLine("- Do not say the picture is safe to send; you are pointing at residue, not giving clearance.")
        appendLine("- Do not output a percentage, a score or a confidence number.")
        appendLine("- Do not name or guess the identity of anyone visible, and do not read out a face.")
        appendLine("- If nothing stands out, reply exactly: no residue I can point at.")
        appendLine("- Format: the bullets only, or that one line. No heading, no preamble.")
    }

    companion object {
        private const val MODEL_MAX_EDGE = 1024
        private const val MODEL_JPEG_QUALITY = 80
    }
}
