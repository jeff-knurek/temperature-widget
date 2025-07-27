# Temp Widget

A temperature widget for Android devices, specifically designed for Amazon Fire tablets. The widget displays current temperature in both Celsius and Fahrenheit using location-based weather data.

## Features

- **Dual Temperature Display**: Shows current temperature in both Celsius and Fahrenheit
- **AppWidgetProvider**: `TempWidgetProvider` - Core widget functionality
- **Weather API**: Modular design with `WeatherApi` interface
- **Location Services**: Native Android LocationManager (no Google Play Services)
- **Background Updates**: WorkManager for reliable background updates

### Current Weather Service
- **Pirate-Weather API**: https://pirateweather.net/en/latest/API/
    - API is key required
- __Open-Meteo API__: https://open-meteo.com/en/docs
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

## Permissions

- `ACCESS_FINE_LOCATION`: For precise location data
- `ACCESS_COARSE_LOCATION`: Fallback for approximate location
- `INTERNET`: For weather API calls
- `ACCESS_NETWORK_STATE`: For network connectivity checks
- `WAKE_LOCK`: For reliable background updates

## Future Enhancements

- Weather forecast display
- Humidity and dew point information
- Multiple weather service support
- Widget configuration activity
- Custom themes and colors

## License

This project is released into the public domain under The Unlicense.

Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

