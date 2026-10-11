import json
import os
import signal
import subprocess
import tempfile
import time
from agent_engine_local.handler.filesystem import digest


class ProcessExecutionHandler:
    def run(self, argv, repository, home, timeout):
        started = time.monotonic()
        with tempfile.TemporaryFile(dir=home) as output:
            environment = {
                k: os.environ[k]
                for k in ("PATH", "HOME", "LANG", "TMPDIR")
                if k in os.environ
            }
            environment["PYTHONDONTWRITEBYTECODE"] = "1"
            process = subprocess.Popen(
                argv,
                cwd=repository,
                stdin=subprocess.DEVNULL,
                stdout=output,
                stderr=subprocess.STDOUT,
                env=environment,
                start_new_session=True,
            )
            timed_out = False
            try:
                code = process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                timed_out = True
                os.killpg(process.pid, signal.SIGKILL)
                code = process.wait()
            # A process may exit after leaving writers in its group. Terminate
            # remaining group members before accepting any result.
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            output.seek(0)
            output_hash = (
                __import__("hashlib").file_digest(output, "sha256").hexdigest()
            )
        return {
            "origin": "LOCAL_CHECKED",
            "command_hash": digest(json.dumps(argv).encode()),
            "output_hash": output_hash,
            "exit_code": code,
            "timed_out": timed_out,
            "duration_ms": int((time.monotonic() - started) * 1000),
        }
