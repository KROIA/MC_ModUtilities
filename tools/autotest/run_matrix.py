#!/usr/bin/env python3
"""Runs the in-game unit tests of a mod project in every server configuration and on every loader.

Stdlib only. See README.md in this folder. Short version:

    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities
    python tools/autotest/run_matrix.py --root E:/.../StockMarket --versions 1.21.1 26.3 --configs sp mp
    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities --world <world folder>
    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities --play fabric mp [--world <world folder>]
    python tools/autotest/run_matrix.py --clean-build [--jobs 4] [--build-kind dev] <project> ...   (clean jars)

Per project it runs Gradle once per run task (runClient/runServer with a capture agent, added by
capture.init.gradle) to record Loom's exact launch commands, then starts every game instance directly,
so master, slave and client of one project run side by side without fighting over Gradle locks.
The project must be a dev build (enable_dev_features=true) for the tests and the autotest hook to exist.
"""
import argparse
import concurrent.futures
import glob
import gzip
import hashlib
import itertools
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
MODS_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))  # tools/autotest -> repo -> mod -> mods
CONFIGS = ('sp', 'mp', 'slave', 'spslave')
LOADERS = ('fabric', 'neoforge')
LP_MODES = ('off', 'on', 'both', 'removed', 'all')  # --luckperms
LP_SUFFIX = '-lp'  # cell label of a config run with LuckPerms, e.g. mp-lp
REMOVED = 'lp-removed'  # two-phase cell: mp with LuckPerms, then mp without it on the same world
PHASE_FILE = os.path.join('testing', 'luckperms_phase1.json')  # in the world folder (LuckPermsPhase.FILE)
MASTER_PORT, SLAVE_PORT, LINK_PORT = 25611, 25612, 25690
# One port lock for every run, whatever its --out (the default out folder's lock): all runs share the ports above.
LOCK = os.path.join(MODS_ROOT, 'autotest-out', 'running.lock')
SECRET = 'autotest-secret'
NO_WINDOW = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0  # no console pop-ups under the windowless GUI
SELECTION = 'test-selection.json'  # <out>/: {"exclude": [patterns], "offline": [patterns]}, the GUI's Topics dialog
TOPICS = 'topics.json'  # <out>/: {modid: [{key: Category/SuiteClass, needsGame}]} from the latest results of each mod
EXCLUDE_PROP, INCLUDE_PROP = 'modutilities.tests.exclude', 'modutilities.tests.include'  # TestRegistry
OFFLINE_PROP = 'modutilities.tests.offline'


# ---------------------------------------------------------------- launch capture

def build_agent(out_dir):
    """Compiles CaptureAgent.java into capture.jar (javac + jar from the JDK on PATH)."""
    cls = os.path.join(out_dir, 'agent-classes')
    jar = os.path.join(out_dir, 'capture.jar')
    os.makedirs(cls, exist_ok=True)
    subprocess.run(['javac', '--release', '21', '-d', cls, os.path.join(HERE, 'CaptureAgent.java')], check=True,
                   creationflags=NO_WINDOW)
    manifest = os.path.join(out_dir, 'agent-manifest.txt')
    with open(manifest, 'w') as f:
        f.write('Premain-Class: CaptureAgent\n')
    subprocess.run(['jar', 'cfm', jar, manifest, '-C', cls, '.'], check=True, creationflags=NO_WINDOW)
    return jar


def gradlew(project):
    return os.path.join(project, 'gradlew.bat' if os.name == 'nt' else 'gradlew')


def build(project, loaders, offline, log, out, kind='dev', clean=False):
    """Builds the loaders' mod jars, kind 'dev' (dev features on: the jars fingerprinted in cell.json are the tested
    code) or 'release'; clean: `clean build` of the whole project (--clean-build). Records the result per loader in
    <out>/<project>/build.json (read by the GUI). A timeout returns -1.
    Non-clean builds include :common:build: dependents compile against the plain common jar (26.x: its -dev jar) in
    common/build/libs via flatDir, which the loader builds do not rebuild (a release one would lack the tests)."""
    tasks = ['clean', 'build'] if clean else [':common:build'] + [f':{l}:build' for l in loaders]
    start, started = os.path.getsize(log), now()
    path = os.path.join(out, os.path.basename(project), 'build.json')
    entry = {'kind': kind, 'started': started, 'log': log}
    write_json(path, {**(read_json(path) or {}), **{l: entry for l in loaders}})  # no 'finished' = building
    try:
        rc = gradle(project, tasks + ['-x', 'test', f"-Penable_dev_features={'true' if kind == 'dev' else 'false'}"],
                    offline, log)
    except subprocess.TimeoutExpired:
        rc = -1
    with open(log, 'rb') as f:  # only what this build wrote
        f.seek(start)
        text = f.read().decode('utf-8', 'replace')
    entry = {**entry, 'rc': rc, 'finished': now()}
    errors = build_errors(text, loaders, rc)
    write_json(path, {**(read_json(path) or {}), **{l: {**entry, **e} for l, e in errors.items()}})  # other loaders stay
    return rc


def build_errors(text, loaders, rc):
    """Per loader {'failed', 'error'} from one build's Gradle output: `> Task :<loader>:<task> FAILED` lines, error =
    the lines after `* What went wrong:`. A failed build without a failed loader task (e.g. :common) fails every
    loader; rc -1 = timeout."""
    failed = set(re.findall(r'^> Task :([^:\s]+):\S+ FAILED', text, re.M)) & set(loaders)
    lines = text.splitlines()
    i = next((n for n, l in enumerate(lines) if l.startswith('* What went wrong:')), None)
    err = ['timeout'] if rc == -1 else [] if i is None else \
        [l for l in itertools.takewhile(lambda l: not l.startswith('* '), lines[i + 1:i + 9]) if l.strip()]
    if rc and not failed:
        failed = set(loaders)
    return {l: {'failed': l in failed, 'error': err if l in failed else []} for l in loaders}


def gradle(project, tasks, offline, log):
    """Runs gradlew with these tasks/args in the project, output appended to log; returns the exit code."""
    cmd = [gradlew(project)] + tasks + (['--offline'] if offline else [])
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        f.write(f'>>> {" ".join(cmd)}\n')
        f.flush()
        return subprocess.run(cmd, cwd=project, stdout=f, stderr=subprocess.STDOUT, timeout=3600,
                              creationflags=NO_WINDOW).returncode


def build_deps(project, loaders, offline, log, built, out):
    """Builds the flatDir dependency projects (found via mod_jars: <dep>/<loader>/build/libs/x.jar), their own
    dependencies first, so the project never runs against stale dependency jars. `built` maps project -> rc and
    keeps every project to one build per run_matrix invocation. Returns the first failed project or None."""
    for dep in dict.fromkeys(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(j))))
                             for l in loaders for j in mod_jars(project, l)[1:]):
        failed = build_deps(dep, loaders, offline, log, built, out)
        if failed:
            return failed
        key = os.path.normcase(dep)
        if key not in built:
            print(f'[{os.path.basename(project)}] building dependency {os.path.basename(dep)} ...', flush=True)
            built[key] = build(dep, loaders, offline, log, out)
        if built[key] != 0:
            return dep
    return None


