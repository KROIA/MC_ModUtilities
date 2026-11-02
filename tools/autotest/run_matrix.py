#!/usr/bin/env python3
"""Runs the in-game unit tests of a mod project in every server configuration and on every loader.

Stdlib only. See README.md in this folder. Short version:

    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities
    python tools/autotest/run_matrix.py --root E:/.../StockMarket --versions 1.21.1 26.3 --configs sp mp
    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities --world <world folder>
    python tools/autotest/run_matrix.py E:/.../mc_1.21.1_MC_ModUtilities --play fabric mp [--world <world folder>]

Per project it runs Gradle once per run task (runClient/runServer with a capture agent, added by
capture.init.gradle) to record Loom's exact launch commands, then starts every game instance directly,
so master, slave and client of one project run side by side without fighting over Gradle locks.
The project must be a dev build (enable_dev_features=true) for the tests and the autotest hook to exist.
"""
import argparse
import glob
import gzip
import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import time
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
MODS_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))  # tools/autotest -> repo -> mod -> mods
CONFIGS = ('sp', 'mp', 'slave', 'spslave')
LOADERS = ('fabric', 'neoforge')
MASTER_PORT, SLAVE_PORT, LINK_PORT = 25611, 25612, 25690
SECRET = 'autotest-secret'


# ---------------------------------------------------------------- launch capture

def build_agent(out_dir):
    """Compiles CaptureAgent.java into capture.jar (javac + jar from the JDK on PATH)."""
    cls = os.path.join(out_dir, 'agent-classes')
    jar = os.path.join(out_dir, 'capture.jar')
    os.makedirs(cls, exist_ok=True)
    subprocess.run(['javac', '--release', '21', '-d', cls, os.path.join(HERE, 'CaptureAgent.java')], check=True)
    manifest = os.path.join(out_dir, 'agent-manifest.txt')
    with open(manifest, 'w') as f:
        f.write('Premain-Class: CaptureAgent\n')
    subprocess.run(['jar', 'cfm', jar, manifest, '-C', cls, '.'], check=True)
    return jar


def gradlew(project):
    return os.path.join(project, 'gradlew.bat' if os.name == 'nt' else 'gradlew')


def build(project, loaders, offline, log):
    """Builds the loaders' mod jars (dev features on), so the jars fingerprinted in cell.json are the tested code."""
    cmd = [gradlew(project)] + [f':{l}:build' for l in loaders] + ['-x', 'test', '-Penable_dev_features=true'] + \
          (['--offline'] if offline else [])
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        f.write(f'>>> {" ".join(cmd)}\n')
        f.flush()
        return subprocess.run(cmd, cwd=project, stdout=f, stderr=subprocess.STDOUT, timeout=3600).returncode


def build_deps(project, loaders, offline, log, built):
    """Builds the flatDir dependency projects (found via mod_jars: <dep>/<loader>/build/libs/x.jar), their own
    dependencies first, so the project never runs against stale dependency jars. `built` maps project -> rc and
    keeps every project to one build per run_matrix invocation. Returns the first failed project or None."""
    for dep in dict.fromkeys(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(j))))
                             for l in loaders for j in mod_jars(project, l)[1:]):
        failed = build_deps(dep, loaders, offline, log, built)
        if failed:
            return failed
        key = os.path.normcase(dep)
        if key not in built:
            print(f'[{os.path.basename(project)}] building dependency {os.path.basename(dep)} ...', flush=True)
            built[key] = build(dep, loaders, offline, log)
        if built[key] != 0:
            return dep
    return None


def clear_remap_cache(project, log):
    """Deletes Loom's remapped copies of the flatDir mod jars (.gradle/loom-cache/remapped_mods): Loom would keep
    using an old remapped dependency after its jar changed, so the run would not test what the fingerprint says.
    On a Windows file lock, stops the project's Gradle daemons and retries once. Returns False if it stays."""
    cache = os.path.join(project, '.gradle', 'loom-cache', 'remapped_mods')
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        for attempt in (1, 2):
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
                        subprocess.run([gradlew(project), '--stop'], cwd=project, stdout=f, stderr=subprocess.STDOUT)
                    except OSError as e:
                        f.write(f'>>> {e}\n')
    return False


def dep_hashes(project, loaders):
    """sha256 of the dependency mod jars (flatDir) of these loaders; empty for mods without such dependencies."""
    return {j: file_sha256(j) for l in loaders for j in mod_jars(project, l)[1:]}


