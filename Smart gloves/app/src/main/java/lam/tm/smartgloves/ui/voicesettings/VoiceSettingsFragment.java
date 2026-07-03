package lam.tm.smartgloves.ui.voicesettings;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentVoiceSettingsBinding;
import lam.tm.smartgloves.model.SpeechHistoryItem;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;

public class VoiceSettingsFragment extends Fragment {

    private FragmentVoiceSettingsBinding binding;
    private VoiceSettingsViewModel viewModel;
    private SharedPreferences sharedPreferences;
    
    // Speech Recognition
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    
    // Voice animation
    private View[] voiceWaves;
    private LinearLayout voiceIndicatorContainer;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private boolean isAnimating = false;
    
    // Silence detection
    private Handler silenceHandler;
    private Runnable silenceRunnable;
    private long lastSoundTime = 0;
    private static final long SILENCE_TIMEOUT = 5000; // 5 seconds
    
    // Speech History
    private DatabaseReference speechHistoryReference;
    private static final int MAX_HISTORY_ITEMS = 10;
    
    private SpeechHistoryAdapter speechHistoryAdapter;
    private List<SpeechHistoryItem> speechHistoryList;

    private static final String PREFS_NAME = "VoiceSettings";
    private static final String KEY_VOICE_SPEED = "voice_speed";
    private static final String KEY_VOICE_VOLUME = "voice_volume";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(VoiceSettingsViewModel.class);

