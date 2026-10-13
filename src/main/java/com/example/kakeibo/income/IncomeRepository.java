package com.example.kakeibo.income;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByOwner_IdOrderByDateDescIdDesc(Long ownerId);
}
