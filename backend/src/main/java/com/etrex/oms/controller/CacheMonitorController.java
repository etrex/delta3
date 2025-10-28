package com.etrex.oms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheMonitorController {

    private final CacheManager cacheManager;

    /**
     * 查看所有快取內容
     * GET /api/cache
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCaches() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> caches = new ArrayList<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);

                if (cache instanceof ConcurrentMapCache) {
                    ConcurrentMapCache mapCache = (ConcurrentMapCache) cache;
                    Object nativeCache = mapCache.getNativeCache();

                    if (nativeCache instanceof ConcurrentHashMap) {
                        @SuppressWarnings("unchecked")
                        ConcurrentHashMap<Object, Object> map = (ConcurrentHashMap<Object, Object>) nativeCache;

                        cacheInfo.put("size", map.size());
                        cacheInfo.put("keys", new ArrayList<>(map.keySet()));

                        Map<String, Object> entries = new HashMap<>();
                        map.forEach((key, value) -> {
                            entries.put(String.valueOf(key), value);
                        });
                        cacheInfo.put("entries", entries);
                    }
                }

                caches.add(cacheInfo);
            }
        }

        result.put("caches", caches);
        result.put("totalCaches", caches.size());

        return ResponseEntity.ok(result);
    }
}