        binding = FragmentVoiceSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, 0);

        // Load saved settings
        loadSettings();

        // Speed SeekBar
        SeekBar seekBarSpeed = binding.seekBarSpeed;
        TextView textSpeedValue = binding.textSpeedValue;
        
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress / 100f); // Range: 0.5 to 1.5
                viewModel.setVoiceSpeed(speed);
                textSpeedValue.setText(String.format("%.1fx", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume SeekBar
        SeekBar seekBarVolume = binding.seekBarVolume;
        TextView textVolumeValue = binding.textVolumeValue;
        
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f; // Range: 0.0 to 1.0
                viewModel.setVoiceVolume(volume);
                textVolumeValue.setText(String.format("%d%%", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save button
        Button buttonSave = binding.buttonSave;
        buttonSave.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(getContext(), "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
        });

        // Initialize voice animation views
        voiceIndicatorContainer = binding.voiceIndicatorContainer;
        voiceWaves = new View[]{
            binding.voiceWave1,
            binding.voiceWave2,
            binding.voiceWave3,
            binding.voiceWave4,
            binding.voiceWave5
        };
        animationHandler = new Handler(Looper.getMainLooper());
        silenceHandler = new Handler(Looper.getMainLooper());

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_voice_settings));
        }

        // Initialize Firebase Database for speech history
        speechHistoryReference = FirebaseDatabaseHelper.getSpeechHistoryReference();
        
        // Initialize Speech History RecyclerView
        initializeSpeechHistory();

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

    private void loadSettings() {
        float speed = sharedPreferences.getFloat(KEY_VOICE_SPEED, 1.0f);
        float volume = sharedPreferences.getFloat(KEY_VOICE_VOLUME, 1.0f);

        // Set speed
        int speedProgress = (int) ((speed - 0.5f) * 100);
        binding.seekBarSpeed.setProgress(speedProgress);
        binding.textSpeedValue.setText(String.format("%.1fx", speed));
        viewModel.setVoiceSpeed(speed);

        // Set volume
        int volumeProgress = (int) (volume * 100);
        binding.seekBarVolume.setProgress(volumeProgress);
        binding.textVolumeValue.setText(String.format("%d%%", volumeProgress));
        viewModel.setVoiceVolume(volume);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_VOICE_SPEED, viewModel.getVoiceSpeed());
        editor.putFloat(KEY_VOICE_VOLUME, viewModel.getVoiceVolume());
        editor.apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop timers and animation
        stopSilenceTimer();
        stopVoiceAnimation();
        // Clean up speech recognizer
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        binding = null;
    }
    
    /**
     * Start voice animation (only when sound is detected)
     */
    private void startVoiceAnimation() {
        if (voiceIndicatorContainer == null || voiceWaves == null || isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        // Create continuous animation that only runs when sound is detected
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating || voiceWaves == null || !isListening) {
                    return;
                }
                
                // Animate each wave with different delays
                for (int i = 0; i < voiceWaves.length; i++) {
                    animateWave(voiceWaves[i], i * 100);
                }
                
                // Repeat animation only if still animating
                if (animationHandler != null && isAnimating && isListening) {
                    animationHandler.postDelayed(this, 600);
                }
            }
        };
        
        animationHandler.post(animationRunnable);
    }
    
    /**
     * Animate a single wave
     */
    private void animateWave(View wave, long delay) {
        if (wave == null || !isAnimating) {
            return;
        }
        
        wave.postDelayed(() -> {
            if (!isAnimating || wave == null) {
                return;
            }
            
            // Scale animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(wave, "scaleX", 1.0f, 1.5f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(wave, "scaleY", 1.0f, 2.0f, 1.0f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(wave, "alpha", 0.3f, 1.0f, 0.3f);
            
            scaleX.setDuration(400);
            scaleY.setDuration(400);
            alpha.setDuration(400);
            
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            alpha.setInterpolator(new AccelerateDecelerateInterpolator());
            
            scaleX.start();
            scaleY.start();
            alpha.start();
        }, delay);
    }
    
    /**
     * Update animation based on audio level
     */
    private void updateVoiceAnimation(float rmsdB) {
        if (voiceWaves == null || !isAnimating) {
            return;
        }
        
        // Normalize RMS value (typically ranges from -10 to 10)
        float normalized = Math.max(0, Math.min(1, (rmsdB + 10) / 20));
        
        // Update wave heights based on audio level
        for (int i = 0; i < voiceWaves.length; i++) {
            if (voiceWaves[i] != null) {
                float baseHeight = 40f;
                float maxHeight = 80f;
                float height = baseHeight + (maxHeight - baseHeight) * normalized;
                
                // Add some variation for visual effect based on sound
                float variation = (float) (Math.sin(System.currentTimeMillis() / 150.0 + i) * 0.2 + 0.8);
                height *= variation;
                
                ViewGroup.LayoutParams params = voiceWaves[i].getLayoutParams();
                params.height = (int) height;
                voiceWaves[i].setLayoutParams(params);
            }
        }
    }
    
    /**
     * Start silence detection timer
     */
    private void startSilenceTimer() {
        resetSilenceTimer();
    }
    
    /**
     * Reset silence timer (called when sound is detected)
     */
    private void resetSilenceTimer() {
        if (silenceHandler == null || !isListening) {
            return;
        }
        
        // Remove existing timer
        if (silenceRunnable != null) {
            silenceHandler.removeCallbacks(silenceRunnable);
        }
        
        // Create new timer that checks continuously
        silenceRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isListening || binding == null) {
                    return;
                }
                
                // Check if enough time has passed since last sound
                long timeSinceLastSound = System.currentTimeMillis() - lastSoundTime;
                if (timeSinceLastSound >= SILENCE_TIMEOUT) {
                    // No sound detected for 5 seconds
                    stopListening();
                    if (binding != null) {
                        binding.textSpeechResult.setText("Không nghe được gì");
                    }
                } else {
                    // Continue checking every second
                    if (silenceHandler != null && isListening) {
                        silenceHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        
        // Start checking immediately
        silenceHandler.post(silenceRunnable);
    }
    
    /**
     * Stop voice animation
     */
    private void stopVoiceAnimation() {
        isAnimating = false;
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        if (voiceIndicatorContainer != null) {
            voiceIndicatorContainer.setVisibility(View.GONE);
        }
        // Reset wave heights
        if (voiceWaves != null) {
            for (View wave : voiceWaves) {
                if (wave != null) {
                    ViewGroup.LayoutParams params = wave.getLayoutParams();
                    params.height = 40;
                    wave.setLayoutParams(params);
                    wave.setScaleX(1.0f);
                    wave.setScaleY(1.0f);
                    wave.setAlpha(0.3f);
                }
            }
        }
    }
    
    /**
     * Stop silence timer
     */
    private void stopSilenceTimer() {
        if (silenceHandler != null && silenceRunnable != null) {
            silenceHandler.removeCallbacks(silenceRunnable);
        }
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
                    lastSoundTime = System.currentTimeMillis();
                    if (binding != null) {
                        binding.buttonSpeechToggle.setText(getString(R.string.stop_listening));
                        binding.textSpeechResult.setText(getString(R.string.listening));
                        // Show indicator but don't animate until sound is detected
                        if (voiceIndicatorContainer != null) {
                            voiceIndicatorContainer.setVisibility(View.VISIBLE);
                        }
                        startSilenceTimer();
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    // Speech started
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Audio level changed - update animation intensity
                    if (isListening && binding != null) {
                        // Update last sound time
                        lastSoundTime = System.currentTimeMillis();
                        
                        // Only animate if there's significant sound (above threshold)
                        if (rmsdB > -5) { // Threshold for detecting actual sound
                            if (!isAnimating) {
                                startVoiceAnimation();
                            }
                            updateVoiceAnimation(rmsdB);
                        } else {
                            // Very quiet, stop animation but keep indicator visible
                            if (isAnimating) {
                                stopVoiceAnimation();
                            }
                        }
                        
                        // Reset silence timer
                        resetSilenceTimer();
                    }
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
                        stopVoiceAnimation();
                        stopSilenceTimer();
                    }
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    if (binding != null) {
                        binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
                        stopVoiceAnimation();
                        stopSilenceTimer();
                        
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
                        // Lưu lịch sử vào Firebase
                        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                            saveSpeechHistory(recognizedText);
                        }
                    }
                    isListening = false;
                    if (binding != null) {
                        binding.buttonSpeechToggle.setText(getString(R.string.start_listening));
                        stopVoiceAnimation();
                        stopSilenceTimer();
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
                stopVoiceAnimation();
                stopSilenceTimer();
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
    
    /**
     * Khởi tạo RecyclerView cho lịch sử giọng nói
     */
    private void initializeSpeechHistory() {
        speechHistoryList = new ArrayList<>();
        speechHistoryAdapter = new SpeechHistoryAdapter(speechHistoryList);
        
        RecyclerView recyclerView = binding.recyclerViewSpeechHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(speechHistoryAdapter);
        
        TextView textEmpty = binding.textEmptySpeechHistory;
        
        // Load lịch sử từ Firebase
        if (speechHistoryReference != null) {
            speechHistoryReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    speechHistoryList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        SpeechHistoryItem item = dataSnapshot.getValue(SpeechHistoryItem.class);
                        if (item != null) {
                            item.id = dataSnapshot.getKey();
                            speechHistoryList.add(item);
                        }
                    }
                    // Sắp xếp theo timestamp (mới nhất trước)
                    Collections.sort(speechHistoryList, new Comparator<SpeechHistoryItem>() {
                        @Override
                        public int compare(SpeechHistoryItem a, SpeechHistoryItem b) {
                            return Long.compare(b.timestamp, a.timestamp);
                        }
                    });
                    speechHistoryAdapter.notifyDataSetChanged();
                    
                    // Hiển thị/ẩn empty message
                    if (speechHistoryList.isEmpty()) {
                        textEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        textEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    error.toException().printStackTrace();
                }
            });
        }
    }
    
    /**
     * Lưu lịch sử giọng nói thành chữ vào Firebase
     */
    private void saveSpeechHistory(String text) {
        if (speechHistoryReference == null || text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Tạo item mới
        SpeechHistoryItem item = new SpeechHistoryItem(text.trim(), System.currentTimeMillis());
        
        // Lưu vào Firebase
        speechHistoryReference.push().setValue(item, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    // Lỗi khi lưu (có thể do quyền truy cập hoặc mạng)
                    error.toException().printStackTrace();
                } else {
                    // Lưu thành công, kiểm tra và giới hạn số lượng items
                    limitHistoryItems();
                }
            }
        });
    }
    
    /**
     * Giới hạn số lượng lịch sử chỉ còn MAX_HISTORY_ITEMS (10) items gần nhất
     * Xóa các đoạn cũ nhất khi có hơn 10 đoạn
     */
    private void limitHistoryItems() {
        if (speechHistoryReference == null) {
            return;
        }
        
        // Lấy tất cả items từ Firebase
        speechHistoryReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SpeechHistoryItem> items = new ArrayList<>();
                
                // Lấy tất cả items
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    SpeechHistoryItem item = dataSnapshot.getValue(SpeechHistoryItem.class);
                    if (item != null) {
                        item.id = dataSnapshot.getKey();
                        items.add(item);
                    }
                }
                
                // Nếu có nhiều hơn MAX_HISTORY_ITEMS, xóa các items cũ nhất
                if (items.size() > MAX_HISTORY_ITEMS) {
                    // Sắp xếp theo timestamp (cũ nhất trước để xóa)
                    Collections.sort(items, new Comparator<SpeechHistoryItem>() {
                        @Override
                        public int compare(SpeechHistoryItem a, SpeechHistoryItem b) {
                            return Long.compare(a.timestamp, b.timestamp);
                        }
                    });
                    
                    // Xóa các items cũ nhất (giữ lại MAX_HISTORY_ITEMS items mới nhất)
                    // Xóa từ đầu danh sách (cũ nhất) cho đến khi còn lại MAX_HISTORY_ITEMS
                    int itemsToDelete = items.size() - MAX_HISTORY_ITEMS;
                    for (int i = 0; i < itemsToDelete; i++) {
                        SpeechHistoryItem oldItem = items.get(i);
                        if (oldItem.id != null) {
                            speechHistoryReference.child(oldItem.id).removeValue();
                        }
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Lỗi khi đọc dữ liệu
                error.toException().printStackTrace();
            }
        });
    }
    
    /**
     * Adapter cho RecyclerView hiển thị lịch sử giọng nói
     */
    private class SpeechHistoryAdapter extends RecyclerView.Adapter<SpeechHistoryAdapter.ViewHolder> {
        private List<SpeechHistoryItem> items;
        
        public SpeechHistoryAdapter(List<SpeechHistoryItem> items) {
            this.items = items;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_speech_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SpeechHistoryItem item = items.get(position);
            holder.textSpeechText.setText(item.text);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String timeString = sdf.format(new Date(item.timestamp));
            holder.textSpeechTime.setText(timeString);
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textSpeechText;
            TextView textSpeechTime;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textSpeechText = itemView.findViewById(R.id.text_speech_text);
                textSpeechTime = itemView.findViewById(R.id.text_speech_time);
            }
        }
    }
}




