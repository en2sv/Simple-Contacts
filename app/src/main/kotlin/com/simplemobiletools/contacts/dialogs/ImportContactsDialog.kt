package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.showContactSourcePicker
import com.simplemobiletools.contacts.helpers.VcfImporter
import com.simplemobiletools.contacts.helpers.VcfImporter.ImportResult.IMPORT_FAIL
import kotlinx.android.synthetic.main.dialog_import_contacts.view.*

class ImportContactsDialog(val activity: SimpleActivity, val path: String, val callback: (refreshView: Boolean) -> Unit) {
    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_contacts, null) as ViewGroup).apply {
            import_contacts_title.text = activity.config.lastUsedContactSource
            import_contacts_title.setOnClickListener {
                activity.showContactSourcePicker(import_contacts_title.value) {
                    import_contacts_title.text = it
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.import_contacts) {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    activity.toast(R.string.importing)
                    Thread {
                        val result = VcfImporter(activity).importContacts(path, view.import_contacts_title.value)
                        handleParseResult(result)
                        dismiss()
                    }.start()
                }
            }
        }
    }

    private fun handleParseResult(result: VcfImporter.ImportResult) {
        activity.toast(when (result) {
            VcfImporter.ImportResult.IMPORT_OK -> R.string.importing_successful
            VcfImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
            else -> R.string.importing_failed
        })
        callback(result != IMPORT_FAIL)
    }
}
