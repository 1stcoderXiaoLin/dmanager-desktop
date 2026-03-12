import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const desktopDir = join(scriptDir, "..");
const rootDir = join(desktopDir, "..");
const tauriCli =
  process.platform === "win32"
    ? join(desktopDir, "node_modules", ".bin", "tauri.cmd")
    : join(desktopDir, "node_modules", ".bin", "tauri");

if (!existsSync(tauriCli)) {
  console.error("Tauri CLI is not installed. Run `npm install --prefix desktop` first.");
  process.exit(1);
}

const env = { ...process.env };
const shouldUseCustomTarget = process.platform === "win32" && !env.CI && !env.CARGO_TARGET_DIR;

if (shouldUseCustomTarget) {
  const homeDir = env.LOCALAPPDATA ?? env.HOME ?? env.USERPROFILE;

  if (!homeDir) {
    console.error("Unable to determine a writable home directory for Cargo target files.");
    process.exit(1);
  }

  const cargoTargetDir = join(homeDir, "dmanager-desktop", "cargo-target");
  mkdirSync(cargoTargetDir, { recursive: true });
  env.CARGO_TARGET_DIR = cargoTargetDir;
}

const result = spawnSync(tauriCli, process.argv.slice(2), {
  cwd: rootDir,
  env,
  stdio: "inherit",
  shell: process.platform === "win32",
});

process.exit(result.status ?? 1);
