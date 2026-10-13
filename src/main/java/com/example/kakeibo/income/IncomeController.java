package com.example.kakeibo.income;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.kakeibo.account.Account;
import com.example.kakeibo.account.AccountRepository;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeRepository incomes;
    private final AccountRepository accounts;

    public IncomeController(IncomeRepository incomes, AccountRepository accounts) {
        this.incomes = incomes;
        this.accounts = accounts;
    }

    @GetMapping
    public List<IncomeView> getAll(Authentication authentication) {
        Account owner = currentAccount(authentication);
        return incomes.findAllByOwner_IdOrderByDateDescIdDesc(owner.getId())
                .stream().map(IncomeView::from).toList();
    }

    @PostMapping
    public IncomeView add(@RequestBody IncomeInput input, Authentication authentication) {
        if (input == null || input.date() == null || input.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "日付と金額を確認してください。");
        }
        Account owner = currentAccount(authentication);
        String category = input.category() == null || input.category().isBlank()
                ? "未分類" : input.category().trim();
        String memo = input.memo() == null ? "" : input.memo().trim();
        Income saved = incomes.save(new Income(input.date(), input.amount(), category, memo, owner));
        return IncomeView.from(saved);
    }

    private Account currentAccount(Authentication authentication) {
        return accounts.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "ログインし直してください。"));
    }

    public record IncomeInput(LocalDate date, int amount, String category, String memo) { }
    public record IncomeView(long id, LocalDate date, int amount, String category, String memo) {
        static IncomeView from(Income income) {
            return new IncomeView(income.getId(), income.getDate(), income.getAmount(),
                    income.getCategory(), income.getMemo());
        }
    }
}
