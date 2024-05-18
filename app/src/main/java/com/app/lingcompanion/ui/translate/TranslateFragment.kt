package com.app.lingcompanion.ui.translate

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.app.lingcompanion.R
import com.google.android.material.button.MaterialButton
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class TranslateFragment : Fragment() {

    private lateinit var sourceLanguageEt: EditText
    private lateinit var targetLanguageTv: TextView
    private lateinit var sourceLanguageChooseBtn: MaterialButton
    private lateinit var targetLanguageChooseBtn: MaterialButton
    private lateinit var translateBtn: MaterialButton

    companion object {
        private const val TAG = "MAIN_TAG"
    }

    private lateinit var languageArrayList: ArrayList<ModelLanguage>

    // Default source and target languages
    private var sourceLanguageCode = "en"
    private var sourceLanguageTitle = "English"
    private var targetLanguageCode = "pl"
    private var targetLanguageTitle = "Polish"

    private lateinit var translatorOptions: TranslatorOptions
    private lateinit var translator: Translator
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_translate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        sourceLanguageEt = view.findViewById(R.id.sourceLanguageEt)
        targetLanguageTv = view.findViewById(R.id.targetLanguageTv)
        sourceLanguageChooseBtn = view.findViewById(R.id.sourceLanguageChooseBtn)
        targetLanguageChooseBtn = view.findViewById(R.id.targetLanguageChooseBtn)
        translateBtn = view.findViewById(R.id.translateBtn)

        // Initialize progress dialog
        progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("Please wait...")
            setCanceledOnTouchOutside(false)
        }

        // Load available languages for translation
        loadAvailableLanguages()

        // Set click listeners for buttons
        sourceLanguageChooseBtn.setOnClickListener { sourceLanguageChoose() }
        targetLanguageChooseBtn.setOnClickListener { targetLanguageChoose() }
        translateBtn.setOnClickListener { validateData() }
    }

    private var sourceLanguageText = ""

    // Validate input data and start translation if valid
    private fun validateData() {
        sourceLanguageText = sourceLanguageEt.text.toString().trim()

        Log.d(TAG, "validateData: sourceLanguageText: $sourceLanguageText")

        if (sourceLanguageText.isEmpty()) {
            showToast("Enter text to translate")
        } else {
            startTranslation()
        }
    }

    // Start the translation process
    private fun startTranslation() {
        progressDialog.setMessage("Processing language model...")
        progressDialog.show()

        // Set up translator options
        translatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguageCode)
            .setTargetLanguage(targetLanguageCode)
            .build()
        translator = Translation.getClient(translatorOptions)

        // Set download conditions for the language model
        val downloadConditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // Download the model if needed and perform translation
        translator.downloadModelIfNeeded(downloadConditions)
            .addOnSuccessListener {
                Log.d(TAG, "startTranslation: model ready, start translation...")

                progressDialog.setMessage("Translating")

                translator.translate(sourceLanguageText)
                    .addOnSuccessListener { translatedText ->
                        Log.d(TAG, "startTranslation: translatedText: $translatedText")

                        progressDialog.dismiss()
                        targetLanguageTv.text = translatedText
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Log.e(TAG, "startTranslation: ", e)
                        showToast("Failed to translate due to ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e(TAG, "startTranslation: ", e)
                showToast("Failed due to ${e.message}")
            }
    }

    // Load available languages for translation
    private fun loadAvailableLanguages() {
        languageArrayList = ArrayList()

        val languageCodeList = TranslateLanguage.getAllLanguages()
        for (languageCode in languageCodeList) {
            val languageTitle = Locale(languageCode).displayLanguage

            Log.d(TAG, "loadAvailableLanguages: languageCode: $languageCode")
            Log.d(TAG, "loadAvailableLanguages: languageTitle: $languageTitle")

            val modelLanguage = ModelLanguage(languageCode, languageTitle)
            languageArrayList.add(modelLanguage)
        }
    }

    // Show popup menu to choose source language
    private fun sourceLanguageChoose() {
        val popupMenu = PopupMenu(requireContext(), sourceLanguageChooseBtn)

        for (i in languageArrayList.indices) {
            popupMenu.menu.add(Menu.NONE, i, i, languageArrayList[i].languageTitle)
        }

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val position = menuItem.itemId

            sourceLanguageCode = languageArrayList[position].languageCode
            sourceLanguageTitle = languageArrayList[position].languageTitle

            sourceLanguageChooseBtn.text = sourceLanguageTitle
            sourceLanguageEt.hint = "Enter $sourceLanguageTitle"

            Log.d(TAG, "sourceLanguageChoose: sourceLanguageCode: $sourceLanguageCode")
            Log.d(TAG, "sourceLanguageChoose: sourceLanguageTitle: $sourceLanguageTitle")

            false
        }
    }

    // Show popup menu to choose target language
    private fun targetLanguageChoose() {
        val popupMenu = PopupMenu(requireContext(), targetLanguageChooseBtn)

        for (i in languageArrayList.indices) {
            popupMenu.menu.add(Menu.NONE, i, i, languageArrayList[i].languageTitle)
        }

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val position = menuItem.itemId

            targetLanguageCode = languageArrayList[position].languageCode
            targetLanguageTitle = languageArrayList[position].languageTitle

            targetLanguageChooseBtn.text = targetLanguageTitle

            Log.d(TAG, "targetLanguageChoose: targetLanguageCode: $targetLanguageCode")
            Log.d(TAG, "targetLanguageChoose: targetLanguageTitle: $targetLanguageTitle")

            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
