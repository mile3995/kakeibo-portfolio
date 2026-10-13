package com.example.kakeibo.expense;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOwner_IdOrderByDateDescIdDesc(Long ownerId);
    Optional<Expense> findByIdAndOwner_Id(Long id, Long ownerId);
}
