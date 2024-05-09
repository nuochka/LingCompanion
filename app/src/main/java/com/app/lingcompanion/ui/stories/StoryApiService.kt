package com.app.lingcompanion.ui.stories

import retrofit2.Call
import retrofit2.http.GET

interface StoryApiService {

    @GET("/")
    fun getRandomStory(): Call<Story>
}
