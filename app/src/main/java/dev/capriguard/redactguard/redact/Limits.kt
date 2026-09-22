package dev.capriguard.redactguard.redact

/**
 * The honest-limits text, kept in one file so the README, the web page and the
 * screen all say the same thing.
 */
object Limits {

    /** What the checks cannot see, stated before the person relies on them. */
    val cannotSee = listOf(
        "A face you did not box. There is no automatic detection in this app — it destroys what you point at, and only that.",
        "Writing inside a picture. Nothing here reads characters off a photo, so a screenshot of a chat keeps every name in it until you box it.",
        "A reflection in a window, a mirror or a switched-off screen.",
        "Your original file. It stays in your gallery exactly as it is; this app never edits or deletes it, and the thing you must not send is the original.",
        "Names, job titles, relationships, routines and places a reader already knows. The shapes are patterns, not understanding.",
        "Metadata under tag names this app does not look up — an XMP block or an ICC profile is not listed. None of them survive the re-encode, because the container is gone.",
    )

    /** Claims the app will not make, however useful they would be. */
    val willNotSay = listOf(
        "That a picture or a text is safe to send. No match is not evidence of no match.",
        "That a mosaic is unrecoverable. At a fine block size it plainly is not; solid fill is the only option that removes the information instead of hiding it.",
        "A percentage saying how anonymous the result is. There is no denominator for that.",
        "That the second read from your own endpoint is complete. It is a model's opinion, and it only ever sees what the shapes already failed to catch.",
        "What lies under a filled region. Nothing can, which is the entire point.",
    )

    /** Shown under the shape table, where the exact order of matching is the whole story. */
    val tableNote = "Two shapes carry a sanity check instead of a wider net: a card number has to pass " +
        "Luhn, and a twelve-digit MyKad has to start with a date that could have happened."

    val applied = listOf(
        "Applying a region is not a layer. The copy this app holds has new pixels written into it, and there is no original underneath to peel back.",
        "The only undo is drawing again on a copy you have not yet thrown away.",
        "The shared file is a fresh JPEG encoding: no EXIF segment, no thumbnail, no text chunk from the file you started with.",
    )
}
