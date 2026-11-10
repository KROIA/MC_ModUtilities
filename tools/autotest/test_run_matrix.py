"""Self-check of run_matrix.client_crashed (no game needed): python tools/autotest/test_run_matrix.py"""
import os
import tempfile
import time

import run_matrix as rm

with tempfile.TemporaryDirectory() as d:
    inst = rm.Instance('client', {}, d, {})
    reports = os.path.join(d, 'crash-reports')
    rm.write(os.path.join(reports, 'crash-old-fml.txt'), 'from an earlier run')
    os.utime(os.path.join(reports, 'crash-old-fml.txt'), (0, 0))
    inst.started = time.time() - 5
    assert not rm.client_crashed(inst), 'no console.log, old report only'
    rm.write(inst.console, '[Render thread/INFO] (Minecraft) Backend library: LWJGL version 3.3.3\n')
    rm.write(os.path.join(reports, 'crash-new-client.txt'), 'Client shutdown from post-main')  # exits by itself
    assert not rm.client_crashed(inst), 'normal log, vanilla client report'
    rm.write(os.path.join(reports, 'crash-new-fml.txt'), 'mod loading error')
    assert rm.client_crashed(inst), 'new NeoForge mod-loading report'
    os.remove(os.path.join(reports, 'crash-new-fml.txt'))
    rm.write(inst.console, 'Exception in thread "main" java.lang.reflect.InvocationTargetException\n')
    assert rm.client_crashed(inst), 'main thread died'
print('client_crashed ok')
