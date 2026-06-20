package com.afetch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afetch.domain.entity.UploadedImage;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {
}
