<template>
  <div class="basic-table vm-margin">
    <VmTable title="我的竞品拍卖" :columns="dataColumns" :data="dataTable"></VmTable>
    
    <!-- 发布竞品拍卖弹窗 -->
    <Modal v-model="publishModal" title="发布竞品到拍卖市场" width="600">
      <div v-if="selectedItem" class="item-info">
        <div class="item-header">
          <h3>{{ selectedItem.title }}</h3>
          <p><strong>描述：</strong> {{ selectedItem.description }}</p>
          <p><strong>类别：</strong> {{ selectedItem.category }}</p>
          <p><strong>起拍价：</strong> {{ selectedItem.startPrice }} ETH</p>
          <p><strong>拍卖开始时间：</strong> {{ formatDateTime(selectedItem.auctionStartTime) }}</p>
          <p><strong>拍卖结束时间：</strong> {{ formatDateTime(selectedItem.auctionEndTime) }}</p>
          <p><strong>创建时间：</strong> {{ formatDateTime(selectedItem.createdTime) }}</p>
        </div>
        
        <div class="item-image" v-if="selectedItem.imgUrl">
          <img :src="require('../assets/img/bg.jpg')" alt="竞品图片" style="max-width: 100%; max-height: 200px;">
        </div>
        
        <div class="attachments" v-if="selectedItem.attachmentPaths">
          <p><strong>附件文件：</strong></p>
          <ul class="attachment-list">
            <li v-for="(attachment, index) in getAttachments(selectedItem.attachmentPaths)" :key="index">
              <a @click="downloadAttachment(attachment)" class="attachment-link">
                <i class="fa fa-file"></i> {{ getAttachmentName(attachment) }}
              </a>
            </li>
          </ul>
        </div>
        
        <div class="price-input">
          <p><strong>请输入起拍价(ETH)：</strong></p>
          <InputNumber 
            v-model="publishPrice" 
            :min="selectedItem.startPrice || 0.01" 
            :step="0.1" 
            style="width: 200px"
            placeholder="请输入起拍价格">
          </InputNumber>
        </div>
      </div>
      <div slot="footer">
        <Button @click="publishModal = false" :disabled="publishing">取消</Button>
        <Button 
          type="primary" 
          @click="confirmPublish" 
          :disabled="!publishPrice || publishing"
          :loading="publishing">
          {{ publishing ? '处理中...' : '确认发布' }}
        </Button>
      </div>
    </Modal>
  </div>
</template>

