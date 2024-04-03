package com.app.lingcompanion.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.app.lingcompanion.R
import com.app.lingcompanion.databinding.FragmentChatBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var queryEdt: TextInputEditText
    private lateinit var messageRV: RecyclerView
    private lateinit var messageRVAdapter: MessageRVAdapter
    private lateinit var messageList: ArrayList<MessageRVModal>

    // OpenAI API configuration
    private var url = "https://api.openai.com/v1/completions"
    private val apiKey = "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"
    private val organizationId = "org-8b4c3vvL9PhyY6Yzs9KbbwiP"

    //Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var editText: EditText? = null
    private var microButton: ImageView? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        //Speech recognizer
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            setupSpeechRecognizer()
        }

        // Initialize views
        queryEdt = binding.idEdtQuery
        messageRV = binding.idRVMessages
        messageList = ArrayList()
        messageRVAdapter = MessageRVAdapter(messageList)
        messageRV.layoutManager = LinearLayoutManager(requireContext())
        messageRV.adapter = messageRVAdapter

        // Set listener for sending queries
        queryEdt.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val query = textView.text.toString()
                if (query.isNotEmpty()) {
                    messageList.add(MessageRVModal(query, "user"))
                    messageRVAdapter.notifyDataSetChanged()
                    getResponse(query)
                } else {
                    Toast.makeText(requireContext(), "Please enter your message", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance()

        // Enable edge-to-edge display
        enableEdgeToEdge()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

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

    // Function to handle API response
    private fun getResponse(query: String) {
        queryEdt.text?.clear()
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObject = JSONObject()
        jsonObject.put("model", "gpt-3.5-turbo-instruct")
        jsonObject.put("prompt", query)
        jsonObject.put("temperature", 0)
        jsonObject.put("max_tokens", 50)
        jsonObject.put("top_p", 1)
        jsonObject.put("frequency_penalty", 0.0)
        jsonObject.put("presence_penalty", 0.0)

        val postRequest = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonObject,
            Response.Listener { response ->
                try {
                    Log.d("Response", "Received response: $response")
                    val responseMsg: String =
                        response.getJSONArray("choices").getJSONObject(0).getString("text")
                    messageList.add(MessageRVModal(responseMsg, "bot"))
                    messageRVAdapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    Log.e("Response", "Error parsing JSON: ${e.message}")
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Log.e("Response", "Volley error: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to get a response",
                    Toast.LENGTH_SHORT
                ).show()
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

                    override fun retry(error: VolleyError) {
                        // You can implement retry logic here if needed
                    }
                }
            }
        }
        queue.add(postRequest)
    }

    // Function to enable edge-to-edge display
    private fun enableEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}
