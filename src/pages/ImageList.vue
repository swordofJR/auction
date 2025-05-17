<template>
  <VmImageList :data="dataImageList" class="vm-margin" 
    :isAdmin="isAdmin"
    @purchase-success="handlePurchaseSuccess"
    @delist-success="handleDelistSuccess">
  </VmImageList>
</template>
<script>
  import VmImageList from '@/components/vm-image-list'
  import axios from 'axios'
  import Web3 from 'web3'
  import { abi } from '../contracts/CopyrightNFT.json'
  import { contractAddress } from '../contracts/config'
  export default {
    name: '竞品拍卖交易',
    components: {
      VmImageList
    },
    data: function () {
      return {
        dataImageList: [],
        web3: null,
        contract: null,
        refreshInterval: null,
        isAdmin: false,
        defaultImage: require('@/assets/img/img-1.jpg'),
        defaultImages: [
          require('@/assets/img/img-1.jpg'),
          require('@/assets/img/img-2.jpg'),
          require('@/assets/img/img-3.jpg'),
          require('@/assets/img/img-4.jpg'),
          require('@/assets/img/img-5.jpg'),
          require('@/assets/img/img-6.jpg'),
          require('@/assets/img/img-7.jpg'),
          require('@/assets/img/img-8.jpg')
        ],
        originalCollectibles: [
          {
            id: '201707101552',
            title: '数字藏品1',
            img: require('@/assets/img/img-1.jpg'),
            desc: '这款数字油画藏品，以细腻笔触勾勒梦幻花园。繁花似锦，蝴蝶翩跹，色彩搭配绝妙，将浪漫氛围凝于方寸之间，极具收藏价值。',
            price: '0.5',
            category: '艺术品',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          },
          {
            id: '201707101553',
            title: '数字藏品2',
            img: require('@/assets/img/img-2.jpg'),
            desc: '以古老神话为蓝本创作的数字雕塑藏品，立体呈现神话人物英姿。线条流畅，细节精致，赋予古老故事全新视觉演绎，是不可多得的佳作。',
            price: '0.8',
            category: '收藏品',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          },
          {
            id: '201707101554',
            title: '数字藏品3',
            img: require('@/assets/img/img-3.jpg'),
            desc: '这组数字摄影藏品，抓拍自然奇景瞬间。或日出云海，或星空璀璨，每一张都定格永恒之美，让自然震撼时刻伴你左右。',
            price: '0.3',
            category: '艺术品',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          },
          {
            id: '201707101555',
            title: '数字藏品4',
            img: require('@/assets/img/img-4.jpg'),
            desc: '融合现代科技与传统剪纸艺术的数字藏品，用创新手法呈现剪纸灵动。图案精美，光影效果独特，传统艺术焕发新活力。',
            price: '0.6',
            category: '电子产品',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          },
          {
            id: '201707101556',
            title: '数字藏品5',
            img: require('@/assets/img/img-5.jpg'),
            desc: '以知名动漫角色为原型打造的数字手办藏品，高度还原角色神态。动作设计俏皮，色彩明艳，动漫迷不容错过的珍藏好物。',
            price: '1.2',
            category: '游戏',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          },
          {
            id: '201707101557',
            title: '数字藏品6',
            img: require('@/assets/img/img-6.jpg'),
            desc: '数字藏品呈现的古老书法名作，笔触行云流水，墨韵悠长。透过屏幕，仿若能感受古人挥毫泼墨时的豪情，文化底蕴深厚。',
            price: '0.4',
            category: '艺术品',
            ownerAddress: '0x' + Math.random().toString(16).substr(2, 40),
            status: 'LISTED',
            isOriginal: true
          }
        ]
      }
    },
    async created() {
      await this.initWeb3()
      // 检查当前用户是否为管理员
      this.checkIfAdmin()
    },
    mounted() {
      this.loadMarketplaceCopyrights()
      // 设置定时刷新，每30秒更新一次数据
      this.refreshInterval = setInterval(() => {
        this.refreshData()
      }, 30000)
    },
    activated() {
      this.loadMarketplaceCopyrights()
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
      checkIfAdmin() {
        const userStr = sessionStorage.getItem('user')
        if (userStr) {
          try {
            const user = JSON.parse(userStr)
            this.isAdmin = user.role === 'admin'
          } catch (e) {
            console.error('解析用户信息失败:', e)
          }
        }
      },
      loadMarketplaceCopyrights() {
        axios.get('/api/jdbc/copyright/marketplace-with-usernames')
          .then(response => {
            // 转换后端数据到VmImageList所需格式
            const dbItems = response.data.map(item => {
              return {
                id: item.id,
                title: item.title,
                img: require('@/assets/img/bg.jpg'), // 始终使用bg.jpg作为默认图片
                desc: item.description,
                price: item.price,
                category: item.category,
                ownerAddress: item.ownerAddress,
                username: item.username || '未知用户',
                detailUrl: '#',
                status: item.status,
                isOriginal: false
              }
            }).filter(item => item.status === 'LISTED') // 确保只显示状态为LISTED的藏品
            // 合并原始收藏品和数据库项
            this.dataImageList = [...this.originalCollectibles, ...dbItems]
          })
          .catch(error => {
            console.error('加载市场版权失败:', error)
            // 如果API调用失败，至少显示原始藏品
            this.dataImageList = [...this.originalCollectibles]
          })
      },
      refreshData() {
        console.log('刷新市场版权信息数据...')
        this.loadMarketplaceCopyrights()
      },
      handlePurchaseSuccess(event) {
        // 处理购买成功事件
        console.log('购买成功事件:', event)
        // 立即刷新市场数据
        this.loadMarketplaceCopyrights()
        // 显示成功通知
        this.$Message.success(`购买 ID:${event.itemId} 的数字藏品成功！`)
      },
      handleDelistSuccess(event) {
        // 处理下架成功事件
        console.log('下架成功事件:', event)
        // 立即刷新市场数据
        this.loadMarketplaceCopyrights()
        // 显示成功通知
        this.$Message.success(`下架 ID:${event.itemId} 的数字藏品成功！`)
      }
    }
  }
</script>
