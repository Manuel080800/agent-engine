#!/usr/bin/env python3
"""Create local deployment secrets without printing them or replacing existing files."""

import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parents[1]
folder = root / ".local/secrets"
folder.mkdir(parents=True, exist_ok=True, mode=0o700)
os.chmod(folder, 0o700)
for name in ("spring.datasource.password", "engine.bootstrap-token", "tls.password"):
    target = folder / name
    if not target.exists():
        fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as stream:
            stream.write(secrets.token_urlsafe(48))
if not (folder / "server.p12").exists():
    subprocess.run(
        [
            "openssl",
            "req",
            "-x509",
            "-newkey",
            "rsa:3072",
            "-nodes",
            "-days",
            "365",
            "-keyout",
            str(folder / "server.key"),
            "-out",
            str(folder / "server.crt"),
            "-subj",
            "/CN=localhost",
            "-addext",
            "subjectAltName=DNS:localhost,IP:127.0.0.1",
        ],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    subprocess.run(
        [
            "openssl",
            "pkcs12",
            "-export",
            "-inkey",
            str(folder / "server.key"),
            "-in",
            str(folder / "server.crt"),
            "-name",
            "server",
            "-out",
            str(folder / "server.p12"),
            "-passout",
            "file:" + str(folder / "tls.password"),
        ],
        check=True,
    )
for target in folder.iterdir():
    os.chmod(target, 0o600)
print("Secrets created in .local/secrets. No existing secrets were replaced.")
