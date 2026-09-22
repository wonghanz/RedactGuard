package dev.capriguard.redactguard.redact

/**
 * Text shapes. Each one is a published regular expression with a stated
 * confidence, and the confidence decides whether the app applies it on its own
 * or waits for a tap. Nothing here is a model: a match is either in the table or
 * it is not, and the table is short on purpose. A shape that fires on ordinary
 * prose is worse than no shape, because it teaches people to stop reading the
 * list.
 */
enum class Confidence { Firm, Possible }

data class Shape(
    val id: String,
    val label: String,
    val givesAway: String,
    val confidence: Confidence,
    val pattern: Regex,
    /** Only a sanity filter, e.g. Luhn on a card number. Null means "every match counts". */
    val accepts: ((String) -> Boolean)? = null,
) {
    val autoApply: Boolean get() = confidence == Confidence.Firm
}

private val ic = setOf(RegexOption.IGNORE_CASE)

object Shapes {

    /**
     * Order matters: a range already claimed by an earlier shape is not offered
     * again, so the address shape never eats an email that matched before it.
     */
    val all: List<Shape> = listOf(
        Shape(
            id = "labelled",
            label = "Labelled identifier",
            givesAway = "the value itself, named by whatever typed it in",
            confidence = Confidence.Firm,
            pattern = Regex(
                """(?i)\b(?:nric|nrp|my ?kad|ic\s*number|identity (?:card|number)|dob|date of birth""" +
                    """|passport\s*(?:no|number)?|account\s*(?:no|number)?|acc\s*no|card\s*(?:no|number)""" +
                    """|cvv|cvc|otp|one[- ]?time (?:code|password)|verification code|api\s*key|secret|token)""" +
                    """\b[\s:for=]{0,6}[A-Za-z0-9][A-Za-z0-9()/+\-.]{2,28}""",
            ),
        ),
        Shape(
            id = "credential-url",
            label = "Link with a login inside it",
            givesAway = "a username and a password, in plain text",
            confidence = Confidence.Firm,
            pattern = Regex(
                """(?i)\b(?:https?|ftp|ftps|ws|wss|postgres(?:ql)?|mysql|mongodb(?:\+srv)?|redis|amqp|smtp|imap)""" +
                    """://[^\s/\\:@]{1,64}:[^\s/@\\]{1,64}[@]""",
            ),
        ),
        Shape(
            id = "token",
            label = "API key or bearer token",
            givesAway = "a live credential, and nothing says which vendor it belongs to",
            confidence = Confidence.Firm,
            pattern = Regex(
                """\b(?:sk-[A-Za-z0-9_\-]{8,}|gh[pousr]_[A-Za-z0-9]{8,}|github_pat_[A-Za-z0-9_]{8,}""" +
                    """|AKIA[0-9A-Z]{8,}|xox[baprs]-[A-Za-z0-9\-]{8,}|ya29\.[A-Za-z0-9_\-]{8,}""" +
                    """|AIza[0-9A-Za-z_\-]{8,}|eyJ[A-Za-z0-9_\-]{8,})\b""" +
                    """|(?i)\bbearer\s+[A-Za-z0-9._~+/\-]{12,}""",
            ),
        ),
        Shape(
            id = "email",
            label = "Email address",
            givesAway = "who you are, and the mailbox to answer as",
            confidence = Confidence.Firm,
            pattern = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,24}"""),
        ),
        Shape(
            id = "mykad",
            label = "Malaysian identity number",
            givesAway = "a birth date, a place of birth encoded in the last six digits, and one person",
            confidence = Confidence.Firm,
            pattern = Regex("""\b\d{6}-?\d{2}-?\d{4}\b"""),
            accepts = { raw -> birthDatePlausible(raw.filter(Char::isDigit)) },
        ),
        Shape(
            id = "card",
            label = "Card number",
            givesAway = "a payment instrument, and with the check digit it is a usable one",
            confidence = Confidence.Firm,
            pattern = Regex("""\b(?:\d[ \-]?){12,18}\d\b"""),
            accepts = { raw -> luhnPass(raw.filter(Char::isDigit)) },
        ),
        Shape(
            id = "iban",
            label = "Bank account (IBAN)",
            givesAway = "an account number in a format a stranger can debit",
            confidence = Confidence.Firm,
            pattern = Regex("""\b[A-Z]{2}\d{2}(?: ?[A-Z0-9]){11,30}\b""", ic),
        ),
        Shape(
            id = "coords",
            label = "Coordinates",
            givesAway = "a point on the ground, usually home or work",
            confidence = Confidence.Firm,
            pattern = Regex("""\b-?\d{1,3}\.\d{4,}\s*[,;]\s*-?\d{1,3}\.\d{4,}\b"""),
        ),
        Shape(
            id = "mac",
            label = "Hardware address",
            givesAway = "this particular device, across any network it joins",
            confidence = Confidence.Firm,
            pattern = Regex("""\b(?:[0-9A-Fa-f]{2}[:\-]){5}[0-9A-Fa-f]{2}\b"""),
        ),
        Shape(
            id = "phone",
            label = "Phone number",
            givesAway = "a line that reaches you directly",
            confidence = Confidence.Firm,
            pattern = Regex(
                """(?<!\w)(?:\+\d{1,3}[ \.\-]?)?(?:\(\d{2,4}\)[ \.\-]?\d{3,4}|\d{2,4}[ \.\-]\d{3,4})[ \.\-]?\d{3,4}\b""" +
                    """|(?<![\w.])\+\d[\d \.\-()]{6,20}\d""",
            ),
            accepts = { raw -> raw.filter(Char::isDigit).length in 8..15 },
        ),
        Shape(
            id = "address",
            label = "Street address",
            givesAway = "where a person can be walked to",
            confidence = Confidence.Possible,
            pattern = Regex(
                """\b\d{1,5}[A-Za-z]?\s+[A-Z][A-Za-z'()\- ]{2,28}(?i:\b(?:road|street|avenue|drive|lane|way|close|crescent|jalan|lorong|tamans?)\b)""" +
                    """|\b(?i:jalan|lorong|tamans?)[ ,.](?:[A-Z0-9][A-Za-z0-9'()&\-]*[ ]?){0,4}[A-Z0-9][A-Za-z0-9'()&\-]{1,}""",
            ),
        ),
        Shape(
            id = "handle",
            label = "Account handle",
            givesAway = "an identity carried across platforms",
            confidence = Confidence.Possible,
            pattern = Regex("""(?<![\w.])@[A-Za-z0-9_][A-Za-z0-9_.]{2,28}\b"""),
        ),
        Shape(
            id = "digits",
            label = "Long digit run",
            givesAway = "possibly an account, order, reference or ID — the app cannot tell which",
            confidence = Confidence.Possible,
            pattern = Regex("""\b\d{7,14}\b"""),
        ),
        Shape(
            id = "ipv4",
            label = "IPv4 address",
            givesAway = "a network location, and sometimes the only trace of who was on it",
            confidence = Confidence.Possible,
            pattern = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b"""),
            accepts = { raw -> raw.split('.').all { it.toIntOrNull()?.let { n -> n in 0..255 } == true } },
        ),
    )

    val byId: Map<String, Shape> = all.associateBy { it.id }

    /**
     * The six-digit prefix of a MyKad is a birth date, and the middle two are a
     * place-of-birth code. Requiring a real date is what keeps an order number
     * printed as twelve digits from being called an identity number.
     */
    fun birthDatePlausible(digits: String): Boolean {
        if (digits.length != 12) return false
        val yy = digits.substring(0, 2).toInt()
        val mm = digits.substring(2, 4).toInt()
        val dd = digits.substring(4, 6).toInt()
        if (mm !in 1..12 || dd !in 1..31) return false
        // Two-digit years in a MyKad span roughly 1926..2025.
        val century = if (yy > 25) 1900 else 2000
        val days = runCatching { java.time.LocalDate.of(century + yy, mm, dd) }.getOrNull() ?: return false
        return !days.isAfter(java.time.LocalDate.now())
    }

    fun luhnPass(digits: String): Boolean {
        if (digits.length < 13 || digits.length > 19) return false
        if (digits.toSet().size == 1) return false
        var sum = 0
        var alt = false
        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (alt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alt = !alt
        }
        return sum % 10 == 0
    }
}

/** One matched span, and the shape that matched it. */
data class Finding(
    val shape: Shape,
    val start: Int,
    val end: Int,
    val fragment: String,
) {
    val length: Int get() = end - start
    val autoApply: Boolean get() = shape.autoApply
}

/**
 * The whole text pass. Findings are non-overlapping by construction, ordered by
 * position, and recomputed from scratch on every edit — there is no cached state
 * that could drift away from what is on screen.
 */
object TextScanner {

    private const val MAX_PER_SHAPE = 400

    fun scan(text: String): List<Finding> {
        if (text.isBlank()) return emptyList()
        val taken = ArrayList<IntRange>()
        val out = ArrayList<Finding>()
        for (shape in Shapes.all) {
            var kept = 0
            for (m in shape.pattern.findAll(text)) {
                if (kept >= MAX_PER_SHAPE) break
                val range = m.range
                if (range.first > range.last) continue
                if (taken.any { it.intersects(range) }) continue
                val raw = m.value
                if (shape.accepts != null && !shape.accepts(raw)) continue
                if (raw.isBlank()) continue
                taken += range
                out += Finding(shape, range.first, range.last + 1, raw)
                kept += 1
            }
        }
        return out.sortedBy { it.start }
    }

    fun placeholderFor(f: Finding): String = "[REDACTED:${f.shape.id}]"

    /** Replaces the enabled spans, left to right, with a label of what stood there. */
    fun apply(text: String, enabled: List<Finding>): String {
        if (enabled.isEmpty()) return text
        val sb = StringBuilder(text.length)
        var cursor = 0
        for (f in enabled.sortedBy { it.start }) {
            if (f.start < cursor) continue
            sb.append(text, cursor, f.start)
            sb.append(placeholderFor(f))
            cursor = f.end
        }
        sb.append(text, cursor, text.length)
        return sb.toString()
    }

    fun counts(findings: List<Finding>): Map<String, Int> =
        findings.groupingBy { it.shape.id }.eachCount()

    /** True when the only thing standing between a value and the reader is a label. */
    fun verdict(findings: List<Finding>): String {
        val firm = findings.count { it.autoApply }
        val possible = findings.size - firm
        return when {
            findings.isEmpty() -> "no shape in the table matched — which is not the same as nothing identifying you"
            possible == 0 -> "$firm firm match" + (if (firm == 1) "" else "es") + ", every one of them a shape this app can be sure about"
            else -> "$firm firm and $possible only-possible — the possible ones need your eyes, and the app will not apply them without you"
        }
    }
}

private fun IntRange.intersects(other: IntRange): Boolean =
    this.first < other.last && other.first < this.last

fun fmt1(n: Double): String = String.format("%.1f", n)
