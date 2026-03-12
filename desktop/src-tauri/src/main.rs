#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::{
    env,
    net::{TcpStream, ToSocketAddrs},
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::Mutex,
    thread,
    time::Duration,
};

use tauri::{AppHandle, Manager, RunEvent};

const BACKEND_HOST: &str = "127.0.0.1";
const BACKEND_PORT: u16 = 18080;
const BACKEND_NAME: &str = "dmanager-backend";
const BACKEND_READY_RETRIES: u32 = 120;
const BACKEND_READY_DELAY_MS: u64 = 250;

struct BackendProcess(Mutex<Option<Child>>);

fn main() {
    let app = tauri::Builder::default()
        .setup(|app| {
            let child = start_backend(app.handle())?;
            app.manage(BackendProcess(Mutex::new(child)));
            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("error while building tauri application");

    let app_handle = app.handle().clone();
    app.run(move |_, event| {
        if matches!(event, RunEvent::Exit) {
            stop_backend(&app_handle);
        }
    });
}

fn start_backend(app: &AppHandle) -> tauri::Result<Option<Child>> {
    if backend_port_ready() {
        return Ok(None);
    }

    let launcher =
        resolve_backend_launcher(app).ok_or_else(|| tauri::Error::AssetNotFound("backend launcher".into()))?;

    let mut command = backend_command_for(&launcher);
    command
        .env("DMANAGER_BACKEND_HOST", BACKEND_HOST)
        .env("DMANAGER_BACKEND_PORT", BACKEND_PORT.to_string())
        .stdin(Stdio::null());

    if cfg!(debug_assertions) {
        command.stdout(Stdio::inherit()).stderr(Stdio::inherit());
    } else {
        command.stdout(Stdio::null()).stderr(Stdio::null());
    }

    if let Some(java_home) = resolve_bundled_java_home(app) {
        let path_key = if cfg!(windows) { "Path" } else { "PATH" };
        let current_path = env::var_os(path_key).unwrap_or_default();
        let java_bin_dir = java_home.join("bin");
        let merged_path = env::join_paths(
            std::iter::once(java_bin_dir).chain(env::split_paths(&current_path)),
        )
        .map_err(|error| tauri::Error::AssetNotFound(format!("failed to merge java path: {error}")))?;

        command.env("JAVA_HOME", &java_home).env(path_key, merged_path);
    }

    let mut child = command.spawn().map_err(tauri::Error::Io)?;

    if let Err(error) = wait_for_backend() {
        let _ = child.kill();
        let _ = child.wait();
        return Err(error);
    }

    Ok(Some(child))
}

fn resolve_backend_launcher(app: &AppHandle) -> Option<PathBuf> {
    let launcher_name = backend_launcher_name();
    let mut candidates = Vec::new();

    if let Ok(resource_dir) = app.path().resource_dir() {
        candidates.push(resource_dir.join("backend").join("bin").join(launcher_name));
    }

    if cfg!(debug_assertions) {
        let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        candidates.push(
            manifest_dir
                .join("resources")
                .join("backend")
                .join("bin")
                .join(launcher_name),
        );
        candidates.push(
            manifest_dir
                .join("..")
                .join("..")
                .join("backend")
                .join("build")
                .join("install")
                .join(BACKEND_NAME)
                .join("bin")
                .join(launcher_name),
        );
    }

    candidates.into_iter().find(|path| path.exists())
}

fn resolve_bundled_java_home(app: &AppHandle) -> Option<PathBuf> {
    let mut candidates = Vec::new();

    if let Ok(resource_dir) = app.path().resource_dir() {
        candidates.push(resource_dir.join("jre"));
    }

    if cfg!(debug_assertions) {
        let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        candidates.push(manifest_dir.join("resources").join("jre"));
    }

    candidates.into_iter().find(|path| path.exists())
}

fn backend_command_for(launcher: &Path) -> Command {
    #[cfg(windows)]
    {
        let extension = launcher
            .extension()
            .and_then(|value| value.to_str())
            .unwrap_or_default()
            .to_ascii_lowercase();

        if extension == "bat" || extension == "cmd" {
            let mut command = Command::new("cmd");
            command.arg("/C").arg(launcher);
            return command;
        }
    }

    let mut command = Command::new(launcher);
    command.current_dir(
        launcher
            .parent()
            .map(Path::to_path_buf)
            .unwrap_or_else(|| PathBuf::from(".")),
    );
    command
}

fn wait_for_backend() -> tauri::Result<()> {
    for _ in 0..BACKEND_READY_RETRIES {
        if backend_port_ready() {
            return Ok(());
        }

        thread::sleep(Duration::from_millis(BACKEND_READY_DELAY_MS));
    }

    Err(tauri::Error::AssetNotFound(
        "backend did not become ready in time".into(),
    ))
}

fn backend_port_ready() -> bool {
    let Ok(addresses) = (BACKEND_HOST, BACKEND_PORT).to_socket_addrs() else {
        return false;
    };

    addresses
        .into_iter()
        .any(|address| TcpStream::connect_timeout(&address, Duration::from_millis(200)).is_ok())
}

fn stop_backend(app: &AppHandle) {
    let state = app.state::<BackendProcess>();
    let Ok(mut child_slot) = state.0.lock() else {
        return;
    };

    if let Some(mut child) = child_slot.take() {
        let _ = child.kill();
        let _ = child.wait();
    }
}

fn backend_launcher_name() -> &'static str {
    #[cfg(windows)]
    {
        "dmanager-backend.bat"
    }

    #[cfg(not(windows))]
    {
        "dmanager-backend"
    }
}
