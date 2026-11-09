import contextlib
import io
import json
import os
import runpy
import sys
import time
import traceback

MAX_OUTPUT_CHARS = 48_000
MAX_EXECUTION_SECONDS = 5.0


class BoundedStringIO(io.StringIO):
    def __init__(self):
        super().__init__()
        self._written = 0
        self.truncated = False

    def write(self, value):
        remaining = MAX_OUTPUT_CHARS - self._written
        if remaining > 0:
            chunk = value[:remaining]
            super().write(chunk)
            self._written += len(chunk)
        if len(value) > max(remaining, 0):
            self.truncated = True
        return len(value)

    def bounded_value(self):
        value = super().getvalue()
        if self.truncated:
            return value + "\\n[output truncated at 48,000 characters]"
        return value


def run_script(script_path, project_root):
    output = BoundedStringIO()
    errors = BoundedStringIO()
    previous_cwd = os.getcwd()
    previous_path = list(sys.path)
    previous_trace = sys.gettrace()
    root = os.path.realpath(project_root)
    script = os.path.realpath(script_path)

    if script != root and not script.startswith(root + os.sep):
        return json.dumps({
            "success": False,
            "output": "",
            "error": "Python script is outside the selected project."
        })

    deadline = time.monotonic() + MAX_EXECUTION_SECONDS

    def deadline_trace(frame, event, arg):
        if time.monotonic() > deadline:
            sys.settrace(None)
            raise TimeoutError("Python script exceeded the 5 second execution limit.")
        return deadline_trace

    try:
        os.chdir(root)
        if root not in sys.path:
            sys.path.insert(0, root)
        sys.settrace(deadline_trace)
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            runpy.run_path(script, run_name="__main__")
        return json.dumps({
            "success": True,
            "output": output.bounded_value(),
            "error": errors.bounded_value()
        })
    except BaseException:
        return json.dumps({
            "success": False,
            "output": output.bounded_value(),
            "error": traceback.format_exc()[-MAX_OUTPUT_CHARS:]
        })
    finally:
        sys.settrace(previous_trace)
        os.chdir(previous_cwd)
        sys.path[:] = previous_path
