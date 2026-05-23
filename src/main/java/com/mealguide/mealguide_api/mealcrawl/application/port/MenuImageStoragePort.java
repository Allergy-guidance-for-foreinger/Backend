package com.mealguide.mealguide_api.mealcrawl.application.port;

import org.springframework.web.multipart.MultipartFile;

public interface MenuImageStoragePort {
    String upload(Long userId, MultipartFile imageFile);
}

