import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'
import { SKELETON_DELAY, SKELETON_MIN, useSkeleton } from '@/composables/useSkeleton'

describe('useSkeleton 骨架屏显示时机', () => {
  const loading = ref(false)
  let scope
  let show

  const finishAfter = async (ms) => {
    loading.value = true
    await nextTick()
    vi.advanceTimersByTime(ms)
    loading.value = false
    await nextTick()
  }

  beforeEach(() => {
    vi.useFakeTimers()
    loading.value = false
    scope = effectScope()
    show = scope.run(() => useSkeleton(() => loading.value))
  })

  afterEach(() => {
    scope.stop()
    vi.useRealTimers()
  })

  it('开始等数据时立刻打开', async () => {
    loading.value = true
    await nextTick()
    expect(show.value).toBe(true)
  })

  it('骨架还没露面就加载完：立刻关掉，不闪', async () => {
    await finishAfter(SKELETON_DELAY - 50)
    expect(show.value).toBe(false)
  })

  it('骨架刚露面就加载完：至少停留 SKELETON_MIN 再关', async () => {
    await finishAfter(SKELETON_DELAY + 100)
    expect(show.value).toBe(true)
    vi.advanceTimersByTime(SKELETON_MIN - 100 - 1)
    expect(show.value).toBe(true)
    vi.advanceTimersByTime(1)
    expect(show.value).toBe(false)
  })

  it('骨架已经露够了：加载完立刻关', async () => {
    await finishAfter(SKELETON_DELAY + SKELETON_MIN + 300)
    expect(show.value).toBe(false)
  })

  it('等最短停留时又开始加载：保持打开，不被旧计时器关掉', async () => {
    await finishAfter(SKELETON_DELAY + 100)
    loading.value = true
    await nextTick()
    vi.advanceTimersByTime(SKELETON_MIN)
    expect(show.value).toBe(true)
  })

  it('初始就在加载时直接为真', () => {
    loading.value = true
    const other = scope.run(() => useSkeleton(() => loading.value))
    expect(other.value).toBe(true)
  })
})
