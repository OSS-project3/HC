package com.example.honorcitizen.domain.event.repository;

import com.example.honorcitizen.domain.event.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    List<EventImage> findByEventPostIdOrderByDisplayOrderAsc(Long eventPostId);

    void deleteByEventPostId(Long eventPostId);
}
