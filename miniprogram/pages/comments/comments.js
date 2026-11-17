const { request } = require('../../utils/request')

const PAGE_SIZE = 20

Page({
  data: {
    postId: '', content: '', comments: [], loading: false, loadingMore: false,
    loadError: false, submitting: false, hasMore: false, nextPage: 0,
  },

  onLoad(options) {
    this.requestVersion = 0
    const postId = String(options.postId || '')
    if (!/^[1-9]\d*$/.test(postId)) {
      this.setData({ loadError: true })
      return
    }
    this.setData({ postId })
    this.loadComments(true)
  },

  onPullDownRefresh() {
    this.loadComments(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadComments(false)
  },

  onContentInput(event) { this.setData({ content: event.detail.value }) },
  retry() { this.loadComments(true) },

  async loadComments(reset) {
    if (!this.data.postId || (!reset && (this.data.loading || this.data.loadingMore || !this.data.hasMore))) return
    const page = reset ? 0 : this.data.nextPage
    const version = reset ? ++this.requestVersion : this.requestVersion
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false } : { loadingMore: true })
    try {
      const result = await request({ path: `/api/posts/${this.data.postId}/comments`, data: { page, size: PAGE_SIZE } })
      if (version !== this.requestVersion) return
      if (!result || !Array.isArray(result.items) || typeof result.total !== 'number') throw new Error('Invalid response')
      const items = result.items.map(item => ({ ...item, displayTime: String(item.createdAt || '').replace('T', ' ').slice(0, 16) }))
      const comments = reset ? items : this.data.comments.concat(items)
      this.setData({ comments, nextPage: page + 1, hasMore: items.length > 0 && comments.length < result.total, loadError: false })
    } catch (error) {
      if (version !== this.requestVersion) return
      if (reset) this.setData({ loadError: true })
      else wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    } finally {
      if (version === this.requestVersion) this.setData({ loading: false, loadingMore: false })
    }
  },

  async submit() {
    if (this.data.submitting || !this.data.postId) return
    const content = this.data.content.trim()
    if (!content || content.length > 500) {
      wx.showToast({ title: '请输入 1 至 500 字的评论', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await request({ path: `/api/posts/${this.data.postId}/comments`, method: 'POST', data: { content } })
      this.setData({ content: '' })
      wx.showToast({ title: '评论成功', icon: 'success' })
      await this.loadComments(true)
    } catch (error) {
      if (!error.identityChanged) wx.showToast({ title: error.statusCode === 401 ? '请先登录' : '评论失败，请重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
