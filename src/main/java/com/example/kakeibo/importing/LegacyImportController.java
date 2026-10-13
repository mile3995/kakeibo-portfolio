package com.example.kakeibo.importing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.kakeibo.account.Account;
import com.example.kakeibo.account.AccountRepository;

@RestController
@RequestMapping("/api/import")
@ConditionalOnProperty(prefix = "app", name = "demo-mode", havingValue = "false", matchIfMissing = true)
public class LegacyImportController {

    private final LegacyCsvImportService importService;
    private final AccountRepository accounts;

    public LegacyImportController(LegacyCsvImportService importService, AccountRepository accounts) {
        this.importService = importService;
        this.accounts = accounts;
    }

    @PostMapping("/preview")
    public LegacyCsvImportService.ImportPreview preview(
            @RequestParam(required = false) MultipartFile expenses,
            @RequestParam(required = false) MultipartFile incomes,
            Authentication authentication) {
        return importService.preview(currentAccount(authentication), expenses, incomes);
    }

    @PostMapping("/confirm")
    public LegacyCsvImportService.ImportResult confirm(
            @org.springframework.web.bind.annotation.RequestBody ConfirmInput input,
            Authentication authentication) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "確認情報がありません。");
        }
        return importService.confirm(currentAccount(authentication), input.previewId());
    }

    private Account currentAccount(Authentication authentication) {
        return accounts.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "ログインし直してください。"));
    }

    public record ConfirmInput(String previewId) { }
}
