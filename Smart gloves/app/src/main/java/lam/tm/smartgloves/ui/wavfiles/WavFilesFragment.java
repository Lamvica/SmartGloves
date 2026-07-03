package lam.tm.smartgloves.ui.wavfiles;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentWavFilesBinding;
import lam.tm.smartgloves.model.WavFileItem;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;

public class WavFilesFragment extends Fragment {

    private FragmentWavFilesBinding binding;
    private WavFilesViewModel viewModel;
    private WavFilesAdapter adapter;
    private DatabaseReference wavFilesReference;
    private List<WavFileItem> wavFilesList;
    private MediaPlayer mediaPlayer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(WavFilesViewModel.class);

        binding = FragmentWavFilesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_wav_files));
        }

        wavFilesList = new ArrayList<>();
        adapter = new WavFilesAdapter(wavFilesList);

        RecyclerView recyclerView = binding.recyclerViewWavFiles;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        TextView textEmpty = binding.textEmpty;
        textEmpty.setText(R.string.no_wav_files);
        textEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Initialize Firebase Database
        wavFilesReference = FirebaseDatabaseHelper.getWavFilesReference();

        // Load audio files from Firebase
        wavFilesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wavFilesList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    WavFileItem item = dataSnapshot.getValue(WavFileItem.class);
                    if (item != null) {
                        item.id = dataSnapshot.getKey();
                        wavFilesList.add(item);
                    }
                }
                // Sort by timestamp descending
                Collections.sort(wavFilesList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                adapter.notifyDataSetChanged();

                if (wavFilesList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPlaying();
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPlaying();
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playAudioFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(getContext(), "Đường dẫn file không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(getContext(), "File không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        stopPlaying();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlaying();
            });

            Toast.makeText(getContext(), "Đang phát...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi phát file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            stopPlaying();
        }
    }

    private void deleteAudioFile(WavFileItem item) {
        if (item.id == null || wavFilesReference == null) return;

        // Delete from Firebase
        wavFilesReference.child(item.id).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Delete local file
                if (item.filePath != null) {
                    File file = new File(item.filePath);
                    if (file.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
                Toast.makeText(getContext(), "Đã xóa file audio", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showDeleteDialog(WavFileItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Xóa file audio")
            .setMessage("Bạn có chắc muốn xóa file \"" + item.fileName + "\"?")
            .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteAudioFile(item))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    // RecyclerView Adapter
    private class WavFilesAdapter extends RecyclerView.Adapter<WavFilesAdapter.ViewHolder> {
        private List<WavFileItem> items;

        public WavFilesAdapter(List<WavFileItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wav_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WavFileItem item = items.get(position);
            holder.textGestureName.setText(item.gestureName != null ? item.gestureName : "");
            holder.textSentence.setText(item.text != null ? item.text : "");
            holder.textFileName.setText(item.fileName != null ? item.fileName : "");

            // Format file size
            String fileSizeText = formatFileSize(item.fileSize);
            holder.textFileSize.setText("Kích thước: " + fileSizeText);

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String timeString = sdf.format(new Date(item.timestamp));
            holder.textTime.setText("Thời gian: " + timeString);

            // Play button
            holder.buttonPlay.setOnClickListener(v -> {
                if (item.filePath != null && !item.filePath.isEmpty()) {
                    playAudioFile(item.filePath);
                } else {
                    Toast.makeText(v.getContext(), "Không có đường dẫn file", Toast.LENGTH_SHORT).show();
                }
            });

            // Delete button
            holder.buttonDelete.setOnClickListener(v -> showDeleteDialog(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textGestureName;
            TextView textSentence;
            TextView textFileName;
            TextView textFileSize;
            TextView textTime;
            Button buttonPlay;
            Button buttonDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textGestureName = itemView.findViewById(R.id.text_gesture_name);
                textSentence = itemView.findViewById(R.id.text_sentence);
                textFileName = itemView.findViewById(R.id.text_file_name);
                textFileSize = itemView.findViewById(R.id.text_file_size);
                textTime = itemView.findViewById(R.id.text_time);
                buttonPlay = itemView.findViewById(R.id.button_play);
                buttonDelete = itemView.findViewById(R.id.button_delete);
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}

