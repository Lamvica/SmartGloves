package lam.tm.smartgloves.ui.listgestures;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentListGesturesBinding;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;
import lam.tm.smartgloves.utils.TTSService;
import lam.tm.smartgloves.model.WavFileItem;

public class ListGesturesFragment extends Fragment {

    private FragmentListGesturesBinding binding;
    private ListGesturesViewModel viewModel;
    private GestureAdapter adapter;
    private DatabaseReference gesturesReference;
    private DatabaseReference wavFilesReference;
    private List<GestureItem> gestureList;
    private TextToSpeech textToSpeech;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ListGesturesViewModel.class);

        binding = FragmentListGesturesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_list_gestures));
        }

        gestureList = new ArrayList<>();
        adapter = new GestureAdapter(gestureList);

        RecyclerView recyclerView = binding.recyclerViewGestures;
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }

        TextView textEmpty = binding.textEmpty;
        if (textEmpty != null) {
            textEmpty.setText(R.string.no_gestures_message);
            textEmpty.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        // Initialize Firebase Database
        gesturesReference = FirebaseDatabaseHelper.getGesturesReference();
        wavFilesReference = FirebaseDatabaseHelper.getWavFilesReference();

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default language
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        // Load gestures from Firebase
        gesturesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gestureList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    GestureItem item = dataSnapshot.getValue(GestureItem.class);
                    if (item != null) {
                        item.id = dataSnapshot.getKey();
                        gestureList.add(item);
                    }
                }
                // Sort by timestamp descending
                Collections.sort(gestureList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                updateListVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String errorMsg;
                if (error.getMessage() != null && error.getMessage().contains("Permission denied")) {
                    errorMsg = "Lỗi: Permission denied.\nVui lòng cấu hình Database Rules trong Firebase Console.";
                } else {
                    errorMsg = "Lỗi đọc dữ liệu: " + (error.getMessage() != null ? error.getMessage() : "Không thể kết nối Firebase");
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                error.toException().printStackTrace();
            }
        });

        return root;
    }

    private void updateListVisibility() {
        if (binding == null) return;

        TextView textEmpty = binding.textEmpty;
        RecyclerView recyclerView = binding.recyclerViewGestures;
        if (textEmpty != null && recyclerView != null) {
            if (gestureList.isEmpty()) {
                textEmpty.setText(R.string.no_gestures_message);
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        binding = null;
    }

    // Gesture Item class
    public static class GestureItem {
        public String id;
        public String name;
        public String sensorValues;
        public String text;
        public String category;
        public long timestamp;

        public GestureItem() {
            // Default constructor required for Firebase
        }
    }

    // RecyclerView Adapter
    private class GestureAdapter extends RecyclerView.Adapter<GestureAdapter.ViewHolder> {
        private List<GestureItem> items;

        public GestureAdapter(List<GestureItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gesture_home, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GestureItem item = items.get(position);
            holder.textGestureName.setText(item.name != null ? item.name : "");
            holder.textGestureSentence.setText(item.text != null ? item.text : "");
            
            // Display category
            if (item.category != null && !item.category.trim().isEmpty()) {
                holder.textGestureCategory.setText(item.category);
                holder.textGestureCategory.setVisibility(View.VISIBLE);
            } else {
                holder.textGestureCategory.setVisibility(View.GONE);
            }

            // Speak button
            holder.buttonSpeak.setOnClickListener(v -> {
                if (item.text != null && !item.text.isEmpty()) {
                    speakText(item.text);
                } else {
                    Toast.makeText(v.getContext(), getString(R.string.no_text_to_speak), Toast.LENGTH_SHORT).show();
                }
            });

            // Edit button
            holder.buttonEdit.setOnClickListener(v -> showEditDialog(item));

            // Delete button
            holder.buttonDelete.setOnClickListener(v -> showDeleteDialog(item));

            // Convert to audio button (TTS via server)
            holder.buttonConvertWav.setOnClickListener(v -> {
                if (item.text != null && !item.text.isEmpty()) {
                    convertToAudioViaServer(item);
                } else {
                    Toast.makeText(v.getContext(), getString(R.string.no_text_to_speak), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textGestureName;
            TextView textGestureSentence;
            TextView textGestureCategory;
            ImageButton buttonSpeak;
            View buttonConvertWav; // accept both Button or ImageButton
            Button buttonEdit;
            Button buttonDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textGestureName = itemView.findViewById(R.id.text_gesture_name);
                textGestureSentence = itemView.findViewById(R.id.text_gesture_sentence);
                textGestureCategory = itemView.findViewById(R.id.text_gesture_category);
                buttonSpeak = itemView.findViewById(R.id.button_speak_item);
                buttonConvertWav = itemView.findViewById(R.id.button_convert_wav);
                buttonEdit = itemView.findViewById(R.id.button_edit_item);
                buttonDelete = itemView.findViewById(R.id.button_delete_item);
            }
        }
    }

    private void speakText(String text) {
        if (textToSpeech != null && text != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showEditDialog(GestureItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_gesture, null, false);

        Spinner spinnerFinger = dialogView.findViewById(R.id.spinner_dialog_finger);
        Spinner spinnerC = dialogView.findViewById(R.id.spinner_dialog_c_selection);
        com.google.android.material.textfield.TextInputEditText editText = dialogView.findViewById(R.id.edit_dialog_text);

        // Prefill values
        if (editText != null) {
            editText.setText(item.text != null ? item.text : "");
            editText.setSelection(editText.getText() != null ? editText.getText().length() : 0);
        }
        if (spinnerFinger != null) {
            spinnerFinger.setSelection(findSpinnerIndex(spinnerFinger, item.name));
        }
        if (spinnerC != null) {
            spinnerC.setSelection(findSpinnerIndex(spinnerC, item.sensorValues));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.edit_gesture))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String selectedFinger = spinnerFinger != null
                            ? spinnerFinger.getSelectedItem().toString()
                            : "";
                    String newText = editText != null && editText.getText() != null
                            ? editText.getText().toString().trim()
                            : "";
                    String cValue = spinnerC != null
                            ? spinnerC.getSelectedItem().toString()
                            : "";

                    if (!selectedFinger.isEmpty() && !newText.isEmpty() && !cValue.isEmpty()) {
                        updateGesture(item.id, selectedFinger, newText, cValue);
                    } else {
                        Toast.makeText(getContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteDialog(GestureItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_gesture))
                .setMessage(getString(R.string.confirm_delete_gesture, item.name))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteGesture(item.id))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void updateGesture(String gestureId, String newName, String newText, String sensorValue) {
        if (gestureId == null || gesturesReference == null) return;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", newName);
        updates.put("text", newText);
        updates.put("sensorValues", sensorValue);

        gesturesReference.child(gestureId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã cập nhật cử chỉ", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteGesture(String gestureId) {
        if (gestureId == null || gesturesReference == null) return;

        gesturesReference.child(gestureId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), getString(R.string.gesture_deleted), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.delete_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Chuyển đổi text sang audio (MP3) qua server TTS
     */
    private void convertToAudioViaServer(GestureItem item) {
        if (item.text == null || item.text.trim().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.no_text_to_speak), Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading message
        Toast.makeText(getContext(), "Đang chuyển đổi text sang audio qua server...", Toast.LENGTH_SHORT).show();

        // Get finger name (ngón tay) - từ item.name
        String fingerName = item.name != null && !item.name.trim().isEmpty() 
            ? item.name 
            : "ngoncai"; // default
        
        // Get c value (c1 hoặc c2) - từ item.sensorValues
        String cValue = item.sensorValues != null && !item.sensorValues.trim().isEmpty()
            ? item.sensorValues.trim()
            : "c1"; // default

        // Call TTS service (server) với thông tin đầy đủ
        TTSService.convertTextToAudio(requireContext(), item.text, fingerName, cValue, new TTSService.TTSCallback() {
            @Override
            public void onSuccess(String filePath, String fileName) {
                // Get file size
                java.io.File file = new java.io.File(filePath);
                long fileSize = file.exists() ? file.length() : 0;

                // Create WavFileItem (không có storageUrl vì chỉ lưu local) - giờ là file MP3
                WavFileItem wavFileItem = new WavFileItem(
                    item.name != null ? item.name : "",
                    item.text,
                    fileName,
                    filePath,
                    null, // storageUrl = null vì không upload lên Storage
                    System.currentTimeMillis(),
                    fileSize
                );

                // Save metadata to Firebase Realtime Database
                if (wavFilesReference != null) {
                        wavFilesReference.push().setValue(wavFileItem)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Đã chuyển đổi thành MP3: " + fileName, Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Đã tạo file MP3: " + fileName + "\nLỗi lưu metadata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                } else {
                    Toast.makeText(getContext(), "Đã tạo file MP3: " + fileName, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                String errorMsg = "Lỗi chuyển đổi TTS (audio) qua server: " + error;
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int findSpinnerIndex(Spinner spinner, String value) {
        if (spinner == null || value == null) return 0;
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && value.equalsIgnoreCase(item.toString())) {
                return i;
            }
        }
        return 0;
    }
}

