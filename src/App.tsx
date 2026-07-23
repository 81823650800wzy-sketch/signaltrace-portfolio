import { useMemo, useRef, useState } from "react";
import type { CSSProperties, KeyboardEvent, RefObject } from "react";
import {
  Boxes,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  Cpu,
  Droplets,
  Factory,
  Flame,
  Gauge,
  Pause,
  Play,
  RefreshCw,
  RotateCcw,
  Wind,
  Zap,
} from "lucide-react";
import incidentsData from "../data/public/incidents.json";
import processGraphData from "../data/public/process-flow.json";
import type {
  EvidenceKind,
  FlowSystem,
  Incident,
  IncidentStatus,
  ProcessEdge,
  ProcessGraph,
  ProcessNode,
  SystemId,
} from "./types";

const graph = processGraphData as ProcessGraph;
const incidents = incidentsData as Incident[];
const NODE_WIDTH = 126;
const NODE_HEIGHT = 54;
const VIEWBOX_WIDTH = 1800;
const VIEWBOX_HEIGHT = 720;

const systemIcons: Record<SystemId, typeof Flame> = {
  fuel: Flame,
  steam: Droplets,
  flue: Wind,
  electric: Zap,
  waste: Boxes,
  control: Cpu,
};

const evidenceLabels: Record<ProcessNode["evidenceLevel"], string> = {
  "public-general": "行业公开资料",
  "public-plant": "厂级公开资料",
  synthetic: "合成演示",
  sanitized: "脱敏观察",
};

const incidentStatusLabels: Record<IncidentStatus, string> = {
  closed: "已闭环",
  tracking: "持续跟踪",
  open: "待处理",
};

const evidenceKindLabels: Record<EvidenceKind, string> = {
  fact: "事实",
  inference: "推断",
  unknown: "待确认",
};

function center(node: ProcessNode): [number, number] {
  return [node.x + NODE_WIDTH / 2, node.y + NODE_HEIGHT / 2];
}

function edgePath(edge: ProcessEdge, nodeMap: Map<string, ProcessNode>): string {
  const from = nodeMap.get(edge.from)!;
  const to = nodeMap.get(edge.to)!;
  const points = [center(from), ...(edge.via ?? []), center(to)];
  return points.map(([x, y], index) => `${index === 0 ? "M" : "L"} ${x} ${y}`).join(" ");
}

function SystemButton({ system, selected, onSelect }: { system: FlowSystem; selected: boolean; onSelect: (id: SystemId) => void }) {
  const Icon = systemIcons[system.id];
  return (
    <button type="button" className="system-button" aria-pressed={selected} onClick={() => onSelect(system.id)} style={{ "--flow-color": system.color } as CSSProperties}>
      <Icon size={16} aria-hidden="true" />
      <span>{system.shortName}</span>
    </button>
  );
}

