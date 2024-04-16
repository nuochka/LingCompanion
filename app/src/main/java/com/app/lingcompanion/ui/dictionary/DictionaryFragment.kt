package com.app.lingcompanion.ui.dictionary

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.lingcompanion.databinding.FragmentDictionaryBinding
import org.json.JSONArray
import org.json.JSONException
import okhttp3.OkHttpClient
import okhttp3.Request
import com.app.lingcompanion.ui.WordFileManager
import com.app.lingcompanion.ui.myWords.Word

class DictionaryFragment : Fragment() {

    private var _binding: FragmentDictionaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val wordTextView: TextView = binding.wordTextView

        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    DictionaryApiTask(wordTextView).execute(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        binding.addButton.setOnClickListener {
            val currentWord = wordTextView.text.toString()
            if (currentWord.isNotEmpty()) {
                val word = Word(currentWord, null, null, null, null)
                WordFileManager.saveWord(requireContext(), word)
                Toast.makeText(requireContext(), "The word was added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "The word is missing", Toast.LENGTH_SHORT).show()
            }
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class DictionaryApiTask(private val textView: TextView) : AsyncTask<String, Void, JSONArray>() {

        override fun doInBackground(vararg params: String?): JSONArray? {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/${params[0]}")
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()
            return responseData?.let { JSONArray(it) }
        }

        override fun onPostExecute(result: JSONArray?) {
            super.onPostExecute(result)
            result?.let { jsonArray ->
                try {
                    if (jsonArray.length() > 0) {
                        val firstItem = jsonArray.getJSONObject(0)
                        val word = firstItem.optString("word", "Word not found")
                        val phonetic = firstItem.optString("phonetic", "Phonetic not found")

                        val meaningsArray = firstItem.optJSONArray("meanings")
                        val partOfSpeech = meaningsArray?.optJSONObject(0)?.optString("partOfSpeech", "Part of speech not found")
                        val definitionsArray = meaningsArray?.optJSONObject(0)?.optJSONArray("definitions")
                        val definition = definitionsArray?.optJSONObject(0)?.optString("definition", "Definition not found")

                        val example = findExample(jsonArray)

                        activity?.runOnUiThread {
                            val displayText = "Word: $word\nPhonetic: $phonetic\nPart of Speech: $partOfSpeech\nDefinition: $definition\nExample: $example"
                            textView.text = displayText
                        }
                    } else {
                        textView.text = "No data received from API"
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    textView.text = "Error processing API response"
                }
            } ?: run {
                textView.text = "No data received from API"
            }
        }

        fun findExample(jsonArray: JSONArray): String {
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val meaningsArray = jsonObject.getJSONArray("meanings")
                for (j in 0 until meaningsArray.length()) {
                    val meaningObject = meaningsArray.getJSONObject(j)
                    val definitionsArray = meaningObject.getJSONArray("definitions")
                    for (k in 0 until definitionsArray.length()) {
                        val definitionObject = definitionsArray.getJSONObject(k)
                        if (definitionObject.has("example")) {
                            return definitionObject.getString("example")
                        }
                    }
                }
            }
            return "Example not found"
        }
    }
}
