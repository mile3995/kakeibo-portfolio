package com.example.kakeibo.expense;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.kakeibo.account.Account;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private int amount;
    @Column(nullable = false, length = 60)
    private String category;
    @Column(nullable = false, length = 500)
    private String memo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    protected Expense() { }

    public Expense(LocalDate date, int amount, String category, String memo, Account owner) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.memo = memo;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getMemo() { return memo; }
}