function FlowDiagram({
  activeSystem,
  selectedNodeId,
  incidentFocusNodeIds,
  speed,
  onSelectNode,
  svgRef,
}: {
  activeSystem: SystemId | "all";
  selectedNodeId: string;
  incidentFocusNodeIds: string[];
  speed: number;
  onSelectNode: (id: string) => void;
  svgRef: RefObject<SVGSVGElement | null>;
}) {
  const nodeMap = useMemo(() => new Map(graph.nodes.map((node) => [node.id, node])), []);
  const systemMap = useMemo(() => new Map(graph.systems.map((system) => [system.id, system])), []);
  const focusedNodeIds = new Set(incidentFocusNodeIds);

  const handleNodeKey = (event: KeyboardEvent<HTMLButtonElement>, nodeId: string) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelectNode(nodeId);
    }
  };

  return (
    <div className="diagram-scroll" aria-label="燃煤机组介质与控制信号流程图">
      <svg ref={svgRef} className="flow-diagram" viewBox={`0 0 ${VIEWBOX_WIDTH} ${VIEWBOX_HEIGHT}`} role="img" aria-labelledby="diagram-title diagram-description">
        <title id="diagram-title">燃煤机组全流程粒子图</title>
        <desc id="diagram-description">六条链路展示燃料与空气、汽水、烟气、电力、副产物和仪控工作闭环。点击设备可查看功能与来源。</desc>
        <defs>
          <pattern id="grid" width="24" height="24" patternUnits="userSpaceOnUse"><path d="M 24 0 L 0 0 0 24" className="grid-line" fill="none" /></pattern>
          {graph.systems.map((system) => (
            <marker key={system.id} id={`arrow-${system.id}`} markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto">
              <path d="M 0 0 L 7 3.5 L 0 7 Z" fill={system.color} />
            </marker>
          ))}
        </defs>
        <rect width={VIEWBOX_WIDTH} height={VIEWBOX_HEIGHT} className="diagram-background" />
        <rect width={VIEWBOX_WIDTH} height={VIEWBOX_HEIGHT} fill="url(#grid)" />
        <g aria-hidden="true">
          {graph.edges.map((edge) => {
            const system = systemMap.get(edge.system)!;
            const active = activeSystem === "all" || activeSystem === edge.system;
            const path = edgePath(edge, nodeMap);
            return (
              <g key={edge.id} className={active ? "edge-group is-active" : "edge-group"}>
                <path d={path} className="flow-edge" stroke={system.color} markerEnd={`url(#arrow-${edge.system})`} />
                {active && Array.from({ length: 4 }, (_, index) => (
                  <circle key={index} r="4" fill={system.color} className="flow-particle">
                    <animateMotion dur={`${edge.duration / speed}s`} begin={`${-(index * edge.duration) / 4}s`} repeatCount="indefinite" path={path} />
                  </circle>
                ))}
              </g>
            );
          })}
        </g>
        <g>
          {graph.nodes.map((node) => {
            const primarySystem = systemMap.get(node.systems[0])!;
            const active = activeSystem === "all" || node.systems.includes(activeSystem);
            const incidentFocused = focusedNodeIds.has(node.id);
            return (
              <foreignObject key={node.id} x={node.x} y={node.y} width={NODE_WIDTH} height={NODE_HEIGHT} className={["node-object", active ? "is-active" : "", incidentFocused ? "is-incident-focus" : ""].filter(Boolean).join(" ")}>
                <button type="button" className="process-node" aria-pressed={selectedNodeId === node.id} aria-label={`${node.name}，${evidenceLabels[node.evidenceLevel]}`} onClick={() => onSelectNode(node.id)} onKeyDown={(event) => handleNodeKey(event, node.id)} style={{ "--flow-color": primarySystem.color } as CSSProperties}>
                  <span className="node-dot" aria-hidden="true" />
                  <span className="node-name">{node.shortName}</span>
                </button>
              </foreignObject>
            );
          })}
        </g>
      </svg>
    </div>
  );
}

function NodeDetails({ node }: { node: ProcessNode }) {
  const systemMap = useMemo(() => new Map(graph.systems.map((system) => [system.id, system])), []);
  const sourceMap = useMemo(() => new Map(graph.sources.map((source) => [source.id, source])), []);
  const upstream = graph.edges.filter((edge) => edge.to === node.id).map((edge) => graph.nodes.find((item) => item.id === edge.from)?.shortName);
  const downstream = graph.edges.filter((edge) => edge.from === node.id).map((edge) => graph.nodes.find((item) => item.id === edge.to)?.shortName);

  return <div className="node-details">
    <div className="details-heading"><div><span className="eyebrow">当前节点</span><h2>{node.name}</h2></div><span className="evidence-badge">{evidenceLabels[node.evidenceLevel]}</span></div>
    <div className="system-tags" aria-label="所属系统">{node.systems.map((systemId) => { const system = systemMap.get(systemId)!; return <span key={systemId} style={{ "--flow-color": system.color } as CSSProperties}><i aria-hidden="true" />{system.name}</span>; })}</div>
    <p className="node-function">{node.function}</p>
    <dl className="detail-list">
      <div><dt>输入</dt><dd>{node.inputs.join("、") || "无"}</dd></div>
      <div><dt>输出</dt><dd>{node.outputs.join("、") || "无"}</dd></div>
      <div><dt>上游</dt><dd>{upstream.filter(Boolean).join("、") || "流程起点"}</dd></div>
      <div><dt>下游</dt><dd>{downstream.filter(Boolean).join("、") || "流程终点"}</dd></div>
    </dl>
    <div className="source-block"><h3>依据</h3>{node.sourceIds.length === 0 ? <p>合成演示节点，不代表现场真实配置。</p> : <ul>{node.sourceIds.map((sourceId) => { const source = sourceMap.get(sourceId); return source ? <li key={sourceId}><a href={source.url} target="_blank" rel="noreferrer">{source.id} · {source.title}</a></li> : null; })}</ul>}</div>
  </div>;
}

