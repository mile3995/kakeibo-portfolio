package com.example.kakeibo.importing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.kakeibo.account.Account;
import com.example.kakeibo.expense.Expense;
import com.example.kakeibo.expense.ExpenseRepository;
import com.example.kakeibo.income.Income;
import com.example.kakeibo.income.IncomeRepository;

@Service
public class LegacyCsvImportService {

    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 50_000;
    private static final Duration PREVIEW_LIFETIME = Duration.ofMinutes(15);
    private static final String EXPENSE_TYPE = "expenses";
    private static final String INCOME_TYPE = "incomes";
    private static final List<String> INCOME_CATEGORIES =
            List.of("給与", "副業", "臨時収入", "賞与", "副収入", "その他", "未分類");

    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final LegacyImportReceiptRepository receipts;
    private final Map<String, PendingImport> pendingImports = new ConcurrentHashMap<>();

    public LegacyCsvImportService(
            ExpenseRepository expenses,
            IncomeRepository incomes,
            LegacyImportReceiptRepository receipts) {
        this.expenses = expenses;
        this.incomes = incomes;
        this.receipts = receipts;
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(Account owner, MultipartFile expenseFile, MultipartFile incomeFile) {
        removeExpiredPreviews();
        if (isMissing(expenseFile) && isMissing(incomeFile)) {
            throw badRequest("支出CSVか収入CSVを少なくとも1つ選んでください。");
        }

        ParsedExpenses parsedExpenses = parseExpenses(expenseFile);
        ParsedIncomes parsedIncomes = parseIncomes(incomeFile);
        if (parsedExpenses.rows().isEmpty() && parsedIncomes.rows().isEmpty()) {
            throw badRequest("CSVに取り込める記録がありません。");
        }

        boolean expensesAlreadyImported = parsedExpenses.included()
                && receipts.existsByOwner_IdAndDataTypeAndFileHash(
                        owner.getId(), EXPENSE_TYPE, parsedExpenses.hash());
        boolean incomesAlreadyImported = parsedIncomes.included()
                && receipts.existsByOwner_IdAndDataTypeAndFileHash(
                        owner.getId(), INCOME_TYPE, parsedIncomes.hash());

        String previewId = UUID.randomUUID().toString();
        pendingImports.put(previewId, new PendingImport(
                owner.getId(), parsedExpenses, parsedIncomes,
                Instant.now().plus(PREVIEW_LIFETIME)));

        return new ImportPreview(
                previewId,
                owner.getUsername(),
                parsedExpenses.rows().size(),
                sumExpenseAmounts(parsedExpenses.rows()),
                parsedIncomes.rows().size(),
                sumIncomeAmounts(parsedIncomes.rows()),
                expensesAlreadyImported,
                incomesAlreadyImported);
    }

    @Transactional
    public synchronized ImportResult confirm(Account owner, String previewId) {
        if (previewId == null || previewId.isBlank()) {
            throw badRequest("確認情報がありません。CSVをもう一度選んでください。");
        }
        PendingImport pending = pendingImports.get(previewId);
        if (pending == null || Instant.now().isAfter(pending.expiresAt())) {
            pendingImports.remove(previewId);
            throw new ResponseStatusException(HttpStatus.GONE,
                    "確認画面の有効時間が過ぎました。CSVをもう一度選んでください。");
        }
        if (!pending.ownerId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "確認したアカウントと現在のログイン先が異なります。");
        }

        ensureNotImported(owner, pending);

        List<Expense> expenseEntities = pending.expenses().rows().stream()
                .map(row -> new Expense(row.date(), row.amount(), row.category(), row.memo(), owner))
                .toList();
        List<Income> incomeEntities = pending.incomes().rows().stream()
                .map(row -> new Income(row.date(), row.amount(), row.category(), row.memo(), owner))
                .toList();
        expenses.saveAll(expenseEntities);
        incomes.saveAll(incomeEntities);

        List<LegacyImportReceipt> newReceipts = new ArrayList<>();
        if (pending.expenses().included()) {
            newReceipts.add(new LegacyImportReceipt(owner, EXPENSE_TYPE, pending.expenses().hash()));
        }
        if (pending.incomes().included()) {
            newReceipts.add(new LegacyImportReceipt(owner, INCOME_TYPE, pending.incomes().hash()));
        }
        receipts.saveAll(newReceipts);
        pendingImports.remove(previewId);

        return new ImportResult(
                expenseEntities.size(),
                sumExpenseAmounts(pending.expenses().rows()),
                incomeEntities.size(),
                sumIncomeAmounts(pending.incomes().rows()));
    }

    private void ensureNotImported(Account owner, PendingImport pending) {
        if (pending.expenses().included()
                && receipts.existsByOwner_IdAndDataTypeAndFileHash(
                        owner.getId(), EXPENSE_TYPE, pending.expenses().hash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "この支出CSVはこのアカウントに取り込み済みです。二重登録を防ぐため中止しました。");
        }
        if (pending.incomes().included()
                && receipts.existsByOwner_IdAndDataTypeAndFileHash(
                        owner.getId(), INCOME_TYPE, pending.incomes().hash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "この収入CSVはこのアカウントに取り込み済みです。二重登録を防ぐため中止しました。");
        }
    }

