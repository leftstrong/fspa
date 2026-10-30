// 用 HBuilderX 自带的 uni-app 编译器构建 fspa_uni 的 H5 生产包
// 等价于 HBuilderX 的「发行 → 网站-H5」，但不依赖 GUI，命令行/CI 都能跑
//
// 用法：bash deploy/h5/build-h5.sh
//       本文件需要 HBuilderX 自带的 node 执行（见 build-h5.sh）

const path = require('path')
const fs = require('fs')

// 仓库根目录 = 本文件上两级（deploy/h5 → 仓库根）
const ROOT = path.resolve(__dirname, '..', '..')

// 当前 node 位于 <HBuilderX>/plugins/node/node.exe，编译器插件在上一级
const CLI_DIR = path.resolve(path.dirname(process.execPath), '..', 'uniapp-cli')
const CLI_BIN = path.join(CLI_DIR, 'bin', 'uniapp-cli.js')

if (!fs.existsSync(CLI_BIN)) {
  console.error('[x] 没找到 HBuilderX 的 uni-app 编译器：' + CLI_DIR)
  console.error('    本脚本必须用 HBuilderX 自带的 node 运行，请用 deploy/h5/build-h5.sh。')
  process.exit(2)
}

const INPUT_DIR = path.join(ROOT, 'fspa_uni')
const OUTPUT_DIR = path.join(INPUT_DIR, 'unpackage', 'dist', 'build', 'h5')

// 这些环境变量必须在加载编译器之前就位（编译器的 lib/env.js 会读一次）
process.env.UNI_INPUT_DIR = INPUT_DIR
process.env.UNI_OUTPUT_DIR = OUTPUT_DIR
process.env.UNI_PLATFORM = 'h5'
process.env.NODE_ENV = 'production'
process.env.VUE_CLI_CONTEXT = CLI_DIR

// 项目自己的 vue.config.js 交给 vue-cli-service
const vueConfig = path.join(INPUT_DIR, 'vue.config.js')
if (fs.existsSync(vueConfig)) {
  process.env.VUE_CLI_SERVICE_CONFIG_PATH = vueConfig
}

// 清掉上一轮产物，否则会累积旧 hash 的分块文件，镜像体积虚高
// 目录被占用（本地预览服务开着）时只告警，不中断构建
try {
  fs.rmSync(OUTPUT_DIR, { recursive: true, force: true })
} catch (e) {
  console.warn('[!] 旧产物没能删除（可能被占用）：' + String(e.message).split('\n')[0])
  console.warn('    继续构建，产物里可能残留上一轮的旧分块文件。')
}

console.log('源目录   ' + INPUT_DIR)
console.log('输出目录 ' + OUTPUT_DIR)
console.log('编译器   ' + CLI_DIR + '\n')

// 编译完成后自动退出，避免命令行一直挂着
require('./exit-after-build')

// 编译器按相对当前目录解析模板与 babel 配置，必须在它的目录里运行
process.chdir(CLI_DIR)
require(CLI_BIN)
