package lam.tm.smartgloves.ui.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.Set;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.databinding.FragmentConnectionSettingsBinding;

public class ConnectionSettingsFragment extends Fragment {

    private FragmentConnectionSettingsBinding binding;
    private ConnectionSettingsViewModel viewModel;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ConnectionSettingsViewModel.class);

        binding = FragmentConnectionSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup back button
        if (binding.toolbarBack != null) {
            binding.toolbarBack.buttonBack.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigateUp();
            });
            binding.toolbarBack.textToolbarTitle.setText(getString(R.string.title_connection));
        }

        // Initialize Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        final RadioGroup radioGroupConnectionType = binding.radioGroupConnectionType;
        final Button buttonConnect = binding.buttonConnect;
        final TextView textConnectionStatus = binding.textConnectionStatus;

        // Request permissions
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }

        // Connection type selection
        radioGroupConnectionType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_bluetooth) {
                viewModel.setConnectionType(ConnectionType.BLUETOOTH);
                textConnectionStatus.setText("Chọn Bluetooth");
            } else if (checkedId == R.id.radio_wifi) {
                viewModel.setConnectionType(ConnectionType.WIFI);
                textConnectionStatus.setText("Chọn WiFi");
            }
        });

        // Connect button
        buttonConnect.setOnClickListener(v -> {
            ConnectionType type = viewModel.getConnectionType();
            if (type == ConnectionType.BLUETOOTH) {
                connectBluetooth();
            } else if (type == ConnectionType.WIFI) {
                connectWiFi();
            }
        });

        // Observe connection status
        viewModel.getIsConnected().observe(getViewLifecycleOwner(), connected -> {
            if (connected) {
                textConnectionStatus.setText(getString(R.string.connected));
                textConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                buttonConnect.setText("Ngắt kết nối");
            } else {
                textConnectionStatus.setText(getString(R.string.disconnected));
                textConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                buttonConnect.setText(getString(R.string.connect_device));
            }
        });

        // Check Bluetooth status
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                textConnectionStatus.setText("Bluetooth đã bật");
            } else {
                textConnectionStatus.setText("Vui lòng bật Bluetooth");
            }
        } else {
            textConnectionStatus.setText("Thiết bị không hỗ trợ Bluetooth");
        }
        return root;
    }

    private void connectBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Vui lòng bật Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get paired devices
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // For now, just simulate connection
            // In real app, you would show a dialog to select device
            Toast.makeText(getContext(), "Đang kết nối...", Toast.LENGTH_SHORT).show();
            viewModel.setIsConnected(true);
        } else {
            Toast.makeText(getContext(), "Không có thiết bị đã ghép nối", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectWiFi() {
        Toast.makeText(getContext(), "Tính năng WiFi sẽ được triển khai", Toast.LENGTH_SHORT).show();
        // WiFi connection logic would go here
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Đã cấp quyền Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Cần quyền Bluetooth để kết nối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public enum ConnectionType {
        BLUETOOTH, WIFI
    }
}




