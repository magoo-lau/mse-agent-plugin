/** 是否运行在主应用（wujie）中 */
export function embedded(): boolean {
  return !!window.__POWERED_BY_WUJIE__
}
