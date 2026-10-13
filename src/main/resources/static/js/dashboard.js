// 画面の入力欄と操作ボタンを取得します。
const form = document.getElementById("expenseForm");
const dateInput = document.getElementById("date");
const amountInput = document.getElementById("amount");
const categoryInput = document.getElementById("category");
const memoInput = document.getElementById("memo");
const addButton = document.getElementById("addButton");
const message = document.getElementById("message");
const incomeForm = document.getElementById("incomeForm");
const incomeDateInput = document.getElementById("incomeDate");
const incomeAmountInput = document.getElementById("incomeAmount");
const incomeCategoryInput = document.getElementById("incomeCategory");
const incomeMemoInput = document.getElementById("incomeMemo");
const addIncomeButton = document.getElementById("addIncomeButton");
const incomeMessage = document.getElementById("incomeMessage");
const monthFilter = document.getElementById("monthFilter");
const themeToggle = document.getElementById("themeToggle");
const themeToggleLabel = document.getElementById("themeToggleLabel");
const logoutButton = document.getElementById("logoutButton");
const currentUser = document.getElementById("currentUser");

// ログイン状態と CSRF 対策を含めて API を呼び出します。
async function apiRequest(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        const tokenResponse = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!tokenResponse.ok) {
            if (tokenResponse.status === 401) window.location.href = "/login.html";
            throw new Error("安全確認の情報を取得できませんでした。");
        }
        const csrf = await tokenResponse.json();
        const xsrfCookie = document.cookie.split(";")
            .map(value => value.trim())
            .find(value => value.startsWith("XSRF-TOKEN="));
        if (!xsrfCookie) throw new Error("安全確認の情報が見つかりません。");
        headers.set(csrf.headerName, decodeURIComponent(xsrfCookie.slice("XSRF-TOKEN=".length)));
    }
    const response = await fetch(url, {
        ...options,
        headers,
        credentials: "same-origin"
    });
    if (response.status === 401) window.location.href = "/login.html";
    return response;
}

logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    try {
        await apiRequest("/logout", { method: "POST" });
        window.location.href = "/login.html?logout";
    } catch (error) {
        logoutButton.disabled = false;
        message.textContent = "ログアウトできませんでした。もう一度お試しください。";
    }
});

apiRequest("/api/auth/me")
    .then(response => response.ok ? response.json() : Promise.reject())
    .then(account => { currentUser.textContent = account.username; })
    .catch(() => {});

// 選択したテーマを画面に反映し、次回も使えるよう保存します。
function applyTheme(theme) {
    const isDark = theme === "dark";
    document.documentElement.dataset.theme = isDark ? "dark" : "light";
    themeToggle.setAttribute("aria-pressed", String(isDark));
    themeToggle.setAttribute(
        "aria-label",
        isDark ? "ライトモードに切り替える" : "ダークモードに切り替える");
    themeToggleLabel.textContent = isDark ? "ライトモード" : "ダークモード";
    localStorage.setItem("kakeibo-theme", isDark ? "dark" : "light");
}

const savedTheme = localStorage.getItem("kakeibo-theme");
applyTheme(savedTheme === "dark" ? "dark" : "light");

themeToggle.addEventListener("click", () => {
    const nextTheme =
        document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    applyTheme(nextTheme);
});

// 日付入力と月選択に使う日付を YYYY-MM-DD 形式で返します。
function todayAsText() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

dateInput.value = todayAsText();
incomeDateInput.value = todayAsText();
monthFilter.value = todayAsText().slice(0, 7);
function yen(amount) {
    return "¥" + Number(amount).toLocaleString("ja-JP");
}

const categoryColors = {
    "食費": "#f07191",
    "住居費": "#5ba5ef",
    "光熱費": "#ffc94f",
    "日用品": "#5bc783",
    "交通費": "#a574e5",
    "娯楽・趣味": "#ff9b4d",
    "娯楽": "#ff9b4d",
    "その他": "#cbd2da",
    "未分類": "#87938d"
};

