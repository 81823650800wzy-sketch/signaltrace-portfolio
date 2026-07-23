import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Box, CheckCircle2, ClipboardList, ClipboardPenLine, Download, FileJson2, Layers3, Plus, RotateCcw, Send, Sparkles, Workflow } from "lucide-react";
import initialContent from "../content/portfolio.json";
import coverImage from "./assets/portfolio-cover.png";

type Capability = { title: string; detail: string; proof: string };
type Work = { title: string; type: string; summary: string; tags: string };
type Model = { id: string; title: string; phase: string; summary: string };
type JournalEntry = { date: string; category: string; title: string; observation: string; participation: string; learning: string; productSignal: string };
type ProductCase = { id: string; title: string; stage: string; problem: string; users: string; insight: string; solution: string; features: string[]; priorities: string[]; metrics: string[]; evidence: string };
type PortfolioContent = {
  meta: { version: string; updatedAt: string };
  profile: { name: string; role: string; headline: string; summary: string };
  capabilities: Capability[];
  works: Work[];
  journal: JournalEntry[];
  productCases: ProductCase[];
  models: Model[];
};

type StudioProps = { onOpenLab: () => void };
const STORAGE_KEY = "signaltrace-portfolio-studio-draft-v3";

const cloneContent = (value: PortfolioContent): PortfolioContent => JSON.parse(JSON.stringify(value)) as PortfolioContent;

function loadDraft(): PortfolioContent {
  const base = cloneContent(initialContent as PortfolioContent);
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return base;
    const saved = JSON.parse(raw) as Partial<PortfolioContent>;
    return {
      ...base,
      ...saved,
      profile: { ...base.profile, ...(saved.profile ?? {}) },
      capabilities: Array.isArray(saved.capabilities) ? saved.capabilities : base.capabilities,
      works: Array.isArray(saved.works) ? saved.works : base.works,
      journal: Array.isArray(saved.journal) ? saved.journal : base.journal,
      productCases: Array.isArray(saved.productCases) ? saved.productCases : base.productCases,
      models: Array.isArray(saved.models) ? saved.models : base.models,
    };
  } catch {
    return base;
  }
}

function toDateStamp() {
  return new Date().toISOString().slice(0, 10);
}

