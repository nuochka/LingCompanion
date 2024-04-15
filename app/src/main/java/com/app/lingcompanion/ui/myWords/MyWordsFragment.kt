package com.app.lingcompanion.ui.myWords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            val textView = TextView(requireContext())
            textView.text = word
            textView.setOnLongClickListener {
                WordFileManager.deleteWord(requireContext(), word)
                savedWordsContainer.removeView(textView)
                true
            }
            savedWordsContainer.addView(textView)
        }

        return root
    }
}
