package lam.tm.smartgloves.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Authentication functionality removed
        // Hide all login/register UI elements
        if (binding.editEmail != null) {
            binding.editEmail.setVisibility(View.GONE);
        }
        if (binding.editPassword != null) {
            binding.editPassword.setVisibility(View.GONE);
        }
        if (binding.buttonLogin != null) {
            binding.buttonLogin.setVisibility(View.GONE);
        }
        if (binding.buttonSignup != null) {
            binding.buttonSignup.setVisibility(View.GONE);
        }
        if (binding.buttonLogout != null) {
            binding.buttonLogout.setVisibility(View.GONE);
        }
        if (binding.textUserInfo != null) {
            binding.textUserInfo.setText("Chức năng tài khoản đã được gỡ bỏ");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
