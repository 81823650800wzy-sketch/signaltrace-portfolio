import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const portfolio = JSON.parse(await readFile(path.join(root, "content/portfolio.json"), "utf8"));
const update = JSON.parse(await readFile(path.join(root, "content/app-update.json"), "utf8"));
const forbidden = [/密码/i, /token/i, /api[_ -]?key/i, /精确坐标/i, /内部截图/i, /真实机组号/i];

for (const key of ["meta", "profile", "capabilities", "works", "journal", "productCases", "models"]) {
  if (!(key in portfolio)) throw new Error(`portfolio.json is missing ${key}`);
}

if (!Array.isArray(portfolio.works) || portfolio.works.length === 0) throw new Error("At least one work is required.");
if (!Array.isArray(portfolio.journal) || portfolio.journal.length === 0) throw new Error("At least one sanitized journal entry is required.");
if (!Array.isArray(portfolio.productCases) || portfolio.productCases.length === 0) throw new Error("At least one product case is required.");
if (!Array.isArray(portfolio.models) || portfolio.models.length === 0) throw new Error("At least one model slot is required.");

for (const entry of portfolio.journal) {
  for (const key of ["date", "category", "title", "observation", "participation", "learning", "productSignal"]) {
    if (!entry[key]) throw new Error(`Journal entry is missing ${key}: ${entry.title ?? "untitled"}`);
  }
}

const ids = new Set();
for (const model of portfolio.models) {
  if (!/^[a-z0-9-]+$/.test(model.id)) throw new Error(`Invalid model id: ${model.id}`);
  if (ids.has(model.id)) throw new Error(`Duplicate model id: ${model.id}`);
  ids.add(model.id);
}

const serialized = JSON.stringify(portfolio);
for (const pattern of forbidden) {
  if (pattern.test(serialized)) throw new Error(`Public content matched blocked pattern: ${pattern}`);
}

if (!Number.isInteger(update.versionCode) || update.versionCode < 1) throw new Error("app-update versionCode must be a positive integer.");
if (update.apkUrl && !update.apkUrl.startsWith("https://")) throw new Error("apkUrl must use HTTPS.");
if (update.releasePageUrl && !update.releasePageUrl.startsWith("https://")) throw new Error("releasePageUrl must use HTTPS.");

console.log(`Validated portfolio ${portfolio.meta.version}, ${portfolio.works.length} works, ${portfolio.journal.length} journal entries, ${portfolio.productCases.length} product case, ${portfolio.models.length} model slots.`);
