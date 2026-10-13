const form = document.getElementById("signupForm");
const button = document.getElementById("submitButton");
const notice = document.getElementById("notice");
fetch("/api/app-config")
    .then(response => response.ok ? response.json() : null)
    .then(config => { if (config?.demoMode) document.getElementById("demoNotice").hidden = false; })
    .catch(() => {});
form.addEventListener("submit", async event => {
    event.preventDefault();
    button.disabled = true;
    notice.textContent = "";
    try {
        const csrfResponse = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!csrfResponse.ok) throw new Error("安全確認を取得できませんでした。");
        const csrf = await csrfResponse.json();
        const cookie = document.cookie.split(";")
            .map(value => value.trim())
            .find(value => value.startsWith("XSRF-TOKEN="));
        if (!cookie) throw new Error("安全確認の情報が見つかりません。");
        const response = await fetch("/api/auth/register", {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: decodeURIComponent(cookie.slice("XSRF-TOKEN=".length))
            },
            body: JSON.stringify({
                username: document.getElementById("username").value,
                password: document.getElementById("password").value
            })
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.detail || "入力内容を確認してください。");
        }
        location.href = "/login.html?registered";
    } catch (error) {
        notice.textContent = error.message || "登録できませんでした。";
        button.disabled = false;
    }
});
