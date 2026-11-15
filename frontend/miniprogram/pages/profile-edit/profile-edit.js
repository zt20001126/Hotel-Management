const app = getApp()

Page({
  data: {
    nickname: '',
    nicknameFocused: false,
    loaded: false,
    loading: false
  },

  onLoad() {
    this.loadUserInfo()
  },

  async loadUserInfo() {
    try {
      const res = await app.request({ url: '/guest/current' })
      const userInfo = res.data || {}
      this.setData({ nickname: userInfo.nickname || '', loaded: true })
    } catch (err) {
      console.error(err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  editNickname() {
    this.setData({ nicknameFocused: true })
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  onNicknameBlur() {
    this.setData({ nicknameFocused: false })
  },

  async save() {
    if (this.data.loading || !this.data.loaded) return
    const { nickname } = this.data
    this.setData({ loading: true })
    try {
      await app.request({
        url: '/guest',
        method: 'PUT',
        data: { nickname }
      })

      const updatedInfo = { ...app.globalData.userInfo, nickname }
      app.login(app.globalData.token, updatedInfo)
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (err) {
      console.error(err)
      this.setData({ loading: false })
    }
  }
})
