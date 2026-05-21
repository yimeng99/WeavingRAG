import { message as antdMessage, notification, Modal } from 'ant-design-vue'

// 全局 message 工具类
export const message = {
  /**
   * 成功提示
   * @param {string} content - 提示内容
   * @param {number} duration - 持续时间（秒）
   */
  success(content, duration = 2) {
    return antdMessage.success(content, duration)
  },

  /**
   * 错误提示
   * @param {string} content - 提示内容
   * @param {number} duration - 持续时间（秒）
   */
  error(content, duration = 3) {
    return antdMessage.error(content, duration)
  },

  /**
   * 警告提示
   * @param {string} content - 提示内容
   * @param {number} duration - 持续时间（秒）
   */
  warning(content, duration = 2) {
    return antdMessage.warning(content, duration)
  },

  /**
   * 信息提示
   * @param {string} content - 提示内容
   * @param {number} duration - 持续时间（秒）
   */
  info(content, duration = 2) {
    return antdMessage.info(content, duration)
  },

  /**
   * 加载提示
   * @param {string} content - 提示内容
   * @returns {object} 包含 destroy 方法的对象，用于手动关闭
   */
  loading(content = '加载中...') {
    return antdMessage.loading(content, 0)
  },

  /**
   * 移除提示
   * @param {string} key - 消息 key
   */
  destroy(key) {
    antdMessage.destroy(key)
  },

  /**
   * 清空所有提示
   */
  destroyAll() {
    antdMessage.destroyAll()
  }
}

// 全局 notification 工具类
export const notify = {
  /**
   * 成功通知
   * @param {object} options - 配置项 { message, description, duration }
   */
  success(options) {
    const { message: msg, description, duration = 4.5 } = typeof options === 'string' ? { message: options } : options
    return antdMessage.success({
      content: msg,
      description,
      duration
    })
  },

  /**
   * 错误通知
   * @param {object} options - 配置项 { message, description, duration }
   */
  error(options) {
    const { message: msg, description, duration = 4.5 } = typeof options === 'string' ? { message: options } : options
    return notification.error({
      message: msg,
      description,
      duration
    })
  },

  /**
   * 警告通知
   * @param {object} options - 配置项 { message, description, duration }
   */
  warning(options) {
    const { message: msg, description, duration = 4.5 } = typeof options === 'string' ? { message: options } : options
    return notification.warning({
      message: msg,
      description,
      duration
    })
  },

  /**
   * 信息通知
   * @param {object} options - 配置项 { message, description, duration }
   */
  info(options) {
    const { message: msg, description, duration = 4.5 } = typeof options === 'string' ? { message: options } : options
    return notification.info({
      message: msg,
      description,
      duration
    })
  },

  /**
   * 打开通知
   * @param {object} options - 配置项 { message, description, type, duration, onClose }
   */
  open(options) {
    return notification.open(options)
  },

  /**
   * 关闭通知
   * @param {string} key - 通知 key
   */
  close(key) {
    notification.close(key)
  },

  /**
   * 清空所有通知
   */
  destroyAll() {
    notification.destroyAll()
  }
}

// 全局 confirm 工具类
export const confirm = {
  /**
   * 确认对话框
   * @param {object} options - 配置项 { title, content, onOk, onCancel }
   */
  show(options = {}) {
    return Modal.confirm({
      centered: true,
      maskClosable: false,
      ...options
    })
  },

  /**
   * 成功确认
   * @param {object} options - 配置项 { title, content, onOk }
   */
  success(options = {}) {
    return Modal.success({
      centered: true,
      maskClosable: false,
      ...options
    })
  },

  /**
   * 错误确认
   * @param {object} options - 配置项 { title, content, onOk }
   */
  error(options = {}) {
    return Modal.error({
      centered: true,
      maskClosable: false,
      ...options
    })
  },

  /**
   * 警告确认
   * @param {object} options - 配置项 { title, content, onOk }
   */
  warning(options = {}) {
    return Modal.warning({
      centered: true,
      maskClosable: false,
      ...options
    })
  },

  /**
   * 信息确认
   * @param {object} options - 配置项 { title, content, onOk }
   */
  info(options = {}) {
    return Modal.info({
      centered: true,
      maskClosable: false,
      ...options
    })
  }
}

// 导出所有工具
export default {
  message,
  notify,
  confirm
}
