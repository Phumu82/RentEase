package com.rentease.app.domain.model

/**
 * Validation for the public Contact form (same limits as the web contact page).
 * Sending is done by handing the message to the device's email app, so nothing is
 * pretended to be "sent" by the app itself.
 */
object ContactForm {
    const val SUPPORT_EMAIL = "support@rentease.co.za"
    const val NAME_MIN = 2
    const val NAME_MAX = 100
    const val EMAIL_MAX = 255
    const val SUBJECT_MIN = 3
    const val SUBJECT_MAX = 120
    const val MESSAGE_MIN = 20
    const val MESSAGE_MAX = 1500

    enum class Field { NAME, EMAIL, SUBJECT, MESSAGE }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun validate(name: String, email: String, subject: String, message: String): Set<Field> {
        val errors = mutableSetOf<Field>()
        if (name.trim().length !in NAME_MIN..NAME_MAX) errors += Field.NAME
        val e = email.trim()
        if (e.length > EMAIL_MAX || !EMAIL_REGEX.matches(e)) errors += Field.EMAIL
        if (subject.trim().length !in SUBJECT_MIN..SUBJECT_MAX) errors += Field.SUBJECT
        if (message.trim().length !in MESSAGE_MIN..MESSAGE_MAX) errors += Field.MESSAGE
        return errors
    }

    /** Body for the email draft: the message plus who wrote it, so support can reply. */
    fun emailBody(name: String, email: String, message: String): String =
        message.trim() + "\n\n-- " + name.trim() + " <" + email.trim() + ">"
}
