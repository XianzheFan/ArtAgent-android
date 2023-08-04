package com.fxz.artagent;

public interface WeatherTextCallback {
    void onWeatherTextReceived(String weatherText);
    void onError(Exception e);
}