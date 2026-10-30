<template>
  <div class="divBox">
    <div class="mb20">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="addHandle()">新增品类</el-button>
    </div>

    <el-table :data="dataList" border v-loading="dataListLoading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" align="center"></el-table-column>
      <el-table-column label="父级" width="120">
        <template slot-scope="scope">{{ scope.row.pid === 0 ? '顶级品类' : (parentName(scope.row.pid)) }}</template>
      </el-table-column>
      <el-table-column prop="name" label="品类名称" min-width="140"></el-table-column>
      <el-table-column prop="icon" label="图标" width="100">
        <template slot-scope="scope"><span v-if="scope.row.icon">{{ scope.row.icon }}</span><span v-else>-</span></template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center"></el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope"><el-tag :type="scope.row.status===1?'success':'info'" size="mini">{{ scope.row.status===1?'启用':'禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="140" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="editHandle(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="deleteHandle(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" append-to-body @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="父级品类">
          <el-select v-model="form.pid" placeholder="顶级品类" size="small" clearable style="width:100%">
            <el-option :value="0" label="顶级品类"></el-option>
            <el-option v-for="p in parentOptions" :key="p.id" :value="p.id" :label="p.name" :disabled="form.id===p.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="品类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：数码电子" size="small"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标标识" size="small"></el-input>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" size="small"></el-input-number>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"></el-switch>
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
  import { barterCategoryListApi, barterCategorySaveApi, barterCategoryUpdateApi, barterCategoryDeleteApi } from '@/api/barter'

  export default {
    data() {
      return {
        dataList: [],
        dataListLoading: false,
        dialogVisible: false,
        dialogTitle: '新增品类',
        form: { id: null, pid: 0, name: '', icon: '', sort: 0, status: 1 },
        rules: { name: [{ required: true, message: '请输入品类名称', trigger: 'blur' }] }
      }
    },
    computed: {
      parentOptions() {
        return this.dataList.filter(c => c.pid === 0)
      }
    },
    mounted() {
      this.getDataList()
    },
    methods: {
      getDataList() {
        this.dataListLoading = true
        barterCategoryListApi().then(res => {
          this.dataList = res || []
          this.dataListLoading = false
        }).catch(() => { this.dataListLoading = false })
      },
      parentName(pid) {
        const p = this.dataList.find(c => c.id === pid)
        return p ? p.name : pid
      },
      resetForm() {
        this.form = { id: null, pid: 0, name: '', icon: '', sort: 0, status: 1 }
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      },
      addHandle() {
        this.resetForm()
        this.dialogTitle = '新增品类'
        this.dialogVisible = true
      },
      editHandle(row) {
        this.form = { ...row }
        this.dialogTitle = '编辑品类'
        this.dialogVisible = true
      },
      submitForm() {
        this.$refs.formRef.validate(valid => {
          if (!valid) return
          const api = this.form.id ? barterCategoryUpdateApi : barterCategorySaveApi
          api(this.form).then(() => {
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.getDataList()
          })
        })
      },
      deleteHandle(row) {
        this.$confirm('确认删除该品类？', '提示').then(() => {
          barterCategoryDeleteApi({ id: row.id }).then(() => {
            this.$message.success('已删除')
            this.getDataList()
          })
        }).catch(() => {})
      }
    }
  }
</script>

<style scoped>
  .mb20 { margin-bottom: 20px; }
</style>
