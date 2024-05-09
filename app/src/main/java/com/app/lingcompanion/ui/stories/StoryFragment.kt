package com.app.lingcompanion.ui.stories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.lingcompanion.R
import com.app.lingcompanion.ui.stories.RetrofitClient
import com.app.lingcompanion.ui.stories.Story
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StoryFragment : Fragment() {

    private lateinit var storyTitleTextView: TextView
    private lateinit var storyAuthorTextView: TextView
    private lateinit var storyContentTextView: TextView
    private lateinit var storyMoralTextView: TextView
    private lateinit var getStoryButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_story, container, false)

        storyTitleTextView = view.findViewById(R.id.storyTitleTextView)
        storyAuthorTextView = view.findViewById(R.id.storyAuthorTextView)
        storyContentTextView = view.findViewById(R.id.storyContentTextView)
        storyMoralTextView = view.findViewById(R.id.storyMoralTextView)
        getStoryButton = view.findViewById(R.id.getStoryButton)

        getStoryButton.setOnClickListener {
            loadRandomStory()
        }

        return view
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
                    // Handle API error
                }
            }

            override fun onFailure(call: Call<Story>, t: Throwable) {
                // Handle network error
            }
        })
    }
    private fun displayStory(story: Story) {
        storyTitleTextView.text = story.title
        storyAuthorTextView.text = "Author: ${story.author}"
        storyContentTextView.text = story.story
        storyMoralTextView.text = "Moral: ${story.moral}"
    }
}
