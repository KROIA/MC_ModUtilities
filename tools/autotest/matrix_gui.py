#!/usr/bin/env python3
"""Live overview of the in-game test matrix (mod x MC version x loader x config). Stdlib only.

    python tools/autotest/matrix_gui.py [--out DIR]

Watches the run_matrix.py output folder (cell.json, capture.json, running.lock, play/play.json) and starts
test runs and manual play sessions. See README.md (GUI section).
"""
import argparse
import glob
import os
import subprocess
import sys
import tkinter as tk
from tkinter import filedialog, messagebox

sys.dont_write_bytecode = True  # no __pycache__ next to the scripts
import run_matrix as rm  # noqa: E402

# Add a new MC version here (see ADDING_A_VERSION.md).
VERSIONS = ['1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10',
            '1.21.11', '26.1', '26.2', '26.3']
MODS = [('ModUtilities', 'MC_ModUtilities'), ('BankSystem', 'BankSystem'), ('StockMarket', 'StockMarket')]
BANK = 'BankSystem'  # no own test runs: its suites run in every StockMarket cell
COLUMNS = [(l, c) for l in rm.LOADERS for c in rm.CONFIGS]
COLORS = {'none': '#c8c8c8', 'capturing': '#a8d8ff', 'running': '#ffc040', 'passed': '#60c060',
          'failed': '#e04040', 'error': '#e08030', 'interrupted': '#b090d0', 'n/a': '#505050', 'playing': '#40c8c8'}


def find_project(mods_root, folder, version):
    """Project folder of a mod for a version, or None. Shortest name wins over copies like '... - Kopie'."""
    hits = sorted(rm.find_projects(os.path.join(mods_root, folder), version), key=len)
    return hits[0] if hits else None


_json_cache = {}


def read_json_cached(path):
    """read_json, re-parsed only when mtime/size change (results.json files are large and polled every 2 s)."""
    try:
        st = os.stat(path)
    except OSError:
        return None
    hit = _json_cache.get(path)
    if not hit or hit[0] != (st.st_mtime_ns, st.st_size):
        hit = _json_cache[path] = ((st.st_mtime_ns, st.st_size), rm.read_json(path))
    return hit[1]


def cell_status(out, project, loader, config, running):
    """(state, cell.json dict). `running`: a live run_matrix holds running.lock."""
    if not project:
        return 'n/a', {}
    pdir = os.path.join(out, os.path.basename(project))
    d = read_json_cached(os.path.join(pdir, loader, config, 'cell.json')) or {}
    state = d.get('state', 'none')
    if state == 'running' and not running:
        return 'interrupted', d
    cap = read_json_cached(os.path.join(pdir, 'capture.json')) or {}
    if running and cap.get('state') == 'capturing' and loader in cap.get('loaders', []) \
            and config in cap.get('configs', []):
        return 'capturing', d
    return state, d


def bank_status(out, sm_project, loader, config, running):
    """BankSystem view of a StockMarket cell: only the banksystem counts of its results.json files."""
    state, d = cell_status(out, sm_project, loader, config, running)
    if state not in ('passed', 'failed', 'error'):
        return state, d
    cell = os.path.join(out, os.path.basename(sm_project), loader, config)
    reports = [read_json_cached(r) or {} for r in glob.glob(os.path.join(cell, '*', 'results.json'))]
    banks = [r.get('mods', {}).get('banksystem') for r in reports]
    counts = {k: sum((b or {}).get(k, 0) for b in banks) for k in ('passed', 'failed', 'errors')}
    complete = reports and all(banks) and all(r.get('status') == 'completed' for r in reports)
    state = 'failed' if counts['failed'] else 'passed' if complete and not counts['errors'] else 'error'
    return state, {**d, **counts, 'status': f"via StockMarket (banksystem suites); SM: {d.get('status', '')}"}


