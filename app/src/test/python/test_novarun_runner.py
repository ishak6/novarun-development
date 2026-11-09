import importlib.util
import json
import os
import pathlib
import sys
import tempfile
import unittest

MODULE_PATH = pathlib.Path(__file__).resolve().parents[2] / "main" / "python" / "novarun_runner.py"
SPEC = importlib.util.spec_from_file_location("novarun_runner", MODULE_PATH)
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


class PythonRunnerTests(unittest.TestCase):
    def test_runs_script_and_captures_output(self):
        with tempfile.TemporaryDirectory() as directory:
            script = pathlib.Path(directory) / "hello.py"
            script.write_text("print('NovaRun Python OK')\n", encoding="utf-8")
            result = json.loads(runner.run_script(str(script), directory))
            self.assertTrue(result["success"])
            self.assertIn("NovaRun Python OK", result["output"])

    def test_bounds_large_output(self):
        with tempfile.TemporaryDirectory() as directory:
            script = pathlib.Path(directory) / "large.py"
            script.write_text("print('x' * 60000)\\n", encoding="utf-8")
            result = json.loads(runner.run_script(str(script), directory))
            self.assertTrue(result["success"])
            self.assertLessEqual(len(result["output"]), 48_100)
            self.assertIn("output truncated", result["output"])

    def test_captures_script_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            script = pathlib.Path(directory) / "failure.py"
            script.write_text("raise ValueError('expected test error')\n", encoding="utf-8")
            result = json.loads(runner.run_script(str(script), directory))
            self.assertFalse(result["success"])
            self.assertIn("expected test error", result["error"])

    def test_rejects_script_outside_project(self):
        with tempfile.TemporaryDirectory() as root, tempfile.TemporaryDirectory() as outside:
            script = pathlib.Path(outside) / "outside.py"
            script.write_text("print('should not run')\n", encoding="utf-8")
            result = json.loads(runner.run_script(str(script), root))
            self.assertFalse(result["success"])
            self.assertIn("outside", result["error"])

    def test_restores_working_directory_and_import_path(self):
        original_cwd = os.getcwd()
        original_path = list(sys.path)
        with tempfile.TemporaryDirectory() as directory:
            script = pathlib.Path(directory) / "path.py"
            script.write_text("print('cwd test')\n", encoding="utf-8")
            runner.run_script(str(script), directory)
        self.assertEqual(original_cwd, os.getcwd())
        self.assertEqual(original_path, sys.path)


if __name__ == "__main__":
    unittest.main()
