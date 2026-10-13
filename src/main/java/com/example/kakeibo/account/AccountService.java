package com.example.kakeibo.account;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("[a-zA-Z0-9._-]{3,30}");

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account register(String submittedUsername, String password) {
        String username = submittedUsername == null
                ? ""
                : submittedUsername.trim().toLowerCase(Locale.ROOT);

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ユーザー名は半角英数字と . _ - を使って3〜30文字で入力してください。");
        }
        if (password == null
                || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "パスワードは12文字以上で設定してください（日本語などは72バイト以内）。");
        }
        if (accounts.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "そのユーザー名はすでに使われています。");
        }

        return accounts.save(new Account(username, passwordEncoder.encode(password)));
    }
}
