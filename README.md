<div align="center">

# RedactGuard

**Offline photo and text redaction for Android — pixel-destructive fills, mosaic, and an
EXIF pass that re-encodes the file, with an optional second read from your own AI key.**

One intent, stated plainly: RedactGuard removes what you point at and reports what it
removed. It does not tell you a picture is safe to send, because nobody can.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDDC8)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![No permissions](https://img.shields.io/badge/permissions-none%20beyond%20INTERNET-0B6C72)](#privacy-stated-plainly)
[![No tracking](https://img.shields.io/badge/telemetry-none-0B6C72)](#privacy-stated-plainly)
[![No keys bundled](https://img.shields.io/badge/AI%20key-you%20supply%20your%20own-E8A33D)](#bring-your-own-key)

</div>

---

## Why this exists

A black rectangle drawn in a markup tool is not a redaction. The pixels underneath stay
in the file, the layer order stays in the PNG, and anyone who wants what is under it has
a program for that. The same trap runs quieter on the metadata side: a phone photo
carries a position, a timestamp to the second, the exact device, and often a thumbnail of
the picture itself — so a person who carefully blanks their address in a screenshot of a
delivery note still sends home the block it was photographed in.

What *is* knowable is what the file actually holds, and whether the pixels under a region
were replaced. So RedactGuard is narrow on purpose:

| You get | You do not get |
|---|---|
| Fills written into the copied pixels — no layer, no original underneath | A claim that covering something makes it unrecoverable elsewhere |
| A measurement of how much of each region actually changed | A "100% anonymous" score, or any percentage of safety |
| The tags the file admits, listed by name with their values | A background sweep of your gallery or your inbox |
| A re-encode: new JPEG, no EXIF segment, no thumbnail, no text chunk | A promise that every metadata flavour was enumerated |
| Fourteen published text shapes, each with a stated confidence | Automatic face detection, or automatic anything |
| An optional second read from a model *you* pay for | An account, a subscription, or a server of ours |

## What it actually runs

Three passes, all of them local, none of them a model:

**Pixels.** Drag over what must go. Solid fill writes a flat colour into the region;
mosaic downsamples the region and puts the small bitmap back with filtering off, which is
what leaves the squares. Both operate on a copy this app holds in memory.

**Proof.** Before the fill, the region's pixels are sampled. After, they are sampled
again, and the app prints how many of them differ. A rectangle that missed the image —
because of a rounding error, a mapping bug, or a finger that slipped — shows up as a
percentage instead of passing silently.

**Metadata.** The file is read tag by tag against a published watch list of 27 names. The
output is not scrubbed field by field; it is a fresh encoding, so the container the tags
lived in is gone rather than emptied. Before anything is claimed, the encoder's own output
is re-read and the surviving count is printed — including the awkward case where the
number is not zero.

Text works the same way: fourteen regular expressions, tried in a fixed order, a span
claimed by an early shape never offered again. Firm shapes apply themselves; the ones that
also fit ordinary prose start switched off and wait for you.

## The table, published

**Text shapes.** `Firm` means the format is specific enough to apply without asking.
`Only possible` means it fits, and so do a hundred ordinary things — so it is shown and
left off. Two shapes carry a sanity check instead of a wider net: a card number must pass
Luhn, and a twelve-digit MyKad must begin with a date that could have happened.

| Applies itself | Needs your tap |
|---|---|
| Labelled identifier (`NRIC…`, `OTP…`, `API key…` followed by a value) | Street address (`Jalan`, `Lorong`, `Road`, `Avenue`…) |
| Link with a login inside it (`scheme://user:pass@`) | Account handle (`@name`) |
| API key or bearer token (`sk-…`, `ghp_…`, `AKIA…`, `eyJ…`, `Bearer …`) | Long digit run (7–14 digits) |
| Email address | IPv4 address (each octet must be ≤ 255) |
| Malaysian identity number (valid birth date in the first six digits) | |
| Card number (Luhn-valid, 13–19 digits) | |
| IBAN | |
| Coordinates (two decimals-fraction numbers as a pair) | |
| Hardware address (MAC) | |
| Phone number (8–15 digits, in the shapes people write) | |

The expressions themselves are in
`app/src/main/java/dev/capriguard/redactguard/redact/Redactor.kt`, one per entry, in the
order they are tried. A shape you disagree with is a line in that file, and an issue
against it is welcome.

**Metadata watch list.** `Where` — GPS latitude, longitude, altitude, fix method, fix time.
`Who` — artist, copyright, user comment, description, unique image id. `Which device` —
make, model, software, lens make, lens model. `When` — modified, taken, sub-second taken,
UTC offset. `How it was shot` — exposure, aperture, ISO, white balance, digital zoom,
flash, and the embedded thumbnail's offset and length. Anything the file carries under a
name not on that list is not claimed absent; it is not claimed at all.

## What "nothing matched" is worth

Nothing, on its own. An empty findings list means fourteen patterns did not fire on this
text — it does not mean a reader cannot work out who you are, because most of what makes
a person recognisable is not a format. A name, a job title, a relationship, a routine, a
view out of a window: none of those are shapes, and the app says so on screen instead of
printing a clean bill of health.

The one empty result that does mean something is the metadata panel: a file carrying none
of the 27 watched tags has almost certainly been through a messaging app that stripped it.
That is reported as what it is — the sender already did this — rather than as a success.

## What it will not tell you

- That a picture or a message is safe to send. No match is not evidence of no match.
- That a mosaic is unrecoverable. At a fine block size it plainly is not; the app prints
  which strength you are on and says when the setting is too weak to hide what it looks like it hides.
- A percentage describing how anonymous a result is. There is no denominator for that.
- What lies under a filled region. Nothing can tell you, which is the point.
- That the optional second read is complete. It sees only what the local shapes failed to
  remove, and it is a model's opinion, not a verdict.

## Bring your own key

The second read is the only network code in the app, and it is off until you turn it on.
What it sends is what the local pass failed to remove: for text, the string *after*
redaction, with values already replaced by markers; for a picture, the already-filled
image downscaled to 1024px. A value the shapes caught is not in the payload, because it is
no longer in what gets sent.

The prompt forbids reconstructing anything behind a marker, describing what is under a
filled region, naming an identity, declaring the result safe, or producing a number. If
nothing stands out it answers `no residue I can point at`.

Base URL, model and key are typed by you, stored in this app's private DataStore, and the
manifest sets `allowBackup=false` — so a key cannot ride out in a cloud backup, a device
transfer or an `adb backup`. No default quota, no vendor account, no proxy through us.

## Requirements

- Android 8.0 (API 26) or newer. No permission is requested at all: the picture arrives
  through the photo picker or the share sheet, saving goes through the system's
  create-document sheet, and sharing hands over a single `FileProvider` URI.
- Input formats the platform can decode: JPEG, PNG, WebP; HEIF only on the Android versions
  that ship a HEIF decoder. The output is always a re-encoded JPEG, which is also how the
  metadata goes.
- Transparency is flattened onto white, because a JPEG has no alpha channel and the encoder
  would otherwise decide the colour for you.
- Images are decoded with the long edge capped at 2400px. Exif orientation is applied to
  the pixels, since a file with no metadata has nothing left to say which way is up.

## Build from source

```bash
export ANDROID_HOME=/path/to/Android/sdk
./gradlew --no-daemon assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Nothing in the build depends on a service of ours, and nothing in the repository contains
a credential. `./gradlew` uses the Gradle version pinned in the wrapper; the app needs no
API key to build, install or run its local passes.

## Privacy, stated plainly

- No storage, camera, media, location or network-state permission. No app can be given
  less.
- The picked picture is copied into the app's cache only because a shared content stream
  cannot reliably be opened twice — once for the tag pass, once for the decode. That copy
  is deleted the moment decoding finishes. The original is never modified, never re-saved,
  never deleted.
- No history, no queue, no gallery index, no account, no analytics, no crash reporting.
- With the second read off — the default — the app makes no network request, and works in
  airplane mode.

## FAQ

**Does drawing a black box over a photo redact it?**
No, if the box is a layer. A markup overlay leaves the original pixels in the file and
they can be recovered by removing the layer or reading the format's own structure. A
redaction has to replace the pixels, which is the only thing RedactGuard's apply step does.

**Can I undo a redaction inside the app?**
Not on a copy you have already applied to. The pixels are written into the working image,
so there is no hidden original to reveal; the way to undo is to start again from your
gallery file, which this app never edits.

**How do I remove EXIF data, like the GPS location from a photo, on Android without an app that uploads it?**
Pick the photo in RedactGuard, look at the list of tags it admits, then Save or Share the
redacted copy. Saving produces a fresh JPEG encoding, so the EXIF segment is gone rather
than scrubbed field by field; the app then re-reads the bytes it produced and prints how
many watched tags survived, which is normally zero and which it reports even when it is not.

**Does it detect faces automatically?**
No. There is no detector in this app, on purpose: an automatic one would be wrong in both
directions, and a wrong "nothing here" is the most harmful thing a redaction tool can say.
You point; it destroys.

**Is a mosaic blur safe?**
It depends entirely on the block size, and the app prints which one you are on. Fine
mosaic leaves silhouettes and can leave text legible. Solid fill is the only option here
that removes the information rather than hiding it.

**What about redacting a PDF?**
Not this app. It works on one image and one block of text at a time, on a phone. A PDF has
layers, embedded fonts and objects, and redacting one properly means rebuilding the
document — which is why tools that claim a one-click PDF redaction are usually drawing
rectangles on top.

**Will it redact text inside a photo, like a name in a screenshot?**
Not by itself. Nothing here runs OCR, so a screenshot of a chat still contains every name
in its pixels until you box it. The text tab handles text you paste; the photo tab handles
whatever you can see.

**Why does it print `only possible` next to some matches?**
Because those shapes also fit ordinary prose — a seven-digit run is a phone number about as
often as it is an order reference. Guessing at those would train people to stop reading the
list, so they are shown and left switched off.

**Can I use it without any AI key?**
Yes, and that is the default. The pixel pass, the region check and the metadata pass are
pure Kotlin over the bytes you picked.

## Repo map

```
app/src/main/java/dev/capriguard/redactguard/
  MainActivity.kt      share-sheet intake (one image or one text payload), routing
  core/Core.kt         palette, continuous-corner shapes, glass material, type ramp
  core/Settings.kt     DataStore settings + OpenAI-compatible client for the second read
  redact/Redactor.kt   the fourteen text shapes, the scan order, the redaction output
  redact/ImageAudit.kt decode, tag watch list, fills, before/after pixel proof, re-encode
  redact/Limits.kt     the honest-limits text, kept in one place for screen and README
  redact/RedactViewModel.kt  memory-only state, save/share, the optional second read
  ui/Screens.kt        editor, findings list, region checks, table, settings
docs/index.html        the public page: same claims, same numbers, no account needed
```

## Contributing

Rules that keep this worth merging: no bundled credential, no permission added without a
failing test, no claim of completeness, no automatic detection without a stated false
positive rate, no network call on the default path. See [CONTRIBUTING.md](CONTRIBUTING.md)
and [SECURITY.md](SECURITY.md). A region check that reports under 100% changed pixels is a
bug report worth opening; so is a shape that keeps firing on ordinary sentences.

## License

MIT — see [LICENSE](LICENSE).
