package lam.tm.smartgloves.ui.speechtotext;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentSpeechToTextBinding;

public class SpeechToTextFragment extends Fragment {

    private FragmentSpeechToTextBinding binding;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSpeechToTextBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Speech Recognition
        initializeSpeechRecognition();

        // Setup speech toggle button
        Button buttonSpeechToggle = binding.buttonSpeechToggle;
        buttonSpeechToggle.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up speech recognizer
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        binding = null;
    }

    /**
     * Initialize Speech Recognition
     */
    private void initializeSpeechRecognition() {
        try {
            // Check if SpeechRecognizer is available
            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Speech Recognition không khả dụng trên thiết bị này", Toast.LENGTH_LONG).show();
                if (binding != null) {
                    binding.buttonSpeechToggle.setEnabled(false);
                }
                return;
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN"); // Vietnamese
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (binding != null) {
                    binding.buttonSpeechToggle.setText(getString(R.string.stop_listening));
                    binding.textSpeechResult.setText(getString(R.string.listening));
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                // Speech started
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Audio level changed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Audio buffer received
            }

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                if (binding != null) {
                    binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
                }
            }

            @Override
            public void onError(int error) {
                isListening = false;
                if (binding != null) {
                    binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
                    
                    String errorMessage;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMessage = "Lỗi audio";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMessage = "Lỗi client";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMessage = getString(R.string.permission_required);
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage = "Lỗi mạng";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMessage = "Hết thời gian chờ mạng";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage = getString(R.string.no_speech);
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMessage = "Recognizer đang bận";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorMessage = "Lỗi server";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage = "Hết thời gian chờ giọng nói";
                            break;
                        default:
                            errorMessage = getString(R.string.speech_error);
                    }
                    binding.textSpeechResult.setText(errorMessage);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    if (binding != null) {
                        binding.textSpeechResult.setText(recognizedText);
                    }
                }
                isListening = false;
                if (binding != null) {
                    binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partialText = matches.get(0);
                    if (binding != null) {
                        binding.textSpeechResult.setText(partialText);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Event occurred
            }
        });
        } catch (Exception e) {
            // Fragment might not be attached yet, will retry later
            e.printStackTrace();
        }
    }

    /**
     * Start listening for speech
     */
    private void startListening() {
        if (speechRecognizer == null || speechRecognizerIntent == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Speech Recognition chưa được khởi tạo", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Check permission
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 
                    PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }

        speechRecognizer.startListening(speechRecognizerIntent);
    }

    /**
     * Stop listening for speech
     */
    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            if (binding != null) {
                binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
            }
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.permission_required), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