def clear_remap_cache(project, log, stop_daemons=True):
    """Deletes Loom's remapped copies of the flatDir mod jars (.gradle/loom-cache/remapped_mods): Loom would keep
    using an old remapped dependency after its jar changed, so the run would not test what the fingerprint says.
    On a Windows file lock, stops the project's Gradle daemons (unless not stop_daemons) and retries once.
    Returns False if it stays."""
    cache = os.path.join(project, '.gradle', 'loom-cache', 'remapped_mods')
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        for attempt in (1, 2) if stop_daemons else (2,):
            if not os.path.exists(cache):
                return True
            try:
                shutil.rmtree(cache)
                f.write(f'>>> cleared {cache}\n')
                return True
            except OSError as e:
                f.write(f'>>> could not clear {cache}: {e}\n')
                if attempt == 1:
                    f.write('>>> gradlew --stop\n')
                    f.flush()
                    try:
                        subprocess.run([gradlew(project), '--stop'], cwd=project, stdout=f, stderr=subprocess.STDOUT,
                                       creationflags=NO_WINDOW)
                    except OSError as e:
                        f.write(f'>>> {e}\n')
    return False


def dep_hashes(project, loaders):
    """sha256 of the dependency mod jars (flatDir) of these loaders; empty for mods without such dependencies."""
    return {j: file_sha256(j) for l in loaders for j in mod_jars(project, l)[1:]}


def capture(project, loaders, agent, out_dir, offline, log, lp=False):
    """Gradle starts every run task with the agent, which writes <loader>-<task>.json and exits.

    One Gradle invocation per task: on Gradle 9 runClient and runServer in one build fail validation
    (runServer reads the run dir that runClient's downloadAssets writes, without a declared dependency).
    lp: capture with -Pwith_luckperms=true (LuckPerms on the dev runtime classpath) into launch-lp/.
    """
    cap = os.path.join(out_dir, 'launch-lp' if lp else 'launch')
    shutil.rmtree(cap, ignore_errors=True)
    os.makedirs(cap)
    rc = 0
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        for task in [f':{l}:{t}' for l in loaders for t in ('runClient', 'runServer')]:
            cmd = [gradlew(project), task,
                   '-I', os.path.join(HERE, 'capture.init.gradle'), f'-Pautotest_agent={agent}={cap}',
                   '-Penable_dev_features=true'] + (['-Pwith_luckperms=true'] if lp else []) \
                + (['--offline'] if offline else [])  # tests only exist in dev builds
            f.write(f'>>> {" ".join(cmd)}\n')
            f.flush()
            rc = subprocess.run(cmd, cwd=project, stdout=f, stderr=subprocess.STDOUT, timeout=3600,
                                creationflags=NO_WINDOW).returncode or rc
    specs = {}
    for l in loaders:
        for t in ('runClient', 'runServer'):
            p = os.path.join(cap, f'{l}-{t}.json')
            if os.path.exists(p):
                with open(p, encoding='utf-8') as f:
                    specs[(l, t)] = json.load(f)
    return rc, specs


# ---------------------------------------------------------------- jar fingerprints

def gradle_props(project):
    props = {}
    try:
        with open(os.path.join(project, 'gradle.properties'), encoding='utf-8') as f:
            for line in f:
                k, sep, v = line.partition('=')
                if sep and not k.lstrip().startswith('#'):
                    props[k.strip()] = v.strip()
    except OSError:
        pass
    return props


def mod_jars(project, loader):
    """The mod jars a project/loader run uses: <loader>/build/libs/<archives_name>-<loader>-<mod_version>.jar, plus
    the dependency jars its <loader>/build.gradle resolves via flatDir (<dep>-<loader>-<gradle.properties dep_version>.jar)."""
    props = gradle_props(project)
    jars = [os.path.join(project, loader, 'build', 'libs',
                         f"{props.get('archives_name')}-{loader}-{props.get('mod_version')}.jar")]
    try:
        with open(os.path.join(project, loader, 'build.gradle'), encoding='utf-8') as f:
            dirs = re.findall(r'^\s*dir\s+"([^"]+)"', f.read(), re.M)
    except OSError:
        dirs = []
    for d in dirs:
        d = os.path.join(project, loader, d.replace('${minecraft_version}', props.get('minecraft_version', '')))
        name = gradle_props(os.path.join(d, '..', '..', '..')).get('archives_name', 'missing-project')
        jars.append(os.path.join(d, f"{name}-{loader}-{props.get(name + '_version')}.jar"))
    return [os.path.normpath(j) for j in jars]


_hashes = {}


def file_sha256(path):
    """sha256 of a file or None if missing; cached by path+mtime+size (the GUI polls this)."""
    try:
        st = os.stat(path)
    except OSError:
        return None
    key = (path, st.st_mtime_ns, st.st_size)
    if key not in _hashes:
        h = hashlib.sha256()
        with open(path, 'rb') as f:
            for chunk in iter(lambda: f.read(1 << 20), b''):
                h.update(chunk)
        _hashes[key] = h.hexdigest()
    return _hashes[key]


def jar_hashes(project, loader):
    return {j: file_sha256(j) for j in mod_jars(project, loader)}


def fingerprint(jars):
    return hashlib.sha256(json.dumps(sorted(jars.items())).encode()).hexdigest()[:16]


def world_fingerprint(world):
    """Cheap fingerprint of a template world (path + name/size/mtime of every file but session.lock), or None."""
    if not world:
        return None
    h = hashlib.sha256(os.path.normcase(os.path.abspath(world)).encode())
    for dirpath, dirs, files in os.walk(world):
        dirs.sort()
        for f in sorted(files):
            if f != 'session.lock':
                try:
                    st = os.stat(os.path.join(dirpath, f))
                except OSError:
                    continue  # the game is saving right now
                h.update(f'{os.path.relpath(os.path.join(dirpath, f), world)}|{st.st_size}|{st.st_mtime_ns}\n'.encode())
    return h.hexdigest()[:16]


def world_data_version(world):
    """DataVersion of a world's level.dat (gzip NBT; the int tag is found by name, no NBT parser), or None."""
    try:
        with gzip.open(os.path.join(world, 'level.dat')) as f:
            raw = f.read()
    except (OSError, EOFError):
        return None
    found = [struct.unpack('>i', m.group(1))[0] for m in re.finditer(rb'\x03\x00\x0bDataVersion(.{4})', raw, re.S)]
    return max(found) if found else None


def game_data_version(spec):
    """world_version of the game a launch spec runs (version.json in its Minecraft jar), or None."""
    for j in spec['classpath'].split(os.pathsep):
        b = os.path.basename(j).lower()
        if b.endswith('.jar') and ('minecraft' in b or 'client-extra' in b):  # Loom merged jar / NeoForge resources
            try:
                with zipfile.ZipFile(j) as z:
                    return json.loads(z.read('version.json'))['world_version']
            except (OSError, KeyError, ValueError, zipfile.BadZipFile):
                continue
    return None


# ---------------------------------------------------------------- instances