def stale_reason(d, jars):
    """Why a finished result no longer matches the current jars (dict path -> sha256 or None), or None."""
    missing = [os.path.basename(j) for j, h in jars.items() if h is None]
    if missing:
        return 'jar missing: ' + ', '.join(missing)
    old = d.get('jars')
    if not old:
        return 'no jar fingerprint (older run)'
    changed = sorted(os.path.basename(j) for j in set(old) | set(jars) if old.get(j) != jars.get(j))
    return 'changed: ' + ', '.join(changed) if changed else None


def lighten(color):
    return '#' + ''.join(f'{(int(color[i:i + 2], 16) + 255) // 2:02x}' for i in (1, 3, 5))


def cell_text(state, d):
    if state == 'passed':
        return str(d.get('passed', 0))
    if state == 'failed':
        return f"{d.get('failed', 0)} fail"
    return {'none': '', 'error': f"{d.get('errors', 0)} err", 'running': 'run', 'capturing': 'gradle',
            'interrupted': 'intr', 'n/a': 'n/a', 'playing': 'PLAY'}.get(state, state)


def failed_tests(cell_dir, mod='*', cap=15):
    lines = []
    for log in glob.glob(os.path.join(cell_dir, '*', 'logs', 'UnitTests', f'{mod}.log')):
        with open(log, encoding='utf-8', errors='replace') as f:
            lines += [l.rstrip() for l in f if l.startswith(('FAIL ', 'ERR  '))]
    return lines[:cap] + (['...'] if len(lines) > cap else [])


def plan_runs(cells):
    """[(project, loader, config)] -> [(project, loaders, configs)]: one run_matrix call per project and set of
    configs, so every call is a full loaders x configs product."""
    by_project = {}
    for p, l, c in cells:
        by_project.setdefault(p, {}).setdefault(l, set()).add(c)
    runs = []
    for p, loaders in by_project.items():
        groups = {}
        for l, cs in loaders.items():
            groups.setdefault(tuple(c for c in rm.CONFIGS if c in cs), []).append(l)
        runs += [(p, ls, list(cs)) for cs, ls in groups.items()]
    return runs