    private ParsedExpenses parseExpenses(MultipartFile file) {
        if (isMissing(file)) return ParsedExpenses.notIncluded();
        CsvContent content = readCsv(file, "支出");
        List<ExpenseRow> rows = new ArrayList<>();
        String[] lines = content.text().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripBom(lines[i]).trim();
            if (line.isEmpty() || isHeader(line)) continue;
            String[] columns = line.split(",", -1);
            try {
                ParsedRow row = parseRow(columns, false);
                rows.add(new ExpenseRow(row.date(), row.amount(), row.category(), row.memo()));
            } catch (RuntimeException e) {
                throw badRequest("支出CSVの " + (i + 1) + " 行目を読み取れません。日付・金額・区切りを確認してください。");
            }
            if (rows.size() > MAX_ROWS) throw badRequest("支出CSVの記録が多すぎます。");
        }
        return new ParsedExpenses(content.hash(), List.copyOf(rows), true);
    }

    private ParsedIncomes parseIncomes(MultipartFile file) {
        if (isMissing(file)) return ParsedIncomes.notIncluded();
        CsvContent content = readCsv(file, "収入");
        List<IncomeRow> rows = new ArrayList<>();
        String[] lines = content.text().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripBom(lines[i]).trim();
            if (line.isEmpty() || isHeader(line)) continue;
            String[] columns = line.split(",", -1);
            try {
                ParsedRow row = parseRow(columns, true);
                rows.add(new IncomeRow(row.date(), row.amount(), row.category(), row.memo()));
            } catch (RuntimeException e) {
                throw badRequest("収入CSVの " + (i + 1) + " 行目を読み取れません。日付・金額・区切りを確認してください。");
            }
            if (rows.size() > MAX_ROWS) throw badRequest("収入CSVの記録が多すぎます。");
        }
        return new ParsedIncomes(content.hash(), List.copyOf(rows), true);
    }

    private CsvContent readCsv(MultipartFile file, String label) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_FILE_BYTES) {
                throw badRequest(label + "CSVは5MB以下のファイルを選んでください。");
            }
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return new CsvContent(text, sha256(bytes));
        } catch (CharacterCodingException e) {
            throw badRequest(label + "CSVの文字コードを読み取れません。UTF-8で保存されたファイルを使ってください。");
        } catch (IOException e) {
            throw badRequest(label + "CSVを読み取れませんでした。");
        }
    }

    private ParsedRow parseRow(String[] columns, boolean income) {
        for (int i = 0; i < columns.length; i++) columns[i] = columns[i].trim();
        boolean hasId = columns.length >= 3 && isLong(columns[0]) && isDate(columns[1]);
        int dateIndex = hasId ? 1 : 0;
        if (columns.length <= dateIndex + 1 || !isDate(columns[dateIndex])) {
            throw new IllegalArgumentException("invalid date");
        }

        LocalDate date = LocalDate.parse(columns[dateIndex]);
        int amount = Integer.parseInt(columns[dateIndex + 1].replace("¥", "").replace("円", ""));
        if (amount <= 0) throw new IllegalArgumentException("invalid amount");

        String category = "未分類";
        String memo = "";
        int remaining = columns.length - (dateIndex + 2);
        if (remaining >= 2) {
            category = blankAsUncategorized(columns[dateIndex + 2]);
            memo = decodeMemo(columns[dateIndex + 3]);
        } else if (remaining == 1) {
            String last = columns[dateIndex + 2];
            if (income && !isIncomeCategory(last)) {
                memo = decodeMemo(last);
            } else {
                category = blankAsUncategorized(last);
            }
        }

        if (category.length() > 60 || memo.length() > 500) {
            throw new IllegalArgumentException("text too long");
        }
        return new ParsedRow(date, amount, category, memo);
    }

    private String decodeMemo(String value) {
        if (value.isEmpty()) return "";
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (decoded.codePoints().anyMatch(Character::isISOControl)) return value;
            return decoded;
        } catch (IllegalArgumentException | CharacterCodingException e) {
            return value;
        }
    }

    private boolean isIncomeCategory(String value) {
        return INCOME_CATEGORIES.contains(value);
    }

    private String blankAsUncategorized(String value) {
        return value.isBlank() ? "未分類" : value;
    }

    private boolean isHeader(String line) {
        String first = stripBom(line.split(",", -1)[0]).trim();
        return first.equalsIgnoreCase("id")
                || first.equalsIgnoreCase("date")
                || first.equals("日付");
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private void removeExpiredPreviews() {
        Instant now = Instant.now();
        pendingImports.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private long sumExpenseAmounts(List<ExpenseRow> rows) {
        return rows.stream().mapToLong(ExpenseRow::amount).sum();
    }

    private long sumIncomeAmounts(List<IncomeRow> rows) {
        return rows.stream().mapToLong(IncomeRow::amount).sum();
    }

    public record ImportPreview(
            String previewId,
            String targetUsername,
            int expenseCount,
            long expenseTotal,
            int incomeCount,
            long incomeTotal,
            boolean expensesAlreadyImported,
            boolean incomesAlreadyImported) {
        public boolean canImport() {
            return !expensesAlreadyImported && !incomesAlreadyImported;
        }
    }

    public record ImportResult(
            int expenseCount,
            long expenseTotal,
            int incomeCount,
            long incomeTotal) { }

    private record CsvContent(String text, String hash) { }
    private record ParsedRow(LocalDate date, int amount, String category, String memo) { }
    private record ExpenseRow(LocalDate date, int amount, String category, String memo) { }
    private record IncomeRow(LocalDate date, int amount, String category, String memo) { }
    private record ParsedExpenses(String hash, List<ExpenseRow> rows, boolean included) {
        static ParsedExpenses notIncluded() { return new ParsedExpenses(null, List.of(), false); }
    }
    private record ParsedIncomes(String hash, List<IncomeRow> rows, boolean included) {
        static ParsedIncomes notIncluded() { return new ParsedIncomes(null, List.of(), false); }
    }
    private record PendingImport(
            Long ownerId,
            ParsedExpenses expenses,
            ParsedIncomes incomes,
            Instant expiresAt) { }
}
