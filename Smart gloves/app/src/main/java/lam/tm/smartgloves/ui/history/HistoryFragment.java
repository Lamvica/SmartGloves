package lam.tm.smartgloves.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentHistoryBinding;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private DatabaseReference historyReference;
    private List<HistoryItem> historyList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_history));
        }

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);

        RecyclerView recyclerView = binding.recyclerViewHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        TextView textEmpty = binding.textEmpty;
        
        // Initialize Firebase Database sử dụng helper
        historyReference = FirebaseDatabaseHelper.getHistoryReference();

        // Load history from Firebase
        historyReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    HistoryItem item = dataSnapshot.getValue(HistoryItem.class);
                    if (item != null) {
                        item.id = dataSnapshot.getKey();
                        historyList.add(item);
                    }
                }
                // Sort by timestamp descending
                historyList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                adapter.notifyDataSetChanged();
                
                if (historyList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error - hiển thị thông báo lỗi
                String errorMsg;
                if (error.getMessage() != null && error.getMessage().contains("Permission denied")) {
                    errorMsg = "Lỗi: Permission denied.\nVui lòng cấu hình Database Rules trong Firebase Console.\nXem file FIREBASE_SETUP.md";
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
        binding = null;
    }

    // Add new history item
    public void addHistoryItem(String gestureName, String sentence) {
        HistoryItem item = new HistoryItem(gestureName, sentence, System.currentTimeMillis());
        historyReference.push().setValue(item);
    }

    // History Item class
    public static class HistoryItem {
        public String id;
        public String gestureName;
        public String sentence;
        public long timestamp;

        public HistoryItem() {
            // Default constructor required for Firebase
        }

        public HistoryItem(String gestureName, String sentence, long timestamp) {
            this.gestureName = gestureName;
            this.sentence = sentence;
            this.timestamp = timestamp;
        }
    }

    // RecyclerView Adapter
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items;

        public HistoryAdapter(List<HistoryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            holder.textGestureName.setText(item.gestureName);
            holder.textSentence.setText(item.sentence);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String timeString = sdf.format(new Date(item.timestamp));
            holder.textTime.setText(timeString);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textGestureName;
            TextView textSentence;
            TextView textTime;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textGestureName = itemView.findViewById(R.id.text_gesture_name);
                textSentence = itemView.findViewById(R.id.text_sentence);
                textTime = itemView.findViewById(R.id.text_time);
            }
        }
    }
}

