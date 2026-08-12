import request from './request'

export const getCaptcha = () => request.get('/auth/captcha')
export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/users/me')
