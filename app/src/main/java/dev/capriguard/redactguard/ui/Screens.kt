package dev.capriguard.redactguard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.capriguard.redactguard.core.GlassCard
import dev.capriguard.redactguard.core.LiquidBackdrop
import dev.capriguard.redactguard.core.LocalGlass
import dev.capriguard.redactguard.core.Palette
import dev.capriguard.redactguard.core.Radii
import dev.capriguard.redactguard.core.tnum
import dev.capriguard.redactguard.redact.BoxPx
import dev.capriguard.redactguard.redact.Fill
import dev.capriguard.redactguard.redact.Finding
import dev.capriguard.redactguard.redact.ExifWatch
import dev.capriguard.redactguard.redact.Given
import dev.capriguard.redactguard.redact.ImageAudit
import dev.capriguard.redactguard.redact.Limits
import dev.capriguard.redactguard.redact.Loaded
import dev.capriguard.redactguard.redact.Mode
import dev.capriguard.redactguard.redact.RedactViewModel
import dev.capriguard.redactguard.redact.RegionCheck
import dev.capriguard.redactguard.redact.Shapes
import dev.capriguard.redactguard.redact.TextScanner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* ------------------------------------------------------------------ shell */

@Composable
fun ScreenShell(
    title: String,
    onBack: (() -> Unit)?,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidBackdrop(base = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconBadge(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                } else {
                    Spacer(Modifier.width(40.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun IconBadge(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(Radii.PillShape)
            .background(LocalGlass.current.scrim)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Radii.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp))
    }
}

@Composable
private fun GhostAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(Radii.ControlShape)
            .border(0.7.dp, LocalGlass.current.hairline, Radii.ControlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Bullet(text: String, tint: Color = MaterialTheme.colorScheme.outline) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(top = 7.dp).size(4.dp).clip(Radii.PillShape).background(tint))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun confidenceTint(firm: Boolean): Color =
    if (firm) MaterialTheme.colorScheme.primary else Palette.Warm

@Composable
private fun GroupRow(label: String, items: List<Given>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        items.forEach { g ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text(
                    g.tag.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(104.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    g.value,
                    style = MaterialTheme.typography.bodySmall.tnum(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------- home */

@Composable
fun HomeScreen(vm: RedactViewModel, onOpenSettings: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    ScreenShell(title = "RedactGuard", onBack = null, action = {
        IconBadge(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") }
    }) {
        ModeRow(vm)
        vm.error?.let { ErrorBar(it, vm::dismissError) }
        vm.note?.let { NoteBar(it, vm::dismissNote) }

        if (vm.mode == Mode.Photo && vm.loaded != null) {
            val loaded = vm.loaded!!
            EditorPane(vm, loaded)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                PhotoResults(vm, loaded)
                AppliedCard()
                CannotSeeCard()
                WillNotSayCard()
                SecondReadCard(vm)
                Spacer(Modifier.height(28.dp))
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (vm.mode) {
                    Mode.Photo -> PhotoIntro(vm)
                    Mode.Text -> TextPane(vm, onPaste = {
                        val clip = clipboard.getText()?.text ?: ""
                        if (clip.isNotBlank()) vm.typeText(clip)
                    })
                }
                TableCard(vm)
                CannotSeeCard()
                WillNotSayCard()
                SecondReadCard(vm)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ModeRow(vm: RedactViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeChip("Photo", vm.mode == Mode.Photo) { vm.pickMode(Mode.Photo) }
        ModeChip("Text", vm.mode == Mode.Text) { vm.pickMode(Mode.Text) }
    }
}

@Composable
private fun ModeChip(label: String, on: Boolean, onClick: () -> Unit) {
    val tokens = LocalGlass.current
    Box(
        modifier = Modifier
            .clip(Radii.PillShape)
            .background(if (on) MaterialTheme.colorScheme.primary else tokens.scrim)
            .border(0.7.dp, if (on) MaterialTheme.colorScheme.primary else tokens.hairline, Radii.PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorBar(text: String, onDismiss: () -> Unit) {
    GlassCard(padding = 13.dp, modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Warning, null, Modifier.size(17.dp), tint = Palette.Alert)
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Dismiss", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun NoteBar(text: String, onDismiss: () -> Unit) {
    GlassCard(padding = 13.dp, modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(17.dp), tint = Palette.Calm)
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Dismiss", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/* ------------------------------------------------------------------ photo */

@Composable
private fun PhotoIntro(vm: RedactViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.takeImage(uri)
    }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("Before it leaves the phone")
            Text(
                "A photograph keeps more than the picture. The file carries where it was taken, when, and " +
                    "which device took it, and anything written on the image — a face, a plate, a name on a " +
                    "document — is only hidden by whatever you draw over it. RedactGuard replaces pixels and " +
                    "drops the metadata by encoding the picture again, then shows you what it removed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (vm.busy) {
                Text("Reading the file…", style = MaterialTheme.typography.labelMedium)
            } else {
                PrimaryAction("Pick a picture", true, Icons.Filled.Image) {
                    picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            Text(
                "Nothing is uploaded and no permission is asked for. The original stays in your gallery exactly " +
                    "as it is; this app only ever produces a new copy, and only when you tell it to.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    LimitsCard()
}

/**
 * Fixed-height pane: the drag that draws a box must not be stolen by the scrolling
 * column the other cards live in, so the editor sits outside the scroll parent.
 */
@Composable
private fun EditorPane(vm: RedactViewModel, loaded: Loaded) {
    val imgW = loaded.bitmap.width
    val imgH = loaded.bitmap.height
    val image = loaded.bitmap.asImageBitmap()

    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(Radii.ControlShape)
                .background(Color(0xFF0A1A1E)),
        ) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val scale = min(wPx / imgW, hPx / imgH).coerceAtLeast(0.0001f)
            val drawW = imgW * scale
            val drawH = imgH * scale
            val offX = (wPx - drawW) / 2f
            val offY = (hPx - drawH) / 2f

            var corner by remember { mutableStateOf(Offset.Zero) }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(scale, offX, offY, imgW, imgH) {
                        detectDragGestures(
                            onDragStart = { start ->
                                corner = start
                                vm.dragStart(pointBox(start, scale, offX, offY, imgW, imgH))
                            },
                            onDrag = { change, _ ->
                                val a = pointBox(corner, scale, offX, offY, imgW, imgH)
                                val b = pointBox(change.position, scale, offX, offY, imgW, imgH)
                                vm.dragTo(join(a, b))
                            },
                            onDragEnd = { vm.dragEnd(vm.drag) },
                            onDragCancel = { vm.dragEnd(null) },
                        )
                    },
            ) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(imgW, imgH),
                    dstOffset = IntOffset(offX.roundToInt(), offY.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                )
                val stroke = 2.dp.toPx()
                (vm.pending + listOfNotNull(vm.drag)).forEach { b ->
                    val topLeft = Offset(offX + b.left * scale, offY + b.top * scale)
                    val size = Size(b.width() * scale, b.height() * scale)
                    drawRect(Color(0x334FE0E4), topLeft, size)
                    drawRect(Palette.CapriBright, topLeft, size, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                }
            }
            Box(Modifier.padding(8.dp)) {
                Text(
                    "${imgW} × ${imgH} px",
                    style = MaterialTheme.typography.labelSmall.tnum(),
                    color = Color(0xCCF2F8F9),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeChip("Solid fill", vm.fill == Fill.Solid) { vm.pickFill(Fill.Solid) }
            ModeChip("Mosaic", vm.fill == Fill.Mosaic) { vm.pickFill(Fill.Mosaic) }
        }
        if (vm.fill == Fill.Mosaic) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostAction("finer", Icons.Filled.BlurOn) { vm.pickBlock(vm.block - 4) }
                Text(
                    "${vm.block} px blocks",
                    style = MaterialTheme.typography.labelMedium.tnum(),
                    modifier = Modifier.weight(1f),
                )
                GhostAction("coarser", Icons.Filled.Refresh) { vm.pickBlock(vm.block + 4) }
            }
            Text(
                ImageAudit.mosaicStrengthLabel(vm.block),
                style = MaterialTheme.typography.labelSmall,
                color = if (vm.block < 12) Palette.Warm else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhostAction("Undo last", Icons.AutoMirrored.Filled.Undo) { vm.undoLast() }
            GhostAction("Clear boxes", Icons.Filled.Delete) { vm.clearBoxes() }
            GhostAction("Discard", Icons.Filled.Block) { vm.discard() }
        }
        val boxes = vm.pending.size + (if (vm.drag != null) 1 else 0)
        PrimaryAction(
            text = if (vm.busy) "Writing…" else "Apply to ${vm.pending.size} region${if (vm.pending.size == 1) "" else "s"}",
            enabled = !vm.busy && vm.pending.isNotEmpty(),
            icon = Icons.Filled.Check,
            onClick = vm::applyRedaction,
        )
        Text(
            "Drag over what must go. Applying writes new pixels into the copy this app holds — not a layer on top, " +
                "and $boxes region${if (boxes == 1) "" else "s"} still pending.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

private fun pointBox(p: Offset, scale: Float, offX: Float, offY: Float, imgW: Int, imgH: Int): BoxPx {
    val x = ((p.x - offX) / scale).roundToInt().coerceIn(0, imgW)
    val y = ((p.y - offY) / scale).roundToInt().coerceIn(0, imgH)
    return BoxPx(x, y, x, y)
}

private fun join(a: BoxPx, b: BoxPx): BoxPx =
    BoxPx(min(a.left, b.left), min(a.top, b.top), max(a.right, b.right), max(a.bottom, b.bottom))

@Composable
private fun PhotoResults(vm: RedactViewModel, loaded: Loaded) {
    GivesAwayCard(loaded)
    if (vm.checks.isNotEmpty()) ChecksCard(vm.checks, vm.rounds)
    OutCheckCard(vm.outCheck)
    SaveSharePane(vm, loaded)
}

@Composable
private fun GivesAwayCard(loaded: Loaded) {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("What the file admits about you")
            if (loaded.givens.isEmpty()) {
                Text(
                    "None of the ${ExifWatch.watched.size} tag names this app looks up are present. A messaging app most " +
                        "likely already stripped them — that is the one reading here that is worth something.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Calm,
                )
            } else {
                listOf(
                    "Where", "Who", "Which device", "When", "How it was shot",
                ).forEach { group ->
                    GroupRow(group, loaded.givens.filter { it.tag.group == group })
                }
                Text(
                    "${loaded.givens.size} of ${ExifWatch.watched.size} watched tags are present. The values " +
                        "are shown because you should see exactly what is being thrown away.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!loaded.location.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = Palette.Alert)
                    Text(
                        "Position: ${loaded.location}",
                        style = MaterialTheme.typography.bodySmall.tnum(),
                        color = Palette.Alert,
                    )
                }
            }
            if (loaded.orientation > 1) {
                Text(
                    "The pixels are stored turned (orientation ${loaded.orientation}) and were rotated before drawing, " +
                        "because a file with no metadata has nothing left to say which way is up.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChecksCard(checks: List<RegionCheck>, rounds: Int) {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("Did it actually change")
            checks.forEach { c ->
                if (c.pixels == 0) {
                    Bullet("Region ${c.index + 1} fell outside the picture and was skipped.")
                } else {
                    val full = c.changedPercent >= 99.999
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            "%.1f%%".format(c.changedPercent),
                            style = MaterialTheme.typography.bodyMedium.tnum(),
                            color = if (full) Palette.Calm else Palette.Warm,
                            modifier = Modifier.width(56.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Region ${c.index + 1} · ${c.pixels} pixels · ${if (c.fill == Fill.Solid) "solid" else "mosaic"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                if (full) "every pixel under it is now new"
                                else "the rest already held this colour; nothing original is left either way",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Text(
                "Counted by sampling the region before and after the fill, on round $rounds of applying. It is a " +
                    "check on this app's own arithmetic, not a claim that what you covered was worth hiding.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OutCheckCard(check: RedactViewModel.OutCheck?) {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("The bytes that would leave")
            if (check == null) {
                Text(
                    "Run the check to encode what is on screen and read the result back. Until then this app makes " +
                        "no claim about a file it has not written.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "${ImageAudit.bytesLabel(check.bytes.toLong())} of JPEG, ${check.moment}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (check.survived.isEmpty()) {
                    Text(
                        "0 of the watched tags are present in the output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Calm,
                    )
                } else {
                    Text(
                        "${check.survived.size} watched tags survived: ${check.survived.joinToString(", ") { it.tag.label }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Alert,
                    )
                }
                Text(
                    "Re-reading the encoder's own output is the only metadata claim this app makes, because the " +
                        "encoder writes no EXIF segment at all — not a strip per tag, a container that is gone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SaveSharePane(vm: RedactViewModel, loaded: Loaded) {
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) vm.writeCopyTo(uri)
    }
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("Take it out")
            PrimaryAction(
                text = if (vm.busy) "Working…" else "Save a redacted copy",
                enabled = !vm.busy,
                icon = Icons.Filled.Save,
                onClick = { save.launch(vm.suggestedName()) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    GhostAction("Share", Icons.Filled.Share) { vm.shareRedacted() }
                }
                Box(Modifier.weight(1f)) {
                    GhostAction("Run the check", Icons.Filled.Visibility) { vm.verifyOutput() }
                }
            }
            Text(
                "Saving asks where to put it, so nothing needs a storage permission. Sharing hands the copy to the " +
                    "app you choose from this app's own private folder. Either way the output is a re-encoding: " +
                    "${loaded.bitmap.width} × ${loaded.bitmap.height} px at quality ${ImageAudit.JPEG_QUALITY}, " +
                    "and your original is untouched.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------------------------------------------- text */

@Composable
private fun TextPane(vm: RedactViewModel, onPaste: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("Text you are about to send")
            OutlinedTextField(
                value = vm.text,
                onValueChange = vm::typeText,
                placeholder = {
                    Text(
                        "Paste a message, a log line, an address block…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                minLines = 4,
                maxLines = 9,
                shape = Radii.ControlShape,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { GhostAction("Paste", Icons.Filled.ContentPaste) { onPaste() } }
                Box(Modifier.weight(1f)) { GhostAction("Clear", Icons.Filled.Delete) { vm.typeText("") } }
            }
            Text(
                TextScanner.verdict(vm.findings),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (vm.findings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostAction("Firm only", Icons.Filled.Check) { vm.selectAllFirm() }
                    GhostAction("None", Icons.Filled.Block) { vm.selectNone() }
                }
            }
        }
    }

    if (vm.findings.isEmpty() && vm.text.isNotBlank()) {
        GlassCard(padding = 15.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SectionTitle("No shape matched")
                Text(
                    "That is not the same as nothing identifying you. Patterns catch formats, and most of what " +
                        "makes a person recognisable is not a format — a name, a title, a place, a routine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Warm,
                )
                Bullet("Read it as the stranger would, then delete or cover by hand: what is here is the switch you are on.")
            }
        }
    }

    if (vm.findings.isNotEmpty()) {
        FindingsCard(vm)
        AbsentCard(vm)
        OutputCard(vm, onCopy = {
            if (vm.redacted.isNotBlank()) clipboard.setText(AnnotatedString(vm.redacted))
        })
    }
}

@Composable
private fun FindingsCard(vm: RedactViewModel) {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("Caught, and what each one gives away")
            vm.findings.forEachIndexed { i, f ->
                val on = i in vm.chosen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(Radii.ControlShape)
                        .clickable { vm.toggle(i) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = { vm.toggle(i) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(28.dp).padding(top = 2.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            f.shape.label + if (f.autoApply) "" else "  ·  only possible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = confidenceTint(f.autoApply),
                        )
                        Text(
                            f.fragment,
                            style = MaterialTheme.typography.bodySmall.tnum(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(f.shape.givesAway, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Text(
                "Anything marked only-possible starts switched off: the shape fits, but so do ordinary things, and " +
                    "an app that guessed at your name would be worse than one that left it for you to see.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AbsentCard(vm: RedactViewModel) {
    val fired = vm.findings.map { it.shape.id }.toSet()
    val absent = Shapes.all.filter { it.id !in fired }
    if (absent.isEmpty()) return
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("Did not fire")
            absent.forEach { Text("· ${it.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                "Printed so that an empty column here is not mistaken for a clean bill of health.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun OutputCard(vm: RedactViewModel, onCopy: () -> Unit) {
    val enabled = vm.enabledFindings.size
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("What would go out")
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(Radii.ControlShape)
                    .background(LocalGlass.current.scrim)
                    .padding(12.dp),
            ) {
                Text(vm.redacted.ifBlank { " " }, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "$enabled of ${vm.findings.size} matches replaced. Each marker says which shape stood there, so the " +
                    "reader can tell a redaction from a typo — which is also what tells them what to try to guess.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { GhostAction("Copy", Icons.Filled.ContentCopy) { onCopy() } }
                Box(Modifier.weight(1f)) { GhostAction("Share", Icons.Filled.Share) { vm.shareText() } }
            }
        }
    }
}

/* --------------------------------------------------------------- the table */

@Composable
private fun TableCard(vm: RedactViewModel) {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("Every shape, published")
            Shapes.all.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        if (s.autoApply) "applies" else "needs you",
                        style = MaterialTheme.typography.labelSmall.tnum(),
                        color = confidenceTint(s.autoApply),
                        modifier = Modifier.width(72.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(s.label, style = MaterialTheme.typography.bodySmall)
                        Text(s.givesAway, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Text(
                "The regular expressions are in app/src/main/java/dev/capriguard/redactguard/redact/Redactor.kt, " +
                    "in the order they are tried: a span an earlier shape claimed is not offered again, so an email " +
                    "never shows up twice as a handle and as free text. ${Limits.tableNote}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppliedCard() {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("What applying does")
            Limits.applied.forEach { Bullet(it) }
        }
    }
}

@Composable
private fun LimitsCard() {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("What this is not")
            Text(
                "Not a watermark remover, not an AI-photo detector, and not a way to make a picture unrecognisable " +
                    "to a person who already knows the subject. It removes what you point at, and it reports what it " +
                    "removed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CannotSeeCard() {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("What it cannot see")
            Limits.cannotSee.forEach { Bullet(it, Palette.Warm) }
        }
    }
}

@Composable
private fun WillNotSayCard() {
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SectionTitle("What it will not tell you")
            Limits.willNotSay.forEach { Bullet(it) }
        }
    }
}

@Composable
private fun SecondReadCard(vm: RedactViewModel) {
    val on = vm.config.usable
    GlassCard(padding = 15.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Second read, if you want it")
            Text(
                if (vm.mode == Mode.Photo) {
                    if (on) {
                        "Sends the picture as it now stands — every region you have applied already filled — to your " +
                            "own endpoint, and asks what still identifies a person or a place. Nothing leaves the " +
                            "phone that the local pass did not already fail to remove."
                    } else {
                        "Optional, and off. It needs an endpoint you configure in Settings. The pixel pass and the " +
                            "metadata pass above run without it, in airplane mode."
                    }
                } else {
                    if (on) {
                        "Sends the redacted text — with the values already replaced by markers — to your own endpoint " +
                            "and asks what still gives you away. Anything the shapes caught is gone from what is sent."
                    } else {
                        "Optional, and off. The shapes above are a table the device already has; this pass adds a " +
                            "model's reading of what that table missed."
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (on) {
                Button(
                    onClick = vm::askSecondRead,
                    enabled = !vm.busyRemote,
                    shape = Radii.ControlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                ) {
                    Text(
                        if (vm.busyRemote) "Asking your endpoint…" else "Look for what is left",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            vm.secondRead?.let {
                Box(Modifier.fillMaxWidth().clip(Radii.ControlShape).background(LocalGlass.current.scrim).padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "A model's opinion, generated after the local pass. It is not part of the table, it " +
                                "cannot add a redaction for you, and nothing here becomes safe because it said so.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.Warm,
                        )
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------- settings */

@Composable
fun SettingsScreen(vm: RedactViewModel, onBack: () -> Unit) {
    var reveal by remember { mutableStateOf(false) }

    ScreenShell(title = "Settings", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Every check that matters runs on the device: the pixels, the metadata and the shape table need " +
                    "none of this. The endpoint below is only for the optional second read.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Second read", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = vm.config.visionOn,
                    onCheckedChange = vm::setSecondReadOn,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
            SettingsField("Base URL", vm.config.baseUrl, "https://api.example.com", onDone = vm::setBaseUrl)
            SettingsField("Model", vm.config.model, "gpt-4o-mini", onDone = vm::setModel)
            SettingsField(
                label = "API key",
                value = vm.config.apiKey,
                placeholder = "Typed here, kept on this device",
                masked = !reveal,
                secret = true,
                onDone = vm::setApiKey,
            )
            Text(
                "What goes out when you use it: the text with its values already replaced, or the picture with its " +
                    "regions already filled. The prompt forbids reconstructing what was redacted, declaring anything " +
                    "safe, or producing a number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { reveal = !reveal }) {
                Text(
                    if (reveal) "Hide key" else "Show key",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                "The key lives in this app's private storage and the manifest sets allowBackup=false, so it cannot " +
                    "ride out in a cloud backup, a device transfer or an adb backup. This build ships without a key " +
                    "and without anyone else's quota.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "The same applies to your pictures: nothing is written to disk unless you press Save or Share, the " +
                    "copy made for decoding is deleted as soon as it is read, and there is no history screen to fill.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    placeholder: String,
    onDone: (String) -> Unit,
    masked: Boolean = false,
    secret: Boolean = false,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = !secret,
        visualTransformation = if (masked) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else KeyboardType.Uri,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone(text) }),
        shape = Radii.ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
