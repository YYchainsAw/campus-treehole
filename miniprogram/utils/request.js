// Local WeChat developer-tool simulator. Change for a deployed backend.
const BASE_URL = 'http://127.0.0.1:18081'
const TOKEN_KEY = 'campus_treehole_session'
const EXPIRED_MARKER = 'expired'

function getToken() {
  const token = wx.getStorageSync(TOKEN_KEY)
  return typeof token === 'string' ? token : ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

function clearToken(expectedToken) {
  if (expectedToken && getToken() !== expectedToken) return
  wx.removeStorageSync(TOKEN_KEY)
}

function request({ path, method = 'GET', data = {}, skipAuth = false }) {
  const identity = getToken()
  if (!skipAuth && identity === EXPIRED_MARKER) {
    const error = new Error('登录已失效，请重新登录或退出登录')
    error.statusCode = 401
    return Promise.reject(error)
  }
  const token = skipAuth ? '' : identity
  const header = token ? { Authorization: `Bearer ${token}` } : {}
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      method, data, header, timeout: 10000,
      success(response) {
        // Results from a previous identity must not update the current page.
        if (!skipAuth && getToken() !== identity) {
          const error = new Error('登录状态已变化，请重试')
          error.identityChanged = true
          reject(error)
          return
        }
        if (response.statusCode >= 200 && response.statusCode < 300) resolve(response.data)
        else {
          // An older 401 must not clear a newer login session.
          if (response.statusCode === 401 && token) {
            try {
              if (getToken() === token) wx.setStorageSync(TOKEN_KEY, EXPIRED_MARKER)
            } catch (_) {
              // The request still fails; no automatic retry with another identity.
            }
          }
          const error = new Error(`HTTP ${response.statusCode}`)
          error.statusCode = response.statusCode
          reject(error)
        }
      },
      fail: reject,
    })
  })
}

module.exports = { request, getToken, setToken, clearToken, EXPIRED_MARKER }
