<template>
  <!-- 基于 Element UI 新增和修改弹窗 -->
  <el-dialog
    :title="!dataForm.id ? '新增' : '编辑'"
    :close-on-click-modal="false"
    :visible.sync="visible" @close="cancel">
    <!-- 新增和创建表单表单 -->
    <el-form :model="dataForm" :rules="dataRule" ref="dataForm" @keyup.enter.native="dataSubmit()" label-width="80px">
    <el-form-item label="所属门店" prop="storeId">
      <el-select v-model="dataForm.storeId" placeholder="请选择"  class="selWidth" clearable filterable>
        <el-option
        :value="item.id" v-for="(item, index) in storeSelectList" :key="index" :label="item.name"
          >
        </el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="用户头像" prop="avatar">
      <div class="upLoadPicBox" @click="modalPicTap('1')">
        <div class="pictrue" v-if="dataForm.avatar"><img :src="dataForm.avatar"></div>
        <div v-else class="upLoad">
          <i class="el-icon-camera cameraIconfont" />
        </div>
      </div>
    </el-form-item>
    <el-form-item label="手机号码" prop="phone">
      <el-input v-model="dataForm.phone" placeholder="手机号码"></el-input>
    </el-form-item>
    <el-form-item label="真实姓名" prop="realName">
      <el-input v-model="dataForm.realName" placeholder="真实姓名"></el-input>
    </el-form-item>
    <el-form-item label="工号" prop="jobNumber">
      <el-input v-model="dataForm.jobNumber" placeholder="工号"></el-input>
    </el-form-item>
    <el-form-item label="用户昵称" prop="nickname">
      <el-input v-model="dataForm.nickname" placeholder="用户昵称"></el-input>
    </el-form-item>
      <!-- <el-input v-model="dataForm.avatar" placeholder="用户头像"></el-input> -->
    <!-- <el-form-item label="门店id" prop="storeId">
      <el-input v-model="dataForm.storeId" placeholder="门店id"></el-input>
    </el-form-item> -->
    <el-form-item label="专业" prop="subject">
      <el-input v-model="dataForm.subject" placeholder="专业"></el-input>
    </el-form-item>
    <!-- <el-form-item label="状态" prop="status">
      <el-input v-model="dataForm.status" placeholder="1为正常，0为禁止"></el-input>
    </el-form-item> -->
    <!-- <el-form-item label="创建时间" prop="createTime">
      <el-input v-model="dataForm.createTime" placeholder="创建时间"></el-input>
    </el-form-item>
    <el-form-item label="更新时间" prop="updateTime">
      <el-input v-model="dataForm.updateTime" placeholder="更新时间"></el-input>
    </el-form-item> -->
   <el-form-item label="值班日期">
    <div>
<!--      <label v-for="(date, index) in dates" :key="index" style="margin-right: 10px;">
        <input type="radio" :value="date" v-model="selectedDate">
        {{ formatDate(date) }}
      </label>-->
      <el-row>
        <el-col :span="4">
          <el-tree
            ref="timeTree0"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[0]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange0()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree1"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[1]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange1()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree2"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[2]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange2()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree3"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[3]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange3()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree4"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[4]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange4()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree5"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[5]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange5()"
            :props="defaultProps">
          </el-tree>
        </el-col>
        <el-col :span="4">
          <el-tree
            ref="timeTree6"
            :highlight-current="true"
            :data="[emptyWorkTimeMapList[6]]"
            show-checkbox
            node-key="id"
            :default-expanded-keys="expandedKeys"
            :default-checked-keys="checkedKeys"
            @check-change="checkChange6()"
            :props="defaultProps">
          </el-tree>
        </el-col>
      </el-row>

    </div>
    </el-form-item>
<!--    <el-form-item label="值班时间">
      &lt;!&ndash; <el-checkbox :indeterminate="isIndeterminate" v-model="checkAll" @change="handleCheckAllChange">全选</el-checkbox>
      <div style="margin: 15px 0;"></div>
      <el-checkbox-group v-model="checkedWorkTime" @change="handleCheckedDayChange">
        <el-checkbox v-for="city in timeOptions" :label="city" :key="city">{{city}}</el-checkbox>
      </el-checkbox-group> &ndash;&gt;
      <label v-for="(date, index) in timeOptions" :key="index" style="margin-right: 10px;">
        <input type="radio" :value="date" v-model="checkedWorkTime" @change="handleCheckedDayChange">
        {{ date }}
      </label>
    </el-form-item>-->
   <el-form-item label="值班列表" v-if="false">
    <el-input
      type="textarea"
      :rows="3"
      v-model="selectedTimesText">
    </el-input>
   </el-form-item>
    <el-form-item label="状态">
        <el-radio-group v-model="dataForm.status">
          <el-radio :label="1">开启</el-radio>
          <el-radio :label="0">关闭</el-radio>
        </el-radio-group>
    </el-form-item>
  </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