function categoryPill(category) {
    const name = category || "未分類";
    const color = categoryColors[name] || "#87938d";
    const pill = document.createElement("span");
    pill.className = "category-pill";
    pill.textContent = name;
    pill.style.color = color;
    pill.style.backgroundColor = color + "22";
    return pill;
}
// 選択月の支出合計、カテゴリーグラフ、支出一覧を更新します。
function showExpenses(expenses) {
    const selectedMonth = monthFilter.value || todayAsText().slice(0, 7);
    const monthExpenses = expenses.filter(expense =>
        expense.date.startsWith(selectedMonth)
    );
    const monthlyTotal = monthExpenses.reduce(
        (sum, expense) => sum + Number(expense.amount),
        0
    );

    document.getElementById("monthlyTotal").textContent = yen(monthlyTotal);
    document.getElementById("expenseCount").textContent = monthExpenses.length + "件";
    const categoryTotals = new Map();

    for (const expense of monthExpenses) {
        const category = expense.category || "未分類";
        const currentTotal = categoryTotals.get(category) || 0;
        categoryTotals.set(category, currentTotal + Number(expense.amount));
    }

    const categoryList = document.getElementById("categoryTotals");
    categoryList.replaceChildren();
    const totalForChart = [...categoryTotals.values()]
        .reduce((sum, amount) => sum + amount, 0);

    const categoryChart = document.getElementById("categoryChart");
    const chartContext = categoryChart.getContext("2d");
    const chartSize = categoryChart.width;
    const center = chartSize / 2;
    const radius = center - 4;
    chartContext.clearRect(0, 0, chartSize, chartSize);

    if (totalForChart === 0) {
        chartContext.beginPath();
        chartContext.arc(center, center, radius, 0, Math.PI * 2);
        chartContext.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--line").trim() || "#e5ece8";
        chartContext.fill();
    } else {
        let startAngle = -Math.PI / 2;
        for (const [category, amount] of categoryTotals.entries()) {
            const portion = amount / totalForChart;
            const endAngle = startAngle + portion * Math.PI * 2;
            const color = categoryColors[category] || "#87938d";

            chartContext.beginPath();
            chartContext.moveTo(center, center);
            chartContext.arc(center, center, radius, startAngle, endAngle);
            chartContext.closePath();
            chartContext.fillStyle = color;
            chartContext.fill();
            chartContext.lineWidth = 3;
            chartContext.strokeStyle = "#ffffff";
            chartContext.stroke();

            if (portion >= 0.045) {
                const middleAngle = (startAngle + endAngle) / 2;
                const labelRadius = radius * 0.64;
                const x = center + Math.cos(middleAngle) * labelRadius;
                const y = center + Math.sin(middleAngle) * labelRadius;
                chartContext.fillStyle = "#ffffff";
                chartContext.font = "bold 17px Segoe UI, sans-serif";
                chartContext.textAlign = "center";
                chartContext.textBaseline = "middle";
                chartContext.fillText(Math.round(portion * 100) + "%", x, y);
            }
            startAngle = endAngle;
        }
    }
    if (categoryTotals.size === 0) {
        const item = document.createElement("li");
        item.textContent = "まだ支出はありません。";
        categoryList.appendChild(item);
    } else {
        const sortedCategories = [...categoryTotals.entries()]
            .sort((a, b) => b[1] - a[1]);

        for (const [category, total] of sortedCategories) {
            const item = document.createElement("li");
            const swatch = document.createElement("span");
            swatch.className = "category-swatch";
            swatch.style.backgroundColor = categoryColors[category] || "#87938d";

            const name = document.createElement("span");
            name.className = "legend-name";
            name.textContent = category;

            const amount = document.createElement("span");
            amount.className = "legend-amount";
            amount.textContent = yen(total);

            const percentage = document.createElement("span");
            percentage.className = "legend-percent";
            percentage.textContent = Math.round(total / totalForChart * 100) + "%";

            item.appendChild(swatch);
            item.appendChild(name);
            item.appendChild(amount);
            item.appendChild(percentage);
            categoryList.appendChild(item);
        }
    }
    const rows = document.getElementById("expenseRows");
    rows.replaceChildren();

    const sortedExpenses = [...monthExpenses].sort((a, b) =>
        (b.date + b.id).localeCompare(a.date + a.id)
    );

    for (const expense of sortedExpenses) {
        const row = document.createElement("tr");

        const dateCell = document.createElement("td");
        dateCell.textContent = expense.date;
        row.appendChild(dateCell);

        const memoCell = document.createElement("td");
        memoCell.textContent = expense.memo || "—";
        row.appendChild(memoCell);

        const categoryCell = document.createElement("td");
        categoryCell.appendChild(categoryPill(expense.category));
        row.appendChild(categoryCell);

        const amountCell = document.createElement("td");
        amountCell.textContent = yen(expense.amount);
        row.appendChild(amountCell);
        const deleteCell = document.createElement("td");
        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "delete-button";
        deleteButton.textContent = "削除";

        deleteButton.addEventListener("click", async () => {
            const confirmed = window.confirm(
                `${expense.date} ${expense.category} ${yen(expense.amount)}を削除しますか？`
            );

            if (!confirmed) {
                return;
            }

            deleteButton.disabled = true;
            message.textContent = "";

            try {
                const response = await apiRequest(`/api/expenses/${expense.id}`, {
                    method: "DELETE"
                });

                if (!response.ok) {
                    throw new Error("削除に失敗しました");
                }

                await loadExpenses();
                message.textContent = "支出を削除しました。";
            } catch (error) {
                message.textContent = "削除できませんでした。";
                deleteButton.disabled = false;
            }
        });

        deleteCell.appendChild(deleteButton);
        row.appendChild(deleteCell);
        rows.appendChild(row);
    }
}

