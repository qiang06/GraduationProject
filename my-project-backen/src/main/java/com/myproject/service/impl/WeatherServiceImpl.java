package com.myproject.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.myproject.entity.vo.response.CityVO;
import com.myproject.entity.vo.response.WeatherVO;
import com.myproject.service.WeatherService;
import com.myproject.utils.Const;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * 和风天气服务实现
 * 接口返回 GZIP 压缩数据，需要手动解压后再解析
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private static final String GEO_API = "https://geoapi.qweather.com/v2/city/lookup";
    private static final String NOW_API = "https://devapi.qweather.com/v7/weather/now";
    private static final String HOURLY_API = "https://devapi.qweather.com/v7/weather/24h";

    @Resource
    RestTemplate rest;

    @Resource
    StringRedisTemplate template;

    @Value("${spring.weather.key:}")
    String key;

    @Override
    public WeatherVO fetchWeather(double longitude, double latitude) {
        if (key == null || key.isBlank()) {
            log.warn("天气服务未配置 API Key，请设置环境变量 WEATHER_API_KEY 或配置 spring.weather.key");
            return null;
        }
        try {
            JSONObject location = this.lookup(longitude + "," + latitude);
            return location == null ? null : this.weatherFor(location);
        } catch (RestClientException e) {
            log.warn("请求和风天气接口失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("获取天气数据失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public WeatherVO fetchWeatherById(String id) {
        if (key == null || key.isBlank()) {
            log.warn("天气服务未配置 API Key，请设置环境变量 WEATHER_API_KEY 或配置 spring.weather.key");
            return null;
        }
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            // 先查缓存：即使和风暂时不可用，已缓存的天气也能正常显示
            String cache = template.opsForValue().get(Const.FORUM_WEATHER_CACHE + id);
            if (cache != null) {
                return JSONObject.parseObject(cache).to(WeatherVO.class);
            }
            JSONObject location = this.lookup(id);
            return location == null ? null : this.weatherFor(location);
        } catch (RestClientException e) {
            log.warn("请求和风天气接口失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("获取天气数据失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<CityVO> searchCities(String keyword) {
        if (key == null || key.isBlank() || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        JSONObject geo;
        try {
            // 不要自己 URLEncoder：RestTemplate 会对 URL 模板再编码一次，中文会被编成 %25E9...
            geo = this.requestJson(GEO_API + "?location=" + keyword.trim()
                    + "&number=10&lang=zh&key=" + key);
        } catch (Exception e) {
            log.warn("检索城市失败: {}", e.getMessage());
            return List.of();
        }
        JSONArray locations = geo == null ? null : geo.getJSONArray("location");
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        List<CityVO> result = new ArrayList<>(locations.size());
        for (int i = 0; i < locations.size(); i++) {
            JSONObject item = locations.getJSONObject(i);
            CityVO vo = new CityVO();
            vo.setId(item.getString("id"));
            vo.setName(item.getString("name"));
            vo.setAdm1(item.getString("adm1"));
            vo.setAdm2(item.getString("adm2"));
            vo.setCountry(item.getString("country"));
            vo.setLat(item.getString("lat"));
            vo.setLon(item.getString("lon"));
            result.add(vo);
        }
        return result;
    }

    /**
     * 按坐标或城市 ID 查和风的城市信息，取第一条
     */
    private JSONObject lookup(String location) {
        JSONObject geo = this.requestJson(GEO_API + "?location=" + location + "&key=" + key);
        JSONArray locations = geo == null ? null : geo.getJSONArray("location");
        if (locations == null || locations.isEmpty()) {
            log.warn("未能从和风天气获取城市信息, location={}, 响应内容: {}", location, geo);
            return null;
        }
        return locations.getJSONObject(0);
    }

    /**
     * 先读缓存，没有再请求实况与逐小时；命中缓存时也要能返回完整的 location
     */
    private WeatherVO weatherFor(JSONObject location) {
        String id = location.getString("id");
        String cacheKey = Const.FORUM_WEATHER_CACHE + id;
        String cache = template.opsForValue().get(cacheKey);
        if (cache != null) {
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        }
        WeatherVO vo = this.fetchFromAPI(id, location);
        if (vo == null) {
            return null;
        }
        template.opsForValue().set(cacheKey, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }

    private WeatherVO fetchFromAPI(String id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);

        JSONObject now = this.requestJson(NOW_API + "?location=" + id + "&key=" + key);
        if (now == null || now.getJSONObject("now") == null) {
            log.warn("未能获取实时天气, 响应内容: {}", now);
            return null;
        }
        vo.setNow(now.getJSONObject("now"));

        JSONObject hourly = this.requestJson(HOURLY_API + "?location=" + id + "&key=" + key);
        JSONArray hours = hourly == null ? null : hourly.getJSONArray("hourly");
        if (hours == null) {
            log.warn("未能获取逐小时天气, 响应内容: {}", hourly);
            return null;
        }
        vo.setHourly(new JSONArray(hours.stream().limit(5).toList()));
        return vo;
    }

    private JSONObject requestJson(String url) {
        byte[] data = rest.getForObject(url, byte[].class);
        if (data == null || data.length == 0) {
            log.warn("和风天气返回空响应: {}", url);
            return null;
        }
        return this.decompressStringToJson(data);
    }

    private JSONObject decompressStringToJson(byte[] data) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                stream.write(buffer, 0, read);
            }
            return JSONObject.parseObject(stream.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("解压和风天气响应失败: {}", e.getMessage());
            return null;
        }
    }

}
