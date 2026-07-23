import { useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpenCheck,
  CheckCircle2,
  ExternalLink,
  FileCheck2,
  GitBranch,
  Layers3,
  MapPinned,
  RadioTower,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Workflow,
} from "lucide-react";
import content from "../content/portfolio.json";
import coverImage from "./assets/portfolio-cover.png";

type ShowcaseProps = {
  onOpenLab: () => void;
  onOpenStudio: () => void;
  onOpenProduct: () => void;
};

const methods = [
  { title: "观察现场", text: "记录设备、工况和参与范围，保留问题发生的上下文。", icon: RadioTower },
  { title: "追踪链路", text: "把仪表、线路、DCS历史与工艺流程放进同一条链路。", icon: Workflow },
  { title: "校验证据", text: "明确区分事实、推断与待确认，不提前替师傅下结论。", icon: FileCheck2 },
  { title: "转成产品", text: "将真实痛点整理为用户、场景、约束和可验证方案。", icon: GitBranch },
];

const workActions = ["进入流程实验室", "查看真实日志", "查看异常复盘", "打开产品案例"];

export default function Showcase({ onOpenLab, onOpenStudio, onOpenProduct }: ShowcaseProps) {
  const dates = useMemo(() => ["ALL", ...Array.from(new Set(content.journal.map((entry) => entry.date)))], []);
  const [activeDate, setActiveDate] = useState("ALL");
  const visibleJournal = activeDate === "ALL" ? content.journal : content.journal.filter((entry) => entry.date === activeDate);
  const journalCount = content.journal.length;
  const workCount = content.works.length;
  const productCaseCount = content.productCases.length;

  const handleWork = (index: number) => {
    if (index === 0 || index === 2) onOpenLab();
    if (index === 1) document.querySelector("#journal")?.scrollIntoView({ behavior: "smooth" });
    if (index === 3) onOpenProduct();
  };

  return <main className="signal-shell">
    <header className="signal-header">
      <a className="signal-brand" href="#top" aria-label="仪控实习作品集首页"><span>ST</span><strong>SignalTrace</strong><small>FIELD LOG / 2026</small></a>
      <nav aria-label="页面导航">
        <a href="#work">成果</a><a href="#journal">日志</a><a href="#method">方法</a>
        <button type="button" onClick={onOpenProduct}>产品案例</button>
        <button type="button" className="nav-console" onClick={onOpenStudio}>内容台</button>
      </nav>
    </header>

    <section className="signal-hero" id="top" style={{ backgroundImage: `url(${coverImage})` }}>
      <div className="hero-code">ST / 00<br />INSTRUMENTATION<br />FIELD ARCHIVE</div>
      <div className="signal-hero-copy">
        <span className="signal-kicker">MY INSTRUMENTATION INTERNSHIP / EVIDENCE BUILD 04</span>
        <h1>我的仪控<br />实习作品集</h1>
        <p className="signal-lead">不只展示“去过现场”，而是展示我如何观察、求证、理解，再把问题转成作品。</p>
        <p>{journalCount}条实际工作日志覆盖仪表消缺、巡检抄表、DCS趋势、测量原理、异常排查和响应特性分析。所有内容均已脱敏，并明确本人参与范围与未确认结论。</p>
        <div className="signal-actions">
          <a className="signal-primary" href="#journal">查看{journalCount}条实际日志 <ArrowRight size={17} /></a>
          <button type="button" className="signal-secondary" onClick={onOpenProduct}>产品经理向项目</button>
        </div>
        <span className="signal-boundary"><ShieldCheck size={15} /> 公开安全版本 / 不接入生产系统 / 不替代正式判断</span>
      </div>
      <div className="hero-stack" aria-hidden="true">
        <span className="stack-a">{String(journalCount).padStart(2, "0")}<small>FIELD LOGS</small></span>
        <span className="stack-b">32<small>FLOW NODES</small></span>
        <span className="stack-c">{String(productCaseCount).padStart(2, "0")}<small>PRODUCT CASE</small></span>
      </div>
    </section>

    <section className="signal-metrics" aria-label="实习成果概览">
      <div><strong>{journalCount}</strong><span>实际工作事件</span><small>07.15—07.23 / 已脱敏</small></div>
      <div><strong>06</strong><span>学习主题</span><small>仪表 · 巡检 · DCS · 原理</small></div>
      <div><strong>32</strong><span>流程节点</span><small>六类介质与控制信号</small></div>
      <div><strong>{String(productCaseCount).padStart(2, "0")}</strong><span>产品经理案例</span><small>来自五组现场证据</small></div>
    </section>

    <section className="signal-band" id="work">
      <div className="signal-section-title"><span className="signal-kicker">SELECTED OUTPUT / {String(workCount).padStart(2, "0")}</span><h2>把跟班见闻转成四种可验证成果。</h2><p>每一项都对应真实学习材料，并保留“我做了什么”和“仍需确认什么”的边界。</p></div>
      <div className="signal-projects">
        {content.works.map((work, index) => <article key={work.title} className={`work-${index}`}>
          <div className="signal-project-top"><span>0{index + 1}</span>{index === 3 ? <MapPinned size={23} /> : index === 1 ? <BookOpenCheck size={23} /> : <Layers3 size={23} />}</div>
          <small>{work.type}</small><h3>{work.title}</h3><p>{work.summary}</p><strong>{work.tags}</strong>
          <button type="button" onClick={() => handleWork(index)}>{workActions[index]} <ArrowRight size={15} /></button>
        </article>)}
      </div>
    </section>

    <section className="journal-band" id="journal">
      <div className="journal-title"><span className="signal-kicker">FIELD JOURNAL / {journalCount} EVENTS</span><h2>真实日志不是流水账，而是一条逐步建立判断力的证据线。</h2><p>从秒哒日志筛选工作内容后逐条脱敏；个人饮食、健身、重复记录和具体机组标识不进入公开作品。</p></div>
      <div className="journal-filter" role="group" aria-label="按日期筛选实习日志">
        {dates.map((date) => <button key={date} type="button" aria-pressed={activeDate === date} onClick={() => setActiveDate(date)}>{date === "ALL" ? "全部" : date}</button>)}
      </div>
      <div className="journal-list">
        {visibleJournal.map((entry, index) => <article key={`${entry.date}-${entry.title}`}>
          <div className="journal-code"><span>{entry.date}</span><small>{String(index + 1).padStart(2, "0")}</small></div>
          <div className="journal-main"><span>{entry.category}</span><h3>{entry.title}</h3><p>{entry.observation}</p></div>
          <dl><div><dt>我的参与</dt><dd>{entry.participation}</dd></div><div><dt>学到什么</dt><dd>{entry.learning}</dd></div></dl>
          <div className="journal-signal"><Sparkles size={16} /><span><strong>产品信号</strong>{entry.productSignal}</span></div>
        </article>)}
      </div>
    </section>

    <section className="product-teaser">
      <div className="teaser-number">PM<br />01</div>
      <div className="teaser-copy"><span className="signal-kicker">PRODUCT CASE / FIELDTRACE</span><h2>从“找不到设备”开始，设计仪控巡检认知地图。</h2><p>连接区域、设备、原理、趋势与复盘，让新人围绕一次任务获得完整上下文。当前仅为概念验证，不读取实时数据，不输出自动根因。</p><button type="button" onClick={onOpenProduct}>进入完整产品案例 <ArrowRight size={17} /></button></div>
      <div className="teaser-model" aria-hidden="true"><span className="plane plane-1" /><span className="plane plane-2" /><span className="plane plane-3" /><i className="model-node m1" /><i className="model-node m2" /><i className="model-node m3" /></div>
    </section>

    <section className="signal-band signal-method-band" id="method">
      <div className="signal-section-title"><span className="signal-kicker">HOW I WORK / 04 STEPS</span><h2>我不只记录结果，也记录判断是怎样生成的。</h2></div>
      <div className="signal-methods">{methods.map(({ icon: Icon, title, text }, index) => <article key={title}><span>0{index + 1}</span><Icon size={22} /><h3>{title}</h3><p>{text}</p></article>)}</div>
    </section>

    <section className="signal-band signal-update-band">
      <div><span className="signal-kicker">CONTINUOUS UPDATE</span><h2>实习继续，证据库继续增长。</h2><p>新增日志先在本地整理和脱敏，验证后发布到网站，并由Android App自动获取。后续可继续加入授权照片、流程模型和新的产品验证结果。</p></div>
      <div className="signal-pipeline"><span><BookOpenCheck size={17} /> 实际日志</span><ArrowRight size={16} /><span><CheckCircle2 size={17} /> 脱敏审核</span><ArrowRight size={16} /><span><GitBranch size={17} /> 网站发布</span><ArrowRight size={16} /><span><RefreshCw size={17} /> App更新</span></div>
    </section>

    <footer className="signal-footer"><span>ST / INSTRUMENTATION INTERNSHIP ARCHIVE</span><a href="https://github.com/81823650800wzy-sketch" target="_blank" rel="noreferrer">GitHub公开主页 <ExternalLink size={14} /></a></footer>
  </main>;
}