def capture(project, loaders, agent, out_dir, offline, log):
    """Gradle starts every run task with the agent, which writes <loader>-<task>.json and exits.

    One Gradle invocation per task: on Gradle 9 runClient and runServer in one build fail validation
    (runServer reads the run dir that runClient's downloadAssets writes, without a declared dependency).
    """
    cap = os.path.join(out_dir, 'launch')
    shutil.rmtree(cap, ignore_errors=True)
    os.makedirs(cap)
    rc = 0
    with open(log, 'a', encoding='utf-8', errors='replace') as f:
        for task in [f':{l}:{t}' for l in loaders for t in ('runClient', 'runServer')]:
            cmd = [gradlew(project), task,
                   '-I', os.path.join(HERE, 'capture.init.gradle'), f'-Pautotest_agent={agent}={cap}',
                   '-Penable_dev_features=true'] + (['--offline'] if offline else [])  # tests only exist in dev builds
            f.write(f'>>> {" ".join(cmd)}\n')
            f.flush()
            rc = subprocess.run(cmd, cwd=project, stdout=f, stderr=subprocess.STDOUT, timeout=3600).returncode or rc
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
        flags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == 'nt' else 0
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
                subprocess.run(['taskkill', '/T', '/F', '/PID', str(self.proc.pid)], capture_output=True)
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


# ---------------------------------------------------------------- one matrix cell

