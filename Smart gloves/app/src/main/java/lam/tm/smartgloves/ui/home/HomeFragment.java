package lam.tm.smartgloves.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentHomeBinding;
import lam.tm.smartgloves.utils.LanguageHelper;
import lam.tm.smartgloves.utils.ThemeHelper;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private GridMenuAdapter menuAdapter;
    private List<MenuGridItem> menuItems;
    private boolean isSettingsPanelVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize menu items
        menuItems = createMenuItems();
        menuAdapter = new GridMenuAdapter(menuItems, this::onMenuItemClick);

        // Setup RecyclerView with LinearLayoutManager (vertical list)
        RecyclerView recyclerView = binding.recyclerViewMenuGrid;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(menuAdapter);

        // Hide profile panel elements since authentication is removed
        if (binding.profilePanelScrim != null) {
            binding.profilePanelScrim.setVisibility(View.GONE);
        }
        if (binding.profileInfoPanel != null) {
            binding.profileInfoPanel.setVisibility(View.GONE);
        }

        // Setup settings menu button
        setupSettingsMenu();

        return root;
    }

    private void setupSettingsMenu() {
        if (binding.buttonSettings == null) {
            return;
        }

        // Cập nhật icon cho theme button
        updateThemeIcon();

        // Xử lý click để hiện/ẩn settings panel
        // Xử lý click để hiển/ ẩn settings panel
        binding.buttonSettings.setOnClickListener(v -> {
            if (isSettingsPanelVisible) {
                hideSettingsPanel();
            } else {
                showSettingsPanel();
            }
        });

        // Xử lý click scrim để ẩn panel
        if (binding.settingsPanelScrim != null) {
            binding.settingsPanelScrim.setOnClickListener(v -> hideSettingsPanel());
        }

        // Xử lý click theme toggle button
        if (binding.buttonThemeToggle != null) {
            binding.buttonThemeToggle.setOnClickListener(v -> {
                ThemeHelper.toggleTheme(requireContext());
                requireActivity().recreate();
            });
        }

        // Xử lý click language toggle button
        if (binding.buttonLanguageToggle != null) {
            binding.buttonLanguageToggle.setOnClickListener(v -> {
                LanguageHelper.toggleLanguage(requireContext());
                requireActivity().recreate();
            });
        }
    }

    private void showSettingsPanel() {
        if (binding == null || binding.settingsPanel == null) {
            return;
        }

        if (isSettingsPanelVisible) {
            return;
        }

        // Hiện scrim
        if (binding.settingsPanelScrim != null) {
            binding.settingsPanelScrim.setVisibility(View.VISIBLE);
            binding.settingsPanelScrim.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setListener(null)
                    .start();
        }

        // Hiện panel với animation slide
        binding.settingsPanel.setVisibility(View.VISIBLE);
        binding.settingsPanel.setAlpha(0f);
        binding.settingsPanel.setTranslationY(-20f);
        binding.settingsPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setListener(null)
                .start();

        isSettingsPanelVisible = true;
    }

    private void hideSettingsPanel() {
        if (binding == null || binding.settingsPanel == null || !isSettingsPanelVisible) {
            return;
        }

        // Ẩn scrim
        if (binding.settingsPanelScrim != null) {
            binding.settingsPanelScrim.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (binding != null && binding.settingsPanelScrim != null) {
                                binding.settingsPanelScrim.setVisibility(View.GONE);
                            }
                        }
                    })
                    .start();
        }

        // Ẩn panel với animation
        binding.settingsPanel.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (binding != null && binding.settingsPanel != null) {
                            binding.settingsPanel.setVisibility(View.GONE);
                        }
                        isSettingsPanelVisible = false;
                    }
                })
                .start();
    }

    private void updateThemeIcon() {
        if (binding == null || binding.buttonThemeToggle == null) {
            return;
        }
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        if (isDark) {
            // Đang ở dark mode, hiển thị icon light mode (để chuyển sang light)
            binding.buttonThemeToggle.setIconResource(R.drawable.ic_light_mode);
        } else {
            // Đang ở light mode, hiển thị icon dark mode (để chuyển sang dark)
            binding.buttonThemeToggle.setIconResource(R.drawable.ic_dark_mode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật icon khi fragment resume
        if (binding != null) {
            updateThemeIcon();
        }
    }


    private List<MenuGridItem> createMenuItems() {
        List<MenuGridItem> items = new ArrayList<>();

        // 4 ô: Thêm cử chỉ, Danh sách cử chỉ, File WAV, Cài đặt
        items.add(new MenuGridItem(
                R.id.navigation_add_gesture,
                R.drawable.ic_add_gesture,
                getString(R.string.title_add_gesture)
        ));

        items.add(new MenuGridItem(
                R.id.navigation_list_gestures,
                R.drawable.ic_list_gestures,
                getString(R.string.title_list_gestures)
        ));

        items.add(new MenuGridItem(
                R.id.navigation_wav_files,
                R.drawable.ic_wav_files,
                getString(R.string.title_wav_files)
        ));

        items.add(new MenuGridItem(
                R.id.navigation_settings,
                R.drawable.ic_settings,
                getString(R.string.title_settings)
        ));

        return items;
    }

    private void onMenuItemClick(int destinationId) {
        // Navigate directly without login check
        navigateSafe(destinationId);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isSettingsPanelVisible = false;
        binding = null;
    }

    private void navigateSafe(int destinationId) {
        try {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(destinationId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Fallback if fragment is not attached to nav host
            View view = getView();
            if (view != null) {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(destinationId);
            }
        }
    }

    // Menu Grid Item class (renamed to avoid conflict with android.view.MenuItem)
    static class MenuGridItem {
        int destinationId;
        int iconResId;
        String title;

        MenuGridItem(int destinationId, int iconResId, String title) {
            this.destinationId = destinationId;
            this.iconResId = iconResId;
            this.title = title;
        }
    }

    // Grid Menu Adapter
    private static class GridMenuAdapter extends RecyclerView.Adapter<GridMenuAdapter.ViewHolder> {
        private List<MenuGridItem> items;
        private OnMenuItemClickListener listener;

        interface OnMenuItemClickListener {
            void onItemClick(int destinationId);
        }

        GridMenuAdapter(List<MenuGridItem> items, OnMenuItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grid_menu, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuGridItem item = items.get(position);
            holder.textTitle.setText(item.title);

            if (holder.icon != null) {
                holder.icon.setImageResource(item.iconResId);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item.destinationId);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle;
            android.widget.ImageView icon;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.text_menu_title);
                icon = itemView.findViewById(R.id.image_menu_icon);
            }
        }
    }
}
