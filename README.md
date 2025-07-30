# Temp Widget

A temperature widget for Android devices, specifically designed for Amazon Fire tablets. The widget displays current temperature in both Celsius and Fahrenheit using location-based weather data.

## Features

- **Dual Temperature Display**: Shows current temperature in both Celsius and Fahrenheit
- **AppWidgetProvider**: `TempWidgetProvider` - Core widget functionality
- **Weather API**: Modular design with `WeatherApi` interface
- **Location Services**: Native Android LocationManager (no Google Play Services)
    - `this isn't quite working yet`
- **Background Updates**: WorkManager for reliable background updates

### Current Weather Service
- **Pirate-Weather API**: https://pirateweather.net/en/latest/API/
    - API is key required
- **REPLACED**:
    - _Open-Meteo API_: https://open-meteo.com/en/docs
        - accuracy of percipitation was not very good
        - No API key required

## Development Setup

1. **Open in Android Studio**
2. **Build and run** on an emulator or device
3. **Add widget** to home screen through long-press menu

## Configuration

### Refresh Interval
Edit `src/main/res/values/config.xml`:
```xml
<integer name="widget_update_interval_ms">300000</integer> <!-- 5 minutes -->
```

### Widget Dimensions
Edit `src/main/res/values/config.xml`:
```xml
<dimen name="widget_min_width">640dp</dimen>
<dimen name="widget_min_height">360dp</dimen>
```

### Weather API
To switch weather services, implement the `WeatherApi` interface and update the provider:
```kotlin
class NewWeatherService : WeatherApi {
    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherResult {
        // Implementation
    }
}
```

### Copying to Fire Tablet

1. Build apk in Android Studio: Build > Generate App Bundles > Generate APK
2. rename the new apk file that was built
3. copy to Downloads folder
4.
```bash
cd ~/Downloads/
ifconfig | grep 192
python3 -m http.server 808
```
5. In tablet browser, go to the computers IP:808 and download the apk file
6. Click on file and Install
7. Use Nova Launcher to open widget

## Permissions

- `ACCESS_FINE_LOCATION`: For precise location data
- `ACCESS_COARSE_LOCATION`: Fallback for approximate location
- `INTERNET`: For weather API calls
- `ACCESS_NETWORK_STATE`: For network connectivity checks
- `WAKE_LOCK`: For reliable background updates

## Future Enhancements

- Weather icon display
    https://github.com/b-reich/MMM-PirateSkyForecast/blob/main/icons/iconsets.png
- Widget configuration activity
- Custom themes and colors

### alternatives for Weather API
- a listing of different ideas: https://www.reddit.com/r/homeassistant/comments/1f2w3xo/best_free_weather_integration_open_weather_dead/
	- https://pirateweather.net/en/latest/API/
	- https://openweathermap.org/api/one-call-3
	- https://www.weatherapi.com/docs/
	- https://www.weather.gov/documentation/services-web-api
	- https://docs.tomorrow.io/reference/welcome

## License

This project is released into the public domain under The Unlicense.

Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

