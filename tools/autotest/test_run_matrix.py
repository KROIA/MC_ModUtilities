"""Self-check of run_matrix.client_crashed and the test-topic selection (no game needed):
python tools/autotest/test_run_matrix.py"""
import argparse
import json
import os
import tempfile
import time

import matrix_gui as gui
import run_matrix as rm

# test-topic matching: same cases as OfflineTestsTest.topicSelection (Java TestRegistry.isExcluded)
key = 'stockmarket/Trading/BotStressTestSuite'
assert rm.excluded(key, [], ['stockmarket/Trading/BotStressTestSuite'])
assert rm.excluded(key, [], ['x', ' stockmarket/Trading'])
assert rm.excluded(key, [], ['stockmarket'])
assert rm.excluded(key, [], ['stockmarket/*/BotStressTestSuite'])
assert not rm.excluded(key, [], ['stockmarket/Trad']), "prefix only on '/' boundaries"
assert not rm.excluded(key, [], ['stockmarket/Trading/BotStressTestSuite/x'])
assert not rm.excluded(key, [], ['*/*/Other'])
assert not rm.excluded(key, [], [])
assert not rm.excluded(key, ['stockmarket/*/BotStressTestSuite'], []), 'included'
assert rm.excluded(key, ['modutilities'], []), 'not included'
assert rm.excluded(key, ['stockmarket'], ['stockmarket/Trading']), 'exclude applies on top of include'

# selection file + CLI merge
with tempfile.TemporaryDirectory() as d:
    def sel(*argv):
        ns = argparse.Namespace(exclude=None, only=None, all_tests=False, selection=None)
        for a in argv:
            setattr(ns, *a)
        return rm.test_selection(ns, d)
    assert sel() == ([], []), 'no file = full run'
    rm.write_json(os.path.join(d, rm.SELECTION), {'exclude': ['modutilities/Gui', ' ', 'stockmarket']})
    assert sel() == ([], ['modutilities/Gui', 'stockmarket'])
    assert sel(('exclude', ['x', 'stockmarket'])) == ([], ['modutilities/Gui', 'stockmarket', 'x']), '--exclude adds'
    assert sel(('all_tests', True)) == ([], []), '--all-tests ignores the file'
    assert sel(('only', ['a/B']), ('exclude', ['x'])) == (['a/B'], ['x']), '--only ignores the file, --exclude applies'
    other = os.path.join(d, 'other.json')
    rm.write_json(other, {'exclude': ['y']})
    assert sel(('selection', other)) == ([], ['y'])
    rm.write(os.path.join(d, rm.SELECTION), 'not json')
    assert sel() == ([], []), 'broken file = full run'
    assert rm.selection_text([], []) == ''
    assert rm.selection_text(['a'], ['b', 'c']) == 'PARTIAL: only a; excluded b, c'

    # topics.json: latest list per mod, other mods kept
    res = os.path.join(d, 'cell', 'master', 'results.json')
    rm.write_json(res, {'mods': {'modutilities': {'topics': ['Gui/B', 'Gui/A']}}})
    rm.write_json(os.path.join(d, rm.TOPICS), {'modutilities': ['Old/X'], 'stockmarket': ['Trading/S']})
    rm.merge_topics(d, [res])
    with open(os.path.join(d, rm.TOPICS)) as f:
        assert json.load(f) == {'modutilities': ['Gui/A', 'Gui/B'], 'stockmarket': ['Trading/S']}

# global port lock: same file for every --out; a live holder makes take_lock exit (without --wait-lock)
assert rm.LOCK == os.path.join(rm.MODS_ROOT, 'autotest-out', 'running.lock')
with tempfile.TemporaryDirectory() as d:
    lock = os.path.join(d, 'sub', 'running.lock')
    rm.take_lock(lock, False)  # no lock yet: taken
    assert rm.lock_pid(lock) == os.getpid()
    try:
        rm.take_lock(lock, False)  # held by a live process (this one)
        assert False, 'should exit'
    except SystemExit as e:
        assert 'holds' in str(e)

# GUI tree click
leaves = ['m/A/S1', 'm/A/S2', 'm/B/S3']
assert gui.toggle_topic(leaves, [], 'm/A') == ['m/A/S1', 'm/A/S2'], 'all on -> off'
assert gui.toggle_topic(leaves, ['m/A/S1'], 'm/A') == [], 'mixed -> on'
assert gui.toggle_topic(leaves, ['m'], 'm/A/S1') == ['m/A/S2', 'm/B/S3'], 'mod key expanded to suites'
assert gui.toggle_topic(leaves, ['gone/X'], 'm') == ['m/A/S1', 'm/A/S2', 'm/B/S3', 'gone/X'], 'unknown key kept'
print('topic selection ok')

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
