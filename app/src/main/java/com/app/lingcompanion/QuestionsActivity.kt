package com.app.lingcompanion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuestionsActivity : AppCompatActivity() {

    private lateinit var sportCheckBox: CheckBox
    private lateinit var cultureCheckBox: CheckBox
    private lateinit var musicCheckBox: CheckBox
    private lateinit var foodCheckBox: CheckBox
    private lateinit var travelCheckBox: CheckBox
    private lateinit var saveButton: Button
    private lateinit var englishLevelRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_questions)

        sportCheckBox = findViewById(R.id.sportCheckBox)
        cultureCheckBox = findViewById(R.id.cultureCheckBox)
        musicCheckBox = findViewById(R.id.musicCheckBox)
        foodCheckBox = findViewById(R.id.foodCheckBox)
        travelCheckBox = findViewById(R.id.travelCheckBox)
        saveButton = findViewById(R.id.saveButton)
        englishLevelRadioGroup = findViewById(R.id.englishLevelRadioGroup)

        // Save button click listener
        saveButton.setOnClickListener {
            // Check if at least one English level is selected
            val selectedLevelId = englishLevelRadioGroup.checkedRadioButtonId
            if (selectedLevelId == -1) {
                Toast.makeText(this, "Please select your English level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if at least one interest is selected
            if (!sportCheckBox.isChecked &&
                !cultureCheckBox.isChecked &&
                !musicCheckBox.isChecked &&
                !foodCheckBox.isChecked &&
                !travelCheckBox.isChecked) {
                Toast.makeText(this, "Please select at least one interest", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If both English level and at least one interest are selected
            Toast.makeText(this, "Register completed successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
