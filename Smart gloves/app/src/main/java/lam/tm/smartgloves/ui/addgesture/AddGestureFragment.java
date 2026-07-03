package lam.tm.smartgloves.ui.addgesture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.database.DatabaseReference;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentAddGestureBinding;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;

public class AddGestureFragment extends Fragment {

    private FragmentAddGestureBinding binding;
    private AddGestureViewModel viewModel;
    private DatabaseReference databaseReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AddGestureViewModel.class);

        binding = FragmentAddGestureBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_add_gesture));
        }

        // Initialize Firebase Database sử dụng helper
        databaseReference = FirebaseDatabaseHelper.getGesturesReference();

        final Spinner spinnerFingers = binding.spinnerFingers;
        final Spinner spinnerCSelection = binding.spinnerCSelection;
        final EditText editGestureText = binding.editGestureText;
        final Button buttonAdd = binding.buttonAdd;

        buttonAdd.setOnClickListener(v -> {
            // Get selected finger from spinner
            String selectedFinger = spinnerFingers.getSelectedItem() != null ? spinnerFingers.getSelectedItem().toString() : "";
            String selectedC = spinnerCSelection.getSelectedItem() != null ? spinnerCSelection.getSelectedItem().toString() : "";
            String text = editGestureText.getText().toString().trim();

            if (selectedFinger.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn ngón tay", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedC.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn c1 hoặc c2", Toast.LENGTH_SHORT).show();
                return;
            }

            if (text.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập câu nói", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use selected C as sensorValues
            String sensorValues = selectedC;
            
            // Use default category
            String category = "default";
            String categoryItemId = "id" + System.currentTimeMillis();

                    // Create gesture object
            Gesture gesture = new Gesture(selectedFinger, sensorValues, text, category, categoryItemId, System.currentTimeMillis());

                    // Save to Firebase
                    String key = databaseReference.push().getKey();
                    if (key != null) {
                        databaseReference.child(key).setValue(gesture)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Đã thêm cử chỉ thành công", Toast.LENGTH_SHORT).show();
                                    // Clear fields
                            if (spinnerFingers.getAdapter() != null && spinnerFingers.getAdapter().getCount() > 0) {
                                spinnerFingers.setSelection(0);
                            }
                            if (spinnerCSelection.getAdapter() != null && spinnerCSelection.getAdapter().getCount() > 0) {
                                spinnerCSelection.setSelection(0);
                            }
                                    editGestureText.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    String errorMsg;
                                    if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                                        errorMsg = "Lỗi: Permission denied.\nVui lòng cấu hình Database Rules trong Firebase Console.\nXem file FIREBASE_SETUP.md";
                                    } else {
                                        errorMsg = "Lỗi: " + (e.getMessage() != null ? e.getMessage() : "Không thể kết nối Firebase");
                                    }
                                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                });
                    }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Gesture data class
    public static class Gesture {
        public String name;
        public String sensorValues;
        public String text;
        public String category;
        public String categoryItemId;
        public long timestamp;

        public Gesture() {
            // Default constructor required for Firebase
        }

        public Gesture(String name, String sensorValues, String text, String category, String categoryItemId, long timestamp) {
            this.name = name;
            this.sensorValues = sensorValues;
            this.text = text;
            this.category = category;
            this.categoryItemId = categoryItemId;
            this.timestamp = timestamp;
        }
    }

}

