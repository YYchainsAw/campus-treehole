const { request } = require('../../utils/request')

const PAGE_SIZE = 20

function displayTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    content: '', posts: [], loading: false, loadingMore: false,
    submitting: false, loadError: false, hasMore: false, nextPage: 0,
  },

  onLoad() {
    this.requestVersion = 0
    this.firstShow = true
    this.loadPosts(true)
  },

  onShow() {
    if (this.firstShow) {
      this.firstShow = false
      return
    }
    this.loadPosts(true)
  },

  openMyPosts() {
    wx.navigateTo({ url: '/pages/mine/mine' })
  },

  onPullDownRefresh() {
    this.loadPosts(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.loadPosts(false)
  },

  onContentInput(event) {
    this.setData({ content: event.detail.value })
  },

  retry() { this.loadPosts(true) },

  async loadPosts(reset) {
    if (!reset && (this.data.loading || this.data.loadingMore || !this.data.hasMore)) return
    const page = reset ? 0 : this.data.nextPage
    const version = reset ? ++this.requestVersion : this.requestVersion
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false } : { loadingMore: true })
    try {
      const result = await request({ path: '/api/posts', data: { page, size: PAGE_SIZE } })
      if (version !== this.requestVersion) return
      if (!result || !Array.isArray(result.items) || typeof result.total !== 'number') throw new Error('Invalid list response')
      const items = result.items.map(item => ({ ...item, displayTime: displayTime(item.createdAt) }))
      const posts = reset ? items : this.data.posts.concat(items)
      this.setData({ posts, nextPage: page + 1, hasMore: items.length > 0 && posts.length < result.total, loadError: false })
    } catch (error) {
      if (version !== this.requestVersion) return
      if (reset) this.setData({ loadError: true })
      else wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    } finally {
      if (version === this.requestVersion) this.setData({ loading: false, loadingMore: false })
    }
  },

  async publish() {
    if (this.data.submitting) return
    const content = this.data.content.trim()
    if (!content) {
      wx.showToast({ title: '请输入树洞内容', icon: 'none' })
      return
    }
    if (content.length > 1000) {
      wx.showToast({ title: '内容不能超过 1000 字', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await request({ path: '/api/posts', method: 'POST', data: { content } })
      this.setData({ content: '' })
      wx.showToast({ title: '发布成功', icon: 'success' })
      await this.loadPosts(true)
    } catch (error) {
      wx.showToast({ title: '发布失败，请稍后重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
