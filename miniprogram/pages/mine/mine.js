const { request } = require('../../utils/request')

const PAGE_SIZE = 20

function displayTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    posts: [], loading: false, loadingMore: false, loadError: false,
    hasMore: false, nextPage: 0, deleting: false, deletingId: null,
  },

  onLoad() {
    this.requestVersion = 0
    this.loadPosts(true)
  },

  onPullDownRefresh() {
    this.loadPosts(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.loadPosts(false)
  },

  retry() { this.loadPosts(true) },

  async loadPosts(reset) {
    if (!reset && (this.data.loading || this.data.loadingMore || !this.data.hasMore)) return
    const page = reset ? 0 : this.data.nextPage
    const version = reset ? ++this.requestVersion : this.requestVersion
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false } : { loadingMore: true })
    try {
      const result = await request({ path: '/api/posts/mine', data: { page, size: PAGE_SIZE } })
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

  confirmDelete(event) {
    if (this.data.deleting) return
    const id = event.currentTarget.dataset.id
    this.setData({ deleting: true, deletingId: id })
    wx.showModal({
      title: '删除树洞',
      content: '删除后无法恢复，确定删除这条树洞吗？',
      confirmText: '删除',
      confirmColor: '#a74d48',
      success: async (result) => {
        try {
          if (!result.confirm) return
          await request({ path: `/api/posts/${id}`, method: 'DELETE' })
          this.setData({ posts: this.data.posts.filter(post => post.id !== id) })
          wx.showToast({ title: '已删除', icon: 'success' })
          await this.loadPosts(true)
        } catch (error) {
          wx.showToast({ title: '删除失败，请重试', icon: 'none' })
        } finally {
          this.setData({ deleting: false, deletingId: null })
        }
      },
      fail: () => {
        this.setData({ deleting: false, deletingId: null })
        wx.showToast({ title: '无法打开确认框', icon: 'none' })
      },
    })
  },
})
