const { request } = require('../../utils/request')
const { toggleLike, mergeLikeResults } = require('../../utils/likes')
const auth = require('../../utils/auth')

const PAGE_SIZE = 20
const CATEGORY_LABELS = { study: '学习', life: '生活', mood: '心情', help: '求助', other: '其他' }

function displayTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    posts: [], loading: false, loadingMore: false, loadError: false,
    hasMore: false, nextPage: 0, deleting: false, deletingId: null, pendingLikes: {},
    sessionState: 'local', signingIn: false, accountMode: '', username: '', password: '',
  },

  onLoad() {
    this.requestVersion = 0
    this.setData({ sessionState: auth.sessionState() })
    this.loadPosts(true)
  },

  async signIn() {
    if (this.data.signingIn || this.data.deleting || Object.keys(this.data.pendingLikes).length) return
    this.setData({ signingIn: true })
    try {
      await auth.login()
      this.likeOverrides = {}
      this.setData({ sessionState: auth.sessionState(), posts: [], hasMore: false })
      await this.loadPosts(true)
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message || '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ signingIn: false })
    }
  },

  showAccountLogin() { this.setData({ accountMode: 'login', password: '' }) },
  showAccountRegister() { this.setData({ accountMode: 'register', password: '' }) },
  hideAccountForm() { this.setData({ accountMode: '', password: '' }) },
  onUsernameInput(event) { this.setData({ username: event.detail.value }) },
  onPasswordInput(event) { this.setData({ password: event.detail.value }) },

  async submitAccount() {
    if (this.data.signingIn || this.data.deleting || Object.keys(this.data.pendingLikes).length) return
    const username = this.data.username.trim()
    const password = this.data.password
    if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) {
      wx.showToast({ title: '账号需为3至32位字母、数字或下划线', icon: 'none' })
      return
    }
    if (password.length < 6 || password.length > 128) {
      wx.showToast({ title: '密码长度需为6至128位', icon: 'none' })
      return
    }
    this.setData({ signingIn: true })
    try {
      await auth.loginWithAccount(username, password, this.data.accountMode === 'register')
      this.likeOverrides = {}
      this.setData({ sessionState: auth.sessionState(), accountMode: '', password: '', posts: [], hasMore: false })
      await this.loadPosts(true)
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (error) {
      if (error.registered) this.setData({ accountMode: 'login' })
      wx.showToast({ title: error.message || '操作失败，请重试', icon: 'none' })
    } finally {
      this.setData({ signingIn: false })
    }
  },

  signOut() {
    if (this.data.signingIn || this.data.deleting || Object.keys(this.data.pendingLikes).length) return
    this.setData({ signingIn: true })
    wx.showModal({
      title: '退出登录',
      content: '退出后，本地开发环境会恢复测试身份。确定继续吗？',
      success: async (result) => {
        if (!result.confirm) {
          this.setData({ signingIn: false })
          return
        }
        try {
          await auth.logout()
          this.likeOverrides = {}
          this.setData({ sessionState: auth.sessionState(), posts: [], hasMore: false })
          await this.loadPosts(true)
          wx.showToast({ title: '已退出登录', icon: 'success' })
        } catch (error) {
          this.setData({ sessionState: auth.sessionState() })
          wx.showToast({ title: '退出失败，请重试', icon: 'none' })
        } finally {
          this.setData({ signingIn: false })
        }
      },
      fail: () => this.setData({ signingIn: false }),
    })
  },

  onAuthExpired() {
    this.requestVersion++
    this.likeOverrides = {}
    this.setData({ sessionState: auth.sessionState(), posts: [], hasMore: false, loading: false, loadingMore: false, loadError: true })
  },

  onPullDownRefresh() {
    this.loadPosts(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.loadPosts(false)
  },

  retry() { this.loadPosts(true) },

  openComments(event) {
    wx.navigateTo({ url: `/pages/comments/comments?postId=${event.currentTarget.dataset.id}` })
  },

  toggleLike,

  async loadPosts(reset) {
    if (!reset && (this.data.loading || this.data.loadingMore || !this.data.hasMore)) return
    const page = reset ? 0 : this.data.nextPage
    const version = reset ? ++this.requestVersion : this.requestVersion
    const likesRevision = this.likesRevision || 0
    this.setData(reset ? { loading: true, loadingMore: false, loadError: false } : { loadingMore: true })
    try {
      const result = await request({ path: '/api/posts/mine', data: { page, size: PAGE_SIZE } })
      if (version !== this.requestVersion) return
      if (!result || !Array.isArray(result.items) || typeof result.total !== 'number') throw new Error('Invalid list response')
      const items = mergeLikeResults(this, result.items, likesRevision)
        .map(item => ({ ...item, categoryLabel: CATEGORY_LABELS[item.category] || '其他', displayTime: displayTime(item.createdAt) }))
      const posts = reset ? items : this.data.posts.concat(items)
      this.setData({ posts, nextPage: page + 1, hasMore: items.length > 0 && posts.length < result.total, loadError: false })
    } catch (error) {
      if (version !== this.requestVersion) return
      if (error.statusCode === 401) this.onAuthExpired()
      else this.setData({ sessionState: auth.sessionState() })
      if (reset) this.setData({ loadError: true })
      else wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    } finally {
      if (version === this.requestVersion) this.setData({ loading: false, loadingMore: false })
    }
  },

  confirmDelete(event) {
    if (this.data.deleting || this.data.signingIn) return
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
          if (error.statusCode === 401) this.onAuthExpired()
          if (!error.identityChanged) wx.showToast({ title: error.statusCode === 401 ? '登录已失效，请重新登录' : '删除失败，请重试', icon: 'none' })
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
