import { copyFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const copies = [
  ["content/portfolio.json", "public/content/portfolio.json"],
  ["content/portfolio.json", "android-app/app/src/main/assets/portfolio.json"],
  ["content/app-update.json", "public/content/app-update.json"],
];

for (const [source, destination] of copies) {
  const target = path.join(root, destination);
  await mkdir(path.dirname(target), { recursive: true });
  await copyFile(path.join(root, source), target);
}

console.log("Synced portfolio content to web and Android offline assets.");
