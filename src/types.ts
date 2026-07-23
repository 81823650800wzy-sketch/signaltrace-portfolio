export type SystemId = "fuel" | "steam" | "flue" | "electric" | "waste" | "control";
export type IncidentStatus = "closed" | "tracking" | "open";
export type EvidenceKind = "fact" | "inference" | "unknown";

export interface FlowSystem {
  id: SystemId;
  name: string;
  shortName: string;
  color: string;
}

export interface Source {
  id: string;
  title: string;
  url: string;
}

export interface IncidentStep {
  id: string;
  label: string;
  detail: string;
  kind: EvidenceKind;
  focusNodeIds?: string[];
}

export interface Incident {
  id: string;
  title: string;
  shortTitle: string;
  primarySystem: SystemId;
  status: IncidentStatus;
  evidenceLevel: "sanitized";
  summary: string;
  productOpportunity: string;
  focusNodeIds: string[];
  steps: IncidentStep[];
}

export interface ProcessNode {
  id: string;
  name: string;
  shortName: string;
  systems: SystemId[];
  x: number;
  y: number;
  function: string;
  inputs: string[];
  outputs: string[];
  evidenceLevel: "public-general" | "public-plant" | "synthetic" | "sanitized";
  sourceIds: string[];
}

export interface ProcessEdge {
  id: string;
  from: string;
  to: string;
  system: SystemId;
  medium: string;
  duration: number;
  via?: [number, number][];
}

export interface ProcessGraph {
  meta: {
    title: string;
    version: string;
    dataMode: string;
  };
  systems: FlowSystem[];
  sources: Source[];
  nodes: ProcessNode[];
  edges: ProcessEdge[];
}
