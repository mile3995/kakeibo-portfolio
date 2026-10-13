const notice = document.getElementById("notice");
if (new URLSearchParams(location.search).has("error")) notice.textContent = "ユーザー名またはパスワードを確認してください。";
if (new URLSearchParams(location.search).has("registered")) notice.textContent = "登録できました。ログインしてください。";
if (new URLSearchParams(location.search).has("logout")) notice.textContent = "ログアウトしました。";
fetch("/api/auth/csrf", { credentials: "same-origin" })
    .then(response => response.json())
    .then(csrf => {
        const input = document.getElementById("csrf");
        input.name = csrf.parameterName;
        input.value = csrf.token;
    })
    .catch(() => { notice.textContent = "ページを再読み込みしてください。"; });
fetch("/api/app-config")
    .then(response => response.ok ? response.json() : null)
    .then(config => { if (config?.demoMode) document.getElementById("demoNotice").hidden = false; })
    .catch(() => {});
