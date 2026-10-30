<template>
  <div class="divBox">
    <el-card class="box-card" shadow="never" v-loading="loading">
      <div slot="header" class="clearfix">
        <span>换物规则配置</span>
        <span class="sub">物为人用，方为良物</span>
        <div class="head-ops">
          <el-button size="mini" @click="resetDefault">恢复默认值</el-button>
          <el-button size="mini" @click="loadConfig">重新加载</el-button>
          <el-button type="primary" size="mini" :loading="saving" @click="saveConfig">保存配置</el-button>
        </div>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="mb18"
        title="说明"
        description="配置项存储于 eb_system_config，后端按 key 实时读取，保存后立即生效、无需重启。留空或未配置的项会自动回落到代码内置默认值，不会报错。" />

      <el-tabs v-model="activeTab">
        <!-- ==================== 积分规则 ==================== -->
        <el-tab-pane label="积分规则" name="points">
          <el-alert type="success" :closable="false" show-icon class="mb18"
            title="积分的来源与去向"
            description="用户通过注册、首发好物、换物成交获得积分；置顶物品、智能换物路径费消耗积分。改小获得值或改大消耗值都会让积分更紧张，请结合运营目标调整。" />
          <el-form label-width="220px" size="small">
            <el-form-item v-for="item in groups.points" :key="item.key" :label="item.label">
              <el-input v-model="form[item.key]" type="number" style="width:200px;">
                <template slot="append">{{ item.unit }}</template>
              </el-input>
              <span class="tip">{{ item.tip }}</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 智能换物 ==================== -->
        <el-tab-pane label="智能换物" name="chain">
          <el-alert type="warning" :closable="false" show-icon class="mb18"
            title="路径规划费按「成交方案人数」分档收取"
            description="检索与出方案始终免费，只有第一棒真正成交时才扣费；方案失败、被拒、取消都不收费。2 人直接互换建议保持 0，用于鼓励最简单的互换。" />
          <el-form label-width="220px" size="small">
            <el-form-item v-for="item in groups.chain" :key="item.key" :label="item.label">
              <el-input v-model="form[item.key]" type="number" style="width:200px;">
                <template slot="append">{{ item.unit }}</template>
              </el-input>
              <span class="tip">{{ item.tip }}</span>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" class="mt12"
            :title="chainPreview" />
        </el-tab-pane>

        <!-- ==================== 信用分 ==================== -->
        <el-tab-pane label="信用分" name="credit">
          <el-alert type="info" :closable="false" show-icon class="mb18"
            title="信用分只增不减到 0 为止"
            description="信用分用于详情页展示与撮合排序参考。差评扣分不会让分数变成负数；运营可在这里调整每次行为的加减幅度。" />
          <el-form label-width="220px" size="small">
            <el-form-item v-for="item in groups.credit" :key="item.key" :label="item.label">
              <el-input v-model="form[item.key]" type="number" style="width:200px;">
                <template slot="append">{{ item.unit }}</template>
              </el-input>
              <span class="tip">{{ item.tip }}</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 时效与履约 ==================== -->
        <el-tab-pane label="时效与履约" name="expire">
          <el-alert type="warning" :closable="false" show-icon class="mb18"
            title="超时规则由定时任务执行，建议不要小于 24 小时"
            description="申请超时、收货超时由后端定时任务扫描处理；改单号窗口用于给填错单号的用户一个短期自救机会，超过次数或时间后只能联系客服。" />
          <el-form label-width="220px" size="small">
            <el-form-item v-for="item in groups.expire" :key="item.key" :label="item.label">
              <el-input v-model="form[item.key]" type="number" style="width:200px;">
                <template slot="append">{{ item.unit }}</template>
              </el-input>
              <span class="tip">{{ item.tip }}</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 订阅消息 ==================== -->
        <el-tab-pane label="订阅消息" name="tpl">
          <el-alert type="warning" :closable="false" show-icon class="mb18"
            title="模板 ID 来自微信小程序后台"
            description="填写后对应节点自动推送；留空则跳过该节点推送，不影响主流程（站内消息仍会照常发送）。" />
          <el-form label-width="220px" size="small">
            <el-form-item v-for="item in groups.tpl" :key="item.key" :label="item.label">
              <el-input v-model="form[item.key]" placeholder="留空则不推送" clearable style="width:380px;" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { barterConfigGetApi, barterConfigSaveApi } from '@/api/barter'

/**
 * 规则配置定义
 * unit  显示在输入框尾部
 * def   内置默认值（与后端 BarterConstants 保持一致），用于「未配置」时兜底展示
 * tip   一句话说明，讲清「改大了会怎样」
 */
