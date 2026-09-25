import { describe, expect, it } from 'vitest'
import { gradePoint, summarizeScores } from '@/utils/gpa'

describe('gradePoint 单门绩点（五分制线性）', () => {
  it('及格线与满分边界', () => {
    expect(gradePoint(59.9)).toBe(0)
    expect(gradePoint(60)).toBe(1)
    expect(gradePoint(85)).toBe(3.5)
    expect(gradePoint(100)).toBe(5)
    expect(gradePoint(120)).toBe(5)
  })

  it('非数值记 0（未出总评、脏数据）', () => {
    expect(gradePoint(null)).toBe(0)
    expect(gradePoint('')).toBe(0)
    expect(gradePoint('abc')).toBe(0)
    expect(gradePoint('75')).toBe(2.5)
  })
})

describe('summarizeScores 成绩汇总', () => {
  it('按学分加权：绩点、加权平均分、已获学分只算及格课程', () => {
    const scores = [
      { courseId: 1, score: 90 },
      { courseId: 2, score: 70 },
      { courseId: 3, score: 50 },
    ]
    const r = summarizeScores(scores, { 1: 4, 2: 2, 3: 2 })
    // 绩点：(4.0×4 + 2.0×2 + 0×2) / 8 = 2.5；平均分：(360 + 140 + 100) / 8 = 75
    expect(r).toEqual({ courseCount: 3, failedCount: 1, earnedCredits: 6, gpa: 2.5, weightedAvg: 75 })
  })

  it('缺学分的课程只计门数与不及格，不参与加权；未出总评的记录跳过', () => {
    const r = summarizeScores(
      [
        { courseId: 1, score: 88 },
        { courseId: 9, score: 40 },
        { courseId: 2, score: null },
      ],
      { 1: 3 },
    )
    expect(r.courseCount).toBe(2)
    expect(r.failedCount).toBe(1)
    expect(r.earnedCredits).toBe(3)
    expect(r.gpa).toBe(3.8)
    expect(r.weightedAvg).toBe(88)
  })

  it('没有可加权的课程时 gpa / 平均分为 null', () => {
    expect(summarizeScores([], {})).toEqual({
      courseCount: 0,
      failedCount: 0,
      earnedCredits: 0,
      gpa: null,
      weightedAvg: null,
    })
    expect(summarizeScores([{ courseId: 1, score: 80 }], {}).gpa).toBe(null)
  })

  it('保留小数：绩点两位、平均分一位', () => {
    const r = summarizeScores(
      [
        { courseId: 1, score: 83 },
        { courseId: 2, score: 76 },
        { courseId: 3, score: 91 },
      ],
      { 1: 3, 2: 2, 3: 2 },
    )
    // 绩点：(3.3×3 + 2.6×2 + 4.1×2) / 7 = 23.3 / 7 ≈ 3.33；平均分：(249 + 152 + 182) / 7 ≈ 83.3
    expect(r.gpa).toBe(3.33)
    expect(r.weightedAvg).toBe(83.3)
  })
})
