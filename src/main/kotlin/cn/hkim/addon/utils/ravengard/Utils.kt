package cn.hkim.addon.utils.ravengard

inline val inRavengardInstance: Boolean
    get() = InstanceState.current.isValid
