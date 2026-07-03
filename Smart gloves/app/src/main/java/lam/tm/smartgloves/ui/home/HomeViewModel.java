package lam.tm.smartgloves.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> gestureName;
    private final MutableLiveData<String> gestureSentence;
    private final MutableLiveData<Boolean> isConnected;

    public HomeViewModel() {
        gestureName = new MutableLiveData<>();
        gestureSentence = new MutableLiveData<>();
        isConnected = new MutableLiveData<>();
        
        gestureName.setValue("");
        gestureSentence.setValue("");
        isConnected.setValue(false);
    }

    public LiveData<String> getGestureName() {
        return gestureName;
    }

    public LiveData<String> getGestureSentence() {
        return gestureSentence;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public void setGestureData(String name, String sentence) {
        gestureName.setValue(name);
        gestureSentence.setValue(sentence);
    }

    public void setConnectionStatus(boolean connected) {
        isConnected.setValue(connected);
    }
}