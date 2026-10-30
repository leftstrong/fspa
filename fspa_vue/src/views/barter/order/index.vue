<template>
  <div class="divBox">
    <!-- ==================== 筛选 ==================== -->
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="search">
      <el-form-item label="用户">
        <el-select
          v-model="dataForm.linkUid"
          filterable
          remote
          clearable
          reserve-keyword
          placeholder="输入 uid 或昵称搜索"
          :remote-method="searchUser"
          :loading="userLoading"
          class="selWide"
          size="small"
          @change="search">
          <el-option
            v-for="u in userOptions"
            :key="u.uid"
            :label="`${u.nickname || '未命名'}（UID ${u.uid}）`"
            :value="u.uid">
            <span style="float:left">{{ u.nickname || '未命名' }}</span>
            <span style="float:right;color:#999;font-size:12px;">UID {{ u.uid }} · 信用 {{ u.creditScore || 0 }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="dataForm.status" placeholder="全部" class="selWidth" clearable @change="search" size="small">
          <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="Number(k)"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="单号">
        <el-input v-model="dataForm.orderSn" placeholder="支持模糊" class="selWidth" size="small" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" icon="el-icon-search" @click="search">查询</el-button>
        <el-button size="small" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 列表 ==================== -->
    <el-table :data="dataList" border v-loading="dataListLoading" style="width: 100%;" @row-click="openDetail" class="clickable">
      <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
      <el-table-column prop="orderSn" label="换物单号" width="175"></el-table-column>
      <el-table-column label="物品" min-width="200">
        <template slot-scope="scope">
          <div class="item-cell">
            <img v-if="scope.row.itemCover" :src="imgUrl(scope.row.itemCover)" class="thumb" />
            <span class="item-title">{{ scope.row.itemTitle || ('物品 #' + scope.row.itemId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="物主" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.ownerNickname || '-' }}</span>
          <span class="uid">({{ scope.row.ownerUid }})</span>
        </template>
      </el-table-column>
      <el-table-column label="申请人" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.applyNickname || '-' }}</span>
          <span class="uid">({{ scope.row.applyUid }})</span>
        </template>
      </el-table-column>
      <el-table-column label="交付" width="90" align="center">
        <template slot-scope="scope">{{ scope.row.deliveryTypeText || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="95" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="mini">{{ scope.row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" align="center"></el-table-column>
      <el-table-column fixed="right" label="操作" width="150" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click.stop="openDetail(scope.row)">详情</el-button>
          <el-button v-if="[2,3].indexOf(scope.row.status) > -1" type="text" size="small" @click.stop="cancelHandle(scope.row)">取消/纠纷</el-button>
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
    <el-drawer title="换物单详情" :visible.sync="detailVisible" size="760px" :destroy-on-close="true">
      <div v-if="detail" class="drawer-body" v-loading="detailLoading">
        <!-- 基础信息 -->
        <div class="sec-title">基础信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="换物单号">{{ detail.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)" size="mini">{{ detail.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="交付方式">{{ detail.deliveryTypeText }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ detail.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="自提地址">{{ detail.selfTakeAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人附言" :span="2">{{ detail.applyMessage || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.chainNo" label="智能换物环" :span="2">
            {{ detail.chainNo }} · 第 {{ detail.chainSeq }} / {{ detail.chainSize }} 棒 ·
            路径费应扣 {{ detail.pathFeePoints || 0 }} / 实扣 {{ detail.pathFeeCharged || 0 }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 物品 -->
        <div class="sec-title">物品</div>
        <div class="goods">
          <div class="goods-card" v-if="detail.item">
            <img :src="imgUrl(detail.item.cover)" class="goods-img" />
            <div class="goods-info">
              <div class="goods-title">{{ detail.item.title }}</div>
              <div class="goods-sub">物主要换出 · {{ detail.item.conditionText }} · {{ detail.item.city || '-' }}</div>
            </div>
          </div>
          <div class="goods-card" v-for="it in detail.offerItems" :key="it.id">
            <img :src="imgUrl(it.cover)" class="goods-img" />
            <div class="goods-info">
              <div class="goods-title">{{ it.title }}</div>
              <div class="goods-sub">申请人提供 · {{ it.conditionText }} · {{ it.city || '-' }}</div>
            </div>
          </div>
          <div v-if="!detail.item && (!detail.offerItems || !detail.offerItems.length)" class="empty">无物品信息</div>
        </div>

        <!-- 双方 -->
        <div class="sec-title">双方信息与物流</div>
        <el-row :gutter="12">
          <el-col :span="12" v-for="p in parties" :key="p.role">
            <div class="party">
              <div class="party-head">
                <img :src="imgUrl(p.data.avatar)" class="avatar" />
                <div>
                  <div class="p-name">{{ p.data.nickname || '未命名' }} <span class="p-role">{{ p.role }}</span></div>
                  <div class="p-sub">UID {{ p.data.uid }} · 信用 {{ p.data.creditScore || 0 }} · 成交 {{ p.data.exchangeCount || 0 }} 次</div>
                </div>
              </div>
              <div class="kv"><span class="k">收货地址</span><span class="v">{{ p.data.address || '未填写' }}</span></div>
              <div class="kv"><span class="k">发货状态</span>
                <span class="v">
                  <el-tag size="mini" :type="p.data.deliverStatus === 1 ? 'success' : 'info'">
                    {{ p.data.deliverStatus === 1 ? '已发货' : '未发货' }}
                  </el-tag>
                  <span v-if="p.data.deliverTime" class="time">（{{ p.data.deliverTime }}）</span>
                </span>
              </div>
              <div class="kv"><span class="k">快递</span>
                <span class="v">
                  <template v-if="p.data.logisticsNo">
                    {{ p.data.logisticsCompanyName || p.data.logisticsCompany }} · {{ p.data.logisticsNo }}
                    <el-button type="text" size="mini" @click="copy(p.data.logisticsNo)">复制</el-button>
                    <span v-if="p.data.logisticsEdit" class="time">已改 {{ p.data.logisticsEdit }} 次</span>
                  </template>
                  <span v-else class="muted">未填写</span>
                </span>
              </div>
              <div class="kv"><span class="k">确认收货</span>
                <span class="v">{{ p.data.confirmRecv === 1 ? '已确认' : '未确认' }}</span>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 聊天记录 -->
        <div class="sec-title">聊天记录（{{ (detail.messages || []).length }} 条）</div>
        <div class="chat" v-if="detail.messages && detail.messages.length">
          <div class="chat-row" v-for="m in detail.messages" :key="m.id">
            <div class="chat-meta">
              <span class="chat-from">{{ m.fromNickname }}</span>
              <el-tag v-if="m.type && m.type !== 1" size="mini" type="warning">{{ m.typeText }}</el-tag>
              <span class="chat-time">{{ m.createTime }}</span>
            </div>
            <div class="chat-content">
              {{ m.content }}
              <div v-if="m.address" class="chat-addr">📍 {{ m.address }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty">双方还没有留言</div>

        <!-- 互评 -->
        <div class="sec-title">互评记录（{{ (detail.evaluates || []).length }} 条）</div>
        <div v-if="detail.evaluates && detail.evaluates.length">
          <div class="eval" v-for="e in detail.evaluates" :key="e.id">
            <div class="eval-head">
              <span>{{ e.nickname }} → {{ e.toNickname }}</span>
              <el-rate :value="e.star || 0" disabled show-score text-color="#ff9900" score-template="{value} 星"></el-rate>
            </div>
            <div class="eval-content">{{ e.content || '（未填写评价内容）' }}</div>
            <div class="eval-time">{{ e.createTime }}</div>
          </div>
        </div>
        <div v-else class="empty">暂无评价</div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { barterOrderRichListApi, barterOrderDetailApi, barterOrderCancelApi, barterUserOptionsApi } from '@/api/barter'
import { imgUrl } from '@/utils/barterImg'

export default {
  name: 'BarterOrder',
  data() {
    return {
      dataForm: { linkUid: '', status: '', orderSn: '', page: 1, limit: 10 },
      dataList: [],
      total: 0,
      dataListLoading: false,
      userOptions: [],
      userLoading: false,
      detailVisible: false,
      detailLoading: false,
      detail: null,
      // 与后端一致：0 待确认 1 已取消 2 进行中 3 待收货 4 已完成 5 已拒绝 6 纠纷中
      statusMap: { 0: '待确认', 1: '已取消', 2: '进行中', 3: '待收货', 4: '已完成', 5: '已拒绝', 6: '纠纷中' }
    }
  },
  computed: {
    parties() {
      if (!this.detail) return []
      return [
        { role: '物主', data: this.detail.owner || {} },
        { role: '申请人', data: this.detail.applyUser || {} }
      ]
    }
  },
  mounted() {
    this.getDataList()
    this.searchUser('')
  },
  methods: {
    imgUrl,
    statusTag(s) {
      return { 0: 'info', 1: 'info', 2: 'warning', 3: 'warning', 4: 'success', 5: 'danger', 6: 'danger' }[s] || 'info'
    },
    searchUser(keyword) {
      this.userLoading = true
      barterUserOptionsApi(keyword || '').then(res => {
        this.userOptions = res || []
        this.userLoading = false
      }).catch(() => { this.userLoading = false })
    },
    search() {
      this.dataForm.page = 1
      this.getDataList()
    },
    reset() {
      this.dataForm = { linkUid: '', status: '', orderSn: '', page: 1, limit: 10 }
      this.getDataList()
    },
    getDataList() {
      this.dataListLoading = true
      const params = { page: this.dataForm.page, limit: this.dataForm.limit }
      if (this.dataForm.linkUid !== '' && this.dataForm.linkUid !== null) params.linkUid = this.dataForm.linkUid
      if (this.dataForm.status !== '' && this.dataForm.status !== null) params.status = this.dataForm.status
      if (this.dataForm.orderSn) params.orderSn = this.dataForm.orderSn
      barterOrderRichListApi(params).then(res => {
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
    openDetail(row) {
      this.detailVisible = true
      this.detailLoading = true
      this.detail = null
      barterOrderDetailApi(row.id).then(res => {
        this.detail = res
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    copy(text) {
      const input = document.createElement('input')
      input.value = text
      document.body.appendChild(input)
      input.select()
      try {
        document.execCommand('copy')
        this.$message.success('已复制')
      } catch (e) {
        this.$message.warning('复制失败，请手动选择')
      }
      document.body.removeChild(input)
    },
    cancelHandle(row) {
      this.$prompt('请输入取消/纠纷原因', '取消或纠纷处理', { inputType: 'textarea' }).then(({ value }) => {
        barterOrderCancelApi(row.id, value || '').then(() => {
          this.$message.success('已处理')
          this.getDataList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.selWidth { width: 180px; }
.selWide { width: 280px; }
.uid { color: #999; font-size: 12px; margin-left: 4px; }
.clickable >>> .el-table__row { cursor: pointer; }
.item-cell { display: flex; align-items: center; }
.thumb { width: 34px; height: 34px; border-radius: 4px; object-fit: cover; margin-right: 8px; background: #f5f5f5; }
.item-title { font-size: 13px; color: #333; }

.drawer-body { padding: 0 20px 40px; }
.sec-title { font-size: 14px; font-weight: 600; color: #2f9e44; margin: 18px 0 10px; padding-left: 8px; border-left: 3px solid #2f9e44; }
.goods { display: flex; flex-wrap: wrap; gap: 12px; }
.goods-card { display: flex; width: 340px; border: 1px solid #eee; border-radius: 6px; padding: 8px; }
.goods-img { width: 60px; height: 60px; border-radius: 4px; object-fit: cover; background: #f5f5f5; }
.goods-info { margin-left: 10px; flex: 1; }
.goods-title { font-size: 13px; color: #333; line-height: 1.4; }
.goods-sub { font-size: 12px; color: #999; margin-top: 6px; }

.party { border: 1px solid #eee; border-radius: 6px; padding: 12px; height: 100%; }
.party-head { display: flex; align-items: center; margin-bottom: 10px; }
.avatar { width: 38px; height: 38px; border-radius: 50%; object-fit: cover; margin-right: 10px; background: #f5f5f5; }
.p-name { font-size: 14px; font-weight: 600; }
.p-role { font-size: 11px; color: #2f9e44; border: 1px solid #b7e4c7; border-radius: 3px; padding: 0 4px; margin-left: 6px; font-weight: 400; }
.p-sub { font-size: 12px; color: #999; margin-top: 3px; }
.kv { display: flex; font-size: 12px; line-height: 1.9; }
.kv .k { width: 66px; color: #999; flex-shrink: 0; }
.kv .v { flex: 1; color: #333; word-break: break-all; }
.time { color: #999; margin-left: 4px; }
.muted { color: #bbb; }

.chat { border: 1px solid #f0f0f0; border-radius: 6px; padding: 8px 12px; max-height: 320px; overflow-y: auto; background: #fafafa; }
.chat-row { padding: 8px 0; border-bottom: 1px dashed #eee; }
.chat-row:last-child { border-bottom: none; }
.chat-meta { display: flex; align-items: center; font-size: 12px; }
.chat-from { color: #2f9e44; font-weight: 600; margin-right: 8px; }
.chat-time { color: #aaa; margin-left: auto; }
.chat-content { font-size: 13px; color: #333; margin-top: 4px; line-height: 1.6; white-space: pre-wrap; }
.chat-addr { font-size: 12px; color: #999; margin-top: 2px; }

.eval { border: 1px solid #f0f0f0; border-radius: 6px; padding: 10px; margin-bottom: 8px; }
.eval-head { display: flex; justify-content: space-between; font-size: 13px; color: #333; }
.eval-content { font-size: 13px; color: #555; margin-top: 6px; line-height: 1.6; }
.eval-time { font-size: 12px; color: #aaa; margin-top: 4px; }
.empty { font-size: 13px; color: #bbb; padding: 12px 0; }
</style>
