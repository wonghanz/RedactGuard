package dev.capriguard.redactguard.redact

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * The metadata an image carries about the person who made it. This is a reading
 * list, not a blocklist: every entry is a tag this app looks up by name, with the
 * reason it is worth a second look. Anything the file holds outside these names is
 * reported as "other tags present" rather than treated as absent.
 */
data class GivenTag(
    val key: String,
    val label: String,
    val group: String,
    val gives: String,
)

object ExifWatch {
    const val Location = "Where"
    const val Identity = "Who"
    const val Device = "Which device"
    const val Time = "When"
    const val Optics = "How it was shot"

    val watched: List<GivenTag> = listOf(
        GivenTag(ExifInterface.TAG_GPS_LATITUDE, "GPS latitude", Location, "a place on the ground, to a few metres"),
        GivenTag(ExifInterface.TAG_GPS_LONGITUDE, "GPS longitude", Location, "a place on the ground, to a few metres"),
        GivenTag(ExifInterface.TAG_GPS_ALTITUDE, "GPS altitude", Location, "how high, which narrows a building"),
        GivenTag(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS method", Location, "which app attached the position"),
        GivenTag(ExifInterface.TAG_GPS_TIMESTAMP, "GPS fix time", Time, "a second timestamp, independent of the camera clock"),
        GivenTag(ExifInterface.TAG_ARTIST, "Artist", Identity, "whoever the camera or tool was told to credit"),
        GivenTag(ExifInterface.TAG_COPYRIGHT, "Copyright", Identity, "a name, and sometimes an email"),
        GivenTag(ExifInterface.TAG_USER_COMMENT, "User comment", Identity, "free text from whatever wrote it"),
        GivenTag(ExifInterface.TAG_IMAGE_DESCRIPTION, "Description", Identity, "free text, often typed by a person"),
        GivenTag(ExifInterface.TAG_IMAGE_UNIQUE_ID, "Unique image id", Identity, "a frame that can be traced back to one device"),
        GivenTag(ExifInterface.TAG_MAKE, "Camera make", Device, "your camera brand"),
        GivenTag(ExifInterface.TAG_MODEL, "Camera or phone model", Device, "your exact device, which narrows who you are"),
        GivenTag(ExifInterface.TAG_SOFTWARE, "Software", Device, "the app that wrote the file"),
        GivenTag(ExifInterface.TAG_LENS_MAKE, "Lens make", Optics, "a lens you own"),
        GivenTag(ExifInterface.TAG_LENS_MODEL, "Lens model", Optics, "a lens you own"),
        GivenTag(ExifInterface.TAG_EXPOSURE_TIME, "Exposure", Optics, "shutter timing, part of a shot's fingerprint"),
        GivenTag(ExifInterface.TAG_F_NUMBER, "Aperture", Optics, "lensing you own"),
        GivenTag(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, "ISO", Optics, "shooting conditions"),
        GivenTag(ExifInterface.TAG_WHITE_BALANCE, "White balance", Optics, "how the camera was set up"),
        GivenTag(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, "Digital zoom", Optics, "how the frame was cropped in"),
        GivenTag(ExifInterface.TAG_FLASH, "Flash", Optics, "whether a flash fired, and where"),
        GivenTag(ExifInterface.TAG_DATETIME, "Modified", Time, "when a device clock said it was"),
        GivenTag(ExifInterface.TAG_DATETIME_ORIGINAL, "Taken", Time, "the moment, to the second"),
        GivenTag(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "Taken (fraction)", Time, "the moment, to the millisecond"),
        GivenTag(ExifInterface.TAG_OFFSET_TIME, "Time offset", Time, "your UTC offset, which is a band of longitude"),
        GivenTag(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT, "Thumbnail offset", Location, "an embedded preview of the picture itself"),
        GivenTag(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, "Thumbnail length", Location, "an embedded preview of the picture itself"),
    )
}

/** One tag that was actually present, with the value the file held. */
data class Given(val tag: GivenTag, val value: String)

enum class Fill { Solid, Mosaic }

/** A region in source-image pixels, not screen points. */
data class BoxPx(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun width(): Int = right - left
    fun height(): Int = bottom - top
    fun degenerate(): Boolean = width() < 2 || height() < 2
}

/**
 * What changed, measured rather than assumed: the pixels under each region are
 * sampled before the fill and again after it, so a mapping bug that left the
 * original in place shows up as a low percentage instead of passing silently.
 */
data class RegionCheck(val index: Int, val pixels: Int, val changedPercent: Double, val fill: Fill)

data class Loaded(
    val bitmap: Bitmap,
    val sourceBytes: Int,
    val givens: List<Given>,
    val location: String?,
    val orientation: Int,
) {
    val groups: List<String> get() = ExifWatch.watched.map { it.group }.distinct().filter { g -> givens.any { it.tag.group == g } }
}

/**
 * The photo pass, all of it on the decoded pixels of a file this app copied into
 * its own private folder. Nothing is uploaded, nothing is written back over the
 * original, and the original is never the thing that gets shared.
 */
object ImageAudit {

