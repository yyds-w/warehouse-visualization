<template>
  <div class="pano-page">
    <div class="pano-header">
      <div class="pano-title">仓库全景可视图</div>
      <div class="pano-sub">以平面图形式展示所有已确认的仓库布局（共 {{ warehouses.length }} 个仓库）</div>
    </div>

    <div class="pano-canvas">
      <div v-if="!warehouses.length" class="pano-empty">暂无仓库数据</div>
      <div
        v-for="wh in warehouses"
        :key="wh.id"
        class="wh-card"
        :class="{ inactive: wh.status !== 1 }"
        @mouseenter="hoverId = wh.id"
        @mouseleave="hoverId = null"
        @dblclick="goBoard(wh)"
      >
        <div class="wh-head">
          <span class="wh-name">{{ wh.name }}</span>
          <span class="wh-dot" :class="wh.status === 1 ? 'dot-green' : 'dot-red'"></span>
        </div>
        <div class="wh-code">{{ wh.code }}</div>
        <div class="wh-line">总库存: {{ wh.totalStock }}</div>
        <div class="wh-line">可用库存: {{ wh.availableStock }}</div>
        <div class="wh-line">使用率: {{ wh.usageRate }}%</div>
        <button class="wh-gear" title="配置货架" @click.stop="goShelf">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7.03 7.03 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.5.5 0 0 0-.61.22L2.66 8.84a.5.5 0 0 0 .12.64l2.03 1.58c-.05.3-.07.63-.07.94s.02.64.07.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32c.14.24.42.34.61.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.05.24.25.42.5.42h3.84c.25 0 .45-.18.5-.42l.36-2.54a7.03 7.03 0 0 0 1.62-.94l2.39.96c.24.1.5 0 .61-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z"/></svg>
        </button>

        <transition name="fade">
          <div v-if="hoverId === wh.id" class="wh-pop">
            <div class="wh-pop-title">{{ wh.name }}</div>
            <div class="wh-pop-row"><span>冻结库存:</span><b>{{ wh.frozenStock }}</b></div>
            <div class="wh-pop-row"><span>出库计划量:</span><b>{{ wh.outboundPlanQty }}</b></div>
            <div class="wh-pop-row"><span>预出库量:</span><b>{{ wh.preOutboundQty }}</b></div>
            <div class="wh-pop-row"><span>预冻结量:</span><b>{{ wh.preFrozenQty }}</b></div>
            <div class="wh-pop-row"><span>出库量:</span><b>{{ wh.outboundQty }}</b></div>
            <div class="wh-pop-row"><span>入库量:</span><b>{{ wh.inboundQty }}</b></div>
            <div class="wh-pop-cat">
              <div class="wh-pop-cat-label">主要存货种类:</div>
              <div class="wh-pop-tags">
                <span v-for="c in wh.categories" :key="c" class="wh-tag">{{ c }}</span>
                <span v-if="!wh.categories.length" class="wh-tag empty">暂无</span>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <div class="pano-legend">
      <span class="lg-item"><i class="dot dot-green"></i>运行中</span>
      <span class="lg-item"><i class="dot dot-red"></i>已停用</span>
      <span class="lg-item"><i class="gear-ic">⚙</i>配置货架</span>
      <span class="lg-tip">💡 悬停查看详情，双击进入详细视图</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { panoramaOverview } from '../api/panorama'

const router = useRouter()
const warehouses = ref([])
const hoverId = ref(null)

onMounted(async () => {
  const res = await panoramaOverview()
  warehouses.value = res.data.warehouses || []
})

const goBoard = (wh) => router.push(`/panorama/warehouse/${wh.id}`)
const goShelf = () => router.push('/shelf')
</script>

<style scoped>
.pano-page {
  min-height: 100vh;
  background: #fff;
  padding: 24px 32px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.pano-header { margin-bottom: 16px; }
.pano-title { font-size: 18px; font-weight: 700; color: #1f2937; }
.pano-sub { font-size: 12px; color: #9ca3af; margin-top: 4px; }

.pano-canvas {
  flex: 1;
  min-height: 420px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background:
    radial-gradient(circle, #f3f4f6 1px, transparent 1px) 0 0 / 24px 24px,
    #fff;
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  justify-content: center;
  gap: 48px;
  padding: 40px;
  position: relative;
}
.pano-empty { color: #9ca3af; font-size: 14px; }

.wh-card {
  position: relative;
  width: 200px;
  border: 1.5px solid #60a5fa;
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px 14px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.12);
  transition: box-shadow 0.2s, transform 0.2s;
}
.wh-card:hover {
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.25);
  transform: translateY(-2px);
}
.wh-card.inactive { border-color: #f87171; }
.wh-head { display: flex; justify-content: space-between; align-items: center; }
.wh-name { font-size: 14px; font-weight: 700; color: #1f2937; }
.wh-dot { width: 10px; height: 10px; border-radius: 50%; }
.dot-green { background: #22c55e; }
.dot-red { background: #ef4444; }
.wh-code { font-size: 11px; color: #9ca3af; margin: 2px 0 8px; }
.wh-line { font-size: 12px; color: #4b5563; line-height: 1.9; }
.wh-gear {
  position: absolute;
  right: -10px;
  bottom: -10px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);
}
.wh-gear:hover { background: #2563eb; }

.wh-pop {
  position: absolute;
  left: calc(100% + 12px);
  top: 0;
  z-index: 20;
  width: 220px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 12px 14px;
  cursor: default;
}
.wh-pop-title { font-size: 13px; font-weight: 700; color: #1f2937; margin-bottom: 8px; }
.wh-pop-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
  line-height: 2;
}
.wh-pop-row b { color: #1f2937; font-weight: 600; }
.wh-pop-cat { margin-top: 8px; border-top: 1px dashed #e5e7eb; padding-top: 8px; }
.wh-pop-cat-label { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.wh-pop-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.wh-tag {
  font-size: 11px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  padding: 1px 8px;
}
.wh-tag.empty { color: #9ca3af; background: #f9fafb; border-color: #e5e7eb; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.pano-legend {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 4px 0;
  font-size: 12px;
  color: #6b7280;
}
.lg-item { display: flex; align-items: center; gap: 6px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.gear-ic { color: #3b82f6; font-size: 13px; }
.lg-tip { color: #9ca3af; }
</style>
