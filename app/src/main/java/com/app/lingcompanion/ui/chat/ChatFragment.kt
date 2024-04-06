package com.app.lingcompanion.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.app.lingcompanion.R
import com.app.lingcompanion.databinding.FragmentChatBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ChatFragment : Fragment() {
    //Firebase auth
    private lateinit var binding: FragmentChatBinding
    private lateinit var auth: FirebaseAuth

    //Chat
    private lateinit var queryEdt: TextInputEditText
    private lateinit var messageRV: RecyclerView
    private lateinit var messageRVAdapter: MessageRVAdapter
    private lateinit var messageList: ArrayList<MessageRVModal>

    //OpenAI API
    private var url = "https://api.openai.com/v1/completions"
    private val apiKey = "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"
    private val organizationId = "org-8b4c3vvL9PhyY6Yzs9KbbwiP"

    //Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var editText: EditText? = null
    private var microButton: ImageView? = null

    private var isWaitingForUserResponse = false

    // Text-to-Speech
    private lateinit var tts: TextToSpeech

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            setupSpeechRecognizer()
        }

        queryEdt = binding.idEdtQuery
        messageRV = binding.idRVMessages
        messageList = ArrayList()
        messageRVAdapter = MessageRVAdapter(messageList)
        messageRV.layoutManager = LinearLayoutManager(requireContext())
        messageRV.adapter = messageRVAdapter

        queryEdt.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val query = textView.text.toString()
                if (query.isNotEmpty()) {
                    messageRVAdapter.notifyDataSetChanged()
                    if (!isWaitingForUserResponse) {
                        getResponse(query)
                    }
                    queryEdt.setText("")
                } else {
                    Toast.makeText(requireContext(), "Please enter your message", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        sendInitialMessage()

        // Text-to-Speech initialization
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }

    //Permission
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    //Speech recognizer
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpeechRecognizer() {
        editText = binding.idEdtQuery
        microButton = binding.microButton
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                editText?.setText("")
                editText?.setHint("Listening...")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(bundle: Bundle?) {
                microButton?.setImageResource(R.drawable.ic_mic_off)
                val data = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                editText?.setText(data?.get(0))
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        microButton?.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                speechRecognizer?.stopListening()
            }
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                microButton?.setImageResource(R.drawable.ic_mic_on)
                speechRecognizer?.startListening(speechRecognizerIntent)
            }
            false
        }
    }

    //Initialization OpenAI API for requests and responses
    private fun getResponse(query: String) {
        messageList.add(MessageRVModal(query, "user"))
        messageRVAdapter.notifyDataSetChanged()

        isWaitingForUserResponse = true

        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObject = JSONObject()
        jsonObject.put("model", "gpt-3.5-turbo-instruct")
        jsonObject.put("prompt", query)
        jsonObject.put("temperature", 0)
        jsonObject.put("max_tokens", 150)
        jsonObject.put("top_p", 1)
        jsonObject.put("frequency_penalty", 0.0)
        jsonObject.put("presence_penalty", 0.0)

        val postRequest = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonObject,
            Response.Listener { response ->
                try {
                    val responseMsg: String =
                        response.getJSONArray("choices").getJSONObject(0).getString("text")
                    val cleanedResponseMsg = removeNewLines(responseMsg)
                    messageList.add(MessageRVModal(cleanedResponseMsg, "bot"))
                    messageRVAdapter.notifyDataSetChanged()
                    speakOut(cleanedResponseMsg) // Speak bot's message
                } catch (e: JSONException) {
                    Log.e("Response", "Error parsing JSON: ${e.message}")
                    e.printStackTrace()
                }
                isWaitingForUserResponse = false

            },
            Response.ErrorListener { error ->
                Log.e("Response", "Volley error: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to get a response",
                    Toast.LENGTH_SHORT
                ).show()
                isWaitingForUserResponse = false
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"
                params["Authorization"] = "Bearer $apiKey"
                params["OpenAI-Organization"] = organizationId
                return params
            }

            override fun getRetryPolicy(): RetryPolicy {
                return object : RetryPolicy {
                    override fun getCurrentTimeout(): Int {
                        return 5000
                    }

                    override fun getCurrentRetryCount(): Int {
                        return 5000
                    }

                    override fun retry(error: VolleyError) {}
                }
            }
        }
        queue.add(postRequest)
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun enableEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //Initial message with random topic for discussion
    private fun sendInitialMessage() {
        val conversationTopics = arrayOf(
            "How's your day going?",
            "Do you have any exciting plans for the weekend?",
            "What's your favorite thing to do in your free time?",
            "Have you seen any good movies or TV shows lately?",
            "Do you enjoy cooking? What's your signature dish?",
            "How do you usually unwind after a long day?",
            "Are you a fan of any particular sports or hobbies?",
            "Do you have any pets? Tell me about them!",
            "What's the most interesting place you've ever visited?",
            "Are you a morning person or a night owl?",
            "What's your favorite season of the year and why?",
            "Do you like to read? Any favorite books or genres?",
            "Are you into any form of art or creative activities?",
            "What's the best piece of advice you've ever received?",
            "If you could travel anywhere in the world right now, where would you go?",
            "Are there any foods you absolutely can't stand?",
            "What's the last song you listened to that you couldn't get out of your head?",
            "Do you believe in superstitions or lucky charms?",
            "What's your go-to comfort food when you're feeling down?",
            "Are there any childhood memories that always make you smile?",
            "Do you prefer spending time outdoors or indoors?",
            "What's the funniest joke you've heard recently?",
            "If you could have any superpower, what would it be and why?",
            "What's the most adventurous thing you've ever done?",
            "Are there any languages you'd love to learn?",
            "Do you enjoy watching sunsets or sunrises?",
            "What's your favorite way to relax and de-stress?",
            "If you could meet any historical figure, who would it be and why?",
            "What's the best part about where you live?",
            "Are there any talents or skills you wish you had?",
            "What's the most memorable concert or live performance you've attended?"
        )
        val initialMessage = "Hello! I'm your ling companion and I want to help you improve your speaking skills!\n"
        val randomTopic = conversationTopics.random()
        val completeMessage = "$initialMessage\n\nLet's talk about this topic: $randomTopic"

        messageList.add(MessageRVModal(completeMessage, "bot"))
        messageRVAdapter.notifyDataSetChanged()
    }

    //Function for removing enters in bot messages
    private fun removeNewLines(input: String): String {
        return input.replace("\n", "")
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}
