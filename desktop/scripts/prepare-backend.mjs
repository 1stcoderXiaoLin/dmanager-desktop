import { existsSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { delimiter, dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const rootDir = join(scriptDir, "..", "..");
const backendDir = join(rootDir, "backend");
const gradleWrapper = process.platform === "win32" ? "gradlew.bat" : "gradlew";
const gradlePath = join(backendDir, gradleWrapper);

function findJavaHome() {
  if (process.env.JAVA_HOME) {
    return process.env.JAVA_HOME;
  }

  const javaBinary = process.platform === "win32" ? "java.exe" : "java";
  const homeEnv = process.env.HOME ?? process.env.USERPROFILE ?? "";
  const candidates =
    process.platform === "win32"
      ? [
          join(homeEnv, ".jdks", "ms-21.0.10"),
          join(homeEnv, ".jdks", "ms-17.0.18"),
          "C:\\Program Files\\Microsoft\\jdk-21",
          "C:\\Program Files\\Microsoft\\jdk-17",
          "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.0.35-hotspot",
          "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.0.35-hotspot",
          "C:\\Program Files\\Java\\jdk-21",
          "C:\\Program Files\\Java\\jdk-17",
        ]
      : [
          "/usr/lib/jvm/default-java",
          "/usr/lib/jvm/java-21-openjdk",
          "/usr/lib/jvm/java-17-openjdk",
        ];

  return candidates.find((candidate) => existsSync(join(candidate, "bin", javaBinary)));
}

const javaHome = findJavaHome();
const env = { ...process.env };

if (javaHome) {
  env.JAVA_HOME = javaHome;
  env.PATH = `${join(javaHome, "bin")}${delimiter}${env.PATH ?? ""}`;
} else {
  console.error("Unable to locate a JDK. Set JAVA_HOME before running `npm run backend:prepare`.");
  process.exit(1);
}

const result = spawnSync(gradlePath, ["syncTauriResources"], {
  cwd: backendDir,
  env,
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}