def argfile_quote(a):
    return '"' + a.replace('\\', '\\\\').replace('"', '\\"') + '"'


class Instance:
    def __init__(self, name, spec, run_dir, props):
        self.name, self.dir, self.proc = name, run_dir, None
        self.results = os.path.join(run_dir, 'results.json')
        self.console = os.path.join(run_dir, 'console.log')
        self.spec, self.props = spec, {'modutilities.autotest': name, 'modutilities.autotest.results': self.results, **props}

    def start(self):
        # Long classpaths/module paths exceed the Windows command line limit: use a JVM @argfile.
        args = list(self.spec['jvmArgs']) + [f'-D{k}={v}' for k, v in self.props.items()]
        args += ['-cp', self.spec['classpath']] + self.spec['command'].split(' ')
        argfile = os.path.join(self.dir, 'jvm.args')
        with open(argfile, 'w', encoding='utf-8') as f:
            f.write('\n'.join(argfile_quote(a) for a in args))
        java = os.path.join(self.spec['javaHome'], 'bin', 'java')
        self.log = open(self.console, 'w', encoding='utf-8', errors='replace')
        self.started = time.time()
        flags = subprocess.CREATE_NEW_PROCESS_GROUP | NO_WINDOW if os.name == 'nt' else 0  # the game window stays
        self.proc = subprocess.Popen([java, '@' + argfile], cwd=self.dir, env=self.spec['env'], stdin=subprocess.PIPE,
                                     stdout=self.log, stderr=subprocess.STDOUT, creationflags=flags)

    def alive(self):
        return self.proc is not None and self.proc.poll() is None

    def log_has(self, text):
        try:
            with open(self.console, encoding='utf-8', errors='replace') as f:
                return text in f.read()
        except OSError:
            return False

    def stop(self, grace):
        """Asks a dedicated server to stop via its console, then kills the process tree."""
        if not self.alive():
            return
        try:
            self.proc.stdin.write(b'stop\n')
            self.proc.stdin.flush()
        except OSError:
            pass
        self.wait(grace)
        self.kill()

    def wait(self, timeout):
        try:
            self.proc.wait(timeout=max(1, timeout))
        except subprocess.TimeoutExpired:
            pass

    def kill(self):
        if self.alive():
            if os.name == 'nt':
                subprocess.run(['taskkill', '/T', '/F', '/PID', str(self.proc.pid)], capture_output=True,
                               creationflags=NO_WINDOW)
            else:
                self.proc.kill()
            self.wait(10)
        self.log.close()


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)


def prepare(run_dir, multiserver, server_port=None):
    """Fresh run directory with the multi-server config, and for dedicated servers eula/server.properties."""
    if os.path.exists(run_dir):
        shutil.rmtree(run_dir)  # fails loudly if a leftover game process still holds files
    os.makedirs(run_dir)
    write(os.path.join(run_dir, 'config', 'modutilities', 'MultiServerConfig.json'), json.dumps(multiserver, indent=2))
    if server_port is None:
        # Client: no accessibility onboarding, no pause when unfocused (that would pause singleplayer), quiet.
        write(os.path.join(run_dir, 'options.txt'), '\n'.join([
            'onboardAccessibility:false', 'pauseOnLostFocus:false', 'tutorialStep:none', 'joinedFirstServer:true',
            'skipMultiplayerWarning:true', 'renderDistance:2', 'simulationDistance:5', 'soundCategory_master:0.0',
            'narrator:0', 'fullscreen:false', '']))
    else:
        write(os.path.join(run_dir, 'eula.txt'), 'eula=true\n')
        # newer versions (26.3) default the whitelist on -> client gets "not white-listed"
        write(os.path.join(run_dir, 'server.properties'), '\n'.join([
            f'server-port={server_port}', 'online-mode=false', 'white-list=false', 'enforce-whitelist=false',
            'enforce-secure-profile=false', 'level-seed=20240101',
            'level-type=minecraft\\:flat', 'generate-structures=false', 'difficulty=peaceful', 'spawn-protection=0',
            'gamemode=creative', 'force-gamemode=true',
            'max-tick-time=-1', 'view-distance=4', 'simulation-distance=4', 'enable-rcon=false', 'motd=autotest', '']))


def ms_off():
    return {'enable': False}


def ms_master():
    return {'enable': True, 'isMaster': True, 'masterTcpPort': LINK_PORT, 'sharedSecret': SECRET}


def ms_slave(slave_id):
    return {'enable': True, 'isMaster': False, 'slaveID': slave_id, 'masterHost': '127.0.0.1',
            'masterTcpPort': LINK_PORT, 'sharedSecret': SECRET}


def wait_until(cond, timeout, *must_live):
    end = time.time() + timeout
    while time.time() < end:
        if cond():
            return True
        if any(not i.alive() for i in must_live):
            return False
        time.sleep(1)
    return False


def client_crashed(inst):
    """A startup crash the client never quits from: its main thread died before Minecraft's crash handler (the dev
    launcher's non-daemon pools keep the JVM up), or NeoForge wrote a mod-loading crash report (crash-*-fml.txt) and
    waits on its error screen. crash-*-client.txt is ignored: Minecraft exits after it, and 26.x writes one on every
    normal quit ("Client shutdown from post-main")."""
    return inst.log_has('Exception in thread "main"') or any(
        os.path.getmtime(p) >= inst.started for p in glob.glob(os.path.join(inst.dir, 'crash-reports', '*-fml.txt')))


# ---------------------------------------------------------------- one matrix cell

