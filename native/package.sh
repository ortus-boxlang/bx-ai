#!/usr/bin/env bash
# After ./gradlew shadowJar, stage one combined, installer-ready module ZIP.
set -euo pipefail
cd "$(dirname "$0")/.."
root=$PWD
sdk=$(cd "${MATCHBOX_SDK:-../matchbox/crates/matchbox-plugin-sdk}" && pwd)
target=$(rustc -vV | awk '/^host:/ {print $2}')
case "$target" in
  *-linux-*) lib=libbx_ai_native.so;;
  *-apple-darwin) lib=libbx_ai_native.dylib;;
  *) echo "Unsupported host for native bx-ai packaging: $target" >&2; exit 1;;
esac
test -f build/module/models/registry/NativeBoxAiService.bx || { echo 'Build the JVM module with ./gradlew shadowJar first.' >&2; exit 1; }
cargo build --manifest-path native/Cargo.toml --release --offline --locked --target "$target" --target-dir "$root/build/bxai-cargo" \
  --config "patch.crates-io.matchbox-plugin-sdk.path=\"$sdk\""
python3 - "$target" "$lib" <<'PY'
from pathlib import Path
import hashlib, json, shutil, sys, tempfile, zipfile
target, library = sys.argv[1:]
source = Path('build/module')
if not any((source / 'libs').glob('*.jar')): raise SystemExit('Missing JVM module JAR')
out = Path('build/distributions'); out.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(dir=out) as staging:
    module = Path(staging) / 'bxai'; shutil.copytree(source, module)
    lib = module / 'native' / library; lib.parent.mkdir(exist_ok=True)
    shutil.copyfile(Path('build/bxai-cargo') / target / 'release' / library, lib)
    manifest = module / 'box.json'; data = json.loads(manifest.read_text())
    data.setdefault('matchbox', {}).setdefault('artifacts', {})[target] = dict(abi=1, path='native/'+library, sha256=hashlib.sha256(lib.read_bytes()).hexdigest())
    manifest.write_text(json.dumps(data, indent=2)+'\n')
    zip_path = out / f"bx-ai-{data['version']}-{target}.zip"
    temporary_zip = Path(staging) / 'combined.zip'
    with zipfile.ZipFile(temporary_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(module.rglob('*')):
            if path.is_file(): archive.write(path, path.relative_to(module))
    temporary_zip.replace(zip_path)
    print(zip_path)
PY