function IncidentDetails({ incident, stepIndex, onChooseIncident, onChooseStep }: { incident: Incident; stepIndex: number; onChooseIncident: (id: string) => void; onChooseStep: (index: number) => void }) {
  const currentStep = incident.steps[stepIndex];
  return <div className="incident-details">
    <div className="details-heading"><div><span className="eyebrow">脱敏故障复盘</span><h2>{incident.title}</h2></div><span className={`incident-status ${incident.status}`}>{incidentStatusLabels[incident.status]}</span></div>
    <div className="incident-picker" role="group" aria-label="选择故障案例">{incidents.map((item) => <button key={item.id} type="button" className="incident-choice" aria-pressed={item.id === incident.id} onClick={() => onChooseIncident(item.id)}>{item.shortTitle}</button>)}</div>
    <p className="incident-summary">{incident.summary}</p>
    <div className="replay-heading"><ClipboardCheck size={15} aria-hidden="true" /><span>回放步骤 {stepIndex + 1} / {incident.steps.length}</span></div>
    <ol className="incident-timeline">{incident.steps.map((step, index) => <li key={step.id} className={index === stepIndex ? "is-current" : index < stepIndex ? "is-complete" : ""}><button type="button" aria-current={index === stepIndex ? "step" : undefined} onClick={() => onChooseStep(index)}><span className={`evidence-kind ${step.kind}`}>{evidenceKindLabels[step.kind]}</span><span>{step.label}</span></button></li>)}</ol>
    <div className="step-detail"><span className={`evidence-kind ${currentStep.kind}`}>{evidenceKindLabels[currentStep.kind]}</span><p>{currentStep.detail}</p></div>
    <div className="replay-controls"><button type="button" className="step-button" disabled={stepIndex === 0} onClick={() => onChooseStep(stepIndex - 1)}><ChevronLeft size={16} aria-hidden="true" />上一步</button><button type="button" className="step-button" disabled={stepIndex === incident.steps.length - 1} onClick={() => onChooseStep(stepIndex + 1)}>下一步<ChevronRight size={16} aria-hidden="true" /></button></div>
    <div className="product-opportunity"><span>产品机会（假设）</span><p>{incident.productOpportunity}</p></div>
  </div>;
}

