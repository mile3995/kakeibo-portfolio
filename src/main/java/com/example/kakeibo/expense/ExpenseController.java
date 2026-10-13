package com.example.kakeibo.expense;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.kakeibo.account.Account;
import com.example.kakeibo.account.AccountRepository;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenses;
    private final AccountRepository accounts;

    public ExpenseController(ExpenseRepository expenses, AccountRepository accounts) {
        this.expenses = expenses;
        this.accounts = accounts;
    }

    @GetMapping
    public List<ExpenseView> getAll(Authentication authentication) {
        Account owner = currentAccount(authentication);
        return expenses.findAllByOwner_IdOrderByDateDescIdDesc(owner.getId())
                .stream().map(ExpenseView::from).toList();
    }

    @PostMapping
    public ExpenseView add(@RequestBody ExpenseInput input, Authentication authentication) {
        if (input == null || input.date() == null || input.amount() <= 0
                || input.category() == null || input.category().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "日付、金額、カテゴリーを確認してください。");
        }
        Account owner = currentAccount(authentication);
        String memo = input.memo() == null ? "" : input.memo().trim();
        Expense saved = expenses.save(new Expense(input.date(), input.amount(),
                input.category().trim(), memo, owner));
        return ExpenseView.from(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id, Authentication authentication) {
        Account owner = currentAccount(authentication);
        Expense expense = expenses.findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "指定した支出が見つかりません。"));
        expenses.delete(expense);
    }

    private Account currentAccount(Authentication authentication) {
        return accounts.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "ログインし直してください。"));
    }

    public record ExpenseInput(LocalDate date, int amount, String category, String memo) { }
    public record ExpenseView(long id, LocalDate date, int amount, String category, String memo) {
        static ExpenseView from(Expense expense) {
            return new ExpenseView(expense.getId(), expense.getDate(), expense.getAmount(),
                    expense.getCategory(), expense.getMemo());
        }
    }
}
