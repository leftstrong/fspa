<template>
  <div class="divBox dashboard" v-loading="loading">
    <!-- ==================== 顶部：核心指标卡 ==================== -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in mainCards" :key="card.key">
        <div class="kpi" :class="card.tone">
          <div class="kpi-main">
            <div class="kpi-label">{{ card.label }}</div>
            <div class="kpi-value">{{ fmt(card.value) }}</div>
          </div>
          <div class="kpi-foot">
            <span class="kpi-today">今日 +{{ fmt(card.today) }}</span>
            <span class="kpi-extra" v-if="card.extra">{{ card.extra }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ==================== 次级指标 ==================== -->
    <el-row :gutter="16" class="mt16">
      <el-col :span="6" v-for="card in subCards" :key="card.key">
        <div class="sub-kpi">
          <span class="sub-label">{{ card.label }}</span>
          <span class="sub-value">{{ fmt(card.value) }}</span>
          <span class="sub-unit">{{ card.unit }}</span>
        </div>
      </el-col>
    </el-row>

    <!-- ==================== 趋势曲线 ==================== -->
    <el-card class="box-card mt16" shadow="never">
      <div slot="header" class="clearfix">
        <span>趋势曲线</span>
        <div class="head-ops">
          <el-radio-group v-model="days" size="mini" @change="loadTrend">
            <el-radio-button :label="7">近 7 天</el-radio-button>
            <el-radio-button :label="14">近 14 天</el-radio-button>
            <el-radio-button :label="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div ref="trendChart" class="chart chart-lg"></div>
    </el-card>

    <!-- ==================== 分布 ==================== -->
    <el-row :gutter="16" class="mt16">
      <el-col :span="10">
        <el-card class="box-card" shadow="never">
          <div slot="header"><span>物品状态分布</span></div>
          <div ref="itemChart" class="chart chart-md"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="box-card" shadow="never">
          <div slot="header"><span>换物单状态分布</span></div>
          <div ref="orderChart" class="chart chart-md"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 品类 + 快捷入口 ==================== -->
    <el-row :gutter="16" class="mt16">
      <el-col :span="16">
        <el-card class="box-card" shadow="never">
          <div slot="header"><span>热门品类（按物品件数）</span></div>
          <div ref="catChart" class="chart chart-md"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="box-card" shadow="never">
          <div slot="header"><span>待处理与快捷入口</span></div>
          <div class="todo" v-for="t in todoList" :key="t.label" @click="go(t.path)">
            <span class="todo-label">{{ t.label }}</span>
            <span class="todo-value" :class="{ warn: t.warn }">{{ fmt(t.value) }}</span>
            <i class="el-icon-arrow-right"></i>
          </div>
          <div class="quick">
            <el-button type="primary" size="mini" icon="el-icon-s-check" @click="go('/barter/item/index')">物品审核</el-button>
            <el-button size="mini" icon="el-icon-s-order" @click="go('/barter/order/index')">换物单</el-button>
            <el-button size="mini" icon="el-icon-s-custom" @click="go('/barter/member/index')">换物用户</el-button>
            <el-button size="mini" icon="el-icon-setting" @click="go('/barter/config/index')">规则配置</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import echarts from 'echarts'
import {
  barterDashboardSummaryApi,
  barterDashboardTrendApi,
  barterDashboardDistributeApi
} from '@/api/barter'

const TONE = { primary: 'tone-primary', green: 'tone-green', orange: 'tone-orange', blue: 'tone-blue' }

export default {
  name: 'BarterDashboard',
  data() {
    return {
      loading: false,
      days: 14,
      summary: {},
      charts: {}
    }
  },
  computed: {
    mainCards() {
      const s = this.summary
      return [
        { key: 'user', label: '换物用户', value: s.userTotal, today: s.userToday, tone: TONE.green,
          extra: `人均交换 ${s.avgExchange || 0} 次` },
        { key: 'item', label: '物品总量', value: s.itemTotal, today: s.itemToday, tone: TONE.blue,
          extra: `在售 ${s.itemOnSale || 0} / 待审 ${s.itemAuditing || 0}` },
        { key: 'order', label: '换物单', value: s.orderTotal, today: s.orderToday, tone: TONE.primary,
          extra: `成交率 ${s.dealRate || 0}%` },
        { key: 'msg', label: '换物留言', value: s.messageTotal, today: s.messageToday, tone: TONE.orange,
          extra: `站内通知 ${s.notifyTotal || 0} 条` }
      ]
    },
    subCards() {
      const s = this.summary
      return [
        { key: 'trading', label: '进行中换物单', value: s.orderTrading, unit: '单' },
        { key: 'completed', label: '已完成换物单', value: s.orderCompleted, unit: '单' },
        { key: 'credit', label: '平均信用分', value: s.creditAvg, unit: '分' },
        { key: 'points', label: '流通积分余额', value: s.pointsBalance, unit: '分' }
      ]
    },
    todoList() {
      const s = this.summary
      return [
        { label: '待审核物品', value: s.itemAuditing, path: '/barter/item/index', warn: (s.itemAuditing || 0) > 0 },
        { label: '待处理举报', value: s.reportPending, path: '/barter/report/index', warn: (s.reportPending || 0) > 0 },
        { label: '纠纷中换物单', value: s.orderDispute, path: '/barter/order/index', warn: (s.orderDispute || 0) > 0 },
        { label: '信用分低于 60 用户', value: s.creditLowCount, path: '/barter/member/index', warn: false }
      ]
    }
  },
  mounted() {
    this.initCharts()
    this.loadAll()
    window.addEventListener('resize', this.onResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.onResize)
    Object.keys(this.charts).forEach(k => this.charts[k] && this.charts[k].dispose())
    this.charts = {}
  },
  methods: {
    fmt(v) {
      if (v === null || v === undefined || v === '') return 0
      return v
    },
    go(path) {
      this.$router.push(path).catch(() => {})
    },
    initCharts() {
      this.charts.trend = echarts.init(this.$refs.trendChart)
      this.charts.item = echarts.init(this.$refs.itemChart)
      this.charts.order = echarts.init(this.$refs.orderChart)
      this.charts.cat = echarts.init(this.$refs.catChart)
    },
    onResize() {
      Object.keys(this.charts).forEach(k => this.charts[k] && this.charts[k].resize())
    },
    loadAll() {
      this.loading = true
      Promise.all([
        barterDashboardSummaryApi().catch(() => ({})),
        barterDashboardTrendApi(this.days).catch(() => null),
        barterDashboardDistributeApi().catch(() => null)
      ]).then(([summary, trend, dist]) => {
        this.summary = summary || {}
        if (trend) this.renderTrend(trend)
        if (dist) {
          this.renderItemPie(dist.itemStatus || [])
          this.renderOrderBar(dist.orderStatus || [])
          this.renderCategory(dist.category || [])
        }
        this.loading = false
        this.$nextTick(this.onResize)
      }).catch(() => { this.loading = false })
    },
    loadTrend() {
      barterDashboardTrendApi(this.days).then(res => {
        if (res) this.renderTrend(res)
      }).catch(() => {})
    },
    /* ---------------- 图表渲染 ---------------- */
    renderTrend(d) {
      const c = this.charts.trend
      if (!c) return
      c.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['新增物品', '新增换物单', '新增留言', '新增用户'], bottom: 0 },
        grid: { left: 40, right: 24, top: 24, bottom: 56 },
        xAxis: { type: 'category', boundaryGap: false, data: d.dates || [], axisLine: { lineStyle: { color: '#ddd' } } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } } },
        series: [
          this.lineSeries('新增物品', d.item, '#409eff'),
          this.lineSeries('新增换物单', d.order, '#2f9e44'),
          this.lineSeries('新增留言', d.message, '#e6a23c'),
          this.lineSeries('新增用户', d.user, '#909399')
        ]
      }, true)
    },
    lineSeries(name, data, color) {
      return {
        name,
        type: 'line',
        smooth: true,
        symbolSize: 5,
        data: data || [],
        itemStyle: { color },
        lineStyle: { color, width: 2 },
        areaStyle: { color, opacity: 0.06 }
      }
    },
    renderItemPie(list) {
      const c = this.charts.item
      if (!c) return
      const colors = ['#2f9e44', '#e6a23c', '#f56c6c', '#409eff', '#909399', '#c0c4cc']
      c.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          label: { show: true, formatter: '{b}\n{c}' },
          data: (list || []).map((x, i) => ({ name: x.name, value: x.value, itemStyle: { color: colors[i % colors.length] } }))
        }]
      }, true)
    },
    renderOrderBar(list) {
      const c = this.charts.order
      if (!c) return
      const data = list || []
      c.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 48, right: 24, top: 24, bottom: 32 },
        xAxis: { type: 'category', data: data.map(x => x.name), axisLabel: { interval: 0 } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } } },
        series: [{
          type: 'bar',
          barWidth: '46%',
          data: data.map(x => x.value),
          itemStyle: {
            color: '#2f9e44',
            borderRadius: [4, 4, 0, 0]
          },
          label: { show: true, position: 'top', color: '#666' }
        }]
      }, true)
    },
    renderCategory(list) {
      const c = this.charts.cat
      if (!c) return
      const data = (list || []).slice().reverse()
      c.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 90, right: 40, top: 16, bottom: 24 },
        xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } } },
        yAxis: { type: 'category', data: data.map(x => x.name) },
        series: [{
          type: 'bar',
          barWidth: '50%',
          data: data.map(x => x.value),
          itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right', color: '#666' }
        }]
      }, true)
    }
  }
}
</script>

