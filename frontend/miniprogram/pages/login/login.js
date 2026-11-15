const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    showLogo: true
  },

  onLogoError() {
    // 如果logo加载失败，隐藏图片
    this.setData({ showLogo: false })
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  async login() {
    const { username, password } = this.data
    if (!username) { wx.showToast({ title: '请输入用户名', icon: 'none' }); return }
    if (!password) { wx.showToast({ title: '请输入密码', icon: 'none' }); return }

    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: '/auth/employee/login',
        method: 'POST',
        data: { username, password }
      })
      app.login(res.data.token, res.data.userInfo)
      wx.showToast({ title: '登录成功', icon: 'success' })
      
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/employee/workbench/workbench' })
      }, 1000)
    } catch (err) {
      console.error(err)
    } finally {
      this.setData({ loading: false })
    }
  },

  async onGetPhoneNumber(event) {
    const phoneCode = event.detail && event.detail.code
    if (!phoneCode) {
      wx.showToast({ title: '需要授权手机号才能登录', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      const loginResult = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: reject
        })
      })
      if (!loginResult.code) {
        throw new Error('未能获取微信登录凭证')
      }

      const result = await app.request({
        url: '/auth/wechat/phone-login',
        method: 'POST',
        data: {
          loginCode: loginResult.code,
          phoneCode
        }
      })
      app.login(result.data.token, result.data.userInfo)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800)
    } catch (error) {
      console.error('微信手机号登录失败', error)
    } finally {
      this.setData({ loading: false })
    }
  }
})
