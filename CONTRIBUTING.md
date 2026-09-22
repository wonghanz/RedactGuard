# Contributing

The rule that covers everything else: **the app may only claim what it can show you.**
Every shape, every watched tag name and every weight lives in one readable file —
`redact/Redactor.kt` for text, `redact/ImageAudit.kt` for metadata. No number, word list
or threshold should be duplicated inside the analyzer, and nothing should be added to the
UI that is not backed by a line in those two files.

## What is here

```
app/src/main/java/dev/capriguard/redactguard/
├── redact/Redactor.kt    the fourteen text shapes, the claim order, the redaction output
├── redact/ImageAudit.kt  decode, 27 watched tags, the fills, the pixel proof, the re-encode
├── redact/Limits.kt      the honest-limits text, shared by screen, README and web page
├── redact/RedactViewModel.kt  memory-only state, save/share, the optional second read
├── core/                 palette, continuous corners, glass, type ramp, DataStore, AI client
└── ui/Screens.kt         editor, findings, region checks, table, settings
```

## The four reports worth filing

1. **A false positive on the auto-applied tier.** Innocent prose that got marked
   `Firm` and redacted itself. These are the most damaging, because a tool that eats
   ordinary text trains people to stop reading the list it just printed.
2. **A miss.** Something the table should have caught and did not. Say what the shape
   looked like — a new format is a new line in `Redactor.kt`, not a widening of an
   existing one, because a looser pattern buys the miss with a hundred false positives.
3. **A region that did not come out at 100% changed**, or came out somewhere other than
   where you dragged. The apply step samples the region before and after, so these are
   measurable rather than a matter of opinion.
4. **A metadata claim that turned out wrong.** A tag shown that the file did not carry,
   or one it did carry that the watch list does not name. The second kind is a gap in
   `ExifWatch`, which is a list meant to be extended.

## Before you open a pull request

Build it: `./gradlew --no-daemon assembleDebug`. Then read the diff against these tests,
all of which have failed this project before:

- Does the app still make **no network request** on the default path? The only socket in
  the codebase is the second read, and it is off until a key is typed.
- Does every new claim come with the thing that makes it checkable? "Metadata removed" is
  not checkable; "0 of the 27 watched tags are present in the bytes we just wrote, and
  here is the re-read" is.
- Did you add a permission? Say exactly which failure it prevents. The absence of storage
  and media permissions is a feature that costs the app nothing.
- Did you make a heuristic sound stronger than it is? `only possible` exists because some
  shapes also fit ordinary prose; a tier that hides that is worse than no tier.
- Does the dark theme and the light theme both still read? The glass material is defined
  once, in `core/Core.kt`.

## The judgement calls, so they are not relitigated by accident

- **No automatic detection.** No face detector, no OCR, no "find sensitive regions". A
  confident nothing-here is the one error a person cannot undo after pressing send, so
  the pointing stays with the person.
- **The original is never touched.** The app produces a new file and leaves your gallery
  alone, including when that makes the workflow one step longer.
- **No percentage of anonymity**, no safety verdict, no score out of 100, in the UI, the
  README or the web page. The pixel count and the surviving-tag count are the only numbers.
- **The second read sees the residue only** — redacted text, filled image — because what
  the shapes caught should not leave the device in order to be discussed.
