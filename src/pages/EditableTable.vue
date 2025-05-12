<template>
  <div>
    <VmTable title="审核竞品申请" 
             type="review" 
           :columns="dataColumns" 
           :data="dataTable"
             v-on:details-ok="showDetails"
             v-on:approve-ok="approveCopyright"
             v-on:reject-ok="rejectCopyright"
           class="vm-margin">
  </VmTable>
    
    <!-- 竞品详情弹窗 -->
    <Modal v-model="detailsModal" title="竞品详情" width="600">
      <div v-if="selectedCopyright">
        <div class="copyright-image" v-if="selectedCopyright.imgUrl">
          <img :src="require('../assets/img/bg.jpg')" style="max-width: 100%; max-height: 300px;" />
        </div>
        <div class="copyright-info">
          <p><strong>标题：</strong> {{ selectedCopyright.title }}</p>
          <p><strong>描述：</strong> {{ selectedCopyright.description }}</p>
          <p><strong>类别：</strong> {{ selectedCopyright.category }}</p>
          <p><strong>状态：</strong> {{ selectedCopyright.status }}</p>
          <p><strong>拥有者地址：</strong> {{ selectedCopyright.ownerAddress }}</p>
          <p><strong>拥有者ID：</strong> {{ selectedCopyright.userId }}</p>
          <p><strong>起拍价：</strong> {{ selectedCopyright.startPrice }} ETH</p>
          <p><strong>拍卖开始时间：</strong> {{ formatDateTime(selectedCopyright.auctionStartTime) }}</p>
          <p><strong>拍卖结束时间：</strong> {{ formatDateTime(selectedCopyright.auctionEndTime) }}</p>
        </div>
      </div>
      <div slot="footer">
        <Button type="primary" @click="detailsModal = false">关闭</Button>
      </div>
    </Modal>
    
    <!-- 驳回原因弹窗 -->
    <Modal v-model="rejectModal" title="驳回原因">
      <Input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因..."></Input>
      <div slot="footer">
        <Button @click="rejectModal = false">取消</Button>
        <Button type="error" @click="confirmReject">确认驳回</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
  import VmTable from '@/components/vm-table'
  import axios from 'axios'
  
  export default {
    name: 'EditableTable',
    components: {
      VmTable
    },
    methods: {
      loadPendingCopyrights() {
        axios.get('/api/jdbc/copyright/pending')
          .then(response => {
            this.dataTable = response.data
          })
          .catch(error => {
            console.error('Failed to load pending items:', error)
          })
      },
      showDetails(copyright) {
        this.selectedCopyright = copyright
        this.detailsModal = true
      },
      approveCopyright(copyright) {
        axios.post(`/api/jdbc/copyright/${copyright.id}/review`, null, {
          params: {
            status: 'APPROVED'
          }
        })
        .then(response => {
          this.$Message.success('竞品已通过审核')
          this.loadPendingCopyrights()
        })
        .catch(error => {
          this.$Message.error('审核操作失败')
          console.error('Approval failed:', error)
        })
      },
      rejectCopyright(copyright) {
        this.copyrightToReject = copyright
        this.rejectModal = true
      },
      confirmReject() {
        if (!this.rejectReason) {
          this.$Message.warning('请输入驳回原因')
          return
        }
        axios.post(`/api/jdbc/copyright/${this.copyrightToReject.id}/review`, null, {
          params: {
            status: 'REJECTED',
            reason: this.rejectReason
          }
        })
        .then(response => {
          this.$Message.success('竞品已驳回')
          this.rejectModal = false
          this.rejectReason = ''
          this.copyrightToReject = null
          this.loadPendingCopyrights()
        })
        .catch(error => {
          this.$Message.error('驳回操作失败')
          console.error('Rejection failed:', error)
        })
      },
      formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '';
        const date = new Date(dateTimeStr);
        return date.toLocaleString();
      }
    },
    data () {
      return {
        dataColumns: [
          {
            id: '20156541',
            title: 'ID',
            key: 'id'
          },
          {
            id: '20156542',
            title: '标题',
            key: 'title'
          },
          {
            id: '20156543',
            title: '状态',
            key: 'status'
          },
          {
            id: '20156544',
            title: '类别',
            key: 'category'
          }
        ],
        dataTable: [],
        selectedCopyright: null,
        detailsModal: false,
        rejectModal: false,
        rejectReason: '',
        copyrightToReject: null
      }
    },
    mounted() {
      this.loadPendingCopyrights()
    },
    activated() {
      // 当组件被激活时也重新加载数据
      this.loadPendingCopyrights()
    }
  }
</script>

<style scoped>
.copyright-info p {
  margin-bottom: 10px;
}
.copyright-image {
  text-align: center;
  margin-bottom: 20px;
}
</style>
