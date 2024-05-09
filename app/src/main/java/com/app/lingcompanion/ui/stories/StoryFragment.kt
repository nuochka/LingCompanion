package com.app.lingcompanion.ui.stories

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.lingcompanion.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StoryFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var storyTitleTextView: TextView
    private lateinit var storyAuthorTextView: TextView
    private lateinit var storyContentTextView: TextView
    private lateinit var storyMoralTextView: TextView
    private lateinit var getStoryButton: Button
    private lateinit var speakButton: Button
    private lateinit var textToSpeech: TextToSpeech
    private var isSpeaking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_story, container, false)

        storyTitleTextView = rootView.findViewById(R.id.storyTitleTextView)
        storyAuthorTextView = rootView.findViewById(R.id.storyAuthorTextView)
        storyContentTextView = rootView.findViewById(R.id.storyContentTextView)
        storyMoralTextView = rootView.findViewById(R.id.storyMoralTextView)
        getStoryButton = rootView.findViewById(R.id.getStoryButton)
        speakButton = rootView.findViewById(R.id.speakButton)
        textToSpeech = TextToSpeech(requireContext(), this)

        getStoryButton.setOnClickListener {
            loadRandomStory()
        }

        speakButton.setOnClickListener {
            val fullStory = "${storyTitleTextView.text}\n" +
                    "${storyAuthorTextView.text}\n\n" +
                    "${storyContentTextView.text}\n\n" +
                    "${storyMoralTextView.text}"

            if (isSpeaking) {
                textToSpeech.stop()
                isSpeaking = false
            } else {
                speakText(fullStory)
                isSpeaking = true
            }
        }

        return rootView
    }

    private fun loadRandomStory() {
        val storyApiService = RetrofitClient.storyApiService
        val call = storyApiService.getRandomStory()

        call.enqueue(object : Callback<Story> {
            override fun onResponse(call: Call<Story>, response: Response<Story>) {
                if (response.isSuccessful) {
                    val story = response.body()
                    if (story != null) {
                        displayStory(story)
                    }
                } else {
                    showToast("API error")
                }
            }
            override fun onFailure(call: Call<Story>, t: Throwable) {
                showToast("Network error")
            }
        })
    }

    private fun displayStory(story: Story) {
        storyTitleTextView.text = story.title
        storyAuthorTextView.text = "Author: ${story.author}"
        storyContentTextView.text = story.story
        storyMoralTextView.text = "Moral: ${story.moral}"
    }

    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "StoryUtterance")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Text-to-Speech engine is initialized successfully
            speakButton.isEnabled = true
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }

                override fun onError(utteranceId: String?) {
                    showToast("Error occurred during Text-to-Speech")
                    isSpeaking = false
                }
            })
        } else {
            showToast("Text-to-Speech initialization failed")
            isSpeaking = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources when fragment is destroyed
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