const GROUP_DEFS = {
  points: [
    { key: 'barter_points_register', label: '新用户注册奖励', unit: '积分', def: '50', tip: '注册即送，是新人第一次换物的启动资金' },
    { key: 'barter_points_first_publish', label: '首发好物奖励', unit: '积分', def: '50', tip: '首件物品审核通过后发放，每人仅一次' },
    { key: 'barter_points_per_deal', label: '换物成交奖励', unit: '积分', def: '10', tip: '每完成一单，双方各得' },
    { key: 'barter_top_points_per_hour', label: '置顶消耗', unit: '积分/小时', def: '1', tip: '置于首页顶部所需积分，按时长计价' },
    { key: 'barter_top_duration_hour', label: '置顶时长', unit: '小时', def: '24', tip: '一次置顶持续多久，24 小时 = 1 天' }
  ],
  chain: [
    { key: 'barter_path_fee_2p', label: '路径费 · 2 人直接互换', unit: '积分', def: '0', tip: '撮合最简单，建议保持 0' },
    { key: 'barter_path_fee_3p', label: '路径费 · 3 人接力', unit: '积分', def: '10', tip: '需要 1 位中间人' },
    { key: 'barter_path_fee_4_6p', label: '路径费 · 4-6 人接力', unit: '积分', def: '50', tip: '需要 2-4 位中间人，撮合成本最高' },
    { key: 'barter_chain_max_hops', label: '最大跳数', unit: '跳', def: '5', tip: '跳数 = 人数 - 1，5 跳即最多 6 人；调大更可能成环，但检索更慢' }
  ],
  credit: [
    { key: 'barter_credit_per_deal', label: '换物成交加分', unit: '分', def: '5', tip: '每完成一单，双方各加' },
    { key: 'barter_credit_good', label: '互评好评加分', unit: '分', def: '2', tip: '收到 4-5 星评价时，被评人加分' },
    { key: 'barter_credit_bad', label: '互评差评扣分', unit: '分', def: '3', tip: '收到 1-2 星评价时，被评人扣分（最低扣到 0）' }
  ],
  expire: [
    { key: 'barter_apply_expire_hour', label: '换物申请超时关闭', unit: '小时', def: '48', tip: '发起后对方一直不确认，到时自动关闭' },
    { key: 'barter_recv_expire_hour', label: '发货后自动确认收货', unit: '小时', def: '168', tip: '168 小时 = 7 天，避免对方拖延不点收货' },
    { key: 'barter_express_edit_hours', label: '可自行改单号时长', unit: '小时', def: '24', tip: '发货后多久内允许自己改单号' },
    { key: 'barter_express_edit_max', label: '最多改单号次数', unit: '次', def: '3', tip: '同一侧最多改几次，防止反复变更让对方查不到' }
  ],
  tpl: [
    { key: 'barter_tpl_apply_created', label: '申请创建通知', def: '' },
    { key: 'barter_tpl_apply_accepted', label: '申请通过通知', def: '' },
    { key: 'barter_tpl_order_delivered', label: '已发货/待取通知', def: '' },
    { key: 'barter_tpl_order_completed', label: '换物完成通知', def: '' },
    { key: 'barter_tpl_order_cancelled', label: '换物取消通知', def: '' }
  ]
}

const ALL_DEFS = Object.keys(GROUP_DEFS).reduce((acc, k) => acc.concat(GROUP_DEFS[k]), [])

export default {
  name: 'BarterConfig',
  data() {
    const form = {}
    ALL_DEFS.forEach(item => { form[item.key] = item.def })
    return {
      groups: GROUP_DEFS,
      activeTab: 'points',
      form,
      loading: false,
      saving: false
    }
  },
  computed: {
    // 实时预览三档定价，避免运营改了数字却不知道前台会怎么显示
    chainPreview() {
      const f2 = this.form['barter_path_fee_2p']
      const f3 = this.form['barter_path_fee_3p']
      const f46 = this.form['barter_path_fee_4_6p']
      const hops = this.form['barter_chain_max_hops']
      const show = v => (v === '' || v === null || v === undefined) ? '-' : v
      return `前台档位卡将显示：2 人 ${show(f2)} 积分 · 3 人 ${show(f3)} 积分 · 4-${Number(hops || 0) + 1} 人 ${show(f46)} 积分（最多 ${Number(hops || 0) + 1} 人）`
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    // 读取配置：后端未配置（返回 null/空）时回落到默认值
    async loadConfig() {
      this.loading = true
      const keys = ALL_DEFS.map(item => item.key)
      try {
        const values = await Promise.all(keys.map(k => barterConfigGetApi(k).catch(() => null)))
        keys.forEach((k, i) => {
          const v = values[i]
          this.$set(this.form, k, (v === null || v === undefined || v === '') ? this.defaultOf(k) : String(v))
        })
      } finally {
        this.loading = false
      }
    },
    defaultOf(key) {
      const hit = ALL_DEFS.find(item => item.key === key)
      return hit ? hit.def : ''
    },
    // 只重置表单，不落库；误点后可以「重新加载」撤回
    resetDefault() {
      ALL_DEFS.forEach(item => { this.$set(this.form, item.key, item.def) })
      this.$message.info('已恢复为默认值，点击「保存配置」后生效')
    },
    // 保存：逐项调用 saveuniq（后端为 @RequestParam，故用 params）
    async saveConfig() {
      this.saving = true
      const keys = ALL_DEFS.map(item => item.key)
      try {
        for (const k of keys) {
          const v = this.form[k]
          await barterConfigSaveApi(k, v === null || v === undefined ? '' : String(v))
        }
        this.$message.success('配置已保存并立即生效')
        this.loadConfig()
      } catch (e) {
        this.$message.error('保存失败，请稍后重试')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.sub { font-size: 12px; color: #999; margin-left: 12px; }
.head-ops { float: right; }
.mb18 { margin-bottom: 18px; }
.mt12 { margin-top: 12px; }
.tip { margin-left: 12px; font-size: 12px; color: #999; }
</style>