    /** Decoding a 108-megapixel phone panorama at full size is how an app crashes. */
    const val MAX_EDGE = 2400
    const val JPEG_QUALITY = 92

    @Throws(IOException::class)
    fun load(file: File): Loaded {
        val bounds = options(bounds = true)
        FileInputStream(file).use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        val longEdge = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longEdge / sample > MAX_EDGE) sample *= 2

        val opts = options(bounds = false).apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = FileInputStream(file).use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException(
                if (bounds.outWidth <= 0) {
                    "The picture could not be decoded. This build reads what the Android system can: JPEG, PNG, " +
                        "WebP, and HEIF only on the Android versions that ship a HEIF decoder."
                } else {
                    "The picture decoded as far as its header and then failed. The file looks truncated."
                },
            )

        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        val givens = exif?.let { readGivens(it) } ?: emptyList()
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) ?: 1
        val location = exif?.let { latLongOf(it) }

        val upright = applyOrientation(decoded, orientation)
        if (upright !== decoded) decoded.recycle()

        // A JPEG has no alpha, so transparency in a PNG is flattened onto white
        // before anything is drawn — otherwise the encoder decides it for us.
        val working = Bitmap.createBitmap(upright.width, upright.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(working)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(upright, 0f, 0f, null)
        if (upright !== decoded) upright.recycle()
        decoded.recycle()

        return Loaded(
            bitmap = working,
            sourceBytes = file.length().coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            givens = givens,
            location = location,
            orientation = orientation,
        )
    }

    private fun options(bounds: Boolean) = android.graphics.BitmapFactory.Options().apply {
        inJustDecodeBounds = bounds
    }

    private fun readGivens(exif: ExifInterface): List<Given> =
        ExifWatch.watched.mapNotNull { tag ->
            val raw = runCatching { exif.getAttribute(tag.key) }.getOrNull()
            if (raw.isNullOrBlank()) null else Given(tag, raw.trim())
        }

