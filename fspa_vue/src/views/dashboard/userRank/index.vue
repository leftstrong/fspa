<template>
  <div class="divBox">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <div class="container">
          <el-form :inline="true">
            <el-form-item label="时间选择：" class="width100">
              <el-radio-group v-model="tableFrom.dateLimit" type="button"  @change="selectChange(tableFrom.dateLimit)" class="mr20"  size="small">
                <el-radio-button :label=item.val v-for="(item,i) in fromList.fromTxt" :key="i">{{item.text}}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </div>
      <el-table
        v-loading="listLoading"
        :data="tableData.data"
        style="width: 100%"
        size="mini"
        class="table"
      >
     <el-table-column
          prop="uid"
          label="ID"
          width="50"
        />
       <el-table-column label="头像" min-width="80">
          <template slot-scope="scope">
            <div class="demo-image__preview">
              <el-image
                style="width: 36px; height: 36px"
                :src="scope.row.user.avatar"
                :preview-src-list="[scope.row.user.avatar]"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="昵称" 
        prop="nickname" 
        min-width="100" 
        :show-overflow-tooltip="true">
        </el-table-column>
        <el-table-column
          prop="phone"
          label="手机号"
          min-width="90"
          align="center"
        />
          <el-table-column
          prop="nowMoney"
          label="账户余额"
          min-width="90"
          align="center"
        />
        <el-table-column
          prop="currentOrderCount"
          label="订单数"
          min-width="90"
          align="center"
        />
        <el-table-column
          prop="currentConsumeCount"
          label="消费金额"
          min-width="90"
          align="center"
        />   
        <!-- <el-table-column
          prop="allConsumeCount"
          label="总消费金额"
          min-width="90"
          align="center"
        />         -->
      </el-table>
      <div class="block">
        <el-pagination
          :page-sizes="[20, 40, 60, 80]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { rankdetail } from '@/api/user';
import { formatDates } from '@/utils/index';
export default {
  name: 'UserRank',
  filters: {
    formatDate (time) {
      if (time !== 0) {
        const date = new Date(time * 1000);
        return formatDates(date, 'yyyy-MM-dd hh:mm');
      }
    }
  },
  data() {
    return {
      merCateList: [],
      props: {
        children: 'child',
        label: 'name',
        value: 'id',
        emitPath: false
      },
      fromList: this.$constants.fromList,
      tableData: {
        data: [],
        total: 0
      },
      listLoading: true,
      tableFrom: {
        page: 1,
        limit: 20,
        isReply: '',
        dateLimit: '',
        // uid: '',
        nickname: '',
        productSearch:'',
        isDel: false
      },
      timeVal: [],
      loading: false,
      uids: [],
      options: [],
      dialogVisible: false,
      timer: ''
    }
  },
  watch: {
    $route(to, from) {
      this.getList()
    }
  },
  mounted() {
    // this.getLstFilterApi()
    this.getList()
  },
  methods:{
    seachList() {
      this.dialogVisible = false
      this.tableFrom.page = 1
      this.getList()
    },
    // 选择时间
    selectChange (tab) {
      this.timeVal = [];
      this.tableFrom.page = 1
      this.getList();
    },
    // 具体日期
    onchangeTime (e) {
      this.timeVal = e;
      this.tableFrom.dateLimit = e ? this.timeVal.join(',') : ''
      this.tableFrom.page = 1
      this.getList();
    },
    // 列表
    getList() {
      this.listLoading = true
      // this.tableFrom.uid = this.uids.join(',')
      rankdetail(this.tableFrom.dateLimit).then(res => {
        this.tableData.data = res
        // this.tableData.total = 1
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },
    pageChange(page) {
      this.tableFrom.page = page
      this.getList()
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList()
    },
  }
}
</script>

<style scoped lang="scss">
  .selWidth{
    width: 350px !important;
  }
  .table{
    .el-image{
      width: auto !important;
      height: 30px !important;
    }
    ::v-deep.el-image__inner, .el-image__placeholder, .el-image__error {
      width: auto !important;
    }
  }
  .info{
    width:63%;
  }

</style>
