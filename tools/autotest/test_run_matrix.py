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
        ns = argparse.Namespace(exclude=None, only=None, all_tests=False, selection=None, offline_tests=None,
                                no_offline_tests=False)
        for a in argv:
            setattr(ns, *a)
        return rm.test_selection(ns, d)
    assert sel() == ([], [], []), 'no file = full run, all in game'
    rm.write_json(os.path.join(d, rm.SELECTION), {'exclude': ['modutilities/Gui', ' ', 'stockmarket'],
                                                  'offline': ['modutilities/mu_utilities']})
    assert sel() == ([], ['modutilities/Gui', 'stockmarket'], ['modutilities/mu_utilities'])
    assert sel(('exclude', ['x', 'stockmarket']))[1] == ['modutilities/Gui', 'stockmarket', 'x'], '--exclude adds'
    assert sel(('all_tests', True)) == ([], [], ['modutilities/mu_utilities']), '--all-tests: no exclude, modes kept'
    assert sel(('only', ['a/B']), ('exclude', ['x'])) == (['a/B'], ['x'], ['modutilities/mu_utilities']), \
        '--only ignores the file exclude list, --exclude and the modes apply'
    assert sel(('offline_tests', ['m/C']))[2] == ['modutilities/mu_utilities', 'm/C'], '--offline-tests adds'
    assert sel(('offline_tests', ['m/C']), ('no_offline_tests', True))[2] == [], '--no-offline-tests: all in game'
    other = os.path.join(d, 'other.json')
    rm.write_json(other, {'exclude': ['y']})
    assert sel(('selection', other)) == ([], ['y'], [])
    rm.write(os.path.join(d, rm.SELECTION), 'not json')
    assert sel() == ([], [], []), 'broken file = full run'
    assert rm.selection_text([], []) == ''
    assert rm.selection_text(['a'], ['b', 'c']) == 'PARTIAL: only a; excluded b, c'

    # topics.json: latest list per mod, other mods kept; old plain-key entries read as needsGame
    res = os.path.join(d, 'cell', 'master', 'results.json')
    rm.write_json(res, {'mods': {'modutilities': {'topics': [{'key': 'Gui/B', 'needsGame': True},
                                                             {'key': 'Gui/A', 'needsGame': False}]}}})
    rm.write_json(os.path.join(d, rm.TOPICS), {'modutilities': ['Old/X'], 'stockmarket': ['Trading/S']})
    rm.merge_topics(d, [res])
    assert rm.read_topics(d) == {'modutilities/Gui/A': False, 'modutilities/Gui/B': True,
                                 'stockmarket/Trading/S': True}

# offline (JUnit) run: XML parse, and the fallback when a project has no offline tests
XML = '''<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="x" tests="6" skipped="2" failures="1" errors="0">
  <testcase name="modutilities/mu_utilities/UtilityTests/&lt;setup&gt;" classname="c"/>
  <testcase name="modutilities/mu_utilities/UtilityTests/colors" classname="c"/>
  <testcase name="modutilities/mu_parser/ParserTests/parse" classname="c"><failure message="m">x</failure></testcase>
  <testcase name="modutilities/mu_parser/ParserTests/skip" classname="c"><skipped/></testcase>
  <testcase name="modutilities/mu_gui/GuiTests/&lt;excluded&gt;" classname="c"><skipped/></testcase>
  <testcase name="topicSelection()" classname="c"/>
</testsuite>'''
with tempfile.TemporaryDirectory() as d:
    r = rm.junit_results(d)
    assert (r['passed'], r['suites']) == (0, []), 'no reports'
    rm.write(os.path.join(d, 'TEST-x.xml'), XML)
    r = rm.junit_results(d)
    assert (r['passed'], r['failed'], r['errors'], r['skipped']) == (3, 1, 0, 1), r
    assert r['suites'] == ['modutilities/mu_utilities/UtilityTests', 'modutilities/mu_parser/ParserTests']
    assert r['excludedSuites'] == ['modutilities/mu_gui/GuiTests']
    assert r['failures'] == ['modutilities/mu_parser/ParserTests/parse']

    project, pdir = os.path.join(d, 'proj'), os.path.join(d, 'out', 'proj')
    xml_dir = os.path.join(project, 'common', 'build', 'test-results', 'test')
    args = argparse.Namespace(only=[], exclude=['modutilities/mu_gui'], offline_topics=['modutilities'], offline=False)
    row, ran = rm.run_offline(args, 'proj', project, pdir, os.path.join(d, 'log'))
    assert (ran, row['ok'], rm.read_json(os.path.join(pdir, 'offline.json'))['state']) == ([], True, 'n/a'), \
        'no OfflineTests caller: nothing skipped in game'
    rm.write(os.path.join(project, 'common', 'src', 'test', 'X.java'), 'OfflineTests.run(() -> {});')
    rm.write(os.path.join(xml_dir, 'TEST-stale.xml'), XML)
    calls = []
    real_gradle = rm.gradle
    rm.gradle = lambda project, tasks, offline, log: calls.append(tasks) or 1  # compile error: no reports
    row, ran = rm.run_offline(args, 'proj', project, pdir, os.path.join(d, 'log'))
    assert ran == [] and row['errors'] == 1 and not row['ok'], 'stale reports deleted, failed gradle = error'
    assert '-Dmodutilities.tests.offline=modutilities' in calls[0] and \
        '-Dmodutilities.tests.exclude=modutilities/mu_gui' in calls[0] and ':common:cleanTest' in calls[0]
    rm.gradle = lambda project, tasks, offline, log: rm.write(os.path.join(xml_dir, 'TEST-x.xml'), XML) or 1
    row, ran = rm.run_offline(args, 'proj', project, pdir, os.path.join(d, 'log'))
    assert len(ran) == 2 and row['failed'] == 1 and row['errors'] == 0 and not row['ok']
    rm.gradle = real_gradle

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

# GUI topic modes (leaves: key -> needsGame)
leaves = {'m/A/S1': False, 'm/A/S2': True, 'm/B/S3': False}
assert gui.topic_mode('m/A/S1', False, ['m'], ['m']) == 'off', 'off wins'
assert gui.topic_mode('m/A/S2', True, [], ['m']) == 'online', 'needs game: never offline'
assert gui.set_mode(leaves, [], [], 'm/A', 'off') == (['m/A/S1', 'm/A/S2'], [])
assert gui.set_mode(leaves, ['m/A/S1'], [], 'm', 'offline') == ([], ['m/A/S1', 'm/B/S3']), \
    'offline only where capable, others keep their mode'
assert gui.set_mode(leaves, ['m/A/S2'], [], 'm', 'offline') == (['m/A/S2'], ['m/A/S1', 'm/B/S3'])
assert gui.set_mode(leaves, ['m'], ['m'], 'm/A/S1', 'online') == (['m/A/S2', 'm/B/S3'], []), 'mod keys expanded'
assert gui.set_mode(leaves, ['gone/X'], ['gone/Y'], 'm', 'online') == (['gone/X'], ['gone/Y']), 'unknown kept'
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
