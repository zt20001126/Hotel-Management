import request from '@/utils/request'

export const getExpenses = params => request.get('/operating-expenses', { params })
export const getExpenseStatistics = params => request.get('/operating-expenses/statistics', { params })
export const createExpense = data => request.post('/operating-expenses', data)
export const updateExpense = (id, data) => request.put(`/operating-expenses/${id}`, data)
export const deleteExpense = id => request.delete(`/operating-expenses/${id}`)