export default function PortfolioStudio({ onOpenLab }: StudioProps) {
  const [draft, setDraft] = useState<PortfolioContent>(loadDraft);
  const [activeModel, setActiveModel] = useState(0);
  const [notice, setNotice] = useState("草稿仅保存在当前浏览器，尚未导出到 Android App。");

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
  }, [draft]);

  const selectedModel = draft.models[activeModel] ?? draft.models[0];
  const releaseReadiness = useMemo(() => ({
    content: draft.profile.headline.trim().length > 0 && draft.works.length > 0,
    models: draft.models.length > 0,
    safety: !/(机组号|DCS截图|内部参数|账号|密码)/i.test(JSON.stringify(draft)),
  }), [draft]);

  const updateProfile = (key: keyof PortfolioContent["profile"], value: string) => {
    setDraft((current) => ({ ...current, profile: { ...current.profile, [key]: value } }));
  };

  const updateWork = (index: number, key: keyof Work, value: string) => {
    setDraft((current) => ({ ...current, works: current.works.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item) }));
  };

  const updateModel = (index: number, key: keyof Model, value: string) => {
    setDraft((current) => ({ ...current, models: current.models.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item) }));
  };

  const addWork = () => {
    setDraft((current) => ({ ...current, works: [...current.works, { title: "新学习成果", type: "学习成果", summary: "填写公开安全的成果摘要。", tags: "待补充" }] }));
    setNotice("已新增成果草稿，请补齐内容后再导出。");
  };

  const addModel = () => {
    setDraft((current) => ({ ...current, models: [...current.models, { id: `model-${current.models.length + 1}`, title: "新流程模型", phase: "规划中", summary: "填写模型用途、当前完成度和公开边界。" }] }));
    setActiveModel(draft.models.length);
    setNotice("已新增模型入口；导入 App 后可关联本机 GLB 文件。");
  };

  const exportPack = () => {
    const pack: PortfolioContent = { ...draft, meta: { ...draft.meta, updatedAt: toDateStamp() } };
    const blob = new Blob([JSON.stringify(pack, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "portfolio.json";
    link.click();
    URL.revokeObjectURL(url);
    setDraft(pack);
    setNotice("已生成 portfolio.json。将此文件传到手机后，在 Android App 的“更新”页导入即可。");
  };

  const restore = () => {
    const original = cloneContent(initialContent as PortfolioContent);
    setDraft(original);
    setActiveModel(0);
    localStorage.removeItem(STORAGE_KEY);
    setNotice("已恢复项目内置内容；当前版本没有上传或发布任何内容。");
  };

  return <main className="studio-shell">
    <header className="studio-header">
      <a className="studio-brand" href="#top"><span>ST</span><strong>Portfolio Studio</strong></a>
      <div className="studio-header-actions">
        <button className="studio-lab-link" type="button" onClick={onOpenLab}><Workflow size={15} /> 流程实验室</button>
        <button className="studio-export" type="button" onClick={exportPack}><Download size={16} /> 导出到 App</button>
      </div>
    </header>

    <section className="studio-titlebar" id="top">
      <div>
        <span className="studio-kicker">LOCAL-FIRST CONTENT WORKBENCH</span>
        <h1>把学习成果变成持续更新的个人名片。</h1>
        <p>实际日志只在人工筛选、脱敏和边界检查后进入公开内容包；本工作台不会直接同步原始日志，也不会上传现场信息。</p>
      </div>
      <img src={coverImage} alt="抽象化仪控与流程学习场景" />
    </section>

    <div className="studio-layout">
      <section className="studio-editor" aria-label="内容编辑器">
        <div className="studio-section-heading"><span><ClipboardPenLine size={18} /> 内容编辑</span><small>自动保存在本机浏览器</small></div>
        <label>公开名称<input value={draft.profile.name} onChange={(event) => updateProfile("name", event.target.value)} /></label>
        <label>发展方向<input value={draft.profile.role} onChange={(event) => updateProfile("role", event.target.value)} /></label>
        <label>核心定位<input value={draft.profile.headline} onChange={(event) => updateProfile("headline", event.target.value)} /></label>
        <label>公开简介<textarea rows={4} value={draft.profile.summary} onChange={(event) => updateProfile("summary", event.target.value)} /></label>

        <div className="editor-subheading"><span>作品成果</span><button type="button" onClick={addWork} aria-label="新增成果"><Plus size={16} /></button></div>
        {draft.works.map((work, index) => <article className="editor-item" key={`${work.title}-${index}`}>
          <input aria-label={`成果 ${index + 1} 标题`} value={work.title} onChange={(event) => updateWork(index, "title", event.target.value)} />
          <input aria-label={`成果 ${index + 1} 类型`} value={work.type} onChange={(event) => updateWork(index, "type", event.target.value)} />
          <textarea aria-label={`成果 ${index + 1} 摘要`} rows={3} value={work.summary} onChange={(event) => updateWork(index, "summary", event.target.value)} />
          <input aria-label={`成果 ${index + 1} 标签`} value={work.tags} onChange={(event) => updateWork(index, "tags", event.target.value)} />
        </article>)}

        <div className="editor-subheading"><span>流程模型</span><button type="button" onClick={addModel} aria-label="新增模型"><Plus size={16} /></button></div>
        {draft.models.map((model, index) => <article className="editor-item model-editor" key={model.id}>
          <input aria-label={`模型 ${index + 1} ID`} value={model.id} onChange={(event) => updateModel(index, "id", event.target.value.toLowerCase().replace(/[^a-z0-9-]/g, "-"))} />
          <input aria-label={`模型 ${index + 1} 标题`} value={model.title} onChange={(event) => updateModel(index, "title", event.target.value)} />
          <input aria-label={`模型 ${index + 1} 阶段`} value={model.phase} onChange={(event) => updateModel(index, "phase", event.target.value)} />
          <textarea aria-label={`模型 ${index + 1} 说明`} rows={3} value={model.summary} onChange={(event) => updateModel(index, "summary", event.target.value)} />
        </article>)}
      </section>

      <section className="studio-preview" aria-label="个人名片实时预览">
        <div className="preview-card">
          <span className="preview-status">OFFLINE PORTFOLIO</span>
          <h2>{draft.profile.name}</h2>
          <p className="preview-role">{draft.profile.role}</p>
          <h3>{draft.profile.headline}</h3>
          <p>{draft.profile.summary}</p>
          <div className="preview-counts"><span>{draft.works.length}<small>成果</small></span><span>{draft.journal.length}<small>日志</small></span><span>{draft.productCases.length}<small>产品案例</small></span><span>{draft.models.length}<small>模型入口</small></span></div>
        </div>
        <div className="preview-heading"><ClipboardList size={17} /> 实习证据摘要</div>
        {draft.journal.slice(0, 4).map((entry) => <article className="preview-work" key={`${entry.date}-${entry.title}`}><span>{entry.date} / {entry.category}</span><h3>{entry.title}</h3><p>{entry.learning}</p><small>产品信号：{entry.productSignal}</small></article>)}
        <div className="preview-heading"><Layers3 size={17} /> 已选成果</div>
        {draft.works.map((work, index) => <article className="preview-work" key={`${work.title}-${index}`}><span>{work.type}</span><h3>{work.title}</h3><p>{work.summary}</p><small>{work.tags}</small></article>)}
      </section>

      <aside className="studio-visual" aria-label="流程模型可视化">
        <div className="studio-section-heading"><span><Box size={18} /> 可视化资产</span><small>演示状态</small></div>
        <div className="visual-stage">
          <span className="visual-grid" />
          <div className="visual-path path-a" /><div className="visual-path path-b" />
          {draft.models.map((model, index) => <button type="button" key={model.id} className={`visual-module module-${index % 3} ${index === activeModel ? "is-selected" : ""}`} onClick={() => setActiveModel(index)}><span>{String(index + 1).padStart(2, "0")}</span><strong>{model.title}</strong><i>{model.phase}</i></button>)}
          <span className="flow-dot dot-a" /><span className="flow-dot dot-b" /><span className="flow-dot dot-c" />
        </div>
        {selectedModel && <div className="visual-inspector"><span>{selectedModel.phase}</span><h2>{selectedModel.title}</h2><p>{selectedModel.summary}</p><button type="button" onClick={onOpenLab}>在流程实验室查看交互 <ArrowRight size={15} /></button></div>}
        <div className="release-checks">
          <div><CheckCircle2 size={15} className={releaseReadiness.content ? "is-ready" : ""} /> 名片与成果内容</div>
          <div><CheckCircle2 size={15} className={releaseReadiness.models ? "is-ready" : ""} /> 模型资产索引</div>
          <div><CheckCircle2 size={15} className={releaseReadiness.safety ? "is-ready" : ""} /> 基础公开边界扫描</div>
        </div>
        <button className="studio-reset" type="button" onClick={restore}><RotateCcw size={15} /> 恢复内置内容</button>
      </aside>
    </div>

    <footer className="studio-footer"><FileJson2 size={16} /> {notice}<button type="button" onClick={exportPack}><Send size={15} /> 生成更新包</button></footer>
  </main>;
}