<style scoped>
.dashboard { padding-bottom: 24px; }
.mt16 { margin-top: 16px; }
.head-ops { float: right; }

.kpi {
  border-radius: 8px;
  padding: 16px 18px;
  color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .06);
}
.tone-primary { background: linear-gradient(135deg, #3d7eff 0%, #2b5fd9 100%); }
.tone-green { background: linear-gradient(135deg, #34b36a 0%, #237a46 100%); }
.tone-orange { background: linear-gradient(135deg, #f0a63a 0%, #d1801c 100%); }
.tone-blue { background: linear-gradient(135deg, #4aa8d8 0%, #2c7ba8 100%); }

.kpi-main { display: flex; align-items: baseline; justify-content: space-between; }
.kpi-label { font-size: 13px; opacity: .9; }
.kpi-value { font-size: 28px; font-weight: 700; letter-spacing: .5px; }
.kpi-foot { margin-top: 10px; display: flex; justify-content: space-between; font-size: 12px; opacity: .92; }
.kpi-extra { opacity: .85; }

.sub-kpi {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: baseline;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .05);
}
.sub-label { font-size: 13px; color: #666; flex: 1; }
.sub-value { font-size: 22px; font-weight: 700; color: #2f9e44; }
.sub-unit { font-size: 12px; color: #999; margin-left: 4px; }

.chart { width: 100%; }
.chart-lg { height: 320px; }
.chart-md { height: 260px; }

.todo {
  display: flex;
  align-items: center;
  padding: 10px 4px;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
}
.todo:hover { background: #fafafa; }
.todo-label { flex: 1; font-size: 13px; color: #555; }
.todo-value { font-size: 16px; font-weight: 600; color: #333; margin-right: 8px; }
.todo-value.warn { color: #e6a23c; }
.quick { margin-top: 14px; }
.quick .el-button { margin-bottom: 8px; }
</style>