def run_cell(specs, loader, config, cell_dir, timeout, mods, world=None, play=None, lp=False, only=(), exclude=(),
             offline=()):
    """Starts the instances of one configuration, waits for them, stops everything.
    lp: specs include LuckPerms; on NeoForge every instance gets LuckPerms as permission handler.
    only/exclude: test-topic keys, passed to every instance as the TestRegistry include/exclude properties;
    offline: the suite keys that already ran offline (run_offline), skipped in game.
    Returns (instances, tested, notes); every instance in `tested` is expected to have written results.json.
    world: template world copied fresh into every instance (client save / server world); None = new superflat.
    play=stop file: manual play, same instances and multi-server setup but no tests;
    runs until the client is closed, a server exits or the stop file appears."""
    client_spec, server_spec = specs.get((loader, 'runClient')), specs.get((loader, 'runServer'))
    if not client_spec or not server_spec:
        return [], [], ['launch spec missing (see gradle-capture.log)']
    if world:
        # A newer world would load with data loss (servers) or a downgrade prompt (client): refuse it.
        wv, gv = world_data_version(world), game_data_version(client_spec)
        if wv and gv and wv > gv:
            return [], [], [f'world {os.path.basename(world)} is from a newer Minecraft (data version {wv} > {gv})']
    common = {'modutilities.autotest.timeout': timeout}
    if mods:
        common['modutilities.autotest.mods'] = mods
    for prop, keys in ((INCLUDE_PROP, only), (EXCLUDE_PROP, exclude), (OFFLINE_PROP, offline)):
        if keys:
            common[prop] = ','.join(keys)
    if play:
        common = {'modutilities.autotest.play': 'true'}
    template = world
    d = lambda n: os.path.join(cell_dir, n)
    notes, servers, tested = [], [], []

    def only_play(props):
        # play: keep only how the client gets into the game; the test sequencing props do not apply
        return {k: v for k, v in props.items() if k in ('connect', 'world')} if play else props

    def copy_world(dst):
        shutil.copytree(template, dst, ignore=shutil.ignore_patterns('session.lock'))

    def lp_config(run_dir):
        # NeoForge routes PermissionAPI node checks to the configured handler only. neoforge-server.toml is a
        # per-world server config; defaultconfigs/ seeds it for worlds that have none yet.
        if lp and loader == 'neoforge':
            write(os.path.join(run_dir, 'defaultconfigs', 'neoforge-server.toml'),
                  'permissionHandler = "luckperms:permission_handler"\n')

    def server(name, port, ms, **props):
        prepare(d(name), ms, port)
        lp_config(d(name))
        props = only_play(props)
        if template:
            copy_world(os.path.join(d(name), 'world'))  # level-name defaults to world
        inst = Instance(name, server_spec, d(name), {**common, **{f'modutilities.autotest.{k}': v for k, v in props.items()}})
        inst.start()
        servers.append(inst)
        return inst

    def client(ms, **props):
        prepare(d('client'), ms)
        lp_config(d('client'))
        props = only_play(props)
        if template and props.get('world'):
            copy_world(os.path.join(d('client'), 'saves', 'play'))
            props['world'] = 'play'  # open this save instead of creating one
        inst = Instance('client', client_spec, d('client'), {**common, **{f'modutilities.autotest.{k}': v for k, v in props.items()}})
        inst.start()
        return inst

    def ready(inst):
        if not wait_until(lambda: inst.log_has('Done ('), 600, inst):
            notes.append(f'{inst.name} did not start')
            return False
        return True

    cl = None
    try:
        if config == 'sp':
            cl = client(ms_off(), world='new')
            tested = [cl]
        elif config == 'mp':
            master = server('master', MASTER_PORT, ms_master(), waitPlayer='true', stop='true')
            tested = [master]
            if ready(master):
                cl = client(ms_off(), connect=f'127.0.0.1:{MASTER_PORT}')
        elif config == 'slave':
            master = server('master', MASTER_PORT, ms_master(), waitPlayer='false', waitSlaves=1, stop='false')
            tested = [master]
            if ready(master):
                slave = server('slave', SLAVE_PORT, ms_slave('autotest_slave'), waitPlayer='true', stop='true',
                               after=master.results)
                tested.append(slave)
                if ready(slave):
                    cl = client(ms_off(), connect=f'127.0.0.1:{SLAVE_PORT}')
        elif config == 'spslave':
            master = server('master', MASTER_PORT, ms_master(), waitPlayer='false', waitSlaves=1, stop='false')
            tested = [master]
            if ready(master):
                cl = client(ms_slave('autotest_sp_slave'), world='new', after=master.results)
                tested.append(cl)
        # The client quits on its own once its world/server is done; its watchdog bounds this.
        # A server that died without a report, or a master that should stay up but exited, leaves the
        # others waiting for a link or a file that never comes: end the cell at once instead.
        def broken():
            return [s.name for s in servers if not s.alive() and
                    (not os.path.exists(s.results) or s.props.get('modutilities.autotest.stop') != 'true')]
        if play and cl:
            wait_until(lambda: not cl.alive() or client_crashed(cl) or any(not s.alive() for s in servers)
                       or os.path.exists(play), 10 ** 9)
            notes.append('client closed' if not cl.alive() else 'client crashed on startup' if client_crashed(cl)
                         else 'stop requested' if os.path.exists(play) else 'server exited')
        elif cl:
            wait_until(lambda: not cl.alive() or broken() or client_crashed(cl), timeout + 60)
            if broken():
                notes.append(f'{"/".join(broken())} exited early (crash?), cell aborted')
            elif cl.alive():
                notes.append('client crashed on startup, cell aborted' if client_crashed(cl) else 'client hung, killed')
        # Servers that stop themselves after their run get time to finish writing (not after a client startup crash:
        # they wait for a player that never comes).
        for s in servers:
            if s.props.get('modutilities.autotest.stop') == 'true' and not (cl and client_crashed(cl)):
                wait_until(lambda: not s.alive() or os.path.exists(s.results), 120)
    finally:
        for s in servers:
            s.stop(60)
        if cl:
            cl.kill()
    return [i for i in [*servers, cl] if i], tested, notes


def read_json(path):
    try:
        with open(path, encoding='utf-8') as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def write_json(path, data):
    """Atomic (tmp + replace): the GUI polls these files."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path + '.tmp', 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
    os.replace(path + '.tmp', path)


def now():
    return time.strftime('%Y-%m-%dT%H:%M:%S')


# ---------------------------------------------------------------- test-topic selection

def matches(key, patterns):
    """Mirror of TestRegistry.matches: a pattern is a '/'-segment-wise prefix of key, '*' = any one segment."""
    k = key.split('/')
    return any(len(s) <= len(k) and all(a in ('*', b) for a, b in zip(s, k))
               for s in (p.strip().split('/') for p in patterns if p.strip()))


def excluded(key, only, exclude):
    """Mirror of TestRegistry.isExcluded: not matched by a non-empty `only`, or matched by `exclude`."""
    return bool(only) and not matches(key, only) or matches(key, exclude)


def read_selection(path, field='exclude'):
    """A pattern list ('exclude' or 'offline') of a selection file; [] if it is missing or broken."""
    data = read_json(path)
    return [k.strip() for k in (data.get(field) or [] if isinstance(data, dict) else []) if k.strip()]


def test_selection(args, out):
    """(only, exclude, offline) of a test run from the selection file (--selection, default <out>/test-selection.json)
    and the flags. exclude: the file's list unless --all-tests or --only (neither silently loses a topic switched off
    in the GUI), plus --exclude. offline: the file's list plus --offline-tests; --no-offline-tests = everything in
    game. --all-tests keeps the offline modes (every topic on, each in its mode)."""
    path = args.selection or os.path.join(out, SELECTION)
    exclude = ([] if args.all_tests or args.only else read_selection(path)) + (args.exclude or [])
    offline = [] if args.no_offline_tests else read_selection(path, 'offline') + (args.offline_tests or [])
    return list(args.only or []), list(dict.fromkeys(exclude)), list(dict.fromkeys(offline))


def selection_text(only, exclude):
    """One line for the summary: '' for a full run."""
    return 'PARTIAL: ' + '; '.join(filter(None, [only and 'only ' + ', '.join(only),
                                                 exclude and 'excluded ' + ', '.join(exclude)])) if only or exclude else ''


_topics_lock = threading.Lock()  # sp cells finish in pool threads


def topic(t):
    """A results.json/topics.json topic entry as (Category/SuiteClass, needsGame); old entries are plain keys
    (needsGame unknown: True)."""
    return (t['key'], t.get('needsGame', True)) if isinstance(t, dict) else (t, True)


def merge_topics(out, results):
    """Updates <out>/topics.json from these results.json files: per mod, the suites it registered in its latest
    report (results.json 'topics', excluded suites included). The GUI's Topics dialog lists them."""
    with _topics_lock:
        path = os.path.join(out, TOPICS)
        topics = read_json(path) or {}
        for r in results:
            for mod, m in ((read_json(r) or {}).get('mods') or {}).items():
                if 'topics' in m:
                    topics[mod] = [{'key': k, 'needsGame': g} for k, g in sorted(map(topic, m['topics']))]
        write_json(path, topics)


