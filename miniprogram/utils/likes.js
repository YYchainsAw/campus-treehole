const { request } = require('./request')

// The per-page lock survives list refreshes and prevents double taps.
async function toggleLike(event) {
  const id = event.currentTarget.dataset.id
  const post = this.data.posts.find(item => item.id === id)
  if (!post) return

  this.activeLikes = this.activeLikes || new Set()
  if (this.activeLikes.has(id)) return
  this.activeLikes.add(id)
  this.setData({ pendingLikes: { ...this.data.pendingLikes, [id]: true } })

  try {
    const result = await request({
      path: `/api/posts/${id}/likes`,
      method: post.likedByMe ? 'DELETE' : 'POST',
    })
    if (!result || !Number.isInteger(result.likeCount) || result.likeCount < 0 || typeof result.likedByMe !== 'boolean') {
      throw new Error('Invalid like response')
    }
    // A list request that started earlier may still return older like data.
    // Keep this response until a list request started after it has returned.
    this.likesRevision = (this.likesRevision || 0) + 1
    this.likeOverrides = this.likeOverrides || {}
    this.likeOverrides[id] = {
      revision: this.likesRevision,
      likeCount: result.likeCount,
      likedByMe: result.likedByMe,
    }
    this.setData({
      posts: this.data.posts.map(item => item.id === id
        ? { ...item, likeCount: result.likeCount, likedByMe: result.likedByMe }
        : item),
    })
  } catch (error) {
    if (error.statusCode === 401 && this.onAuthExpired) this.onAuthExpired()
    if (!error.identityChanged) wx.showToast({ title: error.statusCode === 401 ? '登录已失效，请重新登录' : '操作失败，请稍后重试', icon: 'none' })
  } finally {
    this.activeLikes.delete(id)
    const pendingLikes = { ...this.data.pendingLikes }
    delete pendingLikes[id]
    this.setData({ pendingLikes })
  }
}

function mergeLikeResults(page, items, requestRevision) {
  const overrides = page.likeOverrides || {}
  return items.map(item => {
    const override = overrides[item.id]
    if (!override) return item
    if (override.revision > requestRevision) {
      return { ...item, likeCount: override.likeCount, likedByMe: override.likedByMe }
    }
    delete overrides[item.id]
    return item
  })
}

module.exports = { toggleLike, mergeLikeResults }
