// Local WeChat developer-tool simulator. Change for a deployed backend.
const BASE_URL = 'http://127.0.0.1:18081'

function request({ path, method = 'GET', data = {} }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      method, data, timeout: 10000,
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) resolve(response.data)
        else reject(new Error(`HTTP ${response.statusCode}`))
      },
      fail: reject,
    })
  })
}

module.exports = { request }
