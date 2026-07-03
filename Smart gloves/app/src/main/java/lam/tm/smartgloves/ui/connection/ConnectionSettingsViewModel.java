package lam.tm.smartgloves.ui.connection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lam.tm.smartgloves.ui.connection.ConnectionSettingsFragment.ConnectionType;

public class ConnectionSettingsViewModel extends ViewModel {

    private ConnectionType connectionType = ConnectionType.BLUETOOTH;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    public ConnectionSettingsViewModel() {
        isConnected.setValue(false);
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType type) {
        this.connectionType = type;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean connected) {
        isConnected.setValue(connected);
    }
}




