<template>
  <div class="divBox">
    <!-- ==================== 筛选 ==================== -->
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="search">
      <el-form-item label="用户">
        <el-input v-model="dataForm.keyword" placeholder="UID / 手机号 / 昵称" class="selWidth" size="small" clearable></el-input>
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="dataForm.city" placeholder="如 杭州市" class="selSmall" size="small" clearable></el-input>
      </el-form-item>
      <el-form-item label="等级">
        <el-select v-model="dataForm.levelId" placeholder="全部" class="selSmall" clearable size="small" @change="search">
          <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="dataForm.sort" class="selSmall" size="small" @change="search">
          <el-option label="最新注册" :value="1"></el-option>
          <el-option label="积分最多" :value="2"></el-option>
          <el-option label="信用最高" :value="3"></el-option>
          <el-option label="交换最多" :value="4"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" icon="el-icon-search" @click="search">查询</el-button>
        <el-button size="small" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 列表 ==================== -->
    <el-table :data="dataList" border v-loading="loading" style="width: 100%;">
      <el-table-column label="用户" min-width="180">
        <template slot-scope="scope">
          <div class="user-cell">
            <img :src="imgUrl(scope.row.avatar)" class="avatar" />
            <div>
              <div class="u-name">{{ scope.row.nickname || '未命名' }}</div>
              <div class="u-uid">UID {{ scope.row.uid }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" align="center">
        <template slot-scope="scope">{{ scope.row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="city" label="城市" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.city || '-' }}</template>
      </el-table-column>
      <el-table-column label="等级" width="110" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.levelName" size="mini" type="success">{{ scope.row.levelName }}</el-tag>
          <span v-else class="muted">未定级</span>
        </template>
      </el-table-column>
      <el-table-column label="交换次数" width="95" align="center">
        <template slot-scope="scope">{{ scope.row.exchangeCount }}</template>
      </el-table-column>
      <el-table-column label="积分" width="120" align="center">
        <template slot-scope="scope">
          <span class="num">{{ scope.row.points }}</span>
          <span v-if="scope.row.pointsFrozen > 0" class="frozen">冻结 {{ scope.row.pointsFrozen }}</span>
        </template>
      </el-table-column>
      <el-table-column label="信用分" width="95" align="center">
        <template slot-scope="scope">
          <span class="credit" :class="{ low: scope.row.creditScore < 60 }">{{ scope.row.creditScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'danger'">{{ scope.row.status === 1 ? '正常' : '已禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160" align="center"></el-table-column>
      <el-table-column fixed="right" label="操作" width="140" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openDetail(scope.row)">详情</el-button>
          <el-button type="text" size="small" @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="dataForm.page"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="dataForm.limit"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper">
    </el-pagination>

    <!-- ==================== 详情抽屉 ==================== -->
    <el-drawer title="换物用户详情" :visible.sync="detailVisible" size="720px" :destroy-on-close="true">
      <div class="drawer-body" v-loading="detailLoading" v-if="detail">
        <div class="profile">
          <img :src="imgUrl(detail.stat.avatar)" class="p-avatar" />
          <div class="p-main">
            <div class="p-name">
              {{ detail.stat.nickname || '未命名' }}
              <el-tag v-if="detail.stat.levelName" size="mini" type="success">{{ detail.stat.levelName }}</el-tag>
              <el-tag size="mini" :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '正常' : '已禁用' }}</el-tag>
            </div>
            <div class="p-sub">UID {{ detail.stat.uid }} · {{ detail.phone || '未绑定手机' }} · {{ detail.stat.city || '未填城市' }}</div>
            <div class="p-sub">注册于 {{ detail.createTime }}</div>
          </div>
        </div>

        <el-row :gutter="12" class="mt12">
          <el-col :span="6"><div class="mini"><div class="mini-v">{{ detail.stat.exchangeCount || 0 }}</div><div class="mini-l">成功交换</div></div></el-col>
          <el-col :span="6"><div class="mini"><div class="mini-v green">{{ detail.stat.points || 0 }}</div><div class="mini-l">可用积分</div></div></el-col>
          <el-col :span="6"><div class="mini"><div class="mini-v warn">{{ detail.stat.pointsFrozen || 0 }}</div><div class="mini-l">冻结积分</div></div></el-col>
          <el-col :span="6"><div class="mini"><div class="mini-v blue">{{ detail.stat.creditScore || 0 }}</div><div class="mini-l">信用分</div></div></el-col>
        </el-row>

        <el-tabs v-model="activeTab" class="mt16">
          <el-tab-pane label="积分明细" name="points">
            <el-table :data="pointsList" border size="small" v-loading="pointsLoading">
              <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
              <el-table-column label="变动" width="90" align="center">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="scope.row.type === 1 ? 'success' : 'danger'">
                    {{ scope.row.type === 1 ? '+' : '-' }}{{ scope.row.value }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="mark" label="说明" min-width="200" show-overflow-tooltip></el-table-column>
              <el-table-column prop="balance" label="余额" width="80" align="center"></el-table-column>
              <el-table-column prop="createTime" label="时间" width="155" align="center"></el-table-column>
            </el-table>
            <el-pagination class="mt12" @current-change="pPageChange" :current-page="pPage" :page-size="pLimit" :total="pTotal"
              layout="total, prev, pager, next" small></el-pagination>
          </el-tab-pane>

          <el-tab-pane label="信用分明细" name="credit">
            <el-table :data="creditList" border size="small" v-loading="creditLoading">
              <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
              <el-table-column label="变动" width="90" align="center">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="scope.row.type === 1 ? 'success' : 'danger'">
                    {{ scope.row.type === 1 ? '+' : '-' }}{{ scope.row.value }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="mark" label="说明" min-width="200" show-overflow-tooltip></el-table-column>
              <el-table-column prop="balance" label="分数" width="80" align="center"></el-table-column>
              <el-table-column prop="createTime" label="时间" width="155" align="center"></el-table-column>
            </el-table>
            <el-pagination class="mt12" @current-change="cPageChange" :current-page="cPage" :page-size="cLimit" :total="cTotal"
              layout="total, prev, pager, next" small></el-pagination>
          </el-tab-pane>

          <el-tab-pane :label="'名下物品' + (iTotal ? '（' + iTotal + '）' : '')" name="items">
            <div class="items-bar">
              <el-select v-model="itemStatus" size="mini" placeholder="全部状态" clearable style="width: 130px" @change="iStatusChange">
                <el-option label="在售" :value="1"></el-option>
                <el-option label="审核中" :value="2"></el-option>
                <el-option label="已驳回" :value="3"></el-option>
                <el-option label="已换出" :value="4"></el-option>
                <el-option label="交换中" :value="5"></el-option>
                <el-option label="已下架" :value="6"></el-option>
              </el-select>
              <span class="items-tip">共 {{ iTotal }} 件，点击图片可放大查看</span>
            </div>
            <el-table :data="itemsList" border size="small" v-loading="itemsLoading">
              <el-table-column label="物品" min-width="220">
                <template slot-scope="scope">
                  <div class="item-cell">
                    <el-image
                      v-if="scope.row.cover"
                      :src="imgUrl(scope.row.cover)"
                      :preview-src-list="[imgUrl(scope.row.cover)]"
                      class="item-img"
                      fit="cover"></el-image>
                    <div v-else class="item-img item-img-empty">无图</div>
                    <div class="item-meta">
                      <div class="item-title">
                        <span class="item-id">#{{ scope.row.id }}</span>
                        {{ scope.row.title || '未命名物品' }}
                      </div>
                      <div class="item-sub">
                        {{ scope.row.conditionText || '-' }} · {{ scope.row.city || '未填城市' }} · {{ scope.row.imageCount }} 张图
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="92" align="center">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="itemStatusType(scope.row.status)">{{ scope.row.statusText }}</el-tag>
                  <el-tag v-if="scope.row.toping" size="mini" type="danger" class="toping">置顶</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="数据" width="150" align="center">
                <template slot-scope="scope">
                  <span class="stat-mini">浏览 {{ scope.row.viewCount }}</span>
                  <span class="stat-mini">想要 {{ scope.row.wantCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="发布时间" width="155" align="center"></el-table-column>
            </el-table>
            <el-pagination class="mt12" @current-change="iPageChange" @size-change="iSizeChange"
              :current-page="iPage" :page-size="iLimit" :page-sizes="[5, 10, 20]"
              :total="iTotal" layout="total, sizes, prev, pager, next" small></el-pagination>
          </el-tab-pane>

          <el-tab-pane label="最近换物单" name="orders">
            <el-table :data="detail.orders || []" border size="small">
              <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
              <el-table-column prop="orderSn" label="单号" min-width="170"></el-table-column>
              <el-table-column label="我的角色" width="100" align="center">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="scope.row.role === '物主' ? 'success' : 'warning'">{{ scope.row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template slot-scope="scope">{{ scope.row.statusText }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="155" align="center"></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  barterUserListApi,
  barterUserDetailApi,
  barterUserStatusApi,
  barterUserPointsLogApi,
  barterUserCreditLogApi,
  barterUserItemsApi,
  barterLevelListApi
} from '@/api/barter'
import { imgUrl } from '@/utils/barterImg'

export default {
  name: 'BarterMember',
  data() {
    return {
      dataForm: { keyword: '', city: '', levelId: '', sort: 1, page: 1, limit: 10 },
      dataList: [],
      total: 0,
      loading: false,
      levels: [],
      detailVisible: false,
      detailLoading: false,
      detail: null,
      activeTab: 'points',
      pointsList: [], pointsLoading: false, pPage: 1, pLimit: 10, pTotal: 0,
      creditList: [], creditLoading: false, cPage: 1, cLimit: 10, cTotal: 0,
      itemsList: [], itemsLoading: false, iPage: 1, iLimit: 5, iTotal: 0, itemStatus: ''
    }
  },
  mounted() {
    this.loadLevels()
    this.getDataList()
  },
  methods: {
    imgUrl,
    loadLevels() {
      barterLevelListApi().then(res => { this.levels = res || [] }).catch(() => {})
    },
    search() {
      this.dataForm.page = 1
      this.getDataList()
    },
    reset() {
      this.dataForm = { keyword: '', city: '', levelId: '', sort: 1, page: 1, limit: 10 }
      this.getDataList()
    },
    getDataList() {
      this.loading = true
      const params = { page: this.dataForm.page, limit: this.dataForm.limit, sort: this.dataForm.sort }
      if (this.dataForm.keyword) params.keyword = this.dataForm.keyword
      if (this.dataForm.city) params.city = this.dataForm.city
      if (this.dataForm.levelId !== '' && this.dataForm.levelId !== null) params.levelId = this.dataForm.levelId
      barterUserListApi(params).then(res => {
        this.dataList = res.list || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    sizeChangeHandle(val) {
      this.dataForm.limit = val
      this.dataForm.page = 1
      this.getDataList()
    },
    currentChangeHandle(val) {
      this.dataForm.page = val
      this.getDataList()
    },
    openDetail(row) {
      this.detailVisible = true
      this.detailLoading = true
      this.detail = null
      this.activeTab = 'points'
      this.pointsList = []; this.creditList = []; this.itemsList = []
      this.pPage = 1; this.cPage = 1; this.iPage = 1; this.iTotal = 0; this.itemStatus = ''
      barterUserDetailApi(row.uid).then(res => {
        this.detail = res
        this.detailLoading = false
        this.loadPoints()
        this.loadCredit()
        this.loadItems()
      }).catch(() => { this.detailLoading = false })
    },
    loadPoints() {
      if (!this.detail) return
      this.pointsLoading = true
      barterUserPointsLogApi({ uid: this.detail.stat.uid, page: this.pPage, limit: this.pLimit }).then(res => {
        this.pointsList = res.list || []
        this.pTotal = res.total || 0
        this.pointsLoading = false
      }).catch(() => { this.pointsLoading = false })
    },
    loadCredit() {
      if (!this.detail) return
      this.creditLoading = true
      barterUserCreditLogApi({ uid: this.detail.stat.uid, page: this.cPage, limit: this.cLimit }).then(res => {
        this.creditList = res.list || []
        this.cTotal = res.total || 0
        this.creditLoading = false
      }).catch(() => { this.creditLoading = false })
    },
    pPageChange(v) { this.pPage = v; this.loadPoints() },
    cPageChange(v) { this.cPage = v; this.loadCredit() },
    loadItems() {
      if (!this.detail) return
      this.itemsLoading = true
      const params = { uid: this.detail.stat.uid, page: this.iPage, limit: this.iLimit }
      if (this.itemStatus !== '' && this.itemStatus !== null) params.status = this.itemStatus
      barterUserItemsApi(params).then(res => {
        this.itemsList = res.list || []
        this.iTotal = res.total || 0
        this.itemsLoading = false
      }).catch(() => { this.itemsLoading = false })
    },
    iPageChange(v) { this.iPage = v; this.loadItems() },
    iSizeChange(v) { this.iLimit = v; this.iPage = 1; this.loadItems() },
    iStatusChange() { this.iPage = 1; this.loadItems() },
    itemStatusType(st) {
      return { 1: 'success', 2: 'warning', 3: 'danger', 4: '', 5: 'warning', 6: 'info' }[st] || 'info'
    },
    toggleStatus(row) {
      const next = row.status === 1 ? 0 : 1
      const tip = next === 1 ? '确认启用该用户？' : '禁用后该用户将无法登录与换物，确认禁用？'
      this.$confirm(tip, '提示', { type: 'warning' }).then(() => {
        barterUserStatusApi(row.uid, next).then(() => {
          this.$message.success(next === 1 ? '已启用' : '已禁用')
          this.getDataList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.selWidth { width: 200px; }
.selSmall { width: 130px; }
.mt12 { margin-top: 12px; }
.mt16 { margin-top: 16px; }
.muted { color: #bbb; font-size: 12px; }
.user-cell { display: flex; align-items: center; }
.avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; margin-right: 10px; background: #f5f5f5; }
.u-name { font-size: 13px; color: #333; }
.u-uid { font-size: 12px; color: #999; }
.num { font-weight: 600; }
.frozen { font-size: 11px; color: #e6a23c; margin-left: 4px; }
.credit { font-weight: 600; color: #2f9e44; }
.credit.low { color: #f56c6c; }

.drawer-body { padding: 0 20px 40px; }
.profile { display: flex; align-items: center; }
.p-avatar { width: 62px; height: 62px; border-radius: 50%; object-fit: cover; margin-right: 14px; background: #f5f5f5; }
.p-name { font-size: 17px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.p-sub { font-size: 12px; color: #999; margin-top: 5px; }
.mini { background: #fafafa; border-radius: 6px; padding: 10px; text-align: center; }
.mini-v { font-size: 20px; font-weight: 700; color: #333; }
.mini-v.green { color: #2f9e44; }
.mini-v.warn { color: #e6a23c; }
.mini-v.blue { color: #409eff; }
.mini-l { font-size: 12px; color: #999; margin-top: 2px; }

.items-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.items-tip { font-size: 12px; color: #999; }
.item-cell { display: flex; align-items: center; }
.item-img { width: 46px; height: 46px; border-radius: 4px; margin-right: 10px; background: #f5f5f5; flex-shrink: 0; }
.item-img-empty { display: flex; align-items: center; justify-content: center; font-size: 11px; color: #bbb; }
.item-meta { min-width: 0; }
.item-title { font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-id { color: #999; font-size: 12px; margin-right: 4px; }
.item-sub { font-size: 12px; color: #999; margin-top: 3px; }
.toping { margin-left: 2px; }
.stat-mini { font-size: 12px; color: #666; margin-right: 8px; }
</style>