export default function App() {
  const [activeSystem, setActiveSystem] = useState<SystemId | "all">("all");
  const [selectedNodeId, setSelectedNodeId] = useState("boiler");
  const [detailsMode, setDetailsMode] = useState<"node" | "incident">("node");
  const [selectedIncidentId, setSelectedIncidentId] = useState(incidents[0].id);
  const [incidentStepIndex, setIncidentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(true);
  const [speed, setSpeed] = useState(1);
  const svgRef = useRef<SVGSVGElement>(null);
  const selectedNode = graph.nodes.find((node) => node.id === selectedNodeId) ?? graph.nodes[0];
  const selectedIncident = incidents.find((incident) => incident.id === selectedIncidentId) ?? incidents[0];
  const currentIncidentFocus = detailsMode === "incident" ? selectedIncident.steps[incidentStepIndex].focusNodeIds ?? selectedIncident.focusNodeIds : [];

  const togglePlayback = () => { const nextPlaying = !isPlaying; setIsPlaying(nextPlaying); if (nextPlaying) svgRef.current?.unpauseAnimations(); else svgRef.current?.pauseAnimations(); };
  const resetView = () => { setActiveSystem("all"); setSelectedNodeId("boiler"); setDetailsMode("node"); setSelectedIncidentId(incidents[0].id); setIncidentStepIndex(0); setSpeed(1); setIsPlaying(true); svgRef.current?.setCurrentTime(0); svgRef.current?.unpauseAnimations(); };
  const selectNode = (id: string) => { setSelectedNodeId(id); setDetailsMode("node"); };
  const selectIncident = (id: string) => { setSelectedIncidentId(id); setIncidentStepIndex(0); setDetailsMode("incident"); setActiveSystem("control"); };

  return <main className="app-shell">
    <header className="app-header"><div className="brand-block"><span className="brand-mark" aria-hidden="true"><Factory size={20} /></span><div><h1>ThermalFlow Lab</h1><p>燃煤机组流程训练 · 仪控视角</p></div></div><div className="data-status"><Gauge size={16} aria-hidden="true" /><span>公开资料 + 合成数据</span></div></header>
    <section className="toolbar" aria-label="流程图控制"><div className="system-selector" role="group" aria-label="系统筛选"><button type="button" className="system-button all-systems" aria-pressed={activeSystem === "all"} onClick={() => setActiveSystem("all")}><RefreshCw size={16} aria-hidden="true" /><span>全流程</span></button>{graph.systems.map((system) => <SystemButton key={system.id} system={system} selected={activeSystem === system.id} onSelect={setActiveSystem} />)}</div><div className="playback-controls"><button type="button" className="icon-button" aria-label={isPlaying ? "暂停粒子" : "播放粒子"} data-tooltip={isPlaying ? "暂停" : "播放"} onClick={togglePlayback}>{isPlaying ? <Pause size={17} aria-hidden="true" /> : <Play size={17} aria-hidden="true" />}</button><label className="speed-control"><span>速度 {speed.toFixed(1)}×</span><input type="range" min="0.5" max="2" step="0.5" value={speed} onChange={(event) => setSpeed(Number(event.target.value))} /></label><button type="button" className="icon-button" aria-label="重置流程图" data-tooltip="重置" onClick={resetView}><RotateCcw size={17} aria-hidden="true" /></button></div></section>
    <section className="workspace"><div className="diagram-panel"><div className="panel-heading"><div><span className="eyebrow">流程画布</span><h2>{detailsMode === "incident" ? `${selectedIncident.title} · 信号链路高亮` : activeSystem === "all" ? "全系统能量与介质流" : graph.systems.find((system) => system.id === activeSystem)?.name}</h2></div><span className="node-count">{graph.nodes.length} 个节点</span></div><FlowDiagram activeSystem={activeSystem} selectedNodeId={selectedNodeId} incidentFocusNodeIds={currentIncidentFocus} speed={speed} onSelectNode={selectNode} svgRef={svgRef} /></div>
      <aside className="details-panel" aria-live="polite"><div className="details-mode-tabs" role="tablist" aria-label="详情模式"><button type="button" role="tab" aria-selected={detailsMode === "node"} onClick={() => setDetailsMode("node")}>设备说明</button><button type="button" role="tab" aria-selected={detailsMode === "incident"} onClick={() => setDetailsMode("incident")}>故障复盘</button></div>{detailsMode === "node" ? <NodeDetails node={selectedNode} /> : <IncidentDetails incident={selectedIncident} stepIndex={incidentStepIndex} onChooseIncident={selectIncident} onChooseStep={setIncidentStepIndex} />}</aside>
    </section>
    <footer className="app-footer"><span>训练原型，不接入生产控制系统</span><span>数据版本 {graph.meta.version} · 案例均为脱敏观察</span></footer>
  </main>;
}
