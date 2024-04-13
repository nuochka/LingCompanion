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

    inner class DictionaryApiTask(private val textView: TextView) : AsyncTask<Void, Void, JSONArray>() {

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
