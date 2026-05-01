package com.transit.cardservice.repository;

import com.transit.cardservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}
