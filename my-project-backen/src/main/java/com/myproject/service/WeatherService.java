package com.myproject.service;

import com.myproject.entity.vo.response.CityVO;
import com.myproject.entity.vo.response.WeatherVO;

import java.util.List;

public interface WeatherService {

	WeatherVO fetchWeather(double longitude, double latitude);

	/** 按和风城市 ID 取天气，供「切换城市」使用 */
	WeatherVO fetchWeatherById(String id);

	/** 按城市名/拼音检索城市，最多 10 条 */
	List<CityVO> searchCities(String keyword);

}
