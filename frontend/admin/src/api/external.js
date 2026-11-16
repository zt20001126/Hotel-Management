import request from '@/utils/request'

export const getPlatforms = params => request.get('/external/platforms', { params })
export const savePlatform = (id, data) => id ? request.put(`/external/platforms/${id}`, data) : request.post('/external/platforms', data)
export const getAnchors = params => request.get('/external/anchors', { params })
export const saveAnchor = (id, data) => id ? request.put(`/external/anchors/${id}`, data) : request.post('/external/anchors', data)
export const getOrders = params => request.get('/external/orders', { params })
export const getUnifiedOrders = params => request.get('/external/unified-orders', { params })
export const getOrder = id => request.get(`/external/orders/${id}`)
export const saveOrder = (id, data) => id ? request.put(`/external/orders/${id}`, data) : request.post('/external/orders', data)
export const setOrderStatus = (id, status) => request.post(`/external/orders/${id}/status`, { status })
export const getCalendar = params => request.get('/external/calendar', { params })
export const getStatistics = params => request.get('/external/statistics', { params })
