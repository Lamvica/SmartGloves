package lam.tm.smartgloves.ui.voicesettings;

import androidx.lifecycle.ViewModel;

public class VoiceSettingsViewModel extends ViewModel {

    private boolean voiceFemale = false;
    private float voiceSpeed = 1.0f;
    private float voiceVolume = 1.0f;

    public boolean isVoiceFemale() {
        return voiceFemale;
    }

    public void setVoiceGender(boolean isFemale) {
        this.voiceFemale = isFemale;
    }

    public float getVoiceSpeed() {
        return voiceSpeed;
    }

    public void setVoiceSpeed(float speed) {
        this.voiceSpeed = speed;
    }

    public float getVoiceVolume() {
        return voiceVolume;
    }

    public void setVoiceVolume(float volume) {
        this.voiceVolume = volume;
    }
}




