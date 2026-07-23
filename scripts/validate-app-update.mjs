import { createHash } from "node:crypto";
import { readFile, stat } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const manifestPath = path.join(root, "content", "app-update.json");
const contentPath = path.join(root, "content", "portfolio.json");
const localApkPath = path.join(root, "android-app", "app", "build", "outputs", "apk", "debug", "app-debug.apk");

const [manifestBytes, contentBytes] = await Promise.all([
  readFile(manifestPath),
  readFile(contentPath),
]);
const manifest = JSON.parse(manifestBytes.toString("utf8"));
const portfolio = JSON.parse(contentBytes.toString("utf8"));
const canonicalContentBytes = Buffer.from(
  contentBytes.toString("utf8").replace(/\r\n/g, "\n"),
  "utf8",
);

const fail = (message) => {
  throw new Error(`App update manifest: ${message}`);
};
const sha256 = (bytes) => createHash("sha256").update(bytes).digest("hex");
const requireHttps = (value, label) => {
  if (typeof value !== "string" || !value.startsWith("https://")) {
    fail(`${label} must be an HTTPS URL`);
  }
};

if (manifest.manifestVersion !== 2) fail("manifestVersion must be 2");
if (!Number.isInteger(manifest.versionCode) || manifest.versionCode < 1) fail("versionCode must be a positive integer");
if (!manifest.versionName) fail("versionName is required");
if (!manifest.contentPack || !manifest.apk) fail("contentPack and apk sections are required");
if (manifest.contentPack.version !== portfolio.meta.version) fail("contentPack.version must match portfolio.meta.version");
if (manifest.contentPack.sizeBytes !== canonicalContentBytes.length) {
  fail("contentPack.sizeBytes does not match canonical LF portfolio.json");
}
if (manifest.contentPack.sha256 !== sha256(canonicalContentBytes)) {
  fail("contentPack.sha256 does not match canonical LF portfolio.json");
}
requireHttps(manifest.contentPack.url, "contentPack.url");
requireHttps(manifest.apk.url, "apk.url");
requireHttps(manifest.releasePageUrl, "releasePageUrl");

const publishedApkPath = path.join(
  root,
  "public",
  "downloads",
  path.basename(new URL(manifest.apk.url).pathname),
);
let verifiedApk = false;
for (const candidate of [publishedApkPath, localApkPath]) {
  try {
    const apkBytes = await readFile(candidate);
    const apkInfo = await stat(candidate);
    if (manifest.apk.sizeBytes !== apkInfo.size) {
      fail(`apk.sizeBytes does not match ${path.relative(root, candidate)}`);
    }
    if (manifest.apk.sha256 !== sha256(apkBytes)) {
      fail(`apk.sha256 does not match ${path.relative(root, candidate)}`);
    }
    verifiedApk = true;
  } catch (error) {
    if (error?.code !== "ENOENT") throw error;
  }
}
if (!verifiedApk) console.warn("APK not present; skipped local APK hash verification.");

console.log(
  `Validated update manifest ${manifest.versionName}, content ${manifest.contentPack.version}, channel ${manifest.channel}.`,
);
