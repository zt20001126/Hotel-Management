Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/rooms/rooms', text: '房型', icon: '/images/tab-room.png', activeIcon: '/images/tab-room-active.png' },
      { pagePath: '/pages/profile/profile', text: '我的', icon: '/images/tab-user.png', activeIcon: '/images/tab-user-active.png' }
    ]
  },
  pageLifetimes: {
    show() {
      const pages = getCurrentPages()
      const route = pages[pages.length - 1].route
      const selected = this.data.list.findIndex(item => item.pagePath === `/${route}`)
      if (selected >= 0) this.setData({ selected })
    }
  },
  methods: {
    switchTab(e) {
      const path = e.currentTarget.dataset.path
      if (!path) return
      wx.switchTab({
        url: path,
        fail(err) {
          console.error('TabBar 页面切换失败:', path, err)
        }
      })
    }
  }
})
