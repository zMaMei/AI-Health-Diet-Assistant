/**
 * 重新计算 6.4 ~ 6.20 所有日期的营养分析 + 健康评分。
 * NutritionService.getDaily() 和 HealthScoreService.getDailyScore()
 * 都会在查询时自动重算并保存到 nutrition_record 表。
 *
 * Usage: node recalculate.mjs
 * Requires server on localhost:8080
 */

const BASE = 'http://localhost:8080'
const USERNAME = 'root'
const PASSWORD = '123456'

// ── login ────────────────────────────────────────────────
const t = await (await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
})).json()
if (t.code !== 200) {
  console.error('登录失败:', JSON.stringify(t))
  process.exit(1)
}
const TOKEN = t.data.token
const auth = { 'Authorization': `Bearer ${TOKEN}` }
console.log(`✅ 已登录 ${USERNAME} (id=${t.data.userId})\n`)

// ── recalculate ──────────────────────────────────────────
const start = new Date(2026, 5, 4)   // June 4
const end = new Date(2026, 5, 20)    // June 20
const format = d => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

let total = 0
for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
  const dateStr = format(d)

  // 触发营养分析（自动重算并保存 nutrition_record）
  const nutRes = await fetch(`${BASE}/api/nutrition/daily?date=${dateStr}`, { headers: auth })
  const nutData = await nutRes.json()
  const nut = nutData.data

  // 触发健康评分（自动重算评分并回写 nutrition_record）
  const scoreRes = await fetch(`${BASE}/api/health-score/daily?date=${dateStr}`, { headers: auth })
  const scoreData = await scoreRes.json()
  const score = scoreData.data

  const cal = nut?.calorieTotal ? Number(nut.calorieTotal).toFixed(0) : '0'
  const s = score?.score != null ? Number(score.score).toFixed(1) + '分' : '不足2餐'
  const strengths = score?.strengths?.length || 0
  const risks = score?.risks?.length || 0
  console.log(`${dateStr} | 🔥${cal}kcal | ⭐${s} | ✅${strengths} ⚠️${risks}`)
  total++
}

console.log(`\n✅ 完成，共 ${total} 天`)
