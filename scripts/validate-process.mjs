import { readFile } from "node:fs/promises";

const graphUrl = new URL("../data/public/process-flow.json", import.meta.url);
const incidentsUrl = new URL("../data/public/incidents.json", import.meta.url);
const graph = JSON.parse(await readFile(graphUrl, "utf8"));
const incidents = JSON.parse(await readFile(incidentsUrl, "utf8"));
const errors = [];

function uniqueIds(items, label) {
  const ids = new Set();
  for (const item of items) {
    if (!item.id) errors.push(`${label} contains an item without id`);
    if (ids.has(item.id)) errors.push(`${label} contains duplicate id: ${item.id}`);
    ids.add(item.id);
  }
  return ids;
}

const systemIds = uniqueIds(graph.systems ?? [], "systems");
const sourceIds = uniqueIds(graph.sources ?? [], "sources");
const nodeIds = uniqueIds(graph.nodes ?? [], "nodes");
uniqueIds(graph.edges ?? [], "edges");

for (const node of graph.nodes ?? []) {
  if (!Array.isArray(node.systems) || node.systems.length === 0) {
    errors.push(`node ${node.id} must belong to at least one system`);
  }
  for (const systemId of node.systems ?? []) {
    if (!systemIds.has(systemId)) errors.push(`node ${node.id} references unknown system ${systemId}`);
  }
  for (const sourceId of node.sourceIds ?? []) {
    if (!sourceIds.has(sourceId)) errors.push(`node ${node.id} references unknown source ${sourceId}`);
  }
  if (node.evidenceLevel === "public-plant" && (node.sourceIds?.length ?? 0) === 0) {
    errors.push(`plant-specific node ${node.id} requires at least one public source`);
  }
}

for (const edge of graph.edges ?? []) {
  if (!nodeIds.has(edge.from)) errors.push(`edge ${edge.id} has unknown from node ${edge.from}`);
  if (!nodeIds.has(edge.to)) errors.push(`edge ${edge.id} has unknown to node ${edge.to}`);
  if (!systemIds.has(edge.system)) errors.push(`edge ${edge.id} references unknown system ${edge.system}`);
  if (!(edge.duration > 0)) errors.push(`edge ${edge.id} duration must be positive`);
}

const incidentIds = uniqueIds(incidents ?? [], "incidents");
const knownStatuses = new Set(["closed", "tracking", "open"]);
const knownEvidenceKinds = new Set(["fact", "inference", "unknown"]);

for (const incident of incidents ?? []) {
  if (!systemIds.has(incident.primarySystem)) errors.push(`incident ${incident.id} has invalid system ${incident.primarySystem}`);
  if (!knownStatuses.has(incident.status)) errors.push(`incident ${incident.id} has invalid status ${incident.status}`);
  if (incident.evidenceLevel !== "sanitized") errors.push(`incident ${incident.id} must be marked sanitized`);
  if (!Array.isArray(incident.steps) || incident.steps.length < 3) errors.push(`incident ${incident.id} requires at least three replay steps`);

  for (const nodeId of [...(incident.focusNodeIds ?? []), ...(incident.steps ?? []).flatMap((step) => step.focusNodeIds ?? [])]) {
    if (!nodeIds.has(nodeId)) errors.push(`incident ${incident.id} references unknown node ${nodeId}`);
  }

  uniqueIds((incident.steps ?? []).map((step) => ({ ...step, id: `${incident.id}:${step.id}` })), `steps for ${incident.id}`);
  for (const step of incident.steps ?? []) {
    if (!knownEvidenceKinds.has(step.kind)) errors.push(`incident ${incident.id} step ${step.id} has invalid evidence kind ${step.kind}`);
  }
}

if (errors.length > 0) {
  console.error(errors.join("\n"));
  process.exit(1);
}

console.log(`Validated ${graph.nodes.length} nodes, ${graph.edges.length} edges, ${graph.sources.length} sources, and ${incidentIds.size} sanitized incident cases.`);
