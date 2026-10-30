<template>
  <div class="divBox">
    <!-- ==================== 筛选 ==================== -->
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="search">
      <el-form-item label="状态">
        <el-select v-model="dataForm.status" placeholder="全部" class="selWidth" clearable @change="search" size="small">
          <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="Number(k)"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="dataForm.keyword" placeholder="物品标题" class="selWidth" size="small" clearable></el-input>
      </el-form-item>
      <el-form-item label="成色">
        <el-select v-model="dataForm.conditionLevel" placeholder="全部" class="selWidth" clearable @change="search" size="small">
          <el-option v-for="(v,k) in conditionMap" :key="k" :label="v" :value="Number(k)"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="dataForm.city" placeholder="城市" class="selSmall" size="small" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" icon="el-icon-search" @click="search">查询</el-button>
        <el-button size="small" @click="reset">重置</el-button>
      </el-form-item>
      <el-form-item class="pull-right">
        <span class="pending-tip">
          待审核 <b>{{ pendingCount }}</b> 件
        </span>
      </el-form-item>
    </el-form>

    <!-- ==================== 列表 ==================== -->
    <el-table :data="dataList" border v-loading="dataListLoading" style="width: 100%;" class="clickable" @row-click="detailHandle">
      <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>

      <el-table-column label="图片" width="86" align="center">
        <template slot-scope="scope">
          <el-image
            v-if="imgList(scope.row.images).length"
            :src="imgList(scope.row.images)[0]"
            :preview-src-list="imgList(scope.row.images)"
            fit="cover"
            class="thumb" />
          <span v-else class="muted">无图</span>
        </template>
      </el-table-column>

      <el-table-column label="标题" min-width="170" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.title || ('物品 #' + scope.row.id) }}</template>
      </el-table-column>

      <el-table-column label="发布者" min-width="130">
        <template slot-scope="scope">
          <div class="user-cell">
            <img :src="imgUrl(scope.row.ownerAvatar)" class="user-avatar" />
            <div class="user-meta">
              <div class="user-name">{{ scope.row.ownerNickname || '未知用户' }}</div>
              <div class="user-uid">UID {{ scope.row.uid }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="信用分" width="80" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.ownerCreditScore != null" size="mini" :type="scope.row.ownerCreditScore >= 80 ? 'success' : 'warning'">
            {{ scope.row.ownerCreditScore }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="交换次数" width="90" align="center">
        <template slot-scope="scope">{{ scope.row.ownerExchangeCount != null ? scope.row.ownerExchangeCount : '-' }}</template>
      </el-table-column>

      <el-table-column label="状态" width="95" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="mini">{{ statusMap[scope.row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="置顶" width="70" align="center">
        <template slot-scope="scope">{{ scope.row.toping ? '是' : '否' }}</template>
      </el-table-column>

      <el-table-column prop="createTime" label="发布时间" width="155" align="center"></el-table-column>

      <el-table-column fixed="right" label="操作" width="200" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click.stop="detailHandle(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 2" type="text" size="small" @click.stop="auditPass(scope.row.id)">通过</el-button>
          <el-button v-if="scope.row.status === 2" type="text" size="small" @click.stop="auditReject(scope.row)">驳回</el-button>
          <el-button v-if="scope.row.status === 1" type="text" size="small" @click.stop="down(scope.row.id)">下架</el-button>
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

    <!-- ==================== 详情弹窗 ==================== -->
    <el-dialog title="物品详情" :visible.sync="detailVisible" width="720px" append-to-body :destroy-on-close="true">
      <div v-if="detail" class="detail">
        <div class="detail-imgs">
          <el-image
            v-for="(src, i) in imgList(detail.images)"
            :key="i"
            :src="src"
            :preview-src-list="imgList(detail.images)"
            fit="cover"
            class="detail-img" />
          <div v-if="!imgList(detail.images).length" class="detail-img empty-img">无图</div>
        </div>
        <div class="row"><span class="label">ID</span><span>{{ detail.id }}</span></div>
        <div class="row"><span class="label">标题</span><span>{{ detail.title }}</span></div>
        <div class="row"><span class="label">描述</span><span class="desc">{{ detail.description || '-' }}</span></div>
        <div class="row"><span class="label">成色</span><span>{{ conditionMap[detail.conditionLevel] || detail.conditionLevel || '-' }}</span></div>
        <div class="row"><span class="label">城市</span><span>{{ detail.city || '-' }}</span></div>
        <div class="row"><span class="label">期望换取</span><span>{{ detail.expectDesc || '-' }}</span></div>
        <div class="row">
          <span class="label">标签</span>
          <span>
            <el-tag v-for="(t,i) in tagList(detail.tags)" :key="i" size="mini" class="tag">{{ t }}</el-tag>
            <span v-if="!tagList(detail.tags).length">-</span>
          </span>
        </div>
        <div class="row"><span class="label">状态</span><span>{{ statusMap[detail.status] || '未知' }}</span></div>
        <div class="row" v-if="detail.rejectReason"><span class="label">驳回原因</span><span class="reject">{{ detail.rejectReason }}</span></div>
        <div class="row"><span class="label">置顶积分</span><span>{{ detail.topPoints != null ? detail.topPoints : '-' }}</span></div>
        <div class="row"><span class="label">浏览 / 想要</span><span>{{ detail.viewCount || 0 }} / {{ detail.wantCount || 0 }}</span></div>
        <div class="row">
          <span class="label">发布者</span>
          <span>
            {{ detail.ownerNickname || '-' }}
            （{{ detail.ownerLevelName || '未定级' }}，交换 {{ detail.ownerExchangeCount || 0 }} 次，信用
            {{ detail.ownerCreditScore != null ? detail.ownerCreditScore : '-' }}）
          </span>
        </div>
        <div class="row"><span class="label">发布时间</span><span>{{ detail.createTime }}</span></div>
      </div>
      <span slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
        <template v-if="detail">
          <el-button v-if="detail.status === 2" type="danger" size="small" @click="auditReject(detail)">驳回</el-button>
          <el-button v-if="detail.status === 2" type="primary" size="small" @click="auditPass(detail.id)">审核通过</el-button>
        </template>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  barterItemListApi,
  barterItemInfoApi,
  barterItemAuditPassApi,
  barterItemAuditRejectApi,
  barterItemDownApi
} from '@/api/barter'
import { imgUrl, imgList } from '@/utils/barterImg'

export default {
  name: 'BarterItem',
  data() {
    return {
      // 默认只看「审核中」，需要查别的状态用上面的下拉切
      dataForm: { keyword: '', status: 2, conditionLevel: '', city: '', page: 1, limit: 10 },
      dataList: [],
      total: 0,
      dataListLoading: false,
      pendingCount: 0,
      detailVisible: false,
      detail: null,
      // 物品状态：1 在售 2 审核中 3 已驳回 4 已换出 5 交换中 6 已下架
      statusMap: { 1: '在售', 2: '审核中', 3: '已驳回', 4: '已换出', 5: '交换中', 6: '已下架' },
      conditionMap: { 1: '全新', 2: '九新', 3: '八新', 4: '七新及以下', 5: '其他' }
    }
  },
  mounted() {
    this.getDataList()
    this.loadPendingCount()
  },
  methods: {
    imgUrl,
    imgList,
    statusTag(s) {
      return { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info', 5: 'primary', 6: 'info' }[s] || 'info'
    },
    tagList(tags) {
      if (!tags) return []
      return String(tags).split(',').map(s => s.trim()).filter(Boolean)
    },
    search() {
      this.dataForm.page = 1
      this.getDataList()
    },
    reset() {
      this.dataForm = { keyword: '', status: 2, conditionLevel: '', city: '', page: 1, limit: 10 }
      this.getDataList()
    },
    getDataList() {
      this.dataListLoading = true
      const params = { page: this.dataForm.page, limit: this.dataForm.limit }
      if (this.dataForm.keyword) params.keyword = this.dataForm.keyword
      if (this.dataForm.status !== '' && this.dataForm.status !== null) params.status = this.dataForm.status
      if (this.dataForm.conditionLevel !== '' && this.dataForm.conditionLevel !== null) params.conditionLevel = this.dataForm.conditionLevel
      if (this.dataForm.city) params.city = this.dataForm.city
      barterItemListApi(params).then(res => {
        this.dataList = res.list || []
        this.total = res.total || 0
        this.dataListLoading = false
      }).catch(() => { this.dataListLoading = false })
    },
    // 角标与筛选无关，单独查一次总数
    loadPendingCount() {
      barterItemListApi({ page: 1, limit: 1, status: 2 }).then(res => {
        this.pendingCount = res.total || 0
      }).catch(() => {})
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
    detailHandle(row) {
      barterItemInfoApi({ id: row.id }).then(res => {
        this.detail = res
        this.detailVisible = true
      })
    },
    refreshAll() {
      this.loadPendingCount()
      this.getDataList()
    },
    auditPass(id) {
      this.$confirm('确认通过该物品上架？', '提示').then(() => {
        barterItemAuditPassApi(id).then(() => {
          this.$message.success('已通过')
          this.detailVisible = false
          this.refreshAll()
        })
      }).catch(() => {})
    },
    auditReject(row) {
      this.$prompt('请输入驳回原因（会通知发布者）', '驳回', {
        inputType: 'textarea',
        inputValidator: v => (v && v.trim() ? true : '请填写驳回原因，便于用户修改'),
        inputPlaceholder: '例如：图片模糊、描述与实物不符'
      }).then(({ value }) => {
        barterItemAuditRejectApi(row.id, value || '').then(() => {
          this.$message.success('已驳回')
          this.detailVisible = false
          this.refreshAll()
        })
      }).catch(() => {})
    },
    down(id) {
      this.$confirm('确认下架该物品？', '提示').then(() => {
        barterItemDownApi(id).then(() => {
          this.$message.success('已下架')
          this.detailVisible = false
          this.refreshAll()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.selWidth { width: 160px; }
.selSmall { width: 120px; }
.muted { color: #bbb; font-size: 12px; }
.pull-right { float: right; }
.pending-tip { font-size: 13px; color: #666; }
.pending-tip b { color: #e6a23c; font-size: 16px; margin: 0 2px; }

.clickable >>> .el-table__row { cursor: pointer; }
.thumb { width: 52px; height: 52px; border-radius: 4px; border: 1px solid #f0f0f0; background: #f7f7f7; }

.user-cell { display: flex; align-items: center; }
.user-avatar {
  width: 30px; height: 30px; border-radius: 50%; object-fit: cover;
  background: #f5f5f5; margin-right: 8px; border: 1px solid #eee; flex-shrink: 0;
}
.user-meta { min-width: 0; }
.user-name { font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-uid { font-size: 12px; color: #999; }

.detail { max-height: 64vh; overflow-y: auto; }
.detail-imgs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.detail-img { width: 96px; height: 96px; border-radius: 6px; border: 1px solid #eee; background: #f7f7f7; }
.empty-img { display: flex; align-items: center; justify-content: center; font-size: 12px; color: #bbb; }
.row { display: flex; padding: 7px 0; border-bottom: 1px dashed #f0f0f0; font-size: 13px; line-height: 1.6; }
.row .label { width: 92px; color: #999; flex-shrink: 0; }
.row span:last-child { flex: 1; color: #333; word-break: break-all; }
.row .desc { max-height: 90px; overflow-y: auto; }
.reject { color: #f56c6c; }
.tag { margin-right: 4px; }
</style>
