# Security and safety

## The one thing to read first

**This app cannot tell you a picture is safe to send, and neither can anyone else.** What
it does is narrower and checkable: it writes new pixels where you point, it drops the
metadata by encoding a fresh JPEG, and it prints what it removed. The failure mode this
file is about is the opposite one — a person who redacts with a tool that draws a layer,
sends the file, and finds out later that the original was still inside it.

RedactGuard has no server, no blocklist, no telemetry and no network call on its default
path, so a vulnerability here is mostly about what happens to the material in front of
the app.

## Attack surface, stated plainly

- **Input.** One image (photo picker or share sheet) or one text payload (typed or
  shared). No folder, no gallery scan, no inbox, no clipboard read without a tap.
- **Decoding.** `BitmapFactory` and `ExifInterface` only, on a copy in the app's own
  cache. A malformed file is a caught exception and a printed sentence, not a silent
  partial result. Long edge is capped at 2400px, so an extreme panorama is an
  allocation problem the platform has already solved.
- **Output.** Only on Save or Share. Saving goes through the system's create-document
  sheet, so the app never needs to be allowed to write anywhere by itself; sharing hands
  one `FileProvider` URI to one receiver with a read grant.
- **Storage.** No database, no history, no thumbnails. The incoming copy is deleted right
  after decoding. A key you type lives in this app's private DataStore, and
  `allowBackup=false` plus `fullBackupContent=false` keep it out of cloud backup, device
  transfer and `adb backup`.
- **Permissions.** None requested. `INTERNET` is declared and used only by the second read.
- **The model.** An OpenAI-compatible endpoint you name. A malicious endpoint gets your
  residue text or your filled image, and its reply is rendered as inert text — it cannot
  add a redaction, change a pixel, or trigger an action. No URL in model output is opened.

## What leaves the device, and only if you configure it

| Path | Sends | Never sends |
|---|---|---|
| Pixel pass | nothing | — |
| Metadata pass | nothing | — |
| Text shapes | nothing | — |
| Second read (text) | the string after redaction, values already replaced | the values the shapes caught |
| Second read (photo) | the filled image, downscaled to 1024px, quality 80 | the pixels under any filled region, the EXIF |

The prompt for both forbids reconstructing anything behind a marker, describing what is
under a fill, naming an identity, declaring the result safe, or producing a number.

## Honest residual risks

- **Mosaic is strength-dependent, and the app prints which one you are on.** A fine block
  size can leave text legible and faces recognisable. Solid fill is the only option here
  that removes information rather than hiding it.
- **Anything you did not point at is still there.** There is no detector, so an
  unboxed face, an unboxed plate and the writing inside a screenshot all survive.
- **Residue the shapes cannot represent survives**: a name, a job title, a relationship, a
  routine, a view, a venue. Fourteen patterns are a format net, not an understanding net.
- **The original is untouched, on purpose.** If you send the gallery file instead of the
  saved copy, none of this applies. The app cannot make that mistake impossible, only
  visible — it never edits or deletes your file, and it only ever shares what it made.
- **Metadata outside the 27 watched names is not enumerated.** An XMP block, an ICC
  profile or a PNG text chunk is not listed on screen; the re-encode still discards them,
  because the container they live in is not written.
- **Video, PDF, HEIF-on-old-Android and layers are out of scope.** A PDF redacted by
  drawing on top of it is the classic way to leak a document, and this app does not claim
  that job.
- **A screenshot of a redacted screenshot re-exposes what the screen showed.** The pixels
  you destroyed are gone; the ones your display painted are a different file.

## Reporting

Open a [private security advisory](https://github.com/wonghanz/RedactGuard/security)
rather than an issue if a report would show someone how to recover a redaction or read a
key out of a backup. Please include the Android version and the exact step, and do not
attach the sensitive file — describe it.
