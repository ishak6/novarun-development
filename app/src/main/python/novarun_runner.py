import contextlib
import io
import json
import os
import runpy
import sys
import traceback


def run_script(script_path, project_root):
    output = io.StringIO()
    errors = io.StringIO()
    previous_cwd = os.getcwd()
    root = os.path.realpath(project_root)
    script = os.path.realpath(script_path)

    if script != root and not script.startswith(root + os.sep):
        return json.dumps({
            "success": False,
            "output": "",
            "error": "Python script is outside the selected project."
        })

    try:
        os.chdir(root)
        if root not in sys.path:
            sys.path.insert(0, root)
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            runpy.run_path(script, run_name="__main__")
        return json.dumps({
            "success": True,
            "output": output.getvalue(),
            "error": errors.getvalue()
        })
    except BaseException:
        return json.dumps({
            "success": False,
            "output": output.getvalue(),
            "error": traceback.format_exc()
        })
    finally:
        os.chdir(previous_cwd)