import {artisanCreate, artisanDetail} from "@/api/artisan";
import {storeListApi} from '@/api/storePoint'
export default {
    data () {
      return {
        selectedTimesText: '',
        selectedDate: '',
        visible: false,
        storeSelectList:[],
        timeOptions: ['9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00'],
        checkAll: false,
        isIndeterminate: false,
        checkedWorkTime: '',
        visible: false,
        checkedWorkDay: '',
        dataForm: {
          id: 0,
          phone: '' ,
          realName: '' ,
          nickname: '' ,
          avatar: '' ,
          storeId: '' ,
          subject: '' ,
          status: '' ,
          jobNumber: '',
          workTime: ''
        },
        dataRule: {
          phone: [
            { required: true, message: '手机号码  为必填项', trigger: 'blur' }
          ],
          realName: [
            { required: true, message: '真实姓名  为必填项', trigger: 'blur' }
          ],
          nickname: [
            { required: true, message: '用户昵称  为必填项', trigger: 'blur' }
          ],
          avatar: [
            { required: true, message: '用户头像  为必填项', trigger: 'blur' }
          ],
          jobNumber: [
            { required: true, message: '用户头像  为必填项', trigger: 'blur' }
          ],
          storeId: [
            { required: true, message: '门店  为必填项', trigger: 'blur' }
          ],
          subject: [
            { required: true, message: '专业  为必填项', trigger: 'blur' }
          ],
          status: [
            { required: true, message: '1为正常，0为禁止  为必填项', trigger: 'blur' }
          ]
          // createTime: [
          //   { required: true, message: '创建时间  为必填项', trigger: 'blur' }
          // ],
          // updateTime: [
          //   { required: true, message: '更新时间  为必填项', trigger: 'blur' }
          // ]
        },
        workTimeMap:{},
        workTimeMapList:[],
        emptyWorkTimeMapList:[],
        treeData:[],
        expandedKeys:[],
        checkedKeys: [],
        defaultProps:{
          children: 'timeList',
          label: 'label'
        },
        timeTree0DateTime:[],
        timeTree1DateTime:[],
        timeTree2DateTime:[],
        timeTree3DateTime:[],
        timeTree4DateTime:[],
        timeTree5DateTime:[],
        timeTree6DateTime:[],
      }
    },
     computed: {
      dates() {
        const today = new Date()
        const dates = []
        for (let i = 0; i < 7; i++) {
          const date = new Date(today.getTime() + i * 24 * 60 * 60 * 1000)
          dates.push(date)
        }
        return dates
      }
    //   selectedTimesText() {
    //   return this.checkedWorkTime.join(', ');
    // }
  },
    methods: {
    handleClose() {
      this.visible = false
    },
    //取消
      cancel (){
      this.visible = false;
      this.selectedDate = '',
      this.checkedWorkTime = '',
      this.selectedTimesText = ''
    },
    handleRadioSelection() {
      // 单选框选中时触发的事件处理函数
      //  const month = String(date.getMonth() + 1).padStart(2, '0')
      // const day = String(date.getDate()).padStart(2, '0')
      // return `${year}-${month}-${day}`
      // alert(this.selectedDate)
      // this.checkedWorkTime = []; // Reset the selected work time options
    },
     formatDate(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    //  handleCheckAllChange(checked) {
    //   this.checkedWorkTime = checked ? [...this.timeOptions] : [];
    //   this.isIndeterminate = false;
    //   },
      handleCheckedDayChange() {
      // const totalTimes = this.timeOptions.length;
      // const checkedTimesCount = checkedTimes.length;
      // this.checkAll = checkedTimesCount === totalTimes;
      // this.isIndeterminate = checkedTimesCount > 0 && checkedTimesCount < totalTimes;
        if (this.selectedDate === '') {
          alert("请先选择日期");
          this.checkedWorkTime = '';
          return;
        }
          const selectedDateText = this.formatDate(this.selectedDate);
          if (selectedDateText === '' || selectedDateText === null) {
            return;
          }
          if (this.selectedTimesText === null) {
            this.selectedTimesText = '';
          }
          this.selectedTimesText += selectedDateText +" "+ this.checkedWorkTime + ",";
      },
      // handleCheckedTimeChange(value) {
        // alert(this.checkedWorkDay);
        // let checkedCount = value.length;
        // this.checkAll = checkedCount === this.cities.length;
        // this.isIndeterminate = checkedCount > 0 && checkedCount < this.cities.length;
      // },
          //上传图片
      modalPicTap (tit) {
        const _this = this
        this.$modalUpload(function(img) {
          _this.dataForm.avatar = img[0].sattDir
        },tit, 'appointment')
      },
      init (id) { // 初始化表单验证规则
        this.dataForm.id = id || 0
        this.visible = true
         this.storeList();
        this.$nextTick(() => {
          this.$refs['dataForm'].resetFields()
          if (this.dataForm.id) {
            const pram = {
               id: this.dataForm.id
            }
            artisanDetail(pram).then(res => {
              this.workTimeMap = res.workTimeMap
              this.expandedKeys = res.expandedKeys;
              this.checkedKeys = res.checkedKeys;
              this.emptyWorkTimeMapList = res.emptyWorkTimeMapList
              this.dataForm = res;
              this.selectedTimesText = res.workTime;
            })
          }
        })
      },
      storeList() {
        let artFrom =  {
            page: 1,
            limit: 9999,
            status: '1',
            keywords: ''
        };
        storeListApi(artFrom).then(res=>{
          this.storeSelectList = res.list;
        })
      },
      // 表单数据提交
      dataSubmit () {
        this.$refs['dataForm'].validate((valid) => {
          if (valid) {
            this.checkChange0()
            this.checkChange1()
            this.checkChange2()
            this.checkChange3()
            this.checkChange4()
            this.checkChange5()
            this.checkChange6()
            const selectedTimeTextArr =[]
            const selectedTimeText0 = this.timeTree0DateTime.join(",")
            if (selectedTimeText0){
              selectedTimeTextArr.push(selectedTimeText0)
            }
            const selectedTimeText1 = this.timeTree1DateTime.join(",")
            if (selectedTimeText1){
              selectedTimeTextArr.push(selectedTimeText1)
            }
            const selectedTimeText2 = this.timeTree2DateTime.join(",")
            if (selectedTimeText2){
              selectedTimeTextArr.push(selectedTimeText2)
            }
            const selectedTimeText3 = this.timeTree3DateTime.join(",")
            if (selectedTimeText3){
              selectedTimeTextArr.push(selectedTimeText3)
            }
            const selectedTimeText4 = this.timeTree4DateTime.join(",")
            if (selectedTimeText4){
              selectedTimeTextArr.push(selectedTimeText4)
            }
            const selectedTimeText5 = this.timeTree5DateTime.join(",")
            if (selectedTimeText5){
              selectedTimeTextArr.push(selectedTimeText5)
            }
            const selectedTimeText6 = this.timeTree6DateTime.join(",")
            if (selectedTimeText6){
              selectedTimeTextArr.push(selectedTimeText6)
            }
            this.dataForm.workTime = selectedTimeTextArr.join(",");
            artisanCreate(this.dataForm).then(res =>{
                // TODO 保存数据
                  this.$message.success('保存数据成功')
                   this.visible = false;
            });
          }
        })
      },
      checkChange0() {
        let res = this.$refs.timeTree0.getCheckedNodes()
        this.timeTree0DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree0DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree0DateTime
      },
      checkChange1() {
        let res = this.$refs.timeTree1.getCheckedNodes()
        this.timeTree1DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree1DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree1DateTime
      },
      checkChange2() {
        let res = this.$refs.timeTree2.getCheckedNodes()
        this.timeTree2DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree2DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree2DateTime
      },
      checkChange3() {
        let res = this.$refs.timeTree3.getCheckedNodes()
        this.timeTree3DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree3DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree2DateTime
      },
      checkChange4() {
        let res = this.$refs.timeTree4.getCheckedNodes()
        this.timeTree4DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree4DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree4DateTime
      },
      checkChange5() {
        let res = this.$refs.timeTree5.getCheckedNodes()
        this.timeTree5DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree5DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree5DateTime
      },
      checkChange6() {
        let res = this.$refs.timeTree6.getCheckedNodes()
        this.timeTree6DateTime =[]
        if (res.length>0){
          res.forEach(item=>{
            let dateTime = item.dateTime
            if (dateTime && dateTime!==''){
              this.timeTree6DateTime.push(dateTime)
            }
          })
        }
        return this.timeTree6DateTime
      },
    }
  }
</script>
