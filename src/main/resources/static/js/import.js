const form = document.getElementById("importForm");
fetch("/api/app-config")
    .then(response => response.ok ? response.json() : null)
    .then(config => {
        if (config?.demoMode) {
            document.getElementById("demoNotice").hidden = false;
            document.getElementById("importPanel").hidden = true;
        }
    })
    .catch(() => {});
const expensesFile = document.getElementById("expensesFile");
const incomesFile = document.getElementById("incomesFile");
const previewButton = document.getElementById("previewButton");
const confirmButton = document.getElementById("confirmButton");
const message = document.getElementById("message");
const result = document.getElementById("result");
const previewPanel = document.getElementById("previewPanel");
const duplicateWarning = document.getElementById("duplicateWarning");
let currentPreviewId = null;

async function apiRequest(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        const tokenResponse = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!tokenResponse.ok) throw new Error("ログイン状態を確認できません。ログインし直してください。");
        const csrf = await tokenResponse.json();
        const cookie = document.cookie.split(";").map(value => value.trim())
            .find(value => value.startsWith("XSRF-TOKEN="));
        if (!cookie) throw new Error("安全確認の情報を取得できません。ページを再読み込みしてください。");
        headers.set(csrf.headerName, decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)));
    }
    const response = await fetch(url, { ...options, headers, credentials: "same-origin" });
    if (response.status === 401) window.location.href = "/login.html";
    return response;
}

async function responseMessage(response, fallback) {
    try {
        const body = await response.json();
        return body.detail || body.message || fallback;
    } catch (_) {
        return fallback;
    }
}

function yen(value) {
    return "¥" + Number(value).toLocaleString("ja-JP");
}

apiRequest("/api/auth/me")
    .then(async response => {
        if (!response.ok) throw new Error("ログインし直してください。");
        const account = await response.json();
        document.getElementById("targetAccount").textContent =
            "取り込み先：" + account.username + " さんの家計簿";
    })
    .catch(error => {
        document.getElementById("targetAccount").textContent = error.message;
    });

function clearPreview() {
    currentPreviewId = null;
    previewPanel.hidden = true;
    confirmButton.disabled = true;
    result.textContent = "";
    duplicateWarning.hidden = true;
}

for (const input of [expensesFile, incomesFile]) {
    input.addEventListener("change", () => {
        clearPreview();
        message.textContent = "";
    });
}

previewButton.addEventListener("click", async () => {
    clearPreview();
    message.classList.remove("error");
    message.textContent = "CSVを確認しています…";
    previewButton.disabled = true;
    const formData = new FormData(form);
    try {
        const response = await apiRequest("/api/import/preview", {
            method: "POST",
            body: formData
        });
        if (!response.ok) throw new Error(await responseMessage(response, "CSVを読み取れませんでした。"));
        const preview = await response.json();
        currentPreviewId = preview.previewId;
        document.getElementById("targetAccount").textContent =
            "取り込み先：" + preview.targetUsername + " さんの家計簿";
        document.getElementById("expenseCount").textContent = preview.expenseCount.toLocaleString("ja-JP") + "件";
        document.getElementById("expenseTotal").textContent = "合計 " + yen(preview.expenseTotal);
        document.getElementById("incomeCount").textContent = preview.incomeCount.toLocaleString("ja-JP") + "件";
        document.getElementById("incomeTotal").textContent = "合計 " + yen(preview.incomeTotal);
        const alreadyImported = preview.expensesAlreadyImported || preview.incomesAlreadyImported;
        duplicateWarning.hidden = !alreadyImported;
        confirmButton.disabled = alreadyImported;
        previewPanel.hidden = false;
        message.textContent = "確認内容が表示されたよ。取り込み先と件数を見てね。";
    } catch (error) {
        message.classList.add("error");
        message.textContent = error.message;
    } finally {
        previewButton.disabled = false;
    }
});

confirmButton.addEventListener("click", async () => {
    if (!currentPreviewId) return;
    const accepted = window.confirm("表示された件数と取り込み先で、CSVの記録を追加しますか？");
    if (!accepted) return;
    confirmButton.disabled = true;
    result.classList.remove("error");
    result.textContent = "取り込んでいます…";
    try {
        const response = await apiRequest("/api/import/confirm", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ previewId: currentPreviewId })
        });
        if (!response.ok) throw new Error(await responseMessage(response, "取り込めませんでした。"));
        const imported = await response.json();
        currentPreviewId = null;
        result.textContent = `取り込みました。支出 ${imported.expenseCount}件、収入 ${imported.incomeCount}件。家計簿に戻って一覧を確認してください。`;
        message.textContent = "取り込みは完了しました。";
    } catch (error) {
        result.classList.add("error");
        result.textContent = error.message;
        confirmButton.disabled = false;
    }
});
