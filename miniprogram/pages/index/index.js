const { request } = require('../../utils/request')
const { toggleLike, mergeLikeResults } = require('../../utils/likes')

const PAGE_SIZE = 20
const CATEGORY_LABELS = { study: '学习', life: '生活', mood: '心情', help: '求助', other: '其他' }
const CATEGORIES = Object.keys(CATEGORY_LABELS).map(value => ({ value, label: CATEGORY_LABELS[value] }))

function displayTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    content: '', posts: [], loading: false, loadingMore: false,
    submitting: false, loadError: false, hasMore: false, nextPage: 0, pendingLikes: {},
    categories: CATEGORIES, filterCategories: [{ value: '', label: '全部' }, ...CATEGORIES],
    category: 'other', filterCategory: '',
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

  openComments(event) {
    wx.navigateTo({ url: `/pages/comments/comments?postId=${event.currentTarget.dataset.id}` })
  },

  chooseCategory(event) {
    const category = event.currentTarget.dataset.category
    if (CATEGORY_LABELS[category] && !this.data.submitting) this.setData({ category })
  },

  filterPosts(event) {
    const category = event.currentTarget.dataset.category
    if (category !== '' && !CATEGORY_LABELS[category]) return
    if (category === this.data.filterCategory) return
    this.setData({ filterCategory: category, posts: [], hasMore: false, nextPage: 0 })
    this.loadPosts(true)
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

  toggleLike,

  async loadPosts(reset) {
    if (!reset && (this.data.loading || this.data.loadingMore || !this.data.hasMore)) return
    const page = reset ? 0 : this.data.nextPage
    const version = reset ? ++this.requestVersion : this.requestVersion
    const likesRevision = this.likesRevision || 0
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false } : { loadingMore: true })
    try {
      const data = { page, size: PAGE_SIZE }
      if (this.data.filterCategory) data.category = this.data.filterCategory
      const result = await request({ path: '/api/posts', data })
      if (version !== this.requestVersion) return
      if (!result || !Array.isArray(result.items) || typeof result.total !== 'number') throw new Error('Invalid list response')
      const items = mergeLikeResults(this, result.items, likesRevision)
        .map(item => ({ ...item, categoryLabel: CATEGORY_LABELS[item.category] || '其他', displayTime: displayTime(item.createdAt) }))
      const posts = reset ? items : this.data.posts.concat(items)
      this.setData({ posts, nextPage: page + 1, hasMore: items.length > 0 && posts.length < result.total, loadError: false })
    } catch (error) {
      if (version !== this.requestVersion) return
      if (reset) this.setData({ loadError: true, loginExpired: error.statusCode === 401 })
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
      await request({ path: '/api/posts', method: 'POST', data: { content, category: this.data.category } })
      this.setData({ content: '', filterCategory: '' })
      wx.showToast({ title: '发布成功', icon: 'success' })
      await this.loadPosts(true)
    } catch (error) {
      if (!error.identityChanged) wx.showToast({ title: error.statusCode === 401 ? '登录已失效，请重新登录' : '发布失败，请稍后重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