def run_cell(specs, loader, config, cell_dir, timeout, mods, world=None, play=None):
    """Starts the instances of one configuration, waits for them, stops everything.
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

    def server(name, port, ms, **props):
        prepare(d(name), ms, port)
        props = only_play(props)
        if template:
            copy_world(os.path.join(d(name), 'world'))  # level-name defaults to world
        inst = Instance(name, server_spec, d(name), {**common, **{f'modutilities.autotest.{k}': v for k, v in props.items()}})
        inst.start()
        servers.append(inst)
        return inst

    def client(ms, **props):
        prepare(d('client'), ms)
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
            wait_until(lambda: not cl.alive() or any(not s.alive() for s in servers) or os.path.exists(play),
                       10 ** 9)
            notes.append('client closed' if not cl.alive() else 'stop requested' if os.path.exists(play)
                         else 'server exited')
        elif cl:
            wait_until(lambda: not cl.alive() or broken(), timeout + 60)
            if broken():
                notes.append(f'{"/".join(broken())} exited early (crash?), cell aborted')
            elif cl.alive():
                notes.append('client hung, killed')
        # Servers that stop themselves after their run get time to finish writing.
        for s in servers:
            if s.props.get('modutilities.autotest.stop') == 'true':
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
    row = {'passed': 0, 'failed': 0, 'errors': 0, 'status': []}
    for i in tested:
        r = read_json(i.results)
        if r is None:
            row['status'].append(f'{i.name}:no-results')
            row['errors'] += 1
            continue
        for k in ('passed', 'failed', 'errors'):
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
    ap.add_argument('--mods', help='comma separated mod ids to test (default: every mod with registered suites)')
    ap.add_argument('--out', default=os.path.join(MODS_ROOT, 'autotest-out'), help='output folder')
    ap.add_argument('--offline', action='store_true', help='pass --offline to Gradle')
    ap.add_argument('--gui', action='store_true', help='open matrix_gui.py on the same --out')
    ap.add_argument('--play', nargs=2, metavar=('LOADER', 'CONFIG'),
                    help='manual play of one project: start the instances of CONFIG without tests until stopped')
    ap.add_argument('--world', help='world folder copied fresh into every instance, for tests and --play '
                                    '(default: new superflat world); the folder itself is never modified')
    args = ap.parse_args()

    out = os.path.abspath(args.out)
    os.makedirs(out, exist_ok=True)
    projects = resolve_projects(args)
    if args.play:
        if args.play[0] not in LOADERS or args.play[1] not in CONFIGS or len(projects) != 1:
            ap.error(f'--play needs one project, a loader {LOADERS} and a config {CONFIGS}')
    if args.world:
        args.world = os.path.normpath(os.path.abspath(args.world))
        if not os.path.isfile(os.path.join(args.world, 'level.dat')):
            ap.error(f'--world {args.world} has no level.dat')
    lock = os.path.join(out, 'running.lock')
    other = lock_pid(lock)
    if other:
        sys.exit(f'another run_matrix (pid {other}) is using {out} and the test ports; stop it first')
    with open(lock, 'w') as f:
        f.write(str(os.getpid()))
    if args.gui:
        start_gui(out)
    try:
        if args.play:
            play(args, out, projects[0])
        else:
            run(args, out, projects)
    finally:
        if lock_pid(lock) == os.getpid():
            os.remove(lock)


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
        failed = build_deps(project, [loader], args.offline, glog, {})
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


def run(args, out, projects):
    agent = build_agent(out)
    rows = []
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
        failed = build_deps(project, args.loaders, args.offline, glog, built)
        deps = dep_hashes(project, args.loaders)
        if failed:
            rc, reason = -1, f'dependency build failed: {os.path.basename(failed)} (see gradle-capture.log)'
        elif deps and not clear_remap_cache(project, glog):  # always for mods with flatDir dependencies
            rc, reason = -1, 'cannot clear loom cache (see gradle-capture.log)'
        else:
            write_json(os.path.join(pdir, 'deps.json'), {**(read_json(os.path.join(pdir, 'deps.json')) or {}), **deps})
            key = os.path.normcase(project)
            rc = built[key] = built[key] if key in built else build(project, args.loaders, args.offline, glog)
            reason = f'build failed (rc {rc}, see gradle-capture.log)'
        if rc != 0:
            write_json(os.path.join(pdir, 'capture.json'), {**cap, 'state': 'done', 'rc': rc, 'finished': now()})
            print(f'[{name}] {reason}', flush=True)
            for loader in args.loaders:
                for config in args.configs:
                    cell = os.path.join(pdir, loader, config)
                    row = {'passed': 0, 'failed': 0, 'errors': 1, 'ok': False, 'project': name, 'loader': loader,
                           'config': config, 'seconds': 0, 'dir': cell, 'status': reason}
                    rows.append(row)
                    write_json(os.path.join(cell, 'cell.json'), {**row, 'state': 'error', 'finished': now()})
            continue
        jars = {l: jar_hashes(project, l) for l in args.loaders}
        rc, specs = capture(project, args.loaders, agent, pdir, args.offline, glog)
        world_fp = world_fingerprint(args.world)  # the template as tested; the GUI compares it to the current one
        write_json(os.path.join(pdir, 'capture.json'), {**cap, 'state': 'done', 'rc': rc, 'finished': now()})
        if rc != 0:
            print(f'[{name}] gradle exited with {rc}, see {glog}', flush=True)
        for loader in args.loaders:
            for config in args.configs:
                cell = os.path.join(pdir, loader, config)
                print(f'[{name}] {loader} {config} ...', flush=True)
                t0 = time.time()
                # cell.json sits in the cell dir; prepare() only replaces the instance subdirs.
                status = os.path.join(cell, 'cell.json')
                write_json(status, {'state': 'running', 'project': name, 'loader': loader, 'config': config,
                                    'world': args.world, 'started': now()})
                insts, tested, notes = run_cell(specs, loader, config, cell, args.timeout, args.mods, args.world)
                row = summarize(insts, tested, notes)
                row.update(project=name, loader=loader, config=config, seconds=int(time.time() - t0), dir=cell)
                rows.append(row)
                state = 'passed' if row['ok'] else 'failed' if row['failed'] else 'error'
                write_json(status, {**row, 'state': state, 'finished': now(), 'jars': jars[loader],
                                    'fingerprint': fingerprint(jars[loader]), 'world': args.world,
                                    'world_fp': world_fp})
                print(f"    {'PASS' if row['ok'] else 'FAIL'} {row['passed']} passed, {row['failed']} failed, "
                      f"{row['errors']} errors  [{row['status']}]", flush=True)

    with open(os.path.join(out, 'summary.json'), 'w', encoding='utf-8') as f:
        json.dump(rows, f, indent=2)
    print()
    hdr = f"{'project':34} {'loader':9} {'config':8} {'result':6} {'pass':>5} {'fail':>5} {'err':>4} {'sec':>5}  status"
    print(hdr)
    print('-' * len(hdr))
    for r in rows:
        print(f"{r['project'][:34]:34} {r['loader']:9} {r['config']:8} {'OK' if r['ok'] else 'FAIL':6} "
              f"{r['passed']:>5} {r['failed']:>5} {r['errors']:>4} {r['seconds']:>5}  {r['status']}")
    print(f'\nlogs and reports: {out}  (per cell: <project>/<loader>/<config>/<instance>/console.log, results.json,'
          f' logs/UnitTests/<mod>.log)')
    sys.exit(0 if rows and all(r['ok'] for r in rows) else 1)


if __name__ == '__main__':
    main()