// サーバーから収入・支出を読み込み、月別の表示を更新します。
async function loadExpenses() {
    const expenseResponse = await apiRequest("/api/expenses");

    if (!expenseResponse.ok) {
        throw new Error("支出データを読み込めませんでした");
    }

    const expenses = await expenseResponse.json();
    showExpenses(expenses);

    const incomeResponse = await apiRequest("/api/incomes");

    if (!incomeResponse.ok) {
        throw new Error("収入データを読み込めませんでした");
    }

    const incomes = await incomeResponse.json();
    const selectedMonth = monthFilter.value || todayAsText().slice(0, 7);
    const monthIncomes = incomes.filter(income =>
        income.date.startsWith(selectedMonth)
    );
    const incomeRows = document.getElementById("incomeRows");
    incomeRows.replaceChildren();

    const sortedIncomes = [...monthIncomes].sort((a, b) =>
        (b.date + b.id).localeCompare(a.date + a.id)
    );

    for (const income of sortedIncomes) {
        const row = document.createElement("tr");

        const dateCell = document.createElement("td");
        dateCell.textContent = income.date;
        row.appendChild(dateCell);

        const memoCell = document.createElement("td");
        memoCell.textContent = income.memo || "—";
        row.appendChild(memoCell);

        const categoryCell = document.createElement("td");
        categoryCell.appendChild(categoryPill(income.category));
        row.appendChild(categoryCell);

        const amountCell = document.createElement("td");
        amountCell.textContent = yen(income.amount);
        row.appendChild(amountCell);
        incomeRows.appendChild(row);
    }
    const totalIncome = monthIncomes.reduce(
        (sum, income) => sum + Number(income.amount),
        0
    );
    const incomeThroughMonth = incomes
        .filter(income => income.date.slice(0, 7) <= selectedMonth)
        .reduce((sum, income) => sum + Number(income.amount), 0);
    const expensesThroughMonth = expenses
        .filter(expense => expense.date.slice(0, 7) <= selectedMonth)
        .reduce((sum, expense) => sum + Number(expense.amount), 0);

    document.getElementById("incomeTotal").textContent = yen(totalIncome);
    document.getElementById("savingsBalance").textContent =
        yen(incomeThroughMonth - expensesThroughMonth);
}

// 支出フォームを送信し、保存後に一覧を読み直します。
form.addEventListener("submit", async event => {
    event.preventDefault();
    addButton.disabled = true;
    message.textContent = "";

    const newExpense = {
        date: dateInput.value,
        amount: Number(amountInput.value),
        category: categoryInput.value,
        memo: memoInput.value
    };

    try {
        const response = await apiRequest("/api/expenses", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(newExpense)
        });

        if (!response.ok) {
            throw new Error("支出を保存できませんでした");
        }

        amountInput.value = "";
        memoInput.value = "";
        await loadExpenses();
        message.textContent = "支出を保存しました。";
    } catch (error) {
        message.textContent =
            "登録できませんでした。黒い画面にエラーが出ていないか確認してください。";
    } finally {
        addButton.disabled = false;
    }
});

// 収入フォームを送信し、保存後に一覧を読み直します。
incomeForm.addEventListener("submit", async event => {
    event.preventDefault();
    addIncomeButton.disabled = true;
    incomeMessage.textContent = "";

    const newIncome = {
        date: incomeDateInput.value,
        amount: Number(incomeAmountInput.value),
        category: incomeCategoryInput.value,
        memo: incomeMemoInput.value
    };

    try {
        const response = await apiRequest("/api/incomes", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(newIncome)
        });

        if (!response.ok) {
            throw new Error("収入を保存できませんでした");
        }

        incomeAmountInput.value = "";
        incomeMemoInput.value = "";
        await loadExpenses();
        incomeMessage.textContent = "収入を保存しました。";
    } catch (error) {
        incomeMessage.textContent = "収入を登録できませんでした。";
    } finally {
        addIncomeButton.disabled = false;
    }
});

monthFilter.addEventListener("change", () => {
    loadExpenses().catch(() => {
        message.textContent = "月のデータを読み込めませんでした。";
    });
});

loadExpenses().catch(() => {
    message.textContent =
        "読み込めませんでした。黒い画面でサーバーが動いているか確認してください。";
});

fetch("/api/app-config")
    .then(response => response.ok ? response.json() : null)
    .then(config => {
        if (config?.demoMode) {
            document.getElementById("demoNotice").hidden = false;
            document.getElementById("importLink").hidden = true;
        }
    })
    .catch(() => {});
