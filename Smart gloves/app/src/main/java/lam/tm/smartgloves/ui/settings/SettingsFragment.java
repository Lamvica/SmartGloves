package lam.tm.smartgloves.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentSettingsBinding;
import lam.tm.smartgloves.utils.LanguageHelper;
import lam.tm.smartgloves.utils.ThemeHelper;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        setupToolbarBack();
        setupActions();

        return binding.getRoot();
    }

    private void setupToolbarBack() {
        if (binding.toolbarBack == null) return;
        binding.toolbarBack.buttonBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
        binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_settings));
    }

    private void setupActions() {
        binding.buttonThemeToggle.setOnClickListener(v -> {
            ThemeHelper.toggleTheme(requireContext());
            requireActivity().recreate();
        });

        binding.buttonLanguageToggle.setOnClickListener(v -> {
            LanguageHelper.toggleLanguage(requireContext());
            requireActivity().recreate();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


