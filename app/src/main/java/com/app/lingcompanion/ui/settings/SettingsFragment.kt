package com.app.lingcompanion.ui.settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.lingcompanion.databinding.FragmentSettingsBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        binding.btnSaveSettings.setOnClickListener {
            changePassword()
        }

        return root
    }

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
}
