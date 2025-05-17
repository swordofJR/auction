<template>
  <div class="basic-table vm-margin">
    <VmTable title="我的竞品拍卖" :columns="dataColumns" :data="dataTable"></VmTable>
    <!-- 发布竞品拍卖弹窗 -->
    <Modal v-model="publishModal" title="发布竞品到拍卖市场" width="600">
      <div v-if="selectedItem" class="item-info">
        <!-- 钱包连接状态 -->
        <div class="wallet-status" :class="{'connected': walletConnected, 'disconnected': !walletConnected}">
          <Icon :type="walletConnected ? 'ios-checkmark-circle' : 'ios-close-circle'" />
          <span v-if="walletConnected">
            MetaMask已连接: {{ walletAddress ? (walletAddress.substring(0, 6) + '...' + walletAddress.substring(walletAddress.length - 4)) : '' }}
          </span>
          <span v-else>MetaMask未连接</span>
          <Button 
            v-if="!walletConnected" 
            type="primary" 
            size="small" 
            @click="checkAndConnectWallet" 
            :loading="connectingWallet"
            class="connect-button">
            连接MetaMask钱包
          </Button>
        </div>
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
          :disabled="!walletConnected || !publishPrice || publishing"
          :loading="publishing">
          {{ publishing ? '处理中...' : (walletConnected ? '确认发布' : '请先连接钱包') }}
        </Button>
      </div>
    </Modal>
    <!-- 出价历史弹窗 -->
    <Modal v-model="showBidHistory" title="竞品出价历史" width="700">
      <div class="bid-history">
        <Table :columns="bidHistoryColumns" :data="bidHistoryData" :loading="loadingBidHistory">
          <template slot="empty">
            <div class="no-bids">
              <p>暂无出价记录</p>
            </div>
          </template>
        </Table>
      </div>
      <div slot="footer">
        <Button @click="showBidHistory = false">关闭</Button>
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
            title: '当前价格',
            key: 'currentPrice',
            sortable: true,
            render: (h, params) => {
              return h('div', (params.row.currentPrice || params.row.startPrice || '0') + ' ETH');
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
            width: 200,
            align: 'center',
            render: (h, params) => {
              return h('div', [
                h('Button', {
                  props: {
                    type: 'primary',
                    size: 'small',
                    disabled: params.row.status !== 'APPROVED'
                  },
                  style: {
                    marginRight: '5px'
                  },
                  on: {
                    click: () => {
                      this.openPublishModal(params.row)
                    }
                  }
                }, '发布拍卖'),
                h('Button', {
                  props: {
                    type: 'info',
                    size: 'small'
                  },
                  on: {
                    click: () => {
                      this.openBidHistoryModal(params.row)
                    }
                  }
                }, '出价历史')
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
        refreshInterval: null,
        // 钱包相关状态
        walletConnected: false,
        walletAddress: '',
        connectingWallet: false,
        // 出价历史相关
        showBidHistory: false,
        bidHistoryData: [],
        loadingBidHistory: false,
        bidHistoryColumns: [
          {
            title: '出价人',
            key: 'bidderName',
            width: '30%'
          },
          {
            title: '出价金额',
            key: 'bidAmount',
            width: '30%'
          },
          {
            title: '出价时间',
            key: 'bidTime',
            width: '40%'
          }
        ]
      }
    },
    async created() {
      // 初始化Web3连接
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
      // 检查MetaMask钱包状态
      this.checkInitialWalletState()
      // 添加MetaMask账户变化监听
      if (window.ethereum) {
        window.ethereum.on('accountsChanged', this.handleAccountsChanged)
        window.ethereum.on('chainChanged', () => window.location.reload())
      }
      // 加载用户竞品数据
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
      // 清除MetaMask监听
      if (window.ethereum) {
        window.ethereum.removeListener('accountsChanged', this.handleAccountsChanged)
      }
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
        // if (!this.currentUser) {
        //   this.$Message.error('请先登录')
        //   return
        // }
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
        // 检查MetaMask钱包连接状态
        this.checkAndConnectWallet().then(connected => {
          if (connected) {
            this.publishModal = true;
          } else {
            this.$Message.warning('请连接MetaMask钱包后再发布拍卖');
          }
        });
      },
      // 检查并连接MetaMask钱包
      async checkAndConnectWallet() {
        if (typeof window.ethereum === 'undefined') {
          this.$Message.error('请安装MetaMask钱包插件');
          this.walletConnected = false;
          return false;
        }
        this.connectingWallet = true;
        try {
          // 检查是否已连接
          let accounts = await window.ethereum.request({ method: 'eth_accounts' });
          // 如果未连接，请求连接
          if (!accounts || accounts.length === 0) {
            this.$Message.info('正在请求连接MetaMask钱包...');
            accounts = await window.ethereum.request({ method: 'eth_requestAccounts' });
          }
          // 验证连接结果
          if (accounts && accounts.length > 0) {
            const account = accounts[0];
            console.log('钱包已连接:', account);
            // 初始化Web3
            this.web3 = new Web3(window.ethereum);
            this.contract = new this.web3.eth.Contract(abi, contractAddress);
            // 验证钱包所有权（通过签名验证）
            const verified = await this.verifyWalletOwnership(account);
            if (!verified) {
              this.$Message.error('钱包所有权验证失败，请重试');
              this.walletConnected = false;
              this.walletAddress = '';
              return false;
            }
            // 更新钱包状态
            this.walletConnected = true;
            this.walletAddress = account;
            // 显示连接信息
            this.$Message.success(`MetaMask钱包已连接: ${account.substring(0, 6)}...${account.substring(account.length - 4)}`);
            return true;
          } else {
            this.walletConnected = false;
            this.walletAddress = '';
            this.$Message.error('钱包连接失败，请重试');
            return false;
          }
        } catch (error) {
          console.error('钱包连接错误:', error);
          this.walletConnected = false;
          this.walletAddress = '';
          this.$Message.error('钱包连接失败: ' + (error.message || '未知错误'));
          return false;
        } finally {
          this.connectingWallet = false;
        }
      },
      // 验证钱包所有权（通过签名）
      async verifyWalletOwnership(account) {
        try {
          // 创建签名消息
          const message = `验证钱包所有权，拍卖平台身份验证 ${new Date().toISOString()}`;
          // 显示签名提示
          this.$Message.info('请在MetaMask中确认签名请求，以验证钱包所有权');
          // 请求用户签名
          const signature = await window.ethereum.request({
            method: 'personal_sign',
            params: [
              this.web3.utils.utf8ToHex(message),
              account
            ]
          });
          console.log('签名成功:', signature);
          // 在实际应用中，这里可以将签名发送到后端进行验证
          // 简化起见，我们只验证签名是否存在
          if (signature) {
            return true;
          }
          return false;
        } catch (error) {
          console.error('签名验证失败:', error);
          return false;
        }
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
        // 再次验证钱包是否连接
        if (!this.walletConnected) {
          const walletConnected = await this.checkAndConnectWallet();
          if (!walletConnected) {
            this.$Message.warning('请先连接MetaMask钱包');
            return;
          }
        }
        // 拍卖确认签名
        try {
          // 创建签名消息
          const message = `确认发布拍卖: 
物品: ${this.selectedItem.title}
价格: ${this.publishPrice} ETH
时间: ${new Date().toISOString()}`;
          this.$Message.info('请在MetaMask中确认操作签名');
          // 请求用户签名
          const signature = await window.ethereum.request({
            method: 'personal_sign',
            params: [
              this.web3.utils.utf8ToHex(message),
              this.walletAddress
            ]
          });
          console.log('确认发布签名成功:', signature);
          if (!signature) {
            this.$Message.warning('未能完成签名确认，操作取消');
            return;
          }
        } catch (error) {
          console.error('签名确认失败:', error);
          this.$Message.error('签名确认失败，操作取消');
          return;
        }
        this.publishing = true
        // 区块链交易结果标志
        let blockchainSuccess = false
        try {
          // 1. 准备基础数据
          const accounts = await this.web3.eth.getAccounts()
          // 如果账户发生变化，更新状态
          if (accounts[0] !== this.walletAddress) {
            this.walletAddress = accounts[0];
            this.$Message.info(`当前使用钱包地址: ${this.walletAddress.substring(0, 6)}...${this.walletAddress.substring(this.walletAddress.length - 4)}`);
          }
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
      },
      // MetaMask账户变化处理
      handleAccountsChanged(accounts) {
        if (accounts.length === 0) {
          // 用户断开了MetaMask连接
          this.walletConnected = false
          this.walletAddress = ''
          this.$Message.warning('MetaMask钱包已断开连接')
        } else if (this.walletAddress && accounts[0] !== this.walletAddress) {
          // 用户切换了账户
          this.walletAddress = accounts[0]
          this.$Message.info(`钱包账户已切换: ${this.walletAddress.substring(0, 6)}...${this.walletAddress.substring(this.walletAddress.length - 4)}`)
        }
      },
      // 初始检查钱包状态
      async checkInitialWalletState() {
        if (window.ethereum) {
          try {
            const accounts = await window.ethereum.request({ method: 'eth_accounts' })
            if (accounts.length > 0) {
              this.walletConnected = true
              this.walletAddress = accounts[0]
              console.log('钱包已连接:', this.walletAddress)
            }
          } catch (error) {
            console.error('检查钱包状态失败:', error)
          }
        }
      },
      openBidHistoryModal(item) {
        this.selectedItem = item;
        this.showBidHistory = true;
        this.loadBidHistory(item.id);
      },
      async loadBidHistory(itemId) {
        if (!itemId) return;
        try {
          this.loadingBidHistory = true;
          const response = await axios.get(`/api/jdbc/copyright/${itemId}/bid-history`);
          this.bidHistoryData = response.data && response.data.length > 0 ? response.data : [];
        } catch (error) {
          console.error('加载出价历史失败:', error);
          this.$Message.error('加载出价历史失败');
          this.bidHistoryData = [];
        } finally {
          this.loadingBidHistory = false;
        }
      },
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
  .wallet-status {
    margin-bottom: 20px;
    padding: 10px 15px;
    border: 1px solid #e8eaec;
    border-radius: 4px;
    display: flex;
    align-items: center;
    font-size: 14px;
    background-color: #f8f8f9;
  }
  .wallet-status.connected {
    border-color: #19be6b;
    background-color: rgba(25, 190, 107, 0.1);
  }
  .wallet-status.disconnected {
    border-color: #ff9900;
    background-color: rgba(255, 153, 0, 0.1);
  }
  .wallet-status i {
    margin-right: 8px;
    font-size: 18px;
  }
  .wallet-status.connected i {
    color: #19be6b;
  }
  .wallet-status.disconnected i {
    color: #ff9900;
  }
  .connect-button {
    margin-left: auto;
    background-color: #2d8cf0;
    border-color: #2d8cf0;
  }
  .bid-history {
    max-height: 400px;
    overflow-y: auto;
  }
  .no-bids {
    text-align: center;
    padding: 20px;
    color: #808695;
  }
</style>
