package com.app.lingcompanion.ui.myWords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.lingcompanion.R
import com.app.lingcompanion.ui.WordFileManager

class MyWordsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_my_words, container, false)
        val savedWordsContainer: LinearLayout = root.findViewById(R.id.savedWordsContainer)
        val savedWords = WordFileManager.loadWords(requireContext())

        savedWords.forEach { word ->
            val wordLayout = LinearLayout(requireContext())
            wordLayout.orientation = LinearLayout.HORIZONTAL

            val textView = TextView(requireContext())
            textView.text = word.word
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.weight = 1.0f
            textView.layoutParams = layoutParams

            val deleteButton =
                inflater.inflate(R.layout.delete_button, savedWordsContainer, false) as Button
            deleteButton.setOnClickListener {
                WordFileManager.deleteWord(requireContext(), word)
                savedWordsContainer.removeView(wordLayout)
            }

            wordLayout.addView(textView)
            wordLayout.addView(deleteButton)
            savedWordsContainer.addView(wordLayout)
        }

        return root
    }
}