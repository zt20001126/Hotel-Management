const app = getApp()

Page({
  data: {
    roomTypes: [],
    loading: false,
    isEmpty: false
  },

  onLoad() {
    this._searchRequestId = 0
    this.loadRoomTypes()
  },
  
  onShow() {
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 0 })
    // 房型页始终展示所有上架房型，不受首页日期搜索条件影响。
    app.globalData.searchParams = null
  },

  async loadRoomTypes() {
    const requestId = ++this._searchRequestId
    this.setData({ loading: true, isEmpty: false })
    
    try {
      const res = await app.request({
        url: '/roomType/available',
        method: 'GET'
      })
      
      // 解析图片列表
      const roomTypes = (res.data || []).map(roomType => {
        if (roomType.images) {
          try {
            roomType.images = typeof roomType.images === 'string' 
              ? JSON.parse(roomType.images) 
              : roomType.images
          } catch (e) {
            roomType.images = []
          }
        } else {
          roomType.images = []
        }
        // 如果没有图片，使用网络占位图
        if (!roomType.images || roomType.images.length === 0) {
          roomType.images = ['https://dummyimage.com/400x300/667eea/ffffff.png&text=Room']
        }
        return roomType
      })
      
      if (requestId !== this._searchRequestId) return
      
      this.setData({ 
        roomTypes,
        isEmpty: roomTypes.length === 0,
        loading: false
      })
      
    } catch (err) {
      if (requestId !== this._searchRequestId) return
      console.error('加载房型失败:', err)
      this.setData({ loading: false, isEmpty: true })
      wx.showToast({ 
        title: err.message || '搜索失败，请稍后重试', 
        icon: 'none',
        duration: 2000
      })
    }
  },

  onImageError(e) {
    const index = e.currentTarget.dataset.index
    const roomTypes = this.data.roomTypes
    if (roomTypes[index]) {
      roomTypes[index].images[0] = 'https://dummyimage.com/400x300/667eea/ffffff.png&text=Room'
      this.setData({ roomTypes })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/room-detail/room-detail?id=${id}`
    })
  }
})
