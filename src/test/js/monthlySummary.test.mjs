import assert from "node:assert/strict";
import test from "node:test";

import { summarizeMonth } from "../../main/resources/static/js/monthlySummary.mjs";

test("選択月の収支と月末時点の残高、カテゴリー別合計を計算する", () => {
    const expenses = [
        { date: "2026-10-05", amount: 3000, category: "食費" },
        { date: "2026-10-07", amount: 1200, category: "交通費" },
        { date: "2026-10-20", amount: 2000, category: "食費" },
        { date: "2026-11-01", amount: 900, category: "日用品" }
    ];
    const incomes = [
        { date: "2026-09-28", amount: 5000 },
        { date: "2026-10-01", amount: 10000 },
        { date: "2026-11-01", amount: 40000 }
    ];

    const summary = summarizeMonth(expenses, incomes, "2026-10");

    assert.equal(summary.monthExpenses.length, 3);
    assert.equal(summary.monthIncomes.length, 1);
    assert.equal(summary.expenseTotal, 6200);
    assert.equal(summary.incomeTotal, 10000);
    assert.equal(summary.savingsBalance, 8800);
    assert.deepEqual([...summary.categoryTotals.entries()], [
        ["食費", 5000],
        ["交通費", 1200]
    ]);
});

test("記録がない月は合計0、残高は選択月までの累計を使う", () => {
    const expenses = [{ date: "2026-09-30", amount: 800, category: "食費" }];
    const incomes = [{ date: "2026-09-01", amount: 5000 }];

    const summary = summarizeMonth(expenses, incomes, "2026-10");

    assert.equal(summary.monthExpenses.length, 0);
    assert.equal(summary.monthIncomes.length, 0);
    assert.equal(summary.expenseTotal, 0);
    assert.equal(summary.incomeTotal, 0);
    assert.equal(summary.savingsBalance, 4200);
    assert.equal(summary.categoryTotals.size, 0);
});

