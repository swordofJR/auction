<template>
  <div class="traceability">
    <h2>竞品溯源系统</h2>
    <Form ref="searchForm" :model="formData" :label-width="80">
      <Form-item label="搜索方式">
        <RadioGroup v-model="searchType">
          <Radio label="id">按竞品ID</Radio>
          <Radio label="keyword">按关键词</Radio>
        </RadioGroup>
      </Form-item>
      <Form-item label="搜索内容" prop="searchTerm">
        <Input v-if="searchType === 'id'" v-model="formData.searchTerm" placeholder="请输入竞品ID" />
        <Input v-else v-model="formData.searchTerm" placeholder="请输入关键词" />
      </Form-item>
      <Form-item>
        <Button type="primary" @click="handleSearch" :loading="loading">搜索</Button>
        <Button style="margin-left: 8px" @click="handleReset">重置</Button>
      </Form-item>
    </Form>
    
    <Spin v-if="loading" size="large" fix></Spin>
    
    <!-- <div v-if="!loading && searchPerformed && searchResults.length === 0" class="no-result">
      未找到相关竞品信息
    </div> -->
    
    <div v-if="searchResults.length > 0 && !auctionItem" class="search-results">
      <Card>
        <p slot="title">搜索结果</p>
        <Table :columns="resultColumns" :data="searchResults" :border="true" @on-row-click="handleRowClick"></Table>
      </Card>
    </div>
    
    <div v-if="auctionItem" class="result-container">
      <Card class="item-details">
        <p slot="title">竞品基本信息</p>
        <div class="item-info">
          <img :src="getItemImage(auctionItem.imgUrl)" class="item-image" alt="拍品图片" @error="handleImageError" />
          <div class="item-content">
            <h3>{{ auctionItem.title }} <Tag :color="getStatusColor(auctionItem.status)">{{ getStatusText(auctionItem.status) }}</Tag></h3>
            <p><strong>ID:</strong> {{ auctionItem.id }}</p>
            <p><strong>类别:</strong> {{ auctionItem.category }}</p>
            <p><strong>描述:</strong> {{ auctionItem.description }}</p>
            <p><strong>所有者地址:</strong> {{ auctionItem.ownerAddress }}</p>
            <p><strong>起拍价格:</strong> {{ auctionItem.startPrice }} ETH</p>
            <p><strong>当前价格:</strong> {{ auctionItem.currentPrice || auctionItem.startPrice }} ETH</p>
            <p><strong>创建时间:</strong> {{ formatDate(auctionItem.createdTime) }}</p>
            <p><strong>拍卖开始时间:</strong> {{ formatDate(auctionItem.auctionStartTime) }}</p>
            <p><strong>拍卖结束时间:</strong> {{ formatDate(auctionItem.auctionEndTime) }}</p>
          </div>
        </div>
        
        <div class="item-attachments">
          <h3>附件文件</h3>
          <div v-if="auctionItem.attachmentPaths">
            <ul class="attachment-list">
              <li v-for="(path, index) in getAttachmentPaths(auctionItem.attachmentPaths)" :key="index">
                <a :href="'/uploads/' + path" target="_blank" class="attachment-link">
                  <Icon type="document" /> {{ getFileName(path) }}
                </a>
              </li>
            </ul>
          </div>
          <div v-else class="no-attachments">无</div>
        </div>
      </Card>
      
      <Card class="transaction-history" v-if="transactionHistory.length > 0">
        <p slot="title">交易流转记录</p>
        <Table :columns="transactionColumns" :data="transactionHistory" :border="true"></Table>
      </Card>
      
      <Card class="transaction-history" v-else>
        <p slot="title">交易流转记录</p>
        <div class="no-records">暂无交易记录</div>
      </Card>
      
      <Card class="bid-history" v-if="bidHistory.length > 0">
        <p slot="title">出价历史</p>
        <Table :columns="bidColumns" :data="bidHistory" :border="true"></Table>
      </Card>
      
      <Card class="bid-history" v-else>
        <p slot="title">出价历史</p>
        <div class="no-records">暂无出价记录</div>
      </Card>
    </div>
  </div>
