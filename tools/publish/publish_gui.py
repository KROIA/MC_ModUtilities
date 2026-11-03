"""Publish GUI: settings editor, dry-run state of every jar, single / all uploads. Thin layer over publish.py."""
import json
import os
import queue
import sys
import threading
import traceback
import tkinter as tk
import types
from tkinter import messagebox, ttk

sys.dont_write_bytecode = True  # no __pycache__ next to the scripts
import publish as pb  # noqa: E402

CFG = os.path.join(pb.HERE, 'publish.json')
PLATFORMS = ('modrinth', 'curseforge')
ENV = {'modrinth': 'MODRINTH_API', 'curseforge': 'CURSEFORGE_API'}
SAMPLE = dict(mod='BankSystem', mc='1.21.1', modver='2.2.0', mod_version='1.21.1-2.2.0', loader='fabric',
              Loader='Fabric', repo='https://github.com/KROIA/BankSystem', branch='mc_1.21.1')


class Gui:
    def __init__(self, root):
        self.root, self.q, self.busy, self.entries, self.ledger = root, queue.Queue(), False, [], {}
        self.status = tk.Label(root, anchor='w', relief='sunken', width=1)  # long text clips, never widens the window
        self.status.pack(fill='x', side='bottom')
        nb = ttk.Notebook(root)
        nb.pack(fill='both', expand=True)
        jars, sett = ttk.Frame(nb), ttk.Frame(nb, padding=6)
        nb.add(jars, text='Jars')
        nb.add(sett, text='Settings')

        bar = ttk.Frame(jars)
        bar.pack(fill='x')
        ttk.Button(bar, text='Refresh', command=self.refresh).pack(side='left')
        ttk.Button(bar, text='Upload selected', command=self.upload_selected).pack(side='left')
        ttk.Button(bar, text='Upload all ready', command=self.upload_all).pack(side='left')
        self.plat = {p: tk.BooleanVar(value=True) for p in PLATFORMS}
        ttk.Label(bar, text='   to:').pack(side='left')
        for p in PLATFORMS:
            ttk.Checkbutton(bar, text=p.capitalize(), variable=self.plat[p]).pack(side='left')
        self.env = ttk.Label(bar)
        self.env.pack(side='right')

        cols = {'mod': 90, 'mc': 60, 'loader': 70, 'games': 140, 'problem': 260, 'modrinth': 130, 'curseforge': 130}
        self.tree = ttk.Treeview(jars, columns=list(cols), show='headings', height=16, selectmode='browse')
        for c, w in cols.items():
            self.tree.heading(c, text=c)
            self.tree.column(c, width=w, stretch=c == 'problem')
        for tag, color in (('problem', '#c03030'), ('ready', '#207020'), ('done', '#707070')):
            self.tree.tag_configure(tag, foreground=color)
        self.tree.pack(fill='both', expand=True)
        self.tree.bind('<<TreeviewSelect>>', lambda _: self.show_detail())
        self.detail = tk.Text(jars, height=8, wrap='word')
        self.detail.pack(fill='x')
        self.log_text = tk.Text(jars, height=10, wrap='word', bg='#f4f4f4')
        self.log_text.pack(fill='both', expand=True)

        self.vars = {k: tk.StringVar() for k in ('release_type', 'display_name', 'version_number', 'changelog')}
        self.manual = tk.BooleanVar()
        for i, k in enumerate(self.vars):
            ttk.Label(sett, text=k).grid(row=i, column=0, sticky='w')
            w = (ttk.Combobox(sett, textvariable=self.vars[k], values=['release', 'beta', 'alpha'], state='readonly')
                 if k == 'release_type' else ttk.Entry(sett, textvariable=self.vars[k], width=100))
            w.grid(row=i, column=1, sticky='we')
        ttk.Checkbutton(sett, text='CurseForge: mark for manual release (approved files stay unreleased until you '
                        'release them)', variable=self.manual).grid(row=4, column=1, sticky='w')
        ttk.Label(sett, text='mods (JSON)').grid(row=5, column=0, sticky='nw')
        self.mods = tk.Text(sett, height=28, width=100, undo=True)
        self.mods.grid(row=5, column=1, sticky='nswe')
        ttk.Button(sett, text='Save', command=self.save).grid(row=6, column=1, sticky='e')
        sett.columnconfigure(1, weight=1)
        sett.rowconfigure(5, weight=1)

        self.load_settings()
        root.after(100, self.poll)
        self.refresh()

    # ------------------------------------------------------------ threading: workers post callables to self.q

    def poll(self):
        while not self.q.empty():
            self.q.get()()
        self.root.after(100, self.poll)

    def log(self, msg):
        def add():
            self.log_text.insert('end', msg + '\n')
            self.log_text.see('end')
        self.q.put(add)

    def run(self, what, fn):
        if self.busy:
            return messagebox.showinfo('Busy', 'Wait for the current task to finish.')
        self.busy = True
        self.status.config(text=f'{what} ...')

        def work():
            try:
                fn()
                self.q.put(lambda: self.status.config(text=f'{what}: done'))
            except SystemExit as e:  # pb.die
                self.log(str(e.code))
                self.q.put(lambda: self.status.config(text=f'{what}: FAILED (see log)'))
            except Exception:
                self.log(traceback.format_exc())
                self.q.put(lambda: self.status.config(text=f'{what}: FAILED (see log)'))
            finally:
                self.q.put(lambda: setattr(self, 'busy', False))
        threading.Thread(target=work, daemon=True).start()

    # ------------------------------------------------------------ jars

    def refresh(self):
        self.env.config(text='   '.join(f"{v}: {'set' if os.environ.get(v) else 'NOT set'}" for v in ENV.values()))

        def work():
            cfg = pb.read_json(CFG)
            args = types.SimpleNamespace(mods=list(cfg['mods']), versions=cfg['versions'], loaders=list(cfg['loaders']))
            entries = pb.plan(cfg, args)
            pb.check_games(entries)
            ledger = pb.read_json(pb.LEDGER, {})
            self.q.put(lambda: self.fill(cfg, entries, ledger))
        self.run('Refresh', work)

    def state(self, e, p):
        done = self.ledger.get(pb.key(e, p)) if 'mod_version' in e else None
        return f"published {done['file_id']}" if done else '-' if e['problems'] else 'ready'

    def fill(self, cfg, entries, ledger):
        self.cfg, self.entries, self.ledger = cfg, entries, ledger
        sel = self.tree.selection()
        self.tree.delete(*self.tree.get_children())
        for i, e in enumerate(entries):
            states = [self.state(e, p) for p in PLATFORMS]
            tag = 'problem' if e['problems'] else 'done' if all(s.startswith('published') for s in states) else 'ready'
            games = e.get('games', '')
            problem = e['problems'][0].split(' (')[0] + (f" (+{len(e['problems']) - 1})" if len(e['problems']) > 1 else '') \
                if e['problems'] else ''
            self.tree.insert('', 'end', iid=str(i), tags=(tag,), values=(
                e['mod'], e['mc'], e['loader'], ' '.join(games) if isinstance(games, list) else games, problem, *states))
        if sel and self.tree.exists(sel[0]):
            self.tree.selection_set(sel)
        bad = sum(bool(e['problems']) for e in entries)
        self.status.config(text=f'{len(entries)} jars, {bad} with problems, {len(ledger)} uploads in published.json')

    def selected(self):
        sel = self.tree.selection()
        return self.entries[int(sel[0])] if sel else None

    def show_detail(self):
        e = self.selected()
        if not e:
            return
        lines = [os.path.basename(e['jar'] or '') or '(no project)']
        if 'range' in e:
            deps = ', '.join(f"{d.get('mod') or d['modrinth']}:{d['type']}" for d in e['deps'])
            lines += [f"name: {e['name']}   number: {e['number']}", f"range: {e['range']!r} -> {e.get('games', '-')}",
                      f'deps: {deps}', f"changelog: {e['changelog']}"]
        lines += [f'{p}: {self.state(e, p)}' for p in PLATFORMS] + [f'PROBLEM: {p}' for p in e['problems']]
        self.detail.delete('1.0', 'end')
        self.detail.insert('end', '\n'.join(lines))

    def platforms(self):
        return [p for p in PLATFORMS if self.plat[p].get()]

    def confirm(self, title, todo):
        manual = self.cfg.get('curseforge_manual_release', True)
        lines = [f"{e['name']} -> {p}" for e, p in todo]
        text = '\n'.join(lines[:25] + ([f'... and {len(lines) - 25} more'] if len(lines) > 25 else []))
        return messagebox.askokcancel(title, f'Really upload {len(todo)} file(s)?\n\n{text}\n\nrelease type: '
                                      f"{self.cfg['release_type']}, CurseForge manual release: {manual}")

    def upload(self, todo):
        cfg, ledger = self.cfg, self.ledger

        def work():
            jars = list({id(e): e for e, _ in todo}.values())
            ctx = pb.preflight(cfg, jars, sorted({p for _, p in todo}), sorted({e['mod'] for e in jars}))
            for e, p in todo:  # stops at the first error (pb.die)
                pb.upload_one(cfg, e, p, ctx, ledger, self.log)
            self.q.put(self.refresh_later)
        self.run('Upload', work)

    def refresh_later(self):
        self.root.after(200, self.refresh)

    def upload_selected(self):
        e = self.selected()
        if not e:
            return messagebox.showinfo('Upload', 'Select a jar first.')
        if e['problems']:
            return messagebox.showerror('Upload', 'This jar has problems:\n\n' + '\n'.join(e['problems']))
        todo = [(e, p) for p in self.platforms() if pb.key(e, p) not in self.ledger]
        if not todo:
            return messagebox.showinfo('Upload', 'Nothing to do (no platform ticked, or already published).')
        if self.confirm('Upload selected', todo):
            self.upload(todo)

    def upload_all(self):
        todo = [(e, p) for e in self.entries if not e['problems'] for p in self.platforms()
                if pb.key(e, p) not in self.ledger]  # entries are in dependency order
        if not todo:
            return messagebox.showinfo('Upload', 'No ready jar left to upload.')
        if self.confirm('Upload all ready', todo):
            self.upload(todo)

    # ------------------------------------------------------------ settings

    def load_settings(self):
        cfg = pb.read_json(CFG)
        for k, v in self.vars.items():
            v.set(cfg[k])
        self.manual.set(cfg.get('curseforge_manual_release', True))
        self.mods.delete('1.0', 'end')
        self.mods.insert('end', json.dumps(cfg['mods'], indent=2))

    def save(self):
        try:
            mods = json.loads(self.mods.get('1.0', 'end'))
            assert isinstance(mods, dict) and mods, 'mods must be a non-empty object'
            for name, m in mods.items():
                for f in ('folder', 'repo', 'modrinth', 'curseforge_id', 'curseforge_slug', 'dependencies'):
                    assert f in m, f'{name}: "{f}" missing'
                for d in m['dependencies']:
                    assert d.get('type') in pb.CF_TYPES, f'{name}: dependency type must be one of {list(pb.CF_TYPES)}'
                    assert d.get('mod') in mods if 'mod' in d else 'modrinth' in d and 'curseforge' in d, \
                        f'{name}: dependency {d} needs "mod" (a mod in this file) or "modrinth" + "curseforge"'
            for k in ('display_name', 'version_number', 'changelog'):
                pb.fmt(self.vars[k].get(), **SAMPLE)
        except (ValueError, AssertionError) as e:
            return messagebox.showerror('Save', f'Not saved: {e}')
        except (KeyError, IndexError) as e:
            return messagebox.showerror('Save', f'Not saved: unknown template placeholder {e}')
        cfg = pb.read_json(CFG)
        cfg.update({k: v.get() for k, v in self.vars.items()}, curseforge_manual_release=self.manual.get(), mods=mods)
        pb.write_json(CFG, cfg)
        self.status.config(text='publish.json saved')
        self.refresh()


def main():
    lock = os.path.join(pb.HERE, 'gui.lock')
    root = tk.Tk()
    root.title('Publish to Modrinth / CurseForge')
    if pb.rm.lock_pid(lock):
        root.withdraw()
        messagebox.showinfo('Publish', 'The publish GUI is already open.')
        return
    with open(lock, 'w') as f:
        f.write(str(os.getpid()))
    gui = Gui(root)
    root.protocol('WM_DELETE_WINDOW', lambda: (not gui.busy or messagebox.askokcancel(
        'Quit', 'A task is running (an upload may be cut off). Quit anyway?')) and root.destroy())
    try:
        root.mainloop()
    finally:
        if pb.rm.lock_pid(lock) == os.getpid():
            os.remove(lock)


if __name__ == '__main__':
    main()
