package com.app.lingcompanion.ui.home


import android.content.res.Resources
import android.os.AsyncTask
import android.os.Bundle
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
import com.app.lingcompanion.R
import java.util.*

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

        val englishWordsArray = resources.getStringArray(R.array.english_words)
        val randomWord = getRandomWord(englishWordsArray)

        val apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$randomWord"
        DictionaryApiTask(wordTextView).execute(apiUrl)

        return root
    }

    private fun getRandomWord(wordsArray: Array<String>): String {
        val random = Random()
        return wordsArray[random.nextInt(wordsArray.size)]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DictionaryApiTask(private val textView: TextView) : AsyncTask<String, Void, JSONArray>() {

        override fun doInBackground(vararg params: String?): JSONArray? {
            val apiUrl = params[0]
            apiUrl?.let {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(apiUrl)
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                return responseData?.let { JSONArray(it) }
            }
            return null
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
                        val definitionsArray = meaningsArray?.optJSONObject(0)?.optJSONArray("definitions")
                        val definition = definitionsArray?.optJSONObject(0)?.optString("definition", "Definition not found")

                        val example = findExample(jsonArray)

                        activity?.runOnUiThread {
                            val displayText = "$word\n$phonetic\n\n- $definition\n$example"
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

        private fun findExample(jsonArray: JSONArray): String {
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