<script>
  import VmTable from '@/components/vm-table'
  import axios from 'axios'
  import Web3 from 'web3'
  import { abi } from '../contracts/CopyrightNFT.json'
  import { contractAddress } from '../contracts/config'

  export default {
    name: 'BasicTable',
    components: {
      VmTable
    },
    data () {
      return {
        dataColumns: [
          {
            id: '2.140710',
            title: 'ID',
            key: 'id',
            sortable: true
          },
          {
            id: '2.140711',
            title: '标题',
            key: 'title',
            sortable: true
          },
          {
            id: '2.140712',
            title: '状态',
            key: 'status',
            sortable: true,
            render: (h, params) => {
              const statusMap = {
                'PENDING': '待审核',
                'APPROVED': '已审核',
                'REJECTED': '已拒绝',
                'LISTED': '拍卖中',
                'SOLD': '已售出',
                'DELISTED': '已下架'
              };
              const colorMap = {
                'PENDING': '#ff9900',
                'APPROVED': '#19be6b',
                'REJECTED': '#ed4014',
                'LISTED': '#2d8cf0',
                'SOLD': '#9c27b0',
                'DELISTED': '#808695'
              };
              return h('Tag', {
                props: {
                  color: colorMap[params.row.status] || 'default'
                }
              }, statusMap[params.row.status] || params.row.status);
            }
          },
          {
            id: '2.140713',
            title: '类别',
            key: 'category',
            sortable: true
          },
          {
            id: '2.140714',
            title: '起拍价',
            key: 'startPrice',
            sortable: true,
            render: (h, params) => {
              return h('div', (params.row.startPrice || '0') + ' ETH');
            }
          },
          {
            id: '2.140715',
            title: '拍卖开始时间',
            key: 'auctionStartTime',
            sortable: true,
            render: (h, params) => {
              return h('div', this.formatDateTime(params.row.auctionStartTime))
            }
          },
          {
            id: '2.140716',
            title: '拍卖结束时间',
            key: 'auctionEndTime',
            sortable: true,
            render: (h, params) => {
              return h('div', this.formatDateTime(params.row.auctionEndTime))
            }
          },
          {
            id: '2.140717',
            title: '操作',
            key: 'action',
            width: 150,
            align: 'center',
            render: (h, params) => {
              return h('div', [
                h('Button', {
                  props: {
                    type: 'primary',
                    size: 'small',
                    disabled: params.row.status !== 'APPROVED'
                  },
                  on: {
                    click: () => {
                      this.openPublishModal(params.row)
                    }
                  }
                }, '发布拍卖')
              ])
            }
          }
        ],
        dataTable: [],
        publishModal: false,
        selectedItem: null,
        publishPrice: null,
        publishing: false,
        web3: null,
        contract: null,
        currentUser: null,
        refreshInterval: null
      }
    },
    async created() {
      await this.initWeb3()
      // 从sessionStorage获取用户信息
      const userStr = sessionStorage.getItem('user')
      if (userStr) {
        try {
          this.currentUser = JSON.parse(userStr)
        } catch (e) {
          console.error('Failed to parse user info:', e)
        }
      }
    },
    mounted() {
      this.loadUserItems()
      // 设置定时刷新，每30秒更新一次数据
      this.refreshInterval = setInterval(() => {
        this.refreshData()
      }, 30000)
    },
    activated() {
      this.loadUserItems()
    },
    beforeDestroy() {
      // 组件销毁前清除定时器
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval)
      }
    },
    methods: {
      async initWeb3() {
        if (window.ethereum) {
          try {
            // 请求用户授权
            await window.ethereum.request({ method: 'eth_requestAccounts' })
            this.web3 = new Web3(window.ethereum)
            // 初始化合约
            this.contract = new this.web3.eth.Contract(abi, contractAddress)
          } catch (error) {
            console.error('Error initializing web3:', error)
          }
        } else {
          console.warn('MetaMask not detected')
        }
      },
      loadUserItems() {
        if (!this.currentUser) {
          this.$Message.error('请先登录')
          return
        }
        // 使用用户ID查询其竞品（优先），包含用户名信息
        if (this.currentUser.id) {
          axios.get(`/api/jdbc/copyright/user-id/${this.currentUser.id}/with-username`)
            .then(response => {
              if (response.data && response.data.length > 0) {
                this.dataTable = response.data
              } else {
                // 如果通过用户ID没有找到竞品，尝试使用钱包地址
                this.tryFetchByAddress()
              }
            })
            .catch(error => {
              console.error('通过用户ID加载竞品失败:', error)
              // 发生错误时尝试使用钱包地址
              this.tryFetchByAddress()
            })
        } else {
          // 如果没有用户ID，使用钱包地址
          this.tryFetchByAddress()
        }
      },
      tryFetchByAddress() {
        if (this.currentUser && this.currentUser.address) {
          axios.get(`/api/jdbc/copyright/user/${this.currentUser.address}/with-username`)
            .then(response => {
              this.dataTable = response.data
            })
            .catch(error => {
              console.error('通过钱包地址加载竞品失败:', error)
              this.$Message.error('加载竞品信息失败')
            })
        }
      },
      formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '';
        const date = new Date(dateTimeStr);
        return date.toLocaleString();
      },
      openPublishModal(item) {
        if (item.status !== 'APPROVED') {
          this.$Message.warning('只有审核通过的竞品才能发布到拍卖市场');
          return;
        }
        this.selectedItem = item;
        this.publishPrice = item.startPrice || 0.01;
        this.publishModal = true;
      },
      getAttachments(attachmentPaths) {
        if (!attachmentPaths) return [];
        return attachmentPaths.split(',').filter(path => path.trim() !== '');
      },
      getAttachmentName(path) {
        const parts = path.split('/');
        return parts[parts.length - 1];
      },
      downloadAttachment(fileName) {
        const downloadUrl = `/api/jdbc/copyright/download/attachment/${fileName}`;
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = this.getAttachmentName(fileName);
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      },
      async confirmPublish() {
        if (!this.publishPrice || this.publishPrice <= 0) {
          this.$Message.warning('请输入有效的价格')
          return
        }
        this.publishing = true
        // 区块链交易结果标志
        let blockchainSuccess = false
        try {
          // 1. 准备基础数据
          const accounts = await this.web3.eth.getAccounts()
          const account = accounts[0]
          // 准备上架元数据
          const metadata = {
            id: this.selectedItem.id,
            title: this.selectedItem.title,
            description: this.selectedItem.description,
            price: this.publishPrice,
            owner: account,
            startPrice: this.selectedItem.startPrice,
            auctionStartTime: this.selectedItem.auctionStartTime,
            auctionEndTime: this.selectedItem.auctionEndTime,
            status: 'LISTED'
          }
          try {
            // 2. 尝试区块链交易
            if (!this.web3 || !this.contract) {
              await this.initWeb3()
              if (!this.web3 || !this.contract) {
                console.warn('MetaMask钱包未连接，将跳过区块链记录')
              }
            }
            if (this.web3 && this.contract) {
              console.log('尝试区块链记录...')
              const methodNames = Object.keys(this.contract.methods)
              console.log('合约可用方法:', methodNames)
              const supportedMethods = ['mint', 'listToken', 'listItem', 'list']
              const method = supportedMethods.find(m => methodNames.includes(m))
              if (method) {
                console.log(`找到可用方法: ${method}，正在调用...`)
                const result = await this.contract.methods[method](JSON.stringify(metadata))
                  .send({ from: account })
                console.log('区块链交易哈希:', result.transactionHash)
                blockchainSuccess = true
              } else {
                console.warn('未找到适用的合约方法，将跳过区块链记录')
              }
            }
          } catch (blockchainError) {
            console.error('区块链交易失败:', blockchainError)
          }
          // 3. 无论区块链是否成功，都更新数据库
          axios.post(`/api/jdbc/copyright/${this.selectedItem.id}/list`, null, {
            params: {
              price: this.publishPrice
            }
          })
          .then(response => {
            this.$Message.success(blockchainSuccess
              ? '竞品已成功上架拍卖市场并写入区块链！'
              : '竞品已成功上架拍卖市场！')
            this.publishModal = false
            this.loadUserItems()
          })
          .catch(error => {
            this.$Message.error('竞品上架失败')
            console.error('API调用失败:', error)
          })
        } catch (error) {
          this.$Message.error('操作失败')
          console.error('Unexpected error:', error)
        } finally {
          this.publishing = false
        }
      },
      refreshData() {
        console.log('刷新用户竞品信息数据...')
        this.loadUserItems()
      }
    }
  }
</script>

<style scoped>
  .basic-table {
    width: 100%;
  }
  
  .item-info {
    margin-bottom: 20px;
  }
  
  .item-header h3 {
    margin-bottom: 15px;
    color: #17233d;
  }
  
  .item-header p {
    margin-bottom: 10px;
  }
  
  .item-image {
    margin: 15px 0;
    text-align: center;
  }
  
  .attachments {
    margin: 15px 0;
    border-top: 1px solid #e8eaec;
    padding-top: 15px;
  }
  
  .attachment-list {
    list-style: none;
    padding: 0;
    margin: 10px 0;
  }
  
  .attachment-link {
    color: #2d8cf0;
    cursor: pointer;
    display: inline-block;
    margin-bottom: 5px;
  }
  
  .attachment-link:hover {
    text-decoration: underline;
  }
  
  .price-input {
    margin-top: 20px;
    padding-top: 15px;
    border-top: 1px solid #e8eaec;
  }
</style>