class Gui:
    def __init__(self, root, out):
        self.root, self.out, self.queue, self.proc = root, out, [], None
        self.projects = {(m, v): find_project(rm.MODS_ROOT, folder, v) for m, folder in MODS for v in VERSIONS}
        self.labels, self.row_labels, self.tip = {}, {}, None
        self.mod_on = {m: tk.BooleanVar(value=True) for m, _ in MODS if m != BANK}
        self.settings_file = os.path.join(out, 'gui-settings.json')
        self.settings = rm.read_json(self.settings_file) or {}

        bar = tk.Frame(root)
        bar.pack(fill='x', padx=4, pady=(4, 0))
        self.buttons = [tk.Button(bar, text='Run all', command=lambda: self.run_where(lambda s, st: True)),
                        tk.Button(bar, text='Run not passed',
                                  command=lambda: self.run_where(lambda s, st: s != 'passed' or st)),
                        tk.Button(bar, text='Clear results', command=lambda: self.clear(None))]
        for b in self.buttons:
            b.pack(side='left')
        tk.Button(bar, text='Stop', command=self.stop).pack(side='left')
        tk.Button(bar, text='Open output folder', command=lambda: open_folder(out)).pack(side='left')
        for m, var in self.mod_on.items():
            tk.Checkbutton(bar, text=m, variable=var).pack(side='left')
        bar2 = tk.Frame(root)
        bar2.pack(fill='x', padx=4, pady=(0, 4))
        tk.Button(bar2, text='Choose play world...', command=self.choose_world).pack(side='left')
        self.world_label = tk.Label(bar2, anchor='w')
        self.world_label.pack(side='left')
        self.show_world()

        # Fixed header outside the scroll area; same widths/padding as the grid below keep the columns aligned.
        head = tk.Frame(root)
        head.pack(anchor='w', padx=4)
        tk.Label(head, width=10).grid(row=0, column=0, rowspan=2)
        for i, l in enumerate(rm.LOADERS):
            tk.Label(head, text=l, relief='groove').grid(row=0, column=1 + 4 * i, columnspan=4, sticky='ew')
        for j, (l, c) in enumerate(COLUMNS):
            tk.Label(head, text=c, width=8, relief='ridge').grid(row=1, column=1 + j, padx=1, pady=1)
        self.status = tk.Label(root, anchor='w', relief='sunken')
        self.status.pack(fill='x', side='bottom')
        area = tk.Frame(root)
        area.pack(fill='both', expand=True, padx=4)
        self.canvas = tk.Canvas(area, highlightthickness=0, bd=0, height=int(root.winfo_screenheight() * 0.6))
        sb = tk.Scrollbar(area, orient='vertical', command=self.canvas.yview)
        self.canvas.config(yscrollcommand=sb.set)
        sb.pack(side='right', fill='y')
        self.canvas.pack(side='left', fill='both', expand=True)
        grid = tk.Frame(self.canvas)
        self.canvas.create_window(0, 0, window=grid, anchor='nw')
        grid.bind('<Configure>', lambda e: self.canvas.config(scrollregion=self.canvas.bbox('all'),
                                                              width=grid.winfo_reqwidth()))
        for ev in ('<MouseWheel>', '<Button-4>', '<Button-5>'):
            root.bind_all(ev, self.wheel)
        r = 0
        for m, _ in MODS:
            # columnspan: the bold title must not widen column 0 (that would shift it against the header)
            tk.Label(grid, text=m + (' (results via StockMarket)' if m == BANK else ''),
                     font=('TkDefaultFont', 10, 'bold'), anchor='w').grid(row=r, column=0, columnspan=9, sticky='w')
            r += 1
            for v in VERSIONS:
                key = (m, v)
                rl = tk.Label(grid, text=v, width=10, anchor='w')
                rl.grid(row=r, column=0, sticky='w')
                rl.bind('<Double-Button-1>', lambda e, k=key: self.run_cells([(k, l, c) for l, c in COLUMNS], k[1]))
                self.bind_tip(rl, lambda k=key: self.row_tip(k))
                self.row_labels[key] = rl
                for j, (l, c) in enumerate(COLUMNS):
                    lab = tk.Label(grid, width=8, relief='ridge')
                    lab.grid(row=r, column=1 + j, padx=1, pady=1)
                    lab.bind('<Double-Button-1>', lambda e, x=(key, l, c): self.run_cells([x], ' '.join(x[1:])))
                    lab.bind('<Button-3>', lambda e, x=(key, l, c): self.menu(e, x))
                    self.bind_tip(lab, lambda x=(key, l, c): self.cell_tip(*x))
                    self.labels[(key, l, c)] = lab
                r += 1
        self.tick()

    def wheel(self, e):
        """Scrolls the grid while the pointer is over it (cell labels included)."""
        w = self.root.winfo_containing(e.x_root, e.y_root)
        if w is None or not str(w).startswith(str(self.canvas)):
            return
        self.hide_tip()  # the cell under the pointer changes without a <Leave>
        step = -1 if e.num == 4 else 1 if e.num == 5 else -int(e.delta / 120) or (-1 if e.delta > 0 else 1)
        self.canvas.yview_scroll(step, 'units')

    def menu(self, e, x):
        m = tk.Menu(self.root, tearoff=0)
        m.add_command(label='Play', command=lambda: self.play(*x))
        m.add_command(label='Run tests', command=lambda: self.run_cells([x], ' '.join(x[1:])))
        m.add_command(label='Clear result', command=lambda: self.clear([x]))
        m.add_command(label='Open folder', command=lambda: open_folder(self.cell_dir(*x)))
        p = self.projects[x[0]]
        pd = rm.play_dir(self.out, p, *x[1:]) if p else ''
        m.add_command(label='Open play folder', command=lambda: open_folder(pd),
                      state='normal' if pd and os.path.isdir(pd) else 'disabled')
        m.tk_popup(e.x_root, e.y_root)

    # ---------------------------------------------------------------- state

    def running_pid(self):
        return rm.lock_pid(os.path.join(self.out, 'running.lock'))

    def busy(self):
        return bool(self.queue or (self.proc and self.proc.poll() is None) or self.running_pid())

    def play_now(self, running):
        """play.json of the active play session, or None."""
        p = read_json_cached(os.path.join(self.out, 'play', 'play.json')) or {}
        return p if running and p.get('state') in ('capturing', 'playing') else None

    def status_project(self, key):
        """Project whose test results a row shows: BankSystem rows show their StockMarket cells."""
        return self.projects[('StockMarket', key[1])] if key[0] == BANK else self.projects[key]

    def cell_info(self, key, loader, config, running, jars):
        """(state, cell dict, stale reason or None). `jars`: per-tick cache of rm.jar_hashes."""
        p, own = self.status_project(key), self.projects[key]
        play = self.play_now(running)
        if play and own and (play['project'], play['loader'], play['config']) == (os.path.basename(own), loader, config):
            return 'playing', play, None
        state, d = (bank_status if key[0] == BANK else cell_status)(self.out, p, loader, config, running)
        if state not in ('passed', 'failed', 'error'):
            return state, d, None
        if (p, loader) not in jars:
            jars[(p, loader)] = rm.jar_hashes(p, loader)
        return state, d, stale_reason(d, jars[(p, loader)])

    def cell_dir(self, key, loader, config):
        p = self.status_project(key)
        d = os.path.join(self.out, os.path.basename(p), loader, config) if p else self.out
        return d if os.path.isdir(d) else self.out

    def tick(self):
        running = bool(self.running_pid())
        counts, jars = {}, {}
        for (key, l, c), lab in self.labels.items():
            state, d, stale = self.cell_info(key, l, c, running, jars)
            color = COLORS.get(state, COLORS['error'])
            lab.config(text=cell_text(state, d) + ('*' if stale else ''), bg=lighten(color) if stale else color,
                       fg='white' if state in ('n/a', 'failed') and not stale else 'black')
            counts['stale' if stale else state] = counts.get('stale' if stale else state, 0) + 1
            if state in ('running', 'capturing'):
                counts['active'] = f'{state}: {os.path.basename(self.status_project(key))} {l} {c}'
        for key, rl in self.row_labels.items():
            p = self.status_project(key)
            cap = read_json_cached(os.path.join(self.out, os.path.basename(p), 'capture.json')) if p else None
            rl.config(fg='red' if cap and cap.get('rc') else 'gray' if not p else 'black')
        if self.proc and self.proc.poll() is not None:
            self.proc = None
        if self.queue and not self.proc and not running:
            p, ls, cs = self.queue.pop(0)
            self.start(p, '--loaders', *ls, '--configs', *cs)
        for b in self.buttons:
            b.config(state='disabled' if self.busy() else 'normal')
        play = self.play_now(running)
        active = (f"playing: {play['project']} {play['loader']} {play['config']}"
                  + (' (gradle)' if play['state'] == 'capturing' else '')) if play else counts.get('active', 'idle')
        queued = f'  ({len(self.queue)} runs queued)' if self.queue else ''
        self.status.config(text=f"{active}{queued}   |   passed {counts.get('passed', 0)}, stale "
                                f"{counts.get('stale', 0)}, failed/error "
                                f"{counts.get('failed', 0) + counts.get('error', 0) + counts.get('interrupted', 0)}, "
                                f"not run {counts.get('none', 0)}, n/a {counts.get('n/a', 0)}")
        self.root.after(2000, self.tick)

    # ---------------------------------------------------------------- runs

    def run_where(self, pred):
        running, jars = bool(self.running_pid()), {}
        cells = [(k, l, c) for (k, l, c) in self.labels if k[0] != BANK and self.mod_on[k[0]].get() and
                 pred(*self.cell_info(k, l, c, running, jars)[::2])]
        self.run_cells(cells, f'{len(cells)} cells')

    def run_cells(self, cells, what):
        cells = [(self.status_project(k), l, c) for k, l, c in cells if self.status_project(k)]
        if self.busy():
            messagebox.showinfo('Busy', 'A run is active (the test ports are shared); stop it or wait.')
        elif cells and messagebox.askokcancel('Run', f'Run {what}?'):
            self.queue = plan_runs(cells)  # tick() starts them one after another

    def play(self, key, loader, config):
        p, world = self.projects[key], self.settings.get('play_world')
        if not p:
            return
        if self.busy():
            messagebox.showinfo('Busy', 'A run is active (the game ports are shared); stop it or wait.')
            return
        if world and not os.path.isfile(os.path.join(world, 'level.dat')):
            messagebox.showerror('Play', f'Play world {world} has no level.dat; choose another one.')
            return
        if messagebox.askokcancel('Play', f'Play {os.path.basename(p)} {loader} {config} with '
                                          f'{world or "a new superflat world"}?\nStop: close the game or press Stop.'):
            self.start(p, '--play', loader, config, *(['--world', world] if world else []))

    def start(self, *args):
        cmd = [sys.executable, os.path.join(rm.HERE, 'run_matrix.py'), *args, '--out', self.out]
        log = open(os.path.join(self.out, 'gui-run.log'), 'a', encoding='utf-8')
        log.write(f'\n>>> {" ".join(cmd)}\n')
        log.flush()
        self.proc = subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                                     creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        log.close()  # the child keeps its own handle

    def stop(self):
        stop_file = os.path.join(self.out, 'play', 'stop')
        if self.play_now(bool(self.running_pid())) and not os.path.exists(stop_file):
            if messagebox.askokcancel('Stop', 'Stop the play session? (servers get "stop", then the client is closed)'):
                open(stop_file, 'w').close()  # run_matrix stops everything gracefully
            return
        pid = self.proc.pid if self.proc and self.proc.poll() is None else self.running_pid()
        if not pid and not self.queue:
            return
        if not messagebox.askokcancel('Stop', f'Kill run_matrix (pid {pid}) and its games, drop the queue?'):
            return
        self.queue = []
        if pid:
            if os.name == 'nt':
                subprocess.run(['taskkill', '/T', '/F', '/PID', str(pid)], capture_output=True,
                               creationflags=subprocess.CREATE_NO_WINDOW)
            else:
                os.kill(pid, 9)  # ponytail: no process-tree kill off Windows

    def clear(self, cells):
        """Deletes cell.json (and with cells=None every capture.json) under <out>/<project>/; logs and play/ stay."""
        if self.busy():
            messagebox.showinfo('Busy', 'A run is active; stop it or wait.')
            return
        if cells is None:
            if not messagebox.askokcancel('Clear results', 'Reset every cell to "not run"? (logs are kept)'):
                return
            files = [f for f in glob.glob(os.path.join(self.out, '*', 'capture.json')) +
                     glob.glob(os.path.join(self.out, '*', '*', '*', 'cell.json'))
                     if os.path.relpath(f, self.out).split(os.sep)[0] != 'play']
        else:
            files = [os.path.join(self.out, os.path.basename(self.status_project(k)), l, c, 'cell.json')
                     for k, l, c in cells if self.status_project(k)]
        for f in files:
            if os.path.exists(f):
                os.remove(f)

    def choose_world(self):
        d = filedialog.askdirectory(title='Minecraft world folder (contains level.dat)',
                                    initialdir=self.settings.get('play_world') or self.out)
        if not d:
            if not (self.settings.get('play_world') and
                    messagebox.askyesno('Play world', 'Use a generated superflat world instead?')):
                return
            self.settings.pop('play_world')
        elif not os.path.isfile(os.path.join(d, 'level.dat')):
            messagebox.showerror('Play world', f'{d} has no level.dat')
            return
        else:
            self.settings['play_world'] = os.path.normpath(d)
        rm.write_json(self.settings_file, self.settings)
        self.show_world()

    def show_world(self):
        w = self.settings.get('play_world')
        self.world_label.config(text=f' play world: {w} (copied fresh on every Play)' if w else
                                ' play world: new superflat world')

    # ---------------------------------------------------------------- tooltips

    def cell_tip(self, key, loader, config):
        state, d, stale = self.cell_info(key, loader, config, bool(self.running_pid()), {})
        if state == 'n/a':
            return f'{key[0]} {key[1]}: project folder missing'
        lines = [f'{state}{" (stale)" if stale else ""}   {key[0]} {key[1]} {loader} {config}']
        if stale:
            lines.append(f'stale: {stale}')
        if state == 'playing':
            lines.append(f"started {d.get('started')}, world: {d.get('world') or 'new superflat'}")
        elif 'passed' in d:
            lines.append(f"passed {d['passed']}, failed {d['failed']}, errors {d['errors']}, {d.get('seconds')} s")
            lines.append(f"finished {d.get('finished')}")
            lines.append(d.get('status', ''))
        elif d.get('started'):
            lines.append(f"started {d['started']}")
        cell = os.path.join(self.out, os.path.basename(self.status_project(key)), loader, config)
        if state in ('failed', 'error'):
            lines += failed_tests(cell, 'banksystem' if key[0] == BANK else '*')
        return '\n'.join(lines + [cell, 'right-click: Play / Run tests / Clear result / Open folder / Open play folder'])

    def row_tip(self, key):
        p = self.status_project(key)
        if not p:
            return 'project folder missing'
        cap = read_json_cached(os.path.join(self.out, os.path.basename(p), 'capture.json')) or {}
        rc = f"gradle rc={cap['rc']} (see gradle-capture.log)" if 'rc' in cap else ''
        via = f'results via {os.path.basename(p)}' if key[0] == BANK else ''
        return '\n'.join(filter(None, [self.projects[key], via, rc, 'double-click: run the whole row']))

    def bind_tip(self, widget, text):
        widget.bind('<Enter>', lambda e: self.show_tip(e, text()))
        widget.bind('<Leave>', lambda e: self.hide_tip())

    def show_tip(self, e, text):
        self.hide_tip()
        self.tip = tk.Toplevel(self.root)
        self.tip.wm_overrideredirect(True)
        self.tip.wm_geometry(f'+{e.x_root + 12}+{e.y_root + 12}')
        tk.Label(self.tip, text=text, justify='left', bg='#ffffe0', relief='solid', bd=1).pack()

    def hide_tip(self):
        if self.tip:
            self.tip.destroy()
            self.tip = None


def open_folder(path):
    if os.name == 'nt':
        os.startfile(path)
    else:
        subprocess.Popen(['xdg-open', path])


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--out', default=os.path.join(rm.MODS_ROOT, 'autotest-out'), help='run_matrix output folder')
    out = os.path.abspath(ap.parse_args().out)
    os.makedirs(out, exist_ok=True)
    lock = os.path.join(out, 'gui.lock')
    root = tk.Tk()
    root.title(f'Autotest matrix - {out}')
    if rm.lock_pid(lock):
        root.withdraw()
        messagebox.showinfo('Autotest matrix', f'A GUI for {out} is already open.')
        return
    with open(lock, 'w') as f:
        f.write(str(os.getpid()))
    gui = Gui(root, out)

    def close():
        if not gui.queue or messagebox.askokcancel('Quit', 'Queued runs are dropped (the current one continues).'):
            root.destroy()
    root.protocol('WM_DELETE_WINDOW', close)
    try:
        root.mainloop()
    finally:
        if rm.lock_pid(lock) == os.getpid():
            os.remove(lock)


if __name__ == '__main__':
    main()
