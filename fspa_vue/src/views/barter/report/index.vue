<template>
  <div class="divBox">
    <!-- ==================== 状态 TAB（带数量红点） ==================== -->
    <el-tabs v-model="tab" @tab-click="onTabChange">
      <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name">
        <span slot="label">
          {{ t.label }}
          <el-badge
            v-if="counts[t.countKey] > 0"
            :value="counts[t.countKey]"
            :type="t.name === 'pending' ? 'danger' : 'info'"
            class="tab-badge" />
        </span>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 列表 ==================== -->
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      style="width: 100%;"
      class="clickable"
      @row-click="openDetail">
      <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>

      <el-table-column label="类型" width="96" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="typeTag(scope.row.type)">{{ scope.row.typeText || '其他' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="被举报物品" min-width="230">
        <template slot-scope="scope">
          <div class="goods-cell" v-if="scope.row.itemId">
            <img :src="imgUrl(scope.row.itemCover)" class="goods-thumb" />
            <div class="goods-meta">
              <div class="goods-title">{{ scope.row.itemTitle || ('物品 #' + scope.row.itemId) }}</div>
              <div class="goods-sub">
                物品 #{{ scope.row.itemId }}
                <el-tag v-if="scope.row.itemStatusText" size="mini" type="info" class="ml6">{{ scope.row.itemStatusText }}</el-tag>
              </div>
            </div>
          </div>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column label="举报内容" min-width="200" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.content || '-' }}</template>
      </el-table-column>

      <el-table-column label="举报人" width="150">
        <template slot-scope="scope">
          <div class="user-cell">
            <img :src="imgUrl(scope.row.avatar)" class="user-avatar" />
            <div class="user-meta">
              <div class="user-name">{{ scope.row.nickname || '未知用户' }}</div>
              <div class="user-uid">UID {{ scope.row.uid }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="被举报人" width="150">
        <template slot-scope="scope">
          <div class="user-cell">
            <img :src="imgUrl(scope.row.targetAvatar)" class="user-avatar warn" />
            <div class="user-meta">
              <div class="user-name">{{ scope.row.targetNickname || '未知用户' }}</div>
              <div class="user-uid">UID {{ scope.row.targetUid }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="92" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="mini">{{ scope.row.statusText }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="举报时间" width="155" align="center"></el-table-column>

      <el-table-column fixed="right" label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click.stop="openDetail(scope.row)">明细</el-button>
          <el-button v-if="scope.row.status === 0" type="text" size="small" @click.stop="openHandle(scope.row)">处理</el-button>
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

    <!-- ==================== 明细弹框 ==================== -->
    <el-dialog title="举报明细" :visible.sync="detailVisible" width="860px" append-to-body :destroy-on-close="true">
      <div v-if="detail" v-loading="detailLoading" class="detail">
        <!-- 举报概况 -->
        <div class="sec-title">举报信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="举报编号">#{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag size="mini" :type="typeTag(detail.type)">{{ detail.typeText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="mini" :type="statusTag(detail.status)">{{ detail.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="举报时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="举报内容" :span="2">
            <div class="content-box">{{ detail.content || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.handleTime" label="处理时间">{{ detail.handleTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.handleTime" label="处理人">
            {{ detail.handleUidName || ('UID ' + detail.handleUid) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.handleResult" label="处理说明" :span="2">
            {{ detail.handleResult }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 被举报物品 -->
        <div class="sec-title">
          被举报物品
          <span v-if="detail.item && detail.item.reportCount > 1" class="warn-tip">
            该物品累计被举报 {{ detail.item.reportCount }} 次
          </span>
        </div>
        <div v-if="detail.item" class="item-card">
          <div class="item-imgs">
            <el-image
              v-for="(src, i) in imgList(detail.item.images)"
              :key="i"
              :src="src"
              :preview-src-list="imgList(detail.item.images)"
              fit="cover"
              class="item-img" />
            <div v-if="!imgList(detail.item.images).length" class="item-img empty-img">无图</div>
          </div>
          <div class="item-info">
            <div class="item-title">{{ detail.item.title }}</div>
            <div class="item-kv"><span class="k">物品 ID</span><span class="v">#{{ detail.item.id }}</span></div>
            <div class="item-kv"><span class="k">当前状态</span>
              <span class="v"><el-tag size="mini" type="info">{{ detail.item.statusText }}</el-tag></span>
            </div>
            <div class="item-kv"><span class="k">成色 / 城市</span>
              <span class="v">{{ detail.item.conditionText || '-' }} · {{ detail.item.city || '-' }}</span>
            </div>
            <div class="item-kv"><span class="k">浏览 / 想要</span>
              <span class="v">{{ detail.item.viewCount || 0 }} / {{ detail.item.wantCount || 0 }}</span>
            </div>
            <div class="item-kv"><span class="k">发布者</span>
              <span class="v">{{ detail.item.ownerNickname || '-' }}（UID {{ detail.item.ownerUid }}）</span>
            </div>
            <div class="item-kv"><span class="k">发布时间</span><span class="v">{{ detail.item.createTime }}</span></div>
            <div class="item-desc">{{ detail.item.description || '（无描述）' }}</div>
          </div>
        </div>
        <div v-else class="empty">该举报未关联物品</div>

        <!-- 关联换物单 -->
        <div v-if="detail.order" class="sec-title">关联换物单</div>
        <el-descriptions v-if="detail.order" :column="3" border size="small">
          <el-descriptions-item label="单号">{{ detail.order.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.order.statusText }}</el-descriptions-item>
          <el-descriptions-item label="交付方式">{{ detail.order.deliveryTypeText }}</el-descriptions-item>
        </el-descriptions>

        <!-- 双方 -->
        <div class="sec-title">双方档案</div>
        <el-row :gutter="12">
          <el-col :span="12" v-for="p in parties" :key="p.role">
            <div class="party" :class="{ danger: p.role === '被举报人' }">
              <div class="party-head">
                <img :src="imgUrl(p.data.avatar)" class="party-avatar" />
                <div class="party-meta">
                  <div class="party-name">
                    {{ p.data.nickname || '未知用户' }}
                    <span class="party-role">{{ p.role }}</span>
                    <el-tag v-if="p.data.status !== 1" size="mini" type="danger">已禁用</el-tag>
                  </div>
                  <div class="party-sub">
                    UID {{ p.data.uid }} · {{ p.data.phone || '未绑定手机' }}
                    <span v-if="p.data.levelName" class="ml6">{{ p.data.levelName }}</span>
                  </div>
                </div>
              </div>
              <div class="party-stats">
                <div class="stat"><b>{{ p.data.creditScore || 0 }}</b><span>信用分</span></div>
                <div class="stat"><b>{{ p.data.exchangeCount || 0 }}</b><span>成功交换</span></div>
                <div class="stat"><b>{{ p.data.points || 0 }}</b><span>积分</span></div>
              </div>
              <div class="party-row">
                <span class="k">举报过</span><span class="v">{{ p.data.reportTotal || 0 }} 次</span>
              </div>
              <div class="party-row">
                <span class="k">被举报</span>
                <span class="v" :class="{ red: (p.data.reportedTotal || 0) > 0 }">{{ p.data.reportedTotal || 0 }} 次</span>
              </div>
              <div class="party-row">
                <span class="k">在售物品</span><span class="v">{{ p.data.onSaleCount || 0 }} 件</span>
              </div>
              <div class="party-row">
                <span class="k">注册时间</span><span class="v">{{ p.data.createTime || '-' }}</span>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 同一被举报人的其他待处理举报 -->
        <template v-if="detail.relatedReports && detail.relatedReports.length">
          <div class="sec-title">该被举报人的其他待处理举报（{{ detail.relatedReports.length }} 条）</div>
          <el-table :data="detail.relatedReports" border size="small">
            <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
            <el-table-column label="物品" min-width="180">
              <template slot-scope="scope">
                <div class="goods-cell">
                  <img :src="imgUrl(scope.row.itemCover)" class="goods-thumb sm" />
                  <span class="goods-title">{{ scope.row.itemTitle || ('#' + scope.row.itemId) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip></el-table-column>
            <el-table-column prop="createTime" label="时间" width="150" align="center"></el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="switchTo(scope.row.id)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
      <span slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && detail.status === 0" type="primary" size="small" @click="openHandle(detail)">处理该举报</el-button>
      </span>
    </el-dialog>

    <!-- ==================== 处理弹框 ==================== -->
    <el-dialog title="处理举报" :visible.sync="handleVisible" width="560px" append-to-body>
      <el-form :model="handleForm" label-width="96px">
        <el-form-item label="举报内容">
          <div class="readonly-box">{{ handleTarget.content || '-' }}</div>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio :label="1">举报成立（已处理）</el-radio>
            <el-radio :label="2">举报不成立（已驳回）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="影响范围">
          <div class="impact" :class="handleForm.status === 1 ? 'impact-danger' : 'impact-safe'">
            <template v-if="handleForm.status === 1">
              <div class="impact-row">
                <i class="el-icon-warning-outline"></i>
                <span v-if="!handleTarget.item">该举报无关联物品，跳过下架</span>
                <span v-else-if="willOffshelf">
                  物品「{{ handleTarget.item.title }}」{{ handleTarget.item.statusText }} → <b>已下架</b>，并清除置顶
                </span>
                <span v-else>
                  物品「{{ handleTarget.item.title }}」当前为 {{ handleTarget.item.statusText }}，不满足下架条件，保持原状态
                </span>
              </div>
              <div class="impact-row">
                <i class="el-icon-warning-outline"></i>
                <span>
                  被举报人信用分 <b>-{{ handleTarget.creditPenalty || 0 }}</b> 分
                  <span class="muted">（分值取「规则配置 · 信用分 · 差评扣分」，同时写入信用流水）</span>
                </span>
              </div>
              <div class="impact-row">
                <i class="el-icon-warning-outline"></i>
                <span>向被举报人推送站内通知，说明处理结果</span>
              </div>
            </template>
            <template v-else>
              <div class="impact-row"><i class="el-icon-info"></i><span>仅记录处理结论，<b>不改动物品状态、不扣信用分</b></span></div>
              <div class="impact-row"><i class="el-icon-info"></i><span>向举报人推送「举报未成立」，避免同一问题反复提交</span></div>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.result"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：已核实图片与实物不符，物品已下架并通知发布者" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="handleVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="submitHandle">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  barterReportRichListApi,
  barterReportCountsApi,
  barterReportDetailApi,
  barterReportHandleApi
} from '@/api/barter'
import { imgUrl, imgList } from '@/utils/barterImg'

export default {
  name: 'BarterReport',
  data() {
    return {
      tab: 'pending',
      tabs: [
        { name: 'pending', label: '待处理', countKey: 'pending' },
        { name: 'handled', label: '已处理', countKey: 'handled' },
        { name: 'rejected', label: '已驳回', countKey: 'rejected' },
        { name: 'all', label: '全部', countKey: 'all' }
      ],
      counts: { all: 0, pending: 0, handled: 0, rejected: 0 },
      dataForm: { page: 1, limit: 10 },
      dataList: [],
      total: 0,
      dataListLoading: false,

      detailVisible: false,
      detailLoading: false,
      detail: null,

      handleVisible: false,
      submitting: false,
      handleTarget: {},
      handleForm: { id: null, status: 1, result: '' }
    }
  },
  computed: {
    statusFilter() {
      // all 传空表示不限状态
      return this.tab === 'all' ? '' : { pending: 0, handled: 1, rejected: 2 }[this.tab]
    },
    willOffshelf() {
      const it = this.handleTarget && this.handleTarget.item
      if (!it || !it.status) return false
      return it.status === 1 || it.status === 2 || it.status === 3
    },
    parties() {
      if (!this.detail) return []
      const list = []
      if (this.detail.reporter) list.push({ role: '举报人', data: this.detail.reporter })
      if (this.detail.target) list.push({ role: '被举报人', data: this.detail.target })
      return list
    }
  },
  mounted() {
    this.loadCounts()
    this.getDataList()
  },
  methods: {
    imgUrl,
    imgList,
    typeTag(t) {
      return { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'info' }[t] || 'info'
    },
    statusTag(s) {
      return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info'
    },
    onTabChange() {
      this.dataForm.page = 1
      this.getDataList()
    },
    loadCounts() {
      barterReportCountsApi().then(res => {
        this.counts = Object.assign({ all: 0, pending: 0, handled: 0, rejected: 0 }, res || {})
      }).catch(() => {})
    },
    getDataList() {
      this.dataListLoading = true
      const params = { page: this.dataForm.page, limit: this.dataForm.limit }
      if (this.statusFilter !== '') params.status = this.statusFilter
      barterReportRichListApi(params).then(res => {
        this.dataList = res.list || []
        this.total = res.total || 0
        this.dataListLoading = false
      }).catch(() => { this.dataListLoading = false })
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
    refresh() {
      this.loadCounts()
      this.getDataList()
    },
    /* ---------------- 明细 ---------------- */
    openDetail(row) {
      this.detailVisible = true
      this.detailLoading = true
      this.detail = null
      barterReportDetailApi(row.id).then(res => {
        this.detail = res
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    // 在同一弹框里切换到另一条举报，不打断处理节奏
    switchTo(id) {
      this.detailLoading = true
      this.detail = null
      barterReportDetailApi(id).then(res => {
        this.detail = res
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    /* ---------------- 处理 ---------------- */
    openHandle(row) {
      this.handleTarget = row || {}
      this.handleForm = { id: row.id, status: 1, result: '' }
      this.handleVisible = true
      // 列表行的数据没有物品/扣分值，补拉一次明细，好让运营在弹框里看到"会改什么"
      if (!row.item) {
        barterReportDetailApi(row.id).then(res => {
          if (res && this.handleVisible) this.handleTarget = Object.assign({}, row, res)
        }).catch(() => {})
      }
    },
    submitHandle() {
      if (!this.handleForm.result || !this.handleForm.result.trim()) {
        this.$message.warning('请填写处理说明，便于后续追溯')
        return
      }
      this.submitting = true
      const isPunish = this.handleForm.status === 1
      barterReportHandleApi(this.handleForm).then(() => {
        this.$message.success(isPunish ? '举报已成立，物品已下架并扣减信用分' : '已驳回该举报，已通知举报人')
        this.handleVisible = false
        this.detailVisible = false
        this.submitting = false
        this.refresh()
      }).catch(() => { this.submitting = false })
    }
  }
}
</script>

<style scoped>
.tab-badge { margin-left: 6px; vertical-align: middle; }
.clickable >>> .el-table__row { cursor: pointer; }
.muted { color: #bbb; font-size: 12px; }
.ml6 { margin-left: 6px; }

/* 处理弹框：数据变更提示 */
.impact { width: 100%; border-radius: 4px; padding: 8px 10px; font-size: 12px; line-height: 1.9; }
.impact-danger { background: #fef0f0; color: #c45656; border: 1px solid #fbc4c4; }
.impact-safe { background: #f4f4f5; color: #606266; border: 1px solid #e9e9eb; }
.impact-row i { margin-right: 4px; }

/* 物品单元格 */
.goods-cell { display: flex; align-items: center; }
.goods-thumb {
  width: 46px; height: 46px; border-radius: 4px; object-fit: cover;
  background: #f5f5f5; margin-right: 8px; flex-shrink: 0; border: 1px solid #f0f0f0;
}
.goods-thumb.sm { width: 32px; height: 32px; }
.goods-meta { min-width: 0; }
.goods-title {
  font-size: 13px; color: #333; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.goods-sub { font-size: 12px; color: #999; margin-top: 3px; }

/* 用户单元格 */
.user-cell { display: flex; align-items: center; }
.user-avatar {
  width: 32px; height: 32px; border-radius: 50%; object-fit: cover;
  background: #f5f5f5; margin-right: 8px; flex-shrink: 0;
  border: 1px solid #eee;
}
.user-avatar.warn { border-color: #fde2e2; }
.user-meta { min-width: 0; }
.user-name {
  font-size: 13px; color: #333; line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.user-uid { font-size: 12px; color: #999; }

/* 明细弹框 */
.detail { max-height: 66vh; overflow-y: auto; padding-right: 6px; }
.sec-title {
  font-size: 14px; font-weight: 600; color: #2f9e44;
  margin: 18px 0 10px; padding-left: 8px; border-left: 3px solid #2f9e44;
  display: flex; align-items: center;
}
.sec-title:first-child { margin-top: 0; }
.warn-tip { font-size: 12px; font-weight: 400; color: #e6a23c; margin-left: 10px; }
.content-box { background: #fafafa; border-radius: 4px; padding: 8px 10px; line-height: 1.7; }

.item-card { display: flex; border: 1px solid #eee; border-radius: 6px; padding: 12px; }
.item-imgs { display: flex; flex-wrap: wrap; gap: 6px; width: 190px; flex-shrink: 0; }
.item-img {
  width: 56px; height: 56px; border-radius: 4px; border: 1px solid #f0f0f0; background: #f7f7f7;
}
.empty-img {
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #bbb;
}
.item-info { flex: 1; margin-left: 14px; min-width: 0; }
.item-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 8px; }
.item-kv { display: flex; font-size: 12px; line-height: 1.9; }
.item-kv .k { width: 84px; color: #999; flex-shrink: 0; }
.item-kv .v { flex: 1; color: #333; }
.item-desc {
  margin-top: 8px; font-size: 12px; color: #666; line-height: 1.7;
  background: #fafafa; border-radius: 4px; padding: 8px 10px;
  max-height: 90px; overflow-y: auto;
}

/* 双方卡片 */
.party { border: 1px solid #eee; border-radius: 6px; padding: 12px; height: 100%; }
.party.danger { border-color: #fde2e2; background: #fffbfb; }
.party-head { display: flex; align-items: center; margin-bottom: 10px; }
.party-avatar {
  width: 42px; height: 42px; border-radius: 50%; object-fit: cover;
  background: #f5f5f5; margin-right: 10px; border: 1px solid #eee;
}
.party-meta { min-width: 0; }
.party-name { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.party-role {
  font-size: 11px; font-weight: 400; color: #2f9e44;
  border: 1px solid #b7e4c7; border-radius: 3px; padding: 0 4px;
}
.party-sub { font-size: 12px; color: #999; margin-top: 4px; }
.party-stats { display: flex; gap: 8px; margin-bottom: 8px; }
.stat {
  flex: 1; background: #fafafa; border-radius: 4px; padding: 6px 4px; text-align: center;
}
.stat b { display: block; font-size: 16px; color: #333; }
.stat span { font-size: 11px; color: #999; }
.party-row { display: flex; font-size: 12px; line-height: 1.9; }
.party-row .k { width: 66px; color: #999; flex-shrink: 0; }
.party-row .v { flex: 1; color: #333; }
.party-row .v.red { color: #f56c6c; font-weight: 600; }

.empty { font-size: 13px; color: #bbb; padding: 12px 0; }
.readonly-box {
  background: #f7f7f7; padding: 8px 12px; border-radius: 4px;
  font-size: 13px; color: #555; line-height: 1.7;
}
</style>
