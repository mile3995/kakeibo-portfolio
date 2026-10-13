package com.example.kakeibo.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/csrf")
    public CsrfInfo csrf(CsrfToken token) {
        return new CsrfInfo(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/me")
    public CurrentAccount current(Authentication authentication) {
        return new CurrentAccount(authentication.getName());
    }

    @PostMapping("/register")
    public RegistrationResult register(@RequestBody RegistrationInput input) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "入力内容を確認してください。");
        }
        Account account = accountService.register(input.username(), input.password());
        return new RegistrationResult(account.getUsername());
    }

    public record CsrfInfo(String token, String headerName, String parameterName) {
    }

    public record CurrentAccount(String username) {
    }

    public record RegistrationInput(String username, String password) {
    }

    public record RegistrationResult(String username) {
    }
}