def read_topics(out):
    """<out>/topics.json as {modid/Category/SuiteClass: needsGame}."""
    return {f'{m}/{k}': g for m, ts in (read_json(os.path.join(out, TOPICS)) or {}).items() for k, g in map(topic, ts)}


# ---------------------------------------------------------------- offline (JUnit) tests

def has_offline_tests(project):
    """True if the project's common/src/test calls OfflineTests.run, i.e. `gradlew :common:test` runs suites."""
    for f in glob.glob(os.path.join(project, 'common', 'src', 'test', '**', '*.java'), recursive=True):
        with open(f, encoding='utf-8', errors='replace') as fh:
            if 'OfflineTests.run(' in fh.read():
                return True
    return False


def junit_results(xml_dir):
    """Counts of Gradle's JUnit XML reports (TEST-*.xml) in xml_dir. OfflineTests names its tests
    <modid>/<Category>/<SuiteClass>/<test>: 'suites' = the suite keys that ran, 'excludedSuites' = those with an
    <excluded> entry (not counted), 'failures' = names of failed tests."""
    row = {'passed': 0, 'failed': 0, 'errors': 0, 'skipped': 0, 'suites': [], 'excludedSuites': [], 'failures': []}
    for f in sorted(glob.glob(os.path.join(xml_dir, 'TEST-*.xml'))):
        try:
            cases = list(ET.parse(f).getroot().iter('testcase'))
        except (OSError, ET.ParseError):
            row['errors'] += 1
            continue
        for tc in cases:
            name = tc.get('name', '')
            parts = name.split('/')
            key = '/'.join(parts[:3]) if len(parts) > 3 else None
            if parts[-1] == '<excluded>':
                row['excludedSuites'].append(key)
                continue
            kind = next((k for t, k in (('failure', 'failed'), ('error', 'errors'), ('skipped', 'skipped'))
                         if tc.find(t) is not None), 'passed')
            row[kind] += 1
            if kind in ('failed', 'errors'):
                row['failures'].append(name)
            if key and key not in row['suites']:
                row['suites'].append(key)
    return row


def run_offline(args, name, project, pdir, log):
    """The project's offline row: `gradlew :common:cleanTest :common:test` with the offline/include/exclude
    properties runs the offline suites that need no game as JUnit tests. Writes <pdir>/offline.json (GUI) and returns
    (summary row, suite keys that ran; the in-game cells skip exactly these). No OfflineTests caller in the project, or
    no result: nothing ran, so nothing is skipped in game."""
    status = os.path.join(pdir, 'offline.json')
    xml_dir = os.path.join(project, 'common', 'build', 'test-results', 'test')
    row = {'project': name, 'loader': 'junit', 'config': 'offline', 'dir': xml_dir, 'only': args.only,
           'excluded': args.exclude, 'passed': 0, 'failed': 0, 'errors': 0, 'skipped': 0, 'seconds': 0}
    if not has_offline_tests(project):
        row.update(ok=True, status='n/a: no offline tests (all suites run in game)')
        write_json(status, {**row, 'state': 'n/a', 'finished': now()})
        return row, []
    print(f'[{name}] offline tests (gradle :common:test) ...', flush=True)
    write_json(status, {**row, 'state': 'running', 'started': now()})
    t0 = time.time()
    for f in glob.glob(os.path.join(xml_dir, 'TEST-*.xml')):
        os.remove(f)  # never count an older run's reports
    props = [f'-D{p}={",".join(v)}' for p, v in
             ((OFFLINE_PROP, args.offline_topics), (INCLUDE_PROP, args.only), (EXCLUDE_PROP, args.exclude)) if v]
    try:
        rc = gradle(project, [':common:cleanTest', ':common:test', '-Penable_dev_features=true'] + props, args.offline,
                    log)
    except subprocess.TimeoutExpired:
        rc = -1
    r = junit_results(xml_dir)
    ran = r.pop('suites')
    if rc and not (r['failed'] or r['errors']):
        r['errors'] += 1  # Gradle failed without a failed test: compile error, timeout
    row.update(r, seconds=int(time.time() - t0), ok=not (r['failed'] or r['errors']),
               status=f"junit: {len(ran)} suites offline" + (f", gradle rc {rc} (see gradle-capture.log)" if rc else ''))
    state = 'passed' if row['ok'] else 'failed' if row['failed'] else 'error'
    write_json(status, {**row, 'state': state, 'finished': now()})
    print(f"[{name}] offline {'PASS' if row['ok'] else 'FAIL'} {row['passed']} passed, {row['failed']} failed, "
          f"{row['errors']} errors, {row['skipped']} skipped  [{row['status']}]", flush=True)
    return row, ran


def pid_alive(pid):
    if os.name == 'nt':
        import ctypes
        k = ctypes.windll.kernel32
        h = k.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
        if not h:
            return False
        code = ctypes.c_ulong()
        ok = k.GetExitCodeProcess(h, ctypes.byref(code))
        k.CloseHandle(h)
        return bool(ok) and code.value == 259  # STILL_ACTIVE
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def lock_pid(path):
    """pid in a lock file if that process is alive, else None."""
    try:
        with open(path) as f:
            pid = int(f.read().strip())
    except (OSError, ValueError):
        return None
    return pid if pid_alive(pid) else None


def take_lock(path, wait):
    """Writes our pid into the port lock. While a live run_matrix holds it: exit, or (wait) poll until it is free."""
    said = None
    while other := lock_pid(path):
        if not wait:
            sys.exit(f'another run_matrix (pid {other}) holds {path}: the game and multi-server ports are shared by '
                     f'every run, whatever its --out; stop it first or use --wait-lock')
        if other != said:
            print(f'waiting for run_matrix pid {other} to end ({path}) ...', flush=True)
            said = other
        time.sleep(5)
    # ponytail: check-then-write, not atomic; two runs starting in the same instant could both pass
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(str(os.getpid()))


