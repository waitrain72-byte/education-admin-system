import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { t } from '@/i18n'
import ExamCountdownTag from '@/components/ExamCountdownTag.vue'

afterEach(() => {
  vi.useRealTimers()
})

describe('ExamCountdownTag 考试倒计时标签', () => {
  it('页面开着跨过零点：标签从「明天」自动变成「今天」', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 8, 25, 23, 58, 30))
    const wrapper = mount(ExamCountdownTag, {
      props: { examTime: '2026-09-26 09:00' },
      global: { plugins: [ElementPlus] },
    })

    // 共享时钟每分钟刷新一次
    await vi.advanceTimersByTimeAsync(60 * 1000)
    expect(wrapper.text()).toBe(t('pages.examplan.countdownTomorrow'))

    await vi.advanceTimersByTimeAsync(60 * 1000)
    expect(wrapper.text()).toBe(t('pages.examplan.countdownToday'))
  })

  it('没有考试时间或格式不对时不渲染标签', () => {
    for (const examTime of [null, '', '2026-02-30 09:00']) {
      const wrapper = mount(ExamCountdownTag, { props: { examTime }, global: { plugins: [ElementPlus] } })
      expect(wrapper.find('.el-tag').exists()).toBe(false)
    }
  })
})
