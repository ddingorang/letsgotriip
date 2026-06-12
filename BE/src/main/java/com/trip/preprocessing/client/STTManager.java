package com.trip.preprocessing.client;

import java.io.File;

public interface STTManager {
    String convertSpeechToText(File audioFile);
}
