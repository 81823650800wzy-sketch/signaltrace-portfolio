import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  ClipboardList,
  Compass,
  DatabaseZap,
  Gauge,
  Layers3,
  MapPinned,
  ScanLine,
  ShieldCheck,
  Users,
} from "lucide-react";
import content from "../content/portfolio.json";

type ProductCaseProps = {
  onBack: () => void;
  onOpenLab: () => void;
};

const product = content.productCases[0];

const evidence = [
  { date: "07.16", title: "设备定位", text: "确认区域后仍需逐一核对设备牌。", icon: MapPinned },
  { date: "07.17", title: "趋势判断", text: "单点读数需要历史基线和现场工况共同解释。", icon: Gauge },
  { date: "07.20", title: "重复异常", text: "相似液位测量问题反复出现，历史信息难复用。", icon: DatabaseZap },
  { date: "07.21", title: "差异诊断", text: "相似表象可能来自完全不同的前置条件。", icon: ScanLine },
];

export default function ProductCase({ onBack, onOpenLab }: ProductCaseProps) {
  return <main className="product-shell">
    <header className="product-header">
      <button type="button" onClick={onBack}><ArrowLeft size={17} /> 返回作品集</button>
      <span>ST / PRODUCT CASE 01</span>
      <strong>CONCEPT VALIDATION</strong>
    </header>

    <section className="product-hero">
      <div className="product-hero-copy">
        <span className="product-index">PRODUCT / 01</span>
        <p className="product-kicker">INSTRUMENTATION × FIELD LEARNING × PRODUCT</p>
        <h1>FieldTrace<br />仪控巡检认知地图</h1>
        <p className="product-intro">从{content.journal.length}条实际实习日志中识别问题，把“设备难找、趋势难判、等待难控、经验难复用”整理成一个有边界的产品概念。</p>
        <div className="product-tags"><span>实习生</span><span>新入班人员</span><span>带教人员</span></div>
      </div>

      <div className="field-device" aria-label="FieldTrace伪三维产品原型">
        <div className="device-shadow" />
        <div className="device-screen">
          <div className="device-top"><span>FIELDTRACE / ZONE 03</span><strong>LEARNING MODE</strong></div>
          <div className="device-map">
            <span className="map-grid" />
            <button type="button" className="map-node node-a"><i />P-021</button>
            <button type="button" className="map-node node-b"><i />LT-04</button>
            <button type="button" className="map-node node-c is-active"><i />PT-17</button>
            <span className="map-route route-a" /><span className="map-route route-b" />
          </div>
          <div className="device-evidence">
            <span>ACTIVE EVIDENCE</span>
            <strong>压力显示异常 · 待确认</strong>
            <div><i className="fact" />事实 02</div><div><i className="inference" />推断 01</div><div><i />待确认 03</div>
          </div>
        </div>
      </div>
    </section>

    <section className="product-evidence-band">
      <div className="product-section-heading">
        <span>01 / EVIDENCE</span>
        <h2>产品问题来自现场记录，不来自想象。</h2>
        <p>{product.evidence}</p>
      </div>
      <div className="product-evidence-grid">
        {evidence.map(({ date, title, text, icon: Icon }) => <article key={title}>
          <span>{date}</span><Icon size={22} /><h3>{title}</h3><p>{text}</p>
        </article>)}
      </div>
    </section>

    <section className="product-definition">
      <div className="definition-problem">
        <span>02 / DEFINE</span>
        <h2>不是再做一个巡检系统，而是补齐新人的任务上下文。</h2>
        <p>{product.problem}</p>
      </div>
      <div className="definition-facts">
        <div><Users size={20} /><span><strong>目标用户</strong>{product.users}</span></div>
        <div><Compass size={20} /><span><strong>核心洞察</strong>{product.insight}</span></div>
        <div><ShieldCheck size={20} /><span><strong>边界</strong>不连接控制指令，不替代工作票、DCS或正式检修系统。</span></div>
      </div>
    </section>

    <section className="product-solution-band">
      <div className="product-section-heading light">
        <span>03 / SOLUTION</span>
        <h2>围绕一次现场任务，组织地图、设备、证据和复盘。</h2>
        <p>{product.solution}</p>
      </div>
      <div className="solution-flow">
        {["定位区域", "识别设备", "查看原理", "记录证据", "带教复核"].map((step, index) => <div key={step}><span>0{index + 1}</span><strong>{step}</strong>{index < 4 && <ArrowRight size={18} />}</div>)}
      </div>
      <div className="feature-list">
        {product.features.map((feature, index) => <div key={feature}><span>{String(index + 1).padStart(2, "0")}</span><CheckCircle2 size={18} /><strong>{feature}</strong></div>)}
      </div>
    </section>

    <section className="product-priority">
      <div className="priority-title"><span>04 / MVP</span><h2>先验证学习闭环，再考虑系统连接。</h2></div>
      <div className="priority-board">
        {product.priorities.map((priority, index) => <article className={`priority-${index}`} key={priority}>
          <span>{index === 0 ? "NOW" : index === 1 ? "NEXT" : "NOT NOW"}</span>
          <h3>{priority.split("：")[0]}</h3>
          <p>{priority.split("：")[1]}</p>
        </article>)}
      </div>
    </section>

    <section className="product-metrics">
      <div><span>05 / MEASURE</span><h2>成功不是页面更炫，而是新人更少迷失、复盘更完整。</h2></div>
      <ol>{product.metrics.map((metric, index) => <li key={metric}><span>{String(index + 1).padStart(2, "0")}</span>{metric}</li>)}</ol>
    </section>

    <section className="product-next">
      <Layers3 size={28} />
      <div><span>NEXT VALIDATION</span><h2>用后续跟班继续验证，而不是提前假设答案。</h2><p>下一步将记录设备定位耗时、提示次数和复盘完整度，并邀请带教师傅检查信息结构是否符合现场认知。</p></div>
      <button type="button" onClick={onOpenLab}><ClipboardList size={17} /> 查看流程与异常复盘</button>
    </section>
  </main>;
}
