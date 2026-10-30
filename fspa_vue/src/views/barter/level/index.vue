<template>
  <div class="divBox">
    <div class="mb20">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="addHandle()">新增等级</el-button>
      <span class="tip">等级按「成功交换次数」自动匹配：min ≤ 次数 < max（max=0 表示无上限）</span>
    </div>

    <el-table :data="dataList" border v-loading="dataListLoading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
      <el-table-column prop="name" label="等级名称" min-width="120"></el-table-column>
      <el-table-column prop="icon" label="图标" width="80" align="center">
        <template slot-scope="scope"><span v-if="scope.row.icon">{{ scope.row.icon }}</span><span v-else>-</span></template>
      </el-table-column>
      <el-table-column label="交换次数区间" width="180" align="center">
        <template slot-scope="scope">{{ scope.row.minCount }} ~ {{ scope.row.maxCount === 0 ? '∞' : scope.row.maxCount }}</template>
      </el-table-column>
      <el-table-column prop="exposureWeight" label="曝光权重" width="100" align="center"></el-table-column>
      <el-table-column label="徽章色" width="90" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.badgeColor" class="badge-dot" :style="{ background: scope.row.badgeColor }"></span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center"></el-table-column>
      <el-table-column label="自动匹配" width="90" align="center">
        <template slot-scope="scope"><el-tag :type="scope.row.autoMatch===1?'success':'info'" size="mini">{{ scope.row.autoMatch===1?'自动':'手动' }}</el-tag></template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="140" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="editHandle(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="deleteHandle(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" append-to-body @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="等级名称" prop="name">
          <el-input v-model="form.name" placeholder="如：青铜换友" size="small"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标标识" size="small"></el-input>
        </el-form-item>
        <el-form-item label="最小交换次数" prop="minCount">
          <el-input-number v-model="form.minCount" :min="0" size="small"></el-input-number>
        </el-form-item>
        <el-form-item label="最大交换次数" prop="maxCount">
          <el-input-number v-model="form.maxCount" :min="0" size="small"></el-input-number>
          <span class="tip">0 表示无上限</span>
        </el-form-item>
        <el-form-item label="曝光权重">
          <el-input-number v-model="form.exposureWeight" :min="0" size="small"></el-input-number>
        </el-form-item>
        <el-form-item label="徽章颜色">
          <el-color-picker v-model="form.badgeColor"></el-color-picker>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" size="small"></el-input-number>
        </el-form-item>
        <el-form-item label="自动匹配">
          <el-switch v-model="form.autoMatch" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" size="small" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
  import { barterLevelListApi, barterLevelSaveApi, barterLevelUpdateApi, barterLevelDeleteApi } from '@/api/barter'

  export default {
    data() {
      return {
        dataList: [],
        dataListLoading: false,
        dialogVisible: false,
        dialogTitle: '新增等级',
        form: {
          id: null,
          name: '',
          icon: '',
          minCount: 0,
          maxCount: 0,
          exposureWeight: 0,
          badgeColor: '#93ac82',
          sort: 0,
          autoMatch: 1
        },
        rules: {
          name: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
          minCount: [{ required: true, message: '请输入最小次数', trigger: 'blur' }],
          maxCount: [{ required: true, message: '请输入最大次数', trigger: 'blur' }]
        }
      }
    },
    mounted() {
      this.getDataList()
    },
    methods: {
      getDataList() {
        this.dataListLoading = true
        barterLevelListApi().then(res => {
          this.dataList = res || []
          this.dataListLoading = false
        }).catch(() => { this.dataListLoading = false })
      },
      resetForm() {
        this.form = { id: null, name: '', icon: '', minCount: 0, maxCount: 0, exposureWeight: 0, badgeColor: '#93ac82', sort: 0, autoMatch: 1 }
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      },
      addHandle() {
        this.resetForm()
        this.dialogTitle = '新增等级'
        this.dialogVisible = true
      },
      editHandle(row) {
        this.form = { ...row }
        this.dialogTitle = '编辑等级'
        this.dialogVisible = true
      },
      submitForm() {
        this.$refs.formRef.validate(valid => {
          if (!valid) return
          const api = this.form.id ? barterLevelUpdateApi : barterLevelSaveApi
          api(this.form).then(() => {
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.getDataList()
          })
        })
      },
      deleteHandle(row) {
        // 兜底等级：matchLevelId 未命中区间时用它，删除会导致用户无等级
        if (row.id === 1) {
          this.$message.warning('默认等级不可删除')
          return
        }
        this.$confirm(`确认删除等级「${row.name}」？该等级下的用户将按成功交换次数重新匹配等级。`, '提示', {
          type: 'warning',
          confirmButtonText: '确定删除',
          cancelButtonText: '取消'
        }).then(() => {
          return barterLevelDeleteApi(row.id)
        }).then(() => {
          this.$message.success('删除成功')
          this.getDataList()
        }).catch(() => {})
      }
    }
  }
</script>

<style scoped>
  .mb20 { margin-bottom: 20px; }
  .tip { color: #999; font-size: 12px; margin-left: 8px; }
  .badge-dot { display: inline-block; width: 14px; height: 14px; border-radius: 50%; vertical-align: middle; }
</style>
