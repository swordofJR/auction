<template>
  <div class="dashboard">
    <!-- <Row :gutter="16">
      <Col :lg="6" :md="12">
        <VmStateOverView color="#41b883" icon="fa fa-user" title="New Users" count="996"></VmStateOverView>
      </Col>
      <Col :lg="6" :md="12">
        <VmStateOverView color="#1d8ce0" icon="fa fa-eye" title="Page Views" count="109009"></VmStateOverView>
      </Col>
      <Col :lg="6" :md="12">
        <VmStateOverView color="#ffa000" icon="fa fa-pencil" title="Comments" count="1010"></VmStateOverView>
      </Col>
      <Col :lg="6" :md="12">
        <VmStateOverView color="#f60000" icon="fa fa-download" title="Downloads" count="9960"></VmStateOverView>
      </Col>
    </Row> -->
    <Row :gutter="16">
      <Col :lg="6">
        <VmUserPreview :brief="dataUserPreview.brief" :property="dataUserPreview.property"></VmUserPreview>
      </Col>
      <Col :lg="18">
        <VmProgress title="版权信息完整度" :data="dataProgress"></VmProgress>
      </Col>  
    </Row>
    <Row :gutter="16">
      <Col :lg="16">
        <VmTimeline title="Timeline" :data="dataTimeline">
        </VmTimeline>
      </Col>
      <Col :lg="8">
        <VmTabs icon="fa fa-user" title="Briefs" content-height="200px">
          <VmTabsItem label="Social" name="01">
            <div class="demo-news">
              <h2>"提交版权信息1"</h2>
            </div>
          </VmTabsItem>
          <VmTabsItem label="Business" name="02">
            <div class="demo-news">
              <h2>"版权信息1通过"</h2>
            </div>
          </VmTabsItem>
          <VmTabsItem label="Entertainment" name="03">
            <div class="demo-news">
              <h2>"铸造数字藏品1"</h2>
            </div>
          </VmTabsItem>
          <VmTabsItem label="Sport" name="04">
            <div class="demo-news">
              <h2>"数字藏品1待交易"</h2>
            </div>
          </VmTabsItem>
          <VmTabsItem label="Health" name="05">
            <div class="demo-news">
              <h2>"数字藏品1交易成功"</h2>
            </div>
          </VmTabsItem>
          <VmTabsItem label="Education" name="06">
            <div class="demo-news">
              <h2>"提交版权信息2"</h2>
            </div>
          </VmTabsItem>
        </VmTabs>
        <VmWeather class="vm-margin" :data="dataWeather">
        </VmWeather>
      </Col> 
    </Row>
  </div>
</template>
<script>
import VmStateOverView from '@/components/vm-state-overview.vue'
import VmUserPreview from '@/components/vm-user-preview.vue'
import VmProgress from '@/components/vm-progress.vue'
import VmTimeline from '@/components/vm-timeline.vue'
import VmTabs from '@/components/vm-tabs.vue'
import VmTabsItem from '@/components/vm-tabs-item.vue'
import VmWeather from '@/components/vm-weather.vue'
export default {
  name: 'Dashboard',
  components: {
    VmStateOverView,
    VmUserPreview,
    VmProgress,
    VmTimeline,
    VmTabs,
    VmTabsItem,
    VmWeather
  },
  data () {
    return {
      dataUserPreview: {
        brief: {
          photo: require('@/assets/img/photo.jpg'),
          name: 'JR',
          role: 'User'
        },
        property: [
          {
            icon: 'fa fa-user',
            key: '性别',
            value: '男'
          },
          {
            icon: 'fa fa-card',
            key: '可兑换信用值',
            value: '100'
          },
          {
            icon: 'fa fa-calendar',
            key: '注册时间',
            value: '2025-4-1 8:00'
          },
          {
            icon: 'fa fa-calendar',
            key: '上次登录时间',
            value: '2025-4-8 10:10'
          }
        ]
      },
      dataProgress: [
        {
          name: '版权1',
          tags: ['艺术品', '收藏品'],
          value: 90
        },
        {
          name: '版权2',
          tags: ['电子产品'],
          value: 30
        },
        {
          name: '版权3',
          tags: ['艺术品', '收藏品'],
          value: 80
        },
        {
          name: '版权4',
          tags: ['不动产'],
          value: 20
        },
        {
          name: '版权5',
          tags: ['电子产品', '收藏品'],
          value: 100
        }
      ],
      dataTimeline: [
        {
          date: '2025-4-1',
          time: '8:35 am',
          content: '提交版权信息1'
        },
        {
          date: '2025-4-2',
          time: '8:35 am',
          content: '铸造数字藏品1'
        },
        {
          date: '2025-4-2',
          time: '8:35 am',
          content: '数字藏品1待交易'
        },
        {
          date: '2025-4-2',
          time: '8:35 am',
          content: '数字藏品1交易成功'
        },
        {
          date: '2025-4-3',
          time: '8:35 am',
          content: '提交版权信息2'
        }
      ],
      dataWeather: {
        location: '成都',
        weather: '多云',
        temprature: '30°',
        humidity: '56%',
        percip: '1.50in',
        winds: '10mph'
      }
    }
  }
}
</script>
<style lang="less">
</style>
