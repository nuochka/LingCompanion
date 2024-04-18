package com.app.lingcompanion.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.lingcompanion.R
import com.app.lingcompanion.databinding.FragmentHomeBinding
import org.json.JSONArray
import org.json.JSONException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var wordTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        wordTextView = binding.wordTextView
        sharedPreferences = requireActivity().getSharedPreferences("WordFetch", Context.MODE_PRIVATE)
        fetchRandomWordIfNeeded()

        val squares = arrayOf(
            binding.square1, // Monday
            binding.square2, // Tuesday
            binding.square3, // Wednesday
            binding.square4, // Thursday
            binding.square5, // Friday
            binding.square6, // Saturday
            binding.square7  // Sunday
        )

        val currentDate = Calendar.getInstance().apply {
            time = Date()
        }
        val dayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK)
        val coloredSquareIndex = (dayOfWeek + 5) % 7 // Monday is the first day in the week.

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val formattedDate = sdf.format(currentDate.time)

        val squaresSharedPreferences = requireActivity().getSharedPreferences("Squares", Context.MODE_PRIVATE)
        val isSquareColoredToday = squaresSharedPreferences.getBoolean(formattedDate, false)

        // Getting a current amount of the days
        var streakCount = squaresSharedPreferences.getInt("StreakCount", 0)

        if (!isSquareColoredToday) {
            // Coloring a square
            squares[coloredSquareIndex].setBackgroundColor(Color.rgb(142, 36, 170))

            streakCount++

            with(squaresSharedPreferences.edit()) {
                putBoolean(formattedDate, true)
                putInt("StreakCount", streakCount)
                apply()
            }

            Toast.makeText(requireContext(), "Your today's streak was saved", Toast.LENGTH_SHORT).show()
        } else {
            // If a square was colored, update the color
            squares[coloredSquareIndex].setBackgroundColor(Color.rgb(142, 36, 170))
            streakCount++
            streakCount = squaresSharedPreferences.getInt("StreakCount", streakCount)
        }

        val button: Button = root.findViewById(R.id.btn_streak)

        button.setOnClickListener {
            if (isSquareColoredToday) {
                Toast.makeText(requireContext(), "You have already marked your progress", Toast.LENGTH_SHORT).show()
            } else {
                for (square in squares) {
                    square.setBackgroundColor(Color.rgb(142, 36, 170))
                }

                // If user skips one day, counter is reset to zero
                streakCount = 0

                with(squaresSharedPreferences.edit()) {
                    putBoolean(formattedDate, true)
                    putInt("StreakCount", streakCount)
                    apply()
                }

                Toast.makeText(requireContext(), "Your today's streak was saved", Toast.LENGTH_SHORT).show()
            }
        }

        val userStreakTextView: TextView = root.findViewById(R.id.user_streak)
        userStreakTextView.text = "Your streak: $streakCount days"

        return root
    }



    //Getting a random word from a list
    private fun fetchRandomWordIfNeeded() {
        val lastFetchTime = sharedPreferences.getLong("lastFetchTime", 0)
        val currentTime = System.currentTimeMillis()

        // Check if 24 hours has passed since the last fetch (temporary change)
        if (currentTime - lastFetchTime > TimeUnit.HOURS.toMillis(24)) {
            val englishWordsArray = resources.getStringArray(R.array.english_words)
            val randomWord = getRandomWord(englishWordsArray)

            val apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$randomWord"
            DictionaryApiTask().execute(apiUrl)
        } else {
            // Load saved data
            val savedDefinition = sharedPreferences.getString("definition", "Definition not found")
            val savedExample = sharedPreferences.getString("example", "Example not found")
            wordTextView.text = "$savedDefinition\n$savedExample"
        }
    }

    private fun getRandomWord(wordsArray: Array<String>): String {
        val random = Random()
        return wordsArray[random.nextInt(wordsArray.size)]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //The word of the day

    inner class DictionaryApiTask : AsyncTask<String, Void, Pair<String, String>>() {

        override fun doInBackground(vararg params: String?): Pair<String, String>? {
            val apiUrl = params[0]
            apiUrl?.let {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(apiUrl)
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                responseData?.let {
                    val jsonArray = JSONArray(it)
                    if (jsonArray.length() > 0) {
                        val firstItem = jsonArray.getJSONObject(0)
                        val word = firstItem.optString("word", "Word not found")
                        val phonetic = firstItem.optString("phonetic", "Phonetic not found")

                        val meaningsArray = firstItem.optJSONArray("meanings")
                        val definitionsArray = meaningsArray?.optJSONObject(0)?.optJSONArray("definitions")
                        val definition = definitionsArray?.optJSONObject(0)?.optString("definition", "Definition not found")

                        val example = findExample(jsonArray)

                        return Pair("$word\n$phonetic\n\n- $definition", example)
                    }
                }
            }
            return null
        }

        override fun onPostExecute(result: Pair<String, String>?) {
            super.onPostExecute(result)
            result?.let { (displayText, example) ->
                wordTextView.text = "$displayText\n$example"
                // Save the results to SharedPreferences
                saveResultsToStorage(displayText, example)
            } ?: run {
                wordTextView.text = "No data received from API"
            }
        }


        private fun saveResultsToStorage(definition: String, example: String) {
            sharedPreferences.edit().apply {
                putString("definition", definition)
                putString("example", example)
                putLong("lastFetchTime", System.currentTimeMillis())
                apply()
            }
        }


        //Function for finding an example in API
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