</template>
<script>
import axios from 'axios';
export default {
  name: 'Traceability',
  data() {
    return {
      searchType: 'id',
      formData: {
        searchTerm: ''
      },
      loading: false,
      searchPerformed: false,
      auctionItem: null,
      searchResults: [],
      bidHistory: [],
      transactionHistory: [],
      defaultImage: require('@/assets/img/image.png'),
      resultColumns: [
        {
          title: 'ID',
          key: 'id',
          width: 80
        },
        {
          title: '标题',
          key: 'title'
        },
        {
          title: '类别',
          key: 'category',
          width: 100
        },
        {
          title: '状态',
          key: 'status',
          width: 100,
          render: (h, params) => {
            return h('Tag', {
              props: {
                color: this.getStatusColor(params.row.status)
              }
            }, this.getStatusText(params.row.status));
          }
        },
        {
          title: '价格(ETH)',
          key: 'currentPrice',
          width: 100,
          render: (h, params) => {
            return h('span', params.row.currentPrice || params.row.startPrice || '0');
          }
        },
        {
          title: '操作',
          width: 100,
          render: (h, params) => {
            return h('Button', {
              props: {
                type: 'primary',
                size: 'small'
              },
              on: {
                click: () => {
                  this.viewItem(params.row.id);
                }
              }
            }, '查看详情');
          }
        }
      ],
      bidColumns: [
        {
          title: '出价者',
          key: 'bidderName',
          width: 160
        },
        {
          title: '出价金额(ETH)',
          key: 'bidAmount',
          width: 120
        },
        {
          title: '出价时间',
          key: 'bidTime',
          width: 170,
          render: (h, params) => {
            return h('span', this.formatDate(params.row.bidTime));
          }
        },
        {
          title: '交易哈希',
          key: 'transactionHash',
          ellipsis: true,
          render: (h, params) => {
            if (!params.row.transactionHash) return h('span', '无');
            return h('a', {
              attrs: {
                href: `https://sepolia.etherscan.io/tx/${params.row.transactionHash}`,
                target: '_blank'
              }
            }, params.row.transactionHash);
          }
        }
      ],
      transactionColumns: [
        {
          title: '卖方',
          key: 'sellerName',
          width: 150
        },
        {
          title: '买方',
          key: 'buyerName',
          width: 150
        },
        {
          title: '成交价格(ETH)',
          key: 'finalPrice',
          width: 120
        },
        {
          title: '交易时间',
          key: 'transactionTime',
          width: 170,
          render: (h, params) => {
            return h('span', this.formatDate(params.row.transactionTime));
          }
        },
        {
          title: '交易哈希',
          key: 'transactionHash',
          ellipsis: true,
          render: (h, params) => {
            if (!params.row.transactionHash) return h('span', '无');
            return h('a', {
              attrs: {
                href: `https://sepolia.etherscan.io/tx/${params.row.transactionHash}`,
                target: '_blank'
              }
            }, params.row.transactionHash);
          }
        }
      ]
    };
  },
  methods: {
    async handleSearch() {
      if (!this.formData.searchTerm) {
        this.$Message.warning('请输入搜索内容');
        return;
      }
      this.loading = true;
      this.searchPerformed = true;
      this.auctionItem = null;
      this.searchResults = [];
      this.bidHistory = [];
      this.transactionHistory = [];
      try {
        if (this.searchType === 'id') {
          await this.searchById();
        } else {
          await this.searchByKeyword();
        }
      } catch (error) {
        console.error('搜索失败:', error);
        this.$Message.error('搜索失败，请稍后再试');
        this.auctionItem = null;
        this.searchResults = [];
        this.bidHistory = [];
        this.transactionHistory = [];
      } finally {
        this.loading = false;
      }
    },
    async searchById() {
      const id = parseInt(this.formData.searchTerm);
      if (isNaN(id)) {
        this.$Message.warning('请输入有效的ID');
        this.loading = false;
        return;
      }
      try {
        const response = await axios.get(`/api/jdbc/copyright/${id}`);
        if (response.data) {
          this.auctionItem = response.data;
          await this.loadBidHistory(id);
          await this.loadTransactionHistory(id);
        }
      } catch (error) {
        console.error('获取竞品信息失败:', error);
        this.$Message.error('获取竞品信息失败');
        this.auctionItem = null;
      }
    },
    async searchByKeyword() {
      try {
        const response = await axios.get(`/api/jdbc/copyright/search`, {
          params: { keyword: this.formData.searchTerm }
        });
        if (response.data && response.data.length > 0) {
          this.searchResults = response.data;
          // 如果只有一个结果，直接显示详情
          if (response.data.length === 1) {
            this.viewItem(response.data[0].id);
          }
        } else {
          this.searchResults = [];
        }
      } catch (error) {
        console.error('搜索竞品信息失败:', error);
        this.$Message.error('搜索失败');
        this.searchResults = [];
      }
    },
    async viewItem(id) {
      this.loading = true;
      try {
        const response = await axios.get(`/api/jdbc/copyright/${id}`);
        this.auctionItem = response.data;
        if (this.auctionItem) {
          await this.loadBidHistory(id);
          await this.loadTransactionHistory(id);
        }
      } catch (error) {
        console.error('获取竞品信息失败:', error);
        this.$Message.error('获取竞品信息失败');
        this.auctionItem = null;
      } finally {
        this.loading = false;
      }
    },
    handleRowClick(row) {
      this.viewItem(row.id);
    },
    async loadBidHistory(itemId) {
      try {
        const response = await axios.get(`/api/jdbc/copyright/${itemId}/bid-history`);
        this.bidHistory = response.data || [];
      } catch (error) {
        console.error('加载出价历史失败:', error);
        this.$Message.error('加载出价历史失败');
        this.bidHistory = [];
      }
    },
    async loadTransactionHistory(itemId) {
      try {
        const response = await axios.get(`/api/jdbc/copyright/${itemId}/transaction-history`);
        this.transactionHistory = response.data || [];
      } catch (error) {
        console.error('加载交易历史失败:', error);
        this.$Message.error('加载交易历史失败');
        this.transactionHistory = [];
      }
    },
    handleReset() {
      this.formData.searchTerm = '';
      this.searchPerformed = false;
      this.auctionItem = null;
      this.searchResults = [];
      this.bidHistory = [];
      this.transactionHistory = [];
    },
    formatDate(dateString) {
      if (!dateString) return '无';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    },
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已审核',
        'REJECTED': '已拒绝',
        'LISTED': '已上架',
        'SOLD': '已售出'
      };
      return statusMap[status] || status;
    },
    getStatusColor(status) {
      const colorMap = {
        'PENDING': 'blue',
        'APPROVED': 'green',
        'REJECTED': 'red',
        'LISTED': 'orange',
        'SOLD': 'purple'
      };
      return colorMap[status] || 'default';
    },
    getAttachmentPaths(pathsString) {
      if (!pathsString) return [];
      return pathsString.split(',');
    },
    getFileName(path) {
      if (!path) return '';
      const parts = path.split('/');
      return parts[parts.length - 1];
    },
    getItemImage(imgUrl) {
      if (!imgUrl || imgUrl === '' || imgUrl === 'null' || imgUrl === 'undefined') {
        return this.defaultImage;
      }
      return '/uploads/' + imgUrl;
    },
    handleImageError(e) {
      e.target.src = this.defaultImage;
    }
  }
};
</script>
<style scoped>
.traceability {width: 100%;}
.no-result {text-align: center;padding: 30px;font-size: 16px;color: #999;}
.result-container {margin-top: 20px;}
.search-results {margin-top: 20px;margin-bottom: 20px;}
.item-details, .transaction-history, .bid-history {margin-bottom: 20px;}
.item-info {display: flex;margin-bottom: 20px;}
.item-image {width: 200px;height: 200px;object-fit: cover;margin-right: 20px;}
.item-content {flex: 1;}
.item-content h3 {margin-top: 0;margin-bottom: 15px;}
.item-content p {margin: 5px 0;}
.item-attachments {margin-top: 20px;padding-top: 15px;border-top: 1px solid #dddee1;}
.attachment-list {list-style: none;padding: 0;margin: 10px 0;}
.attachment-link {color: #2d8cf0;cursor: pointer;display: inline-block;margin-right: 15px;margin-bottom: 8px;}
.attachment-link:hover {text-decoration: underline;}
.no-attachments {padding: 10px 0;color: #999;font-style: italic;}
.no-records {text-align: center;padding: 20px;color: #999;font-style: italic;}
</style> 