def start_gui(out):
    if lock_pid(os.path.join(out, 'gui.lock')):
        return
    exe = os.path.join(os.path.dirname(sys.executable), 'pythonw.exe')
    exe = exe if os.path.exists(exe) else sys.executable
    flags = subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP if os.name == 'nt' else 0
    subprocess.Popen([exe, os.path.join(HERE, 'matrix_gui.py'), '--out', out], creationflags=flags,
                     stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def summarize(insts, tested, notes):
    """passed/failed/errors over every instance that should have run tests, plus a status string."""
    row = {'passed': 0, 'failed': 0, 'errors': 0, 'skipped': 0, 'status': []}
    for i in tested:
        r = read_json(i.results)
        if r is None:
            row['status'].append(f'{i.name}:no-results')
            row['errors'] += 1
            continue
        for k in ('passed', 'failed', 'errors', 'skipped'):
            row[k] += r.get(k, 0)
        row['status'].append(f"{i.name}:{r.get('status')}/{r.get('role')}")
    for i in insts:
        if i.name == 'client':
            c = read_json(i.results + '.client.json')
            row['status'].append('client:' + (c['status'] if c else 'no-status'))
    row['status'] = ' '.join(row['status'] + notes)
    row['ok'] = bool(tested) and row['failed'] == 0 and row['errors'] == 0 and all(
        (read_json(i.results) or {}).get('status') == 'completed' for i in tested)
    return row


# ---------------------------------------------------------------- main

def find_projects(root, version):
    """mc_<version>_* folders under root (case-insensitive; the trailing _ keeps 1.21.1 from matching 1.21.10)."""
    return [p for p in glob.glob(os.path.join(root, '*')) if os.path.basename(p).lower().startswith(f'mc_{version}_')]


def resolve_projects(args):
    projects = [os.path.abspath(p) for p in args.projects]
    for v in args.versions or []:
        hits = find_projects(args.root, v)
        if not hits:
            sys.exit(f'no project folder for version {v} under {args.root}')
        projects += hits
    if not projects:
        sys.exit('give project folders, or --root with --versions')
    return projects


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('projects', nargs='*', help='MC version project folders (e.g. .../mc_1.21.1_MC_ModUtilities)')
    ap.add_argument('--root', help='folder holding mc_<version>_* project folders (with --versions)')
    ap.add_argument('--versions', nargs='*', help='versions to pick from --root')
    ap.add_argument('--loaders', nargs='*', default=list(LOADERS), choices=LOADERS)
    ap.add_argument('--configs', nargs='*', default=list(CONFIGS), choices=CONFIGS)
    ap.add_argument('--timeout', type=int, default=900, help='per-instance watchdog, seconds')
    ap.add_argument('--luckperms', choices=LP_MODES, default='off',
                    help='off: plain cells; on: <config>-lp cells (LuckPerms in the dev runtime); both: plain + lp; '
                         'removed: only the two-phase lp-removed cell per loader; all: plain + lp + lp-removed')
    ap.add_argument('--mods', help='comma separated mod ids to test (default: every mod with registered suites)')
    ap.add_argument('--out', default=os.path.join(MODS_ROOT, 'autotest-out'), help='output folder')
    ap.add_argument('--offline', action='store_true', help='pass --offline to Gradle')
    ap.add_argument('--gui', action='store_true', help='open matrix_gui.py on the same --out')
    ap.add_argument('--play', nargs=2, metavar=('LOADER', 'CONFIG'),
                    help='manual play of one project: start the instances of CONFIG without tests until stopped')
    ap.add_argument('--world', help='world folder copied fresh into every instance, for tests and --play '
                                    '(default: new superflat world); the folder itself is never modified')
    ap.add_argument('--clean-build', action='store_true',
                    help='clean build (no tests) of --build-kind; per MC version in the given order')
    ap.add_argument('--build-kind', choices=('release', 'dev'), default='release',
                    help='--clean-build: release (dev features off) or dev (dev features on, tests included)')
    ap.add_argument('--jobs', type=int, default=4, help='--clean-build: MC versions built at once')
    ap.add_argument('--test-jobs', type=int, default=2,
                    help='sp cells run at once, beside the one-at-a-time multi-server cells (1 = all sequential)')
    ap.add_argument('--exclude', nargs='+', metavar='KEY',
                    help='test topics not to run (modid[/Category[/SuiteClass]], * = any segment), added to the '
                         'selection file')
    ap.add_argument('--only', nargs='+', metavar='KEY', help='run only these test topics; ignores the selection file')
    ap.add_argument('--all-tests', action='store_true', help='ignore the selection file (full run unless --exclude)')
    ap.add_argument('--offline-tests', nargs='+', metavar='KEY',
                    help='also run these test topics offline (JUnit) instead of in game, if they need no game')
    ap.add_argument('--no-offline-tests', action='store_true',
                    help="run every topic in game (ignores the selection file's offline list)")
    ap.add_argument('--selection', metavar='FILE', help=f'selection file (default <out>/{SELECTION})')
    ap.add_argument('--wait-lock', action='store_true',
                    help='if another run_matrix holds the port lock, wait until it ends instead of exiting')
    args = ap.parse_args()

    out = os.path.abspath(args.out)
    os.makedirs(out, exist_ok=True)
    args.only, args.exclude, args.offline_topics = test_selection(args, out)
    projects = resolve_projects(args)
    if args.play:
        if args.play[0] not in LOADERS or args.play[1] not in CONFIGS or len(projects) != 1:
            ap.error(f'--play needs one project, a loader {LOADERS} and a config {CONFIGS}')
    if args.world:
        args.world = os.path.normpath(os.path.abspath(args.world))
        if not os.path.isfile(os.path.join(args.world, 'level.dat')):
            ap.error(f'--world {args.world} has no level.dat')
    take_lock(LOCK, args.wait_lock)
    if args.gui:
        start_gui(out)
    try:
        if args.clean_build:
            clean_build(args, out, projects)
        elif args.play:
            play(args, out, projects[0])
        else:
            run(args, out, projects)
    finally:
        if lock_pid(LOCK) == os.getpid():
            os.remove(LOCK)


def play_dir(out, project, loader, config):
    return os.path.join(out, 'play', os.path.basename(project), loader, config)


def play(args, out, project):
    """Manual play: <out>/play/<project>/<loader>/<config>/, status in <out>/play/play.json, stop via <out>/play/stop."""
    loader, config = args.play
    name = os.path.basename(project)
    pdir = os.path.join(out, 'play', name)
    os.makedirs(pdir, exist_ok=True)
    stop_file = os.path.join(out, 'play', 'stop')
    if os.path.exists(stop_file):
        os.remove(stop_file)
    status = os.path.join(out, 'play', 'play.json')
    info = {'project': name, 'loader': loader, 'config': config, 'world': args.world, 'started': now()}
    write_json(status, {**info, 'state': 'capturing'})
    glog = os.path.join(pdir, 'gradle-capture.log')
    open(glog, 'w').close()
    notes = []
    try:
        # The capture compiles the project itself; rebuild its dependency projects, then clear Loom's remapped
        # dependencies only when a dependency jar changed since last time.
        failed = build_deps(project, [loader], args.offline, glog, {}, out)
        if failed:
            notes = [f'dependency build failed: {os.path.basename(failed)} (see gradle-capture.log)']
            return
        deps_file = os.path.join(out, name, 'deps.json')
        old, deps = read_json(deps_file) or {}, dep_hashes(project, [loader])
        if any(old.get(j) != h for j, h in deps.items()):
            if not clear_remap_cache(project, glog):
                notes = ['cannot clear loom cache (see gradle-capture.log)']
                return
            write_json(deps_file, {**old, **deps})
        print(f'[{name}] capturing launch commands (gradle) ...', flush=True)
        rc, specs = capture(project, [loader], build_agent(out), pdir, args.offline, glog)
        write_json(status, {**info, 'state': 'playing'})
        print(f'[{name}] playing {loader} {config}; close the game or create {stop_file} to stop', flush=True)
        notes = run_cell(specs, loader, config, play_dir(out, project, loader, config), args.timeout, None,
                         world=args.world, play=stop_file)[2]
    finally:
        write_json(status, {**info, 'state': 'stopped', 'notes': notes, 'finished': now()})
        if os.path.exists(stop_file):
            os.remove(stop_file)
        print(f'[{name}] play ended: {" ".join(notes)}', flush=True)


def clean_build(args, out, projects):
    """`gradlew clean build` of every project, args.build_kind release (dev features off) or dev. Projects are
    grouped into one chain per MC version (mc_<version>_ folder prefix), built in the given order (dependencies first: dependents
    read their jars via flatDir); up to args.jobs chains run at once. A failure skips the rest of its chain only.
    Log <out>/<project>/clean-build.log, status <out>/clean-build.json; exit code 1 if any failed."""
    status, lock = os.path.join(out, 'clean-build.json'), threading.Lock()
    chains = {}
    for p in projects:
        m = re.match(r'mc_([^_]+)_', os.path.basename(p), re.I)
        chains.setdefault(m.group(1).lower() if m else p, []).append(p)
    info = {'state': 'building', 'running': [], 'done': 0, 'total': len(projects), 'failed': [], 'started': now()}

    def fail(name, log, reason):  # build.json entry, so the GUI build cell shows why
        path = os.path.join(out, name, 'build.json')
        entry = {'kind': args.build_kind, 'rc': None, 'started': now(), 'finished': now(), 'failed': True,
                 'error': [reason], 'log': log}
        write_json(path, {**(read_json(path) or {}), **{l: entry for l in LOADERS}})
        with open(log, 'a', encoding='utf-8') as f:
            f.write(f'>>> {reason}\n')

    def chain(ps):
        for i, project in enumerate(ps):
            name = os.path.basename(project)
            log = os.path.join(out, name, 'clean-build.log')
            os.makedirs(os.path.dirname(log), exist_ok=True)
            open(log, 'w').close()
            with lock:
                info['running'].append(name)
                write_json(status, info)
            print(f'[{name}] clean build ...', flush=True)
            # remap cache first: Loom would compile against stale remapped dependency jars; no `gradlew --stop`
            # fallback, it would kill the parallel builds of this Gradle version
            ok = clear_remap_cache(project, log, stop_daemons=False)
            if not ok:
                fail(name, log, 'loom cache locked')
            ok = ok and build(project, list(LOADERS), args.offline, log, out, args.build_kind, clean=True) == 0
            skipped = [] if ok else [os.path.basename(p) for p in ps[i + 1:]]
            for s in skipped:
                os.makedirs(os.path.join(out, s), exist_ok=True)
                fail(s, os.path.join(out, s, 'clean-build.log'), f'dependency {name} failed')
                print(f'[{s}] skipped: dependency {name} failed', flush=True)
            print(f"[{name}] {'ok' if ok else 'FAILED, see ' + log}", flush=True)
            with lock:
                info['running'].remove(name)
                info['done'] += 1 + len(skipped)
                info['failed'] += ([] if ok else [name]) + skipped
                write_json(status, info)
            if not ok:
                return

    with concurrent.futures.ThreadPoolExecutor(max(1, args.jobs)) as ex:
        list(ex.map(chain, chains.values()))  # list(): re-raises a chain's exception
    write_json(status, {**info, 'state': 'done', 'finished': now()})
    print(f"clean build done, failed: {', '.join(info['failed']) or 'none'}", flush=True)
    sys.exit(1 if info['failed'] else 0)


def test_cell(args, name, pdir, specs, jars, world_fp, loader, config, offline=()):
    """Runs one cell and writes its cell.json; returns its summary row. Thread-safe: own cell dir and instances.
    config: a CONFIGS entry, <config>-lp (that config with LuckPerms; specs = the LuckPerms launch specs) or
    lp-removed (specs = (plain specs, LuckPerms specs), see run_removed). offline: see run_cell."""
    cell = os.path.join(pdir, loader, config)
    print(f'[{name}] {loader} {config} ...', flush=True)
    t0 = time.time()
    # cell.json sits in the cell dir; prepare() only replaces the instance subdirs.
    status = os.path.join(cell, 'cell.json')
    write_json(status, {'state': 'running', 'project': name, 'loader': loader, 'config': config,
                        'world': args.world, 'started': now()})
    if config == REMOVED:
        row = run_removed(args, specs, loader, cell, offline)
    else:
        lp = config.endswith(LP_SUFFIX)
        insts, tested, notes = run_cell(specs, loader, config.removesuffix(LP_SUFFIX), cell, args.timeout, args.mods,
                                        args.world, lp=lp, only=args.only, exclude=args.exclude, offline=offline)
        row = summarize(insts, tested, notes)
    merge_topics(os.path.dirname(pdir), glob.glob(os.path.join(cell, '*', 'results.json'))
                 + glob.glob(os.path.join(cell, 'phase*', '*', 'results.json')))
    row.update(project=name, loader=loader, config=config, seconds=int(time.time() - t0), dir=cell,
               only=args.only, excluded=args.exclude)
    state = 'passed' if row['ok'] else 'failed' if row['failed'] else 'error'
    write_json(status, {**row, 'state': state, 'finished': now(), 'jars': jars, 'fingerprint': fingerprint(jars),
                        'world': args.world, 'world_fp': world_fp})
    print(f"[{name}] {loader} {config} {'PASS' if row['ok'] else 'FAIL'} {row['passed']} passed, {row['failed']} "
          f"failed, {row['errors']} errors, {row['skipped']} skipped  [{row['status']}]", flush=True)
    return row


def run_removed(args, specs, loader, cell, offline=()):
    """"LuckPerms removed from a save": phase 1 runs mp WITH LuckPerms (tests record expectations in the world via
    LuckPermsPhase), phase 2 runs mp WITHOUT LuckPerms on a copy of phase 1's master world (LuckPermsRemovedTests and
    dependents' phase-2 checks). Instances in <cell>/phase1/, <cell>/phase2/. Returns the combined summary row."""
    plain, with_lp = specs
    rows, notes = [], []
    sel = {'only': args.only, 'exclude': args.exclude, 'offline': offline}
    insts, tested, n = run_cell(with_lp, loader, 'mp', os.path.join(cell, 'phase1'), args.timeout, args.mods,
                                args.world, lp=True, **sel)
    rows.append(summarize(insts, tested, n))
    world = os.path.join(cell, 'phase1', 'master', 'world')
    if not os.path.isfile(os.path.join(world, PHASE_FILE)):
        notes.append(f'phase1 wrote no {PHASE_FILE} (LuckPerms missing from the dev run? see README)')
    else:
        insts, tested, n = run_cell(plain, loader, 'mp', os.path.join(cell, 'phase2'), args.timeout, args.mods, world,
                                    **sel)
        rows.append(summarize(insts, tested, n))
        ran = [t for i in tested for m in ((read_json(i.results) or {}).get('mods') or {}).values()
               for suite, ts in (m.get('suites') or {}).items() if 'LuckPermsRemoved' in suite
               for t in ts if t.get('status') != 'SKIP']
        if not ran:
            notes.append('phase2 ran no LuckPermsRemoved test (all skipped or missing)')
    row = {k: sum(r[k] for r in rows) for k in ('passed', 'failed', 'errors', 'skipped')}
    row['errors'] += len(notes)
    row['status'] = ' | '.join(f'phase{i + 1}: {r["status"]}' for i, r in enumerate(rows)) + ''.join(' ' + x for x in notes)
    row['ok'] = len(rows) == 2 and not notes and all(r['ok'] for r in rows)
    return row


def cell_labels(configs, mode):
    """Cell labels of one loader for --luckperms mode: plain configs, <config>-lp, lp-removed."""
    plain = list(configs) if mode in ('off', 'both', 'all') else []
    lp = [c + LP_SUFFIX for c in configs] if mode in ('on', 'both', 'all') else []
    return plain + lp + ([REMOVED] if mode in ('removed', 'all') else [])


def label_order(label):
    return (CONFIGS + tuple(c + LP_SUFFIX for c in CONFIGS) + (REMOVED,)).index(label)


def run(args, out, projects):
    """Per project (in order): dependency builds, build, offline (JUnit) tests if any topic is set offline, launch
    capture, then its cells. Multi-server cells run one
    at a time (shared ports); sp cells (no ports, own game dir) go to a pool of args.test_jobs and run beside the
    following cells and projects, so sp cells of several projects overlap within one call."""
    agent = build_agent(out)
    labels = cell_labels(args.configs, args.luckperms)
    need_plain = any(not l.endswith(LP_SUFFIX) for l in labels)  # lp-removed phase 2 runs without LuckPerms
    need_lp = any(l.endswith(LP_SUFFIX) or l == REMOVED for l in labels)
    rows, pending = [], []
    pool = concurrent.futures.ThreadPoolExecutor(max(1, args.test_jobs))
    built = {}
    for project in projects:
        name = os.path.basename(project)
        pdir = os.path.join(out, name)
        os.makedirs(pdir, exist_ok=True)
        glog = os.path.join(pdir, 'gradle-capture.log')
        print(f'[{name}] building jars and capturing launch commands (gradle) ...', flush=True)
        cap = {'state': 'capturing', 'loaders': args.loaders, 'configs': args.configs, 'started': now()}
        write_json(os.path.join(pdir, 'capture.json'), cap)
        open(glog, 'w').close()
        if os.path.exists(os.path.join(pdir, 'offline.json')):
            os.remove(os.path.join(pdir, 'offline.json'))  # no offline row from an older run
        failed = build_deps(project, args.loaders, args.offline, glog, built, out)
        deps = dep_hashes(project, args.loaders)
        if failed:
            rc, reason = -1, f'dependency build failed: {os.path.basename(failed)} (see gradle-capture.log)'
        elif deps and not clear_remap_cache(project, glog):  # always for mods with flatDir dependencies
            rc, reason = -1, 'cannot clear loom cache (see gradle-capture.log)'
        else:
            write_json(os.path.join(pdir, 'deps.json'), {**(read_json(os.path.join(pdir, 'deps.json')) or {}), **deps})
            key = os.path.normcase(project)
            rc = built[key] = built[key] if key in built else build(project, args.loaders, args.offline, glog, out)
            reason = f'build failed (rc {rc}, see gradle-capture.log)'
        if rc != 0:
            write_json(os.path.join(pdir, 'capture.json'), {**cap, 'state': 'done', 'rc': rc, 'finished': now()})
            print(f'[{name}] {reason}', flush=True)
            for loader in args.loaders:
                for config in labels:
                    cell = os.path.join(pdir, loader, config)
                    row = {'passed': 0, 'failed': 0, 'errors': 1, 'skipped': 0, 'ok': False, 'project': name, 'loader': loader,
                           'config': config, 'seconds': 0, 'dir': cell, 'status': reason, 'only': args.only,
                           'excluded': args.exclude}
                    rows.append(row)
                    write_json(os.path.join(cell, 'cell.json'), {**row, 'state': 'error', 'finished': now()})
            continue
        jars = {l: jar_hashes(project, l) for l in args.loaders}
        ran_offline = []
        if args.offline_topics:
            row, ran_offline = run_offline(args, name, project, pdir, glog)
            rows.append(row)
        rc, specs = capture(project, args.loaders, agent, pdir, args.offline, glog) if need_plain else (0, {})
        lp_specs = {}
        if need_lp:
            lp_rc, lp_specs = capture(project, args.loaders, agent, pdir, args.offline, glog, lp=True)
            rc = rc or lp_rc
        world_fp = world_fingerprint(args.world)  # the template as tested; the GUI compares it to the current one
        write_json(os.path.join(pdir, 'capture.json'), {**cap, 'state': 'done', 'rc': rc, 'finished': now()})
        if rc != 0:
            print(f'[{name}] gradle exited with {rc}, see {glog}', flush=True)
        for loader in args.loaders:
            for config in labels:
                cell_specs = (specs, lp_specs) if config == REMOVED else lp_specs if config.endswith(LP_SUFFIX) else specs
                job = (name, pdir, cell_specs, jars[loader], world_fp, loader, config, ran_offline)
                if config.removesuffix(LP_SUFFIX) == 'sp' and args.test_jobs > 1:  # no ports: runs beside everything else
                    pending.append(pool.submit(test_cell, args, *job))
                else:
                    rows.append(test_cell(args, *job))
    pool.shutdown(wait=True)
    rows += [f.result() for f in pending]  # re-raises a crashed cell
    names = [os.path.basename(p) for p in projects]
    rows.sort(key=lambda r: (names.index(r['project']), (LOADERS.index(r['loader']), label_order(r['config']))
                             if r['loader'] in LOADERS else (len(LOADERS), 0)))  # the offline row last

    with open(os.path.join(out, 'summary.json'), 'w', encoding='utf-8') as f:
        json.dump(rows, f, indent=2)
    print()
    hdr = f"{'project':34} {'loader':9} {'config':10} {'result':6} {'pass':>5} {'fail':>5} {'err':>4} {'skip':>5} {'sec':>5}  status"
    print(hdr)
    print('-' * len(hdr))
    for r in rows:
        print(f"{r['project'][:34]:34} {r['loader']:9} {r['config']:10} {'OK' if r['ok'] else 'FAIL':6} "
              f"{r['passed']:>5} {r['failed']:>5} {r['errors']:>4} {r.get('skipped', 0):>5} {r['seconds']:>5}  {r['status']}")
    print(f"\ntest topics: {selection_text(args.only, args.exclude) or 'all (full run)'}"
          + (f"; offline (JUnit, row 'junit offline'): {', '.join(args.offline_topics)}" if args.offline_topics else ''))
    print(f'logs and reports: {out}  (per cell: <project>/<loader>/<config>/<instance>/console.log, results.json,'
          f' logs/UnitTests/<mod>.log)')
    sys.exit(0 if rows and all(r['ok'] for r in rows) else 1)


if __name__ == '__main__':
    main()
