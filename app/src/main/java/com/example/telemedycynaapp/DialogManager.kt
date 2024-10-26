package com.example.telemedycynaapp

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AlertDialog

class DialogManager {
    companion object {

        fun showDialogPermDenied(context: Context, action: () -> Unit) {
            AlertDialog.Builder(context)
                .setTitle("Brak uprawnień")
                .setMessage("Odmówiono pewnych uprawnień przyznaj je w aplikacji")
                .setPositiveButton(
                    "OK"
                ) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    action()
                }
                .setNegativeButton(
                    "Anuluj"
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
        }

        fun showSettingsDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle("Wymagane uprawnienia")
                .setMessage("Niektóre uprawnienia zostały trwale odrzucone. Proszę przejść do ustawień i je przyznać.")
                .setPositiveButton("Przejdź do ustawień") { dialog, _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("Anuluj") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        fun showBtLocationDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle("Uruchom lokalizację i Bluetooth")
                .setMessage("Aby skanować urządzenia lokalizacja i bluetooth muszą być włączone. Uruchom je i spróbuj ponownie.")
                .setPositiveButton(
                    "OK",
                    (DialogInterface.OnClickListener { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                            )).show()
        }
    }
}