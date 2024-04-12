package com.app.lingcompanion.ui.home

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.lingcompanion.databinding.FragmentHomeBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val wordTextView: TextView = binding.wordTextView

        DictionaryApiTask(wordTextView).execute()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class DictionaryApiTask(private val textView: TextView) : AsyncTask<Void, Void, JSONArray>() {

        override fun doInBackground(vararg params: Void?): JSONArray? {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/particular")
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

                        val meaningArray = jsonArray.getJSONObject(0).optJSONArray("meanings")
                        val example = meaningArray?.optJSONObject(0)?.optJSONArray("definitions")?.optJSONObject(0)?.optString("example", "Example not found")

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

    }
}
