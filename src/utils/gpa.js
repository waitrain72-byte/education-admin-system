/**
 * 学生成绩汇总（「我的成绩」页顶部）：平均学分绩点、加权平均分、已获学分、不及格门数。
 * 绩点换算采用国内高校常用的五分制线性规则：及格（≥60）时 绩点 =（成绩 − 50）÷ 10，
 * 即 60 分 1.0、90 分 4.0、100 分 5.0，不及格记 0。各校规则不同，改 gradePoint 一处即可。
 */

/** 及格线 */
export const PASS_SCORE = 60

const toNumber = (v) => (v === null || v === undefined || v === '' ? NaN : Number(v))

/** 单门课程绩点（非数值返回 0） */
export function gradePoint(score) {
  const s = toNumber(score)
  if (!Number.isFinite(s) || s < PASS_SCORE) return 0
  return Math.min(5, (s - 50) / 10)
}

const round = (n, digits) => Math.round(n * 10 ** digits) / 10 ** digits

/**
 * 汇总成绩。
 * @param scores 成绩记录：[{ courseId, score }]（score 为总评，未出总评的记录不计入）
 * @param credits 学分表：{ [courseId]: 学分 }
 * @returns {{ courseCount, failedCount, earnedCredits, gpa, weightedAvg }}
 *   缺学分（或学分为 0）的课程只计入门数与不及格数，不参与加权；没有可加权的课程时 gpa / weightedAvg 为 null
 */
export function summarizeScores(scores, credits) {
  let courseCount = 0
  let failedCount = 0
  let earnedCredits = 0
  let creditSum = 0
  let pointSum = 0
  let scoreSum = 0
  for (const row of scores || []) {
    const s = toNumber(row && row.score)
    if (!Number.isFinite(s)) continue
    courseCount += 1
    if (s < PASS_SCORE) failedCount += 1
    const credit = toNumber(credits && credits[row.courseId])
    if (!Number.isFinite(credit) || credit <= 0) continue
    creditSum += credit
    pointSum += gradePoint(s) * credit
    scoreSum += s * credit
    if (s >= PASS_SCORE) earnedCredits += credit
  }
  return {
    courseCount,
    failedCount,
    earnedCredits,
    gpa: creditSum ? round(pointSum / creditSum, 2) : null,
    weightedAvg: creditSum ? round(scoreSum / creditSum, 1) : null,
  }
}
