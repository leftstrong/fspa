// 预加载模块：让 HBuilderX 的 uni-app 编译进程在 webpack 结束后自动退出
//
// 背景：<HBuilderX>/plugins/uniapp-cli/bin/uniapp-cli.js 编译完成后不退出进程
//      （webpack 残留句柄 + terser 缓存清理子进程），命令行里会一直挂着。
// 做法：拦截 require('webpack')，在 compiler 的 done 钩子（产物已落盘）里退出。
//       done 之后还有 800ms 余量，留给写盘收尾。

const Module = require('module')
const origLoad = Module._load

Module._load = function (request, parent, isMain) {
  const mod = origLoad.apply(this, arguments)
  if (request !== 'webpack' || typeof mod !== 'function' || mod.__exitHooked) {
    return mod
  }
  mod.__exitHooked = true

  const wrapped = function () {
    const compiler = mod.apply(this, arguments)
    if (compiler && compiler.hooks && compiler.hooks.done) {
      compiler.hooks.done.tap('wb-auto-exit', stats => {
        const failed = stats && typeof stats.hasErrors === 'function' && stats.hasErrors()
        console.log(failed ? '\nBUILD_FAIL' : '\nBUILD_OK')
        setTimeout(() => process.exit(failed ? 1 : 0), 800)
      })
    }
    return compiler
  }

  Object.assign(wrapped, mod)
  return wrapped
}
