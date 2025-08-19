package com.myproject.service;

import com.myproject.entity.vo.response.WeatherVO;

public interface WeatherService {

	WeatherVO fetchWeather(double longitude, double latitude);

}