    /**
     * Android's own helper, which folds the N/S and E/W references in for you. The
     * DMS strings are what the file actually stores, so they are shown when the
     * conversion refuses to run.
     */
    private fun latLongOf(exif: ExifInterface): String? {
        val pair = runCatching { exif.latLong }.getOrNull()
        if (pair != null && pair.size == 2 && (pair[0] != 0.0 || pair[1] != 0.0)) {
            return "%.5f, %.5f".format(pair[0], pair[1])
        }
        val lat = runCatching { exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) }.getOrNull()
        val lon = runCatching { exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) }.getOrNull()
        return if (lat.isNullOrBlank() || lon.isNullOrBlank()) null
        else "degrees/minutes/seconds: ${lat.trim()} ${lon.trim()}"
    }

    /**
     * BitmapFactory ignores the orientation tag, so a portrait photo whose pixels are
     * stored sideways has to be turned here — an output with no EXIF has nothing
     * left to tell a viewer which way is up. Orientations 5 and 7 are a rotation
     * plus a mirror; every pixel survives either way.
     */
    private fun applyOrientation(src: Bitmap, orientation: Int): Bitmap {
        if (orientation <= 1) return src
        val m = Matrix()
        when (orientation) {
            2 -> m.postScale(-1f, 1f)
            3 -> m.postRotate(180f)
            4 -> m.postScale(1f, -1f)
            5 -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            6 -> m.postRotate(90f)
            7 -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            8 -> m.postRotate(270f)
            else -> return src
        }
        return runCatching { Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true) }.getOrDefault(src)
    }

    fun redact(src: Bitmap, boxes: List<BoxPx>, fill: Fill, mosaicBlock: Int): Pair<Bitmap, List<RegionCheck>> {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val solid = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = false }
        val checks = boxes.mapIndexed { index, box ->
            val r = clamp(box, out.width, out.height)
            if (r == null) {
                RegionCheck(index, 0, 0.0, fill)
            } else {
                val w = r.width()
                val h = r.height()
                val pixels = w * h
                val before = IntArray(pixels)
                src.getPixels(before, 0, w, r.left, r.top, w, h)
                when (fill) {
                    Fill.Solid -> canvas.drawRect(r, solid)
                    Fill.Mosaic -> mosaic(canvas, src, r, mosaicBlock)
                }
                val after = IntArray(pixels)
                out.getPixels(after, 0, w, r.left, r.top, w, h)
                var changed = 0
                for (i in 0 until pixels) if (before[i] != after[i]) changed += 1
                RegionCheck(index, pixels, 100.0 * changed / pixels, fill)
            }
        }
        return out to checks
    }

    private fun clamp(box: BoxPx, imgW: Int, imgH: Int): Rect? {
        val l = box.left.coerceIn(0, imgW - 1)
        val t = box.top.coerceIn(0, imgH - 1)
        val r = box.right.coerceIn(l + 1, imgW)
        val b = box.bottom.coerceIn(t + 1, imgH)
        val rect = Rect(l, t, r, b)
        return if (rect.width() < 2 || rect.height() < 2) null else rect
    }

    /**
     * Downsample, then put the small bitmap back at the original size with
     * filtering off, which is what leaves the squares. Block is in source pixels, so
     * a stronger setting is a smaller intermediate, not a blur.
     */
    private fun mosaic(canvas: Canvas, src: Bitmap, r: Rect, block: Int) {
        val w = r.width()
        val h = r.height()
        val crop = Bitmap.createBitmap(src, r.left, r.top, w, h)
        val smallW = max(1, w / block)
        val smallH = max(1, h / block)
        val small = Bitmap.createScaledBitmap(crop, smallW, smallH, false)
        val paint = Paint().apply { isFilterBitmap = false; isDither = false }
        canvas.drawBitmap(
            small, null,
            RectF(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()),
            paint,
        )
        if (small !== crop) small.recycle()
        if (crop !== small) crop.recycle()
    }

    /**
     * A fresh encoding of pixels we already own. Android's JPEG encoder writes no
     * EXIF segment, no thumbnail, no colour-profile chunk and no PNG text chunk, so
     * the metadata does not have to be removed one tag at a time — there is no
     * container left for it to live in.
     */
    fun encode(bmp: Bitmap, quality: Int = JPEG_QUALITY): ByteArray {
        val bos = ByteArrayOutputStream()
        if (!bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)) {
            throw IOException("The system JPEG encoder refused this picture.")
        }
        return bos.toByteArray()
    }

    /** Re-reads the bytes that are about to be handed over, and lists what survived. */
    fun survivingTags(jpeg: ByteArray): List<Given> = runCatching {
        readGivens(ExifInterface(ByteArrayInputStream(jpeg)))
    }.getOrDefault(emptyList())

    fun mosaicStrengthLabel(block: Int): String = when {
        block >= 24 -> "coarse — shapes only, at this size"
        block >= 12 -> "medium — large blocks, edges still readable"
        else -> "fine — text and faces can still be legible; this is not a substitute for solid fill"
    }

    fun bytesLabel(n: Long): String = when {
        n >= 1_000_000 -> "%.1f MB".format(n / 1_000_000.0)
        n >= 1_000 -> "%.0f kB".format(n / 1_000.0)
        else -> "$n B"
    }

    /** Pixels per displayed point, used to turn a finger rectangle into image pixels. */
    fun scaleFor(imgW: Int, imgH: Int, boxW: Int, boxH: Int): Float {
        if (imgW <= 0 || imgH <= 0 || boxW <= 0 || boxH <= 0) return 1f
        return min(boxW.toFloat() / imgW, boxH.toFloat() / imgH)
    }
}
