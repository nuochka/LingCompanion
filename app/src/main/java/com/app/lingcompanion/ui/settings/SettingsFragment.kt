package com.app.lingcompanion.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.lingcompanion.NotificationReceiver
import com.app.lingcompanion.databinding.FragmentSettingsBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        sharedPreferences = requireActivity().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

        // Set the state of the notification switch
        binding.notificationSwitch.isChecked = loadNotificationState()

        binding.btnSaveSettings.setOnClickListener {
            changePassword()
        }

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }
        }

        return root
    }

    // Method to enable notifications
    private fun enableNotifications() {
        NotificationReceiver.setNotificationsEnabled(true)
        saveNotificationState(true) // Save the notification state
        Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
    }

    // Method to disable notifications
    private fun disableNotifications() {
        NotificationReceiver.setNotificationsEnabled(false)
        saveNotificationState(false) // Save the notification state
        Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
    }

    // Method to change password
    private fun changePassword(){
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmNewPassword.text.toString()

        if(currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()){
            if(newPassword == confirmPassword){
                val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                currentUser.reauthenticate(credential)
                    .addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            // Change password
                            currentUser.updatePassword(newPassword)
                                .addOnCompleteListener { updatePasswordTask ->
                                    if (updatePasswordTask.isSuccessful) {
                                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                        // Clear fields after successful change
                                        binding.etCurrentPassword.text.clear()
                                        binding.etNewPassword.text.clear()
                                        binding.etConfirmNewPassword.text.clear()
                                    } else {
                                        Toast.makeText(context, "Failed to change password", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Authentication failed. Please check your current password.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Please enter all the fields", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to save the state of notifications using SharedPreferences
    private fun saveNotificationState(enabled: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("notification_enabled", enabled)
        editor.apply()
    }

    // Method to load the state of notifications from SharedPreferences
    private fun loadNotificationState(): Boolean {
        return sharedPreferences.getBoolean("notification_enabled", true) // Default is true
    }
}
