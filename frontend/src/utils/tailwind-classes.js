// 常用的 Tailwind CSS 类名组合
// 用于在 Vue 组件中快速引用

export const layout = {
  // 容器
  container: 'w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8',
  containerFull: 'w-full px-4 sm:px-6 lg:px-8',
  
  // Flex 布局
  flex: 'flex',
  flexCol: 'flex flex-col',
  flexCenter: 'flex items-center justify-center',
  flexBetween: 'flex items-center justify-between',
  flexAround: 'flex items-center justify-around',
  flexStart: 'flex items-center justify-start',
  flexEnd: 'flex items-center justify-end',
  
  // Grid 布局
  grid2: 'grid grid-cols-2 gap-4',
  grid3: 'grid grid-cols-3 gap-4',
  grid4: 'grid grid-cols-4 gap-4',
}

export const spacing = {
  // 内边距
  p0: 'p-0',
  p1: 'p-1',
  p2: 'p-2',
  p3: 'p-3',
  p4: 'p-4',
  p6: 'p-6',
  p8: 'p-8',
  
  // 外边距
  m0: 'm-0',
  m1: 'm-1',
  m2: 'm-2',
  m3: 'm-3',
  m4: 'm-4',
  m6: 'm-6',
  m8: 'm-8',
  
  // 水平间距
  gap2: 'gap-2',
  gap3: 'gap-3',
  gap4: 'gap-4',
  gap6: 'gap-6',
  gap8: 'gap-8',
}

export const typography = {
  // 标题
  h1: 'text-4xl font-bold text-gray-900',
  h2: 'text-3xl font-bold text-gray-900',
  h3: 'text-2xl font-bold text-gray-900',
  h4: 'text-xl font-bold text-gray-900',
  h5: 'text-lg font-bold text-gray-900',
  h6: 'text-base font-bold text-gray-900',
  
  // 正文
  textSm: 'text-sm text-gray-600',
  textBase: 'text-base text-gray-700',
  textLg: 'text-lg text-gray-700',
  
  // 辅助文字
  textXs: 'text-xs text-gray-500',
  textMuted: 'text-sm text-gray-500',
}

export const colors = {
  // 主色
  primary: 'text-primary-600',
  primaryBg: 'bg-primary-500',
  primaryBgHover: 'hover:bg-primary-600',
  
  // 成功色
  success: 'text-success-600',
  successBg: 'bg-success-500',
  successBgHover: 'hover:bg-success-600',
  
  // 警告色
  warning: 'text-warning-600',
  warningBg: 'bg-warning-500',
  warningBgHover: 'hover:bg-warning-600',
  
  // 危险色
  danger: 'text-danger-600',
  dangerBg: 'bg-danger-500',
  dangerBgHover: 'hover:bg-danger-600',
}

export const components = {
  // 按钮
  btn: 'px-4 py-2 rounded-lg font-medium transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2',
  btnPrimary: 'px-4 py-2 bg-primary-500 text-white rounded-lg font-medium hover:bg-primary-600 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2',
  btnSecondary: 'px-4 py-2 bg-gray-100 text-gray-700 rounded-lg font-medium hover:bg-gray-200 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2',
  btnDanger: 'px-4 py-2 bg-danger-500 text-white rounded-lg font-medium hover:bg-danger-600 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-danger-500 focus:ring-offset-2',
  btnGhost: 'px-4 py-2 text-gray-600 rounded-lg font-medium hover:bg-gray-100 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2',
  
  // 卡片
  card: 'bg-white rounded-xl shadow-sm border border-gray-200 p-6',
  cardHover: 'bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow duration-200',
  
  // 输入框
  input: 'w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-200',
  
  // 标签
  tag: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
  tagPrimary: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-800',
  tagSuccess: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-success-100 text-success-800',
  tagWarning: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-warning-100 text-warning-800',
  tagDanger: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-danger-100 text-danger-800',
}

export const effects = {
  // 阴影
  shadowSm: 'shadow-sm',
  shadow: 'shadow',
  shadowMd: 'shadow-md',
  shadowLg: 'shadow-lg',
  shadowXl: 'shadow-xl',
  shadowNone: 'shadow-none',
  
  // 圆角
  roundedNone: 'rounded-none',
  roundedSm: 'rounded-sm',
  rounded: 'rounded',
  roundedMd: 'rounded-md',
  roundedLg: 'rounded-lg',
  roundedXl: 'rounded-xl',
  rounded2xl: 'rounded-2xl',
  roundedFull: 'rounded-full',
  
  // 过渡
  transition: 'transition-all duration-200',
  transitionFast: 'transition-all duration-150',
  transitionSlow: 'transition-all duration-300',
}

// 导出默认对象，方便整体导入
export default {
  layout,
  spacing,
  typography,
  colors,
  components,
  effects,
}
