// 旧版默认头像仍可能保存在用户资料中，统一映射到当前默认头像。
const DEFAULT_AVATAR = '/images/default-user-avatar.webp'
const LEGACY_DEFAULT_AVATAR = '/images/avatar-ink.png'

function resolveAvatar(avatar) {
  return !avatar || avatar === LEGACY_DEFAULT_AVATAR ? DEFAULT_AVATAR : avatar
}

module.exports = { DEFAULT_AVATAR, resolveAvatar }
