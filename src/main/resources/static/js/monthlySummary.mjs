// 月別の収支とカテゴリー別支出をまとめる純粋関数です。
// 画面と自動テストの両方から同じ計算を使います。
export function summarizeMonth(expenses, incomes, selectedMonth) {
    const isSelectedMonth = record =>
        typeof record.date === "string"
        && record.date.slice(0, 7) === selectedMonth;
    const isThroughSelectedMonth = record =>
        typeof record.date === "string"
        && record.date.slice(0, 7) <= selectedMonth;
    const sumAmounts = records =>
        records.reduce((sum, record) => sum + Number(record.amount), 0);

    const monthExpenses = expenses.filter(isSelectedMonth);
    const monthIncomes = incomes.filter(isSelectedMonth);
    const categoryTotals = new Map();

    for (const expense of monthExpenses) {
        const category = expense.category || "未分類";
        categoryTotals.set(
            category,
            (categoryTotals.get(category) || 0) + Number(expense.amount)
        );
    }

    const incomeThroughMonth = sumAmounts(incomes.filter(isThroughSelectedMonth));
    const expenseThroughMonth = sumAmounts(expenses.filter(isThroughSelectedMonth));

    return {
        monthExpenses,
        monthIncomes,
        expenseTotal: sumAmounts(monthExpenses),
        incomeTotal: sumAmounts(monthIncomes),
        savingsBalance: incomeThroughMonth - expenseThroughMonth,
        categoryTotals
    };
}

