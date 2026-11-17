const { request, getToken, setToken, clearToken, EXPIRED_MARKER } = require('./request')

let ongoingLogin = null

function saveSession(result) {
  if (!result || typeof result.token !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(result.token)
      || typeof result.expiresAt !== 'string' || !Number.isFinite(Date.parse(result.expiresAt))) {
    throw new Error('登录响应无效，请稍后重试')
  }
  try {
    setToken(result.token)
  } catch (_) {
    throw new Error('无法保存登录状态，请检查小程序存储权限')
  }
  return { expiresAt: result.expiresAt }
}

function getWechatCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(result) {
        if (result && typeof result.code === 'string' && result.code) resolve(result.code)
        else reject(new Error('微信登录凭证获取失败，请检查小程序账号配置'))
      },
      fail() {
        reject(new Error('微信登录失败，请检查网络和小程序账号配置'))
      },
    })
  })
}

function login() {
  if (ongoingLogin) return ongoingLogin
  ongoingLogin = (async () => {
    const code = await getWechatCode()
    let result
    try {
      // Do not attach a previous token while exchanging a fresh code.
      result = await request({ path: '/api/auth/wechat', method: 'POST', data: { code }, skipAuth: true })
    } catch (error) {
      if (error.statusCode === 503) throw new Error('微信登录暂不可用，请检查后端的 AppID 和 AppSecret 配置')
      throw new Error('微信登录失败，请检查账号配置或稍后重试')
    }
    return saveSession(result)
  })().finally(() => { ongoingLogin = null })
  return ongoingLogin
}

async function loginWithAccount(username, password, register = false) {
  if (ongoingLogin) throw new Error('正在登录，请稍候')
  ongoingLogin = (async () => {
    const data = { username, password }
    if (register) {
      try {
        await request({ path: '/api/auth/local/register', method: 'POST', data, skipAuth: true })
      } catch (error) {
        if (error.statusCode === 409) throw new Error('账号已存在，请直接登录')
        if (error.statusCode === 400) throw new Error('账号或密码格式不正确')
        throw new Error('注册失败，请检查后端是否启动本地模式')
      }
    }
    let result
    try {
      result = await request({ path: '/api/auth/local/login', method: 'POST', data, skipAuth: true })
    } catch (error) {
      if (register) {
        const failure = new Error('注册成功，请重新登录')
        failure.registered = true
        throw failure
      }
      if (error.statusCode === 401) throw new Error('账号或密码错误')
      if (error.statusCode === 400) throw new Error('账号或密码格式不正确')
      throw new Error('登录失败，请检查后端是否启动本地模式')
    }
    return saveSession(result)
  })().finally(() => { ongoingLogin = null })
  return ongoingLogin
}

function isLoggedIn() {
  const token = getToken()
  return Boolean(token && token !== EXPIRED_MARKER)
}

function sessionState() {
  const token = getToken()
  if (token === EXPIRED_MARKER) return 'expired'
  return token ? 'loggedIn' : 'local'
}

async function logout() {
  const token = getToken()
  if (!token) return
  if (token === EXPIRED_MARKER) {
    clearToken(token)
    return
  }
  await request({ path: '/api/auth/logout', method: 'POST' })
  clearToken(token)
}

module.exports = { login, loginWithAccount, isLoggedIn, sessionState, logout }
