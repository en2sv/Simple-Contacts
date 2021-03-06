package com.simplemobiletools.contacts.helpers

import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import android.provider.MediaStore
import android.util.Base64
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.commons.extensions.writeLn
import com.simplemobiletools.contacts.helpers.VcfExporter.ExportResult.*
import com.simplemobiletools.contacts.models.Contact
import java.io.ByteArrayOutputStream
import java.io.File

class VcfExporter {
    private val ENCODED_PHOTO_LINE_LENGTH = 74

    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var contactsExported = 0
    private var contactsFailed = 0

    fun exportContacts(activity: BaseSimpleActivity, file: File, contacts: ArrayList<Contact>, callback: (result: ExportResult) -> Unit) {
        activity.getFileOutputStream(file) {
            if (it == null) {
                callback(EXPORT_FAIL)
                return@getFileOutputStream
            }

            it.bufferedWriter().use { out ->
                for (contact in contacts) {
                    out.writeLn(BEGIN_VCARD)
                    out.writeLn(VERSION_2_1)
                    out.writeLn("$N${contact.surname};${contact.firstName};${contact.middleName};;")

                    contact.phoneNumbers.forEach {
                        out.writeLn("$TEL;${getPhoneNumberLabel(it.type)}:${it.value}")
                    }

                    contact.emails.forEach {
                        val type = getEmailTypeLabel(it.type)
                        val delimiterType = if (type.isEmpty()) "" else ";$type"
                        out.writeLn("$EMAIL$delimiterType:${it.value}")
                    }

                    contact.events.forEach {
                        if (it.type == CommonDataKinds.Event.TYPE_BIRTHDAY) {
                            out.writeLn("$BDAY${it.value}")
                        }
                    }

                    if (contact.thumbnailUri.isNotEmpty()) {
                        val firstLine = "$PHOTO;$ENCODING=$BASE64;$JPEG:"
                        val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, Uri.parse(contact.thumbnailUri))
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
                        bitmap.recycle()
                        val byteArray = byteArrayOutputStream.toByteArray()
                        val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                        val encodedFirstLineSection = encoded.substring(0, ENCODED_PHOTO_LINE_LENGTH - firstLine.length)
                        out.writeLn(firstLine + encodedFirstLineSection)
                        var curStartIndex = encodedFirstLineSection.length
                        do {
                            val part = encoded.substring(curStartIndex, Math.min(curStartIndex + ENCODED_PHOTO_LINE_LENGTH - 1, encoded.length))
                            out.writeLn(" $part")
                            curStartIndex += ENCODED_PHOTO_LINE_LENGTH - 1
                        } while (curStartIndex < encoded.length)

                        out.writeLn("")
                    }

                    out.writeLn(END_VCARD)
                    contactsExported++
                }
            }
        }

        callback(when {
            contactsExported == 0 -> EXPORT_FAIL
            contactsFailed > 0 -> EXPORT_PARTIAL
            else -> EXPORT_OK
        })
    }

    private fun getPhoneNumberLabel(type: Int) = when (type) {
        CommonDataKinds.Phone.TYPE_MOBILE -> CELL
        CommonDataKinds.Phone.TYPE_HOME -> HOME
        CommonDataKinds.Phone.TYPE_WORK -> WORK
        CommonDataKinds.Phone.TYPE_MAIN -> PREF
        CommonDataKinds.Phone.TYPE_FAX_WORK -> WORK_FAX
        CommonDataKinds.Phone.TYPE_FAX_HOME -> HOME_FAX
        CommonDataKinds.Phone.TYPE_PAGER -> PAGER
        else -> VOICE
    }

    private fun getEmailTypeLabel(type: Int) = when (type) {
        CommonDataKinds.Email.TYPE_HOME -> HOME
        CommonDataKinds.Email.TYPE_WORK -> WORK
        CommonDataKinds.Email.TYPE_MOBILE -> MOBILE
        else -> ""
    }
}
