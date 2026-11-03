"""Upload already-built mod jars to Modrinth and CurseForge. Dry run unless --upload. See README.md."""
import argparse
import collections
import datetime
import io
import json
import os
import re
import subprocess
import sys
import tomllib
import urllib.error
import urllib.request
import uuid
import zipfile

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'autotest'))
import run_matrix as rm  # noqa: E402

LEDGER = os.path.join(HERE, 'published.json')
UA = 'KROIA/publish-tool (https://github.com/KROIA/MC_ModUtilities)'
MODRINTH = 'https://api.modrinth.com/v2'
CURSEFORGE = 'https://minecraft.curseforge.com/api'
CF_TYPES = {'required': 'requiredDependency', 'optional': 'optionalDependency',
            'embedded': 'embeddedLibrary', 'incompatible': 'incompatible'}
VER = r'\d+(?:\.\d+)*'


def die(msg):
    sys.exit(f'ERROR: {msg}')


def read_json(path, default=None):
    if default is not None and not os.path.exists(path):
        return default
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def write_json(path, data):
    tmp = path + '.tmp'
    with open(tmp, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
    os.replace(tmp, path)


def fmt(template, **kw):
    return template.format(**kw)


def multipart(parts):
    """parts: [(field, filename or None, content_type, bytes)] -> (body, content-type header)."""
    b = uuid.uuid4().hex
    out = io.BytesIO()
    for name, filename, ctype, data in parts:
        disp = f'form-data; name="{name}"' + (f'; filename="{filename}"' if filename else '')
        out.write(f'--{b}\r\nContent-Disposition: {disp}\r\nContent-Type: {ctype}\r\n\r\n'.encode())
        out.write(data + b'\r\n')
    out.write(f'--{b}--\r\n'.encode())
    return out.getvalue(), f'multipart/form-data; boundary={b}'


def dev_entries(src, prefixes):
    """Entries under dev-only packages, also inside nested (jar-in-jar) jars. src: path or bytes."""
    hits = []
    with zipfile.ZipFile(io.BytesIO(src) if isinstance(src, bytes) else src) as z:
        for n in z.namelist():
            if n.startswith(tuple(prefixes)):
                hits.append(n)
            elif n.endswith('.jar'):
                hits += [f'{n}!/{h}' for h in dev_entries(z.read(n), prefixes)]
    return hits


# ---------------------------------------------------------------- plan

def http(method, url, headers=None, body=None):
    req = urllib.request.Request(url, data=body, method=method, headers={'User-Agent': UA, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return json.loads(r.read() or b'null')
    except urllib.error.HTTPError as e:
        die(f'{method} {url} -> HTTP {e.code}\n{e.read().decode("utf-8", "replace")}')


def modrinth_releases():
    """Modrinth release game versions. Public endpoint, no token."""
    return [v['version'] for v in http('GET', f'{MODRINTH}/tag/game_version') if v['version_type'] == 'release']


# ---------------------------------------------------------------- Minecraft range (jar metadata = source of truth)

def mc_range(jar, loader):
    """fabric.mod.json depends.minecraft (str or list) / neoforge.mods.toml minecraft versionRange."""
    with zipfile.ZipFile(jar) as z:
        if loader == 'fabric':
            return json.loads(z.read('fabric.mod.json'))['depends']['minecraft']
        toml = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
        return next(d['versionRange'] for ds in toml['dependencies'].values() for d in ds if d['modId'] == 'minecraft')


def vt(v):
    t = tuple(int(x) for x in v.split('.'))
    return t + (0,) * (3 - len(t))


def intervals(rng, loader):
    """[(lo, hi, hi_inclusive)] or None if the form is unsupported/open.
    ponytail: only exact, '>=a <b' and lists of those (fabric) and [a] / [a,b) / [a,b] (maven) are accepted;
    ~, ^, wildcards, open ranges and maven unions are refused on purpose: tighten the mod metadata instead."""
    if loader == 'fabric':
        out = []
        for r in rng if isinstance(rng, list) else [rng]:
            m = re.fullmatch(rf'\s*(?:({VER})|>=\s*({VER})\s+<\s*({VER}))\s*', r)
            if not m:
                return None
            out.append((m[1], m[1], True) if m[1] else (m[2], m[3], False))
        return out or None
    m = re.fullmatch(rf'\s*\[\s*({VER})\s*(?:\]|,\s*({VER})\s*([)\]]))\s*', rng)
    return m and [(m[1], m[1], True) if not m[2] else (m[1], m[2], m[3] == ']')]


def matching(ivs, versions):
    """Versions (release names) inside any interval, sorted."""
    ok = lambda v: any(vt(lo) <= vt(v) and (vt(v) <= vt(hi) if inc else vt(v) < vt(hi)) for lo, hi, inc in ivs)
    return sorted((v for v in versions if re.fullmatch(VER, v) and ok(v)), key=vt)


def find_project(folder, version):
    hits = sorted(rm.find_projects(os.path.join(rm.MODS_ROOT, folder), version), key=len)
    return hits[0] if hits else None


def branch(project):
    r = subprocess.run(['git', '-C', project, 'branch', '--show-current'], capture_output=True, text=True,
                       creationflags=rm.NO_WINDOW)
    return r.stdout.strip()


def deps_for(cfg, mod, loader):
    return [d for d in cfg['mods'][mod]['dependencies'] if loader in d.get('loaders', [loader])]


def plan(cfg, args):
    """One entry per (mod, mc, loader) jar, mods in config order (= dependency order)."""
    entries = []
    for mod in [m for m in cfg['mods'] if m in args.mods]:
        m = cfg['mods'][mod]
        mod_entries = []
        for mc in args.versions:
            project = find_project(m['folder'], mc)
            if not project:
                mod_entries.append(dict(mod=mod, mc=mc, loader='-', jar=None, problems=['project folder missing']))
                continue
            props = rm.gradle_props(project)
            mod_version = props.get('mod_version', '')
            prefix, _, modver = mod_version.partition('-')
            br = branch(project)
            game = props.get('minecraft_version', '')
            kw = dict(mod=mod, mc=mc, modver=modver, mod_version=mod_version, repo=m['repo'], branch=br)
            common = []
            if prefix != mc or not (game == mc or game.startswith(mc + '.')):
                common.append(f"mod_version {mod_version} / minecraft_version {game} do not match folder version {mc}")
            if br != f'mc_{mc}':
                common.append(f'git branch is {br!r}, expected mc_{mc}')
            if not os.path.exists(os.path.join(project, 'changelog', f'v{modver}.md')):
                common.append(f'changelog/v{modver}.md missing')
            enabled = props.get('enabled_platforms', '').split(',')
            for loader in [l for l in args.loaders if l in enabled]:
                jar = rm.mod_jars(project, loader)[0]
                e = dict(kw, loader=loader, Loader=cfg['loaders'][loader]['curseforge'], jar=jar, problems=list(common))
                e['name'] = fmt(cfg['display_name'], **e)
                e['number'] = fmt(cfg['version_number'], **e)
                e['changelog'] = fmt(cfg['changelog'], **e)
                e['deps'] = deps_for(cfg, mod, loader)
                if not os.path.exists(jar):
                    e['problems'].append('jar missing')
                    mod_entries.append(e)
                    continue
                if props.get('enable_dev_features', 'false').lower() == 'true' or dev_entries(jar, cfg['dev_packages']):
                    hits = dev_entries(jar, cfg['dev_packages'])
                    e['problems'].append(f'DEV build ({len(hits)} dev entries, e.g. {hits[0] if hits else "enable_dev_features=true"})')
                e['range'] = mc_range(jar, loader)
                e['ivs'] = intervals(e['range'], loader)
                if not e['ivs']:
                    e['problems'].append(f"unsupported/open range {e['range']!r}: tighten the mod metadata")
                mod_entries.append(e)
        common_ver = collections.Counter(e['modver'] for e in mod_entries if 'modver' in e).most_common(1)
        for e in mod_entries:
            if common_ver and e.get('modver', common_ver[0][0]) != common_ver[0][0]:
                e['problems'].append(f"mod version {e['modver']} differs from {common_ver[0][0]} used by the other versions")
        entries += mod_entries
    return entries


def key(e, platform):
    return f"{e['mod']}|{e['mc']}|{e['loader']}|{platform}|{e['mod_version']}"


def dep_label(cfg, d, platform):
    if 'mod' in d:
        own = cfg['mods'][d['mod']]
        return f"{d['mod']}({own['modrinth' if platform == 'modrinth' else 'curseforge_slug'] or '?'})"
    return d[platform]


def check_games(entries):
    """e['games']: Modrinth releases inside the jar's range (public tag list, no token). CurseForge: preflight."""
    try:
        releases = modrinth_releases()
    except (OSError, SystemExit):
        releases = None
    for e in entries:
        if e.get('ivs'):
            e['games'] = matching(e['ivs'], releases) if releases else '? (Modrinth tag list unreachable)'
            if e['games'] == []:
                e['problems'].append(f"range {e['range']!r} matches no Modrinth release")


def show(cfg, entries, platforms, ledger):
    todo = 0
    for e in entries:
        print(f"\n{e['mod']} {e['mc']} {e['loader']}  {os.path.basename(e['jar']) if e['jar'] else ''}")
        if 'range' in e:
            print(f"  name: {e['name']} | number: {e['number']} | loaders: modrinth {cfg['loaders'][e['loader']]['modrinth']}, "
                  f"curseforge {e['Loader']}\n  range: {e['range']!r} -> {e.get('games', '-')}")
            for p in platforms:
                deps = ', '.join(f"{dep_label(cfg, d, p)}:{d['type']}" for d in e['deps'])
                done = ledger.get(key(e, p))
                todo += not done
                print(f"  {p}: {'already published (file ' + str(done['file_id']) + ')' if done else 'upload'} | deps: {deps}")
            print(f"  changelog: {e['changelog']}")
        for p in e['problems']:
            print(f'  PROBLEM: {p}')
    bad = sum(bool(e['problems']) for e in entries)
    print(f'\n{len(entries)} jars, {todo} uploads pending, {bad} with problems')
    return bad


def preflight(cfg, entries, platforms, mods):
    """Resolve everything that needs the network before the first upload, so a bad config fails early."""
    for mod in mods:
        for p, f in (('modrinth', 'modrinth'), ('curseforge', 'curseforge_id'), ('curseforge', 'curseforge_slug')):
            if p in platforms and not cfg['mods'][mod][f]:
                die(f'{mod}: {f} is empty in publish.json')
    ctx = {}
    if 'modrinth' in platforms:
        ctx['mr_token'] = os.environ.get('MODRINTH_API') or die('MODRINTH_API not set')
        releases = modrinth_releases()
        slugs = {d['modrinth'] for e in entries for d in e['deps'] if 'modrinth' in d}
        slugs |= {cfg['mods'][m]['modrinth'] for m in mods}
        slugs |= {cfg['mods'][d['mod']]['modrinth'] for e in entries for d in e['deps'] if 'mod' in d}
        ctx['mr_ids'] = {s: http('GET', f'{MODRINTH}/project/{s}')['id'] for s in slugs}
        for e in entries:
            e['games'] = matching(e['ivs'], releases) or die(
                f"{key(e, 'modrinth')}: range {e['range']!r} matches no Modrinth release")
    if 'curseforge' in platforms:
        tok = ctx['cf_token'] = os.environ.get('CURSEFORGE_API') or die('CURSEFORGE_API not set')
        types = http('GET', f'{CURSEFORGE}/game/version-types', {'X-Api-Token': tok})
        versions = http('GET', f'{CURSEFORGE}/game/versions', {'X-Api-Token': tok})
        mc_types = {t['id'] for t in types if t['slug'].startswith('minecraft-')}
        loader_types = {t['id'] for t in types if t['slug'] == 'modloader'}
        cf_mc = {v['name']: v['id'] for v in versions if v['gameVersionTypeID'] in mc_types}
        cf_loader = {v['name']: v['id'] for v in versions if v['gameVersionTypeID'] in loader_types}
        for e in entries:
            games = matching(e['ivs'], cf_mc) or die(
                f"{key(e, 'curseforge')}: range {e['range']!r} matches no CurseForge Minecraft release")
            loader = cf_loader.get(e['Loader']) or die(f"CurseForge has no mod loader named {e['Loader']!r}")
            e['cf_games'] = [cf_mc[g] for g in games] + [loader]
    return ctx


def upload_modrinth(cfg, e, ctx, data):
    deps = [{'project_id': ctx['mr_ids'][cfg['mods'][d['mod']]['modrinth'] if 'mod' in d else d['modrinth']],
             'dependency_type': d['type']} for d in e['deps']]
    meta = {'name': e['name'], 'version_number': e['number'], 'changelog': e['changelog'], 'dependencies': deps,
            'game_versions': e['games'], 'version_type': cfg['release_type'],
            'loaders': cfg['loaders'][e['loader']]['modrinth'], 'featured': False,
            'project_id': ctx['mr_ids'][cfg['mods'][e['mod']]['modrinth']], 'file_parts': ['file'], 'primary_file': 'file'}
    body, ctype = multipart([('data', None, 'application/json', json.dumps(meta).encode()),
                             ('file', os.path.basename(e['jar']), 'application/java-archive', data)])
    return http('POST', f'{MODRINTH}/version', {'Authorization': ctx['mr_token'], 'Content-Type': ctype}, body)['id']


def cf_metadata(cfg, e, game_ids):
    rel = []
    for d in e['deps']:
        own = cfg['mods'].get(d.get('mod'))
        rel.append({'slug': own['curseforge_slug'], 'projectID': own['curseforge_id'], 'type': CF_TYPES[d['type']]}
                   if own else {'slug': d['curseforge'], 'type': CF_TYPES[d['type']]})
    return {'changelog': e['changelog'], 'changelogType': 'markdown', 'displayName': e['name'],
            'gameVersions': game_ids, 'releaseType': cfg['release_type'], 'relations': {'projects': rel},
            'isMarkedForManualRelease': bool(cfg.get('curseforge_manual_release', True))}


def upload_curseforge(cfg, e, ctx, data):
    meta = cf_metadata(cfg, e, e['cf_games'])
    body, ctype = multipart([('metadata', None, 'application/json', json.dumps(meta).encode()),
                             ('file', os.path.basename(e['jar']), 'application/java-archive', data)])
    url = f"{CURSEFORGE}/projects/{cfg['mods'][e['mod']]['curseforge_id']}/upload-file"
    return http('POST', url, {'X-Api-Token': ctx['cf_token'], 'Content-Type': ctype}, body)['id']


def upload_one(cfg, e, p, ctx, ledger, log=print):
    """Upload one jar to one platform and record it in the ledger right away. Returns the file/version id."""
    k = key(e, p)
    with open(e['jar'], 'rb') as f:
        data = f.read()
    log(f'uploading {k} ...')
    fid = (upload_modrinth if p == 'modrinth' else upload_curseforge)(cfg, e, ctx, data)
    ledger[k] = {'file_id': fid, 'jar': os.path.basename(e['jar']),
                 'time': datetime.datetime.now().isoformat(timespec='seconds')}
    write_json(LEDGER, ledger)
    log(f'  ok: {fid}')
    return fid


def selftest():
    import email.parser, email.policy, tempfile
    body, ctype = multipart([('data', None, 'application/json', b'{"a": 1}'), ('file', 'x.jar', 'application/java-archive', b'\x00PK\r\n')])
    msg = email.parser.BytesParser(policy=email.policy.HTTP).parsebytes(f'Content-Type: {ctype}\r\n\r\n'.encode() + body)
    parts = list(msg.iter_parts())
    assert [p.get_param('name', header='content-disposition') for p in parts] == ['data', 'file']
    assert parts[0].get_content() == b'{"a": 1}' and parts[1].get_filename() == 'x.jar'
    assert parts[1].get_content() == b'\x00PK\r\n'
    cfg = read_json(os.path.join(HERE, 'publish.json'))
    kw = dict(mod='BankSystem', mc='1.21.1', modver='2.2.0', mod_version='1.21.1-2.2.0', loader='fabric',
              Loader='Fabric', repo='https://github.com/KROIA/BankSystem', branch='mc_1.21.1')
    assert fmt(cfg['display_name'], **kw) == 'BankSystem 2.2.0 (Fabric 1.21.1)'
    assert fmt(cfg['version_number'], **kw) == '1.21.1-2.2.0-fabric'
    assert fmt(cfg['changelog'], **kw) == ('See the full changelog on GitHub: [v2.2.0]'
                                           '(https://github.com/KROIA/BankSystem/blob/mc_1.21.1/changelog/v2.2.0.md)')
    assert [d.get('modrinth') for d in deps_for(cfg, 'BankSystem', 'neoforge')] == ['architectury-api']
    rel = ['1.20.6', '1.21', '1.21.1', '1.21.2', '1.21.10', '26.1', '26.1.1', '26.1.2', '26.2', '26.2-rc1']
    m = lambda r, loader: matching(intervals(r, loader), rel)
    assert m('1.21.1', 'fabric') == ['1.21.1'] and m('26.1.2', 'fabric') == ['26.1.2']
    assert m('>=1.21.1 <1.21.10', 'fabric') == ['1.21.1', '1.21.2']
    assert m(['1.20.6', '>=26.1 <26.1.2'], 'fabric') == ['1.20.6', '26.1', '26.1.1']
    assert m('[26.1.2]', 'neoforge') == ['26.1.2'] and m('[1.21.1, 1.21.10)', 'neoforge') == ['1.21.1', '1.21.2']
    assert m('[26.1,26.2]', 'neoforge') == ['26.1', '26.1.1', '26.1.2', '26.2']
    assert m('1.19', 'fabric') == []
    for r, loader in (('~1.21', 'fabric'), ('>=1.21', 'fabric'), ('1.21.x', 'fabric'), (['1.21.1', '~1.21'], 'fabric'),
                      ('[1.21.1,)', 'neoforge'), ('(,1.21]', 'neoforge'), ('[1.21,1.22),[26.1,)', 'neoforge')):
        assert intervals(r, loader) is None, r
    e = dict(kw, name='n', changelog='c', deps=deps_for(cfg, 'StockMarket', 'fabric'))
    meta = cf_metadata(dict(cfg, curseforge_manual_release=True), e, [1, 2])
    assert meta['isMarkedForManualRelease'] is True and meta['gameVersions'] == [1, 2]
    assert [r['type'] for r in meta['relations']['projects']] == ['requiredDependency'] * 3
    assert cf_metadata(dict(cfg, curseforge_manual_release=False), e, [])['isMarkedForManualRelease'] is False
    with tempfile.TemporaryDirectory() as tmp:
        inner = io.BytesIO()
        with zipfile.ZipFile(inner, 'w') as z:
            z.writestr('net/kroia/modutilities/sandbox/A.class', b'')
        clean, dirty = os.path.join(tmp, 'clean.jar'), os.path.join(tmp, 'dirty.jar')
        with zipfile.ZipFile(clean, 'w') as z:
            z.writestr('net/kroia/banksystem/Bank.class', b'')
        with zipfile.ZipFile(dirty, 'w') as z:
            z.writestr('net/kroia/banksystem/Bank.class', b'')
            z.writestr('META-INF/jars/modutilities.jar', inner.getvalue())
        assert dev_entries(clean, cfg['dev_packages']) == []
        assert dev_entries(dirty, cfg['dev_packages']) == ['META-INF/jars/modutilities.jar!/net/kroia/modutilities/sandbox/A.class']
        with zipfile.ZipFile(clean, 'a') as z:
            z.writestr('META-INF/neoforge.mods.toml', '[[dependencies.bank]]\nmodId="minecraft"\nversionRange="[1.21.1]"\n')
            z.writestr('fabric.mod.json', '{"depends": {"minecraft": "1.21.1"}}')
        assert mc_range(clean, 'neoforge') == '[1.21.1]' and mc_range(clean, 'fabric') == '1.21.1'
    print('selftest ok')


def main():
    cfg = read_json(os.path.join(HERE, 'publish.json'))
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--mods', nargs='+', default=list(cfg['mods']), choices=list(cfg['mods']))
    ap.add_argument('--versions', nargs='+', default=cfg['versions'])
    ap.add_argument('--loaders', nargs='+', default=list(cfg['loaders']), choices=list(cfg['loaders']))
    ap.add_argument('--platforms', nargs='+', default=['modrinth', 'curseforge'], choices=['modrinth', 'curseforge'])
    ap.add_argument('--upload', action='store_true', help='really upload (default: dry run)')
    ap.add_argument('--selftest', action='store_true')
    args = ap.parse_args()
    if args.selftest:
        return selftest()
    ledger = read_json(LEDGER, {})
    entries = plan(cfg, args)
    check_games(entries)
    if show(cfg, entries, args.platforms, ledger):
        args.upload and die('fix the problems above (or narrow --mods/--versions/--loaders) before uploading')
    if not args.upload:
        return print('dry run; pass --upload to publish')
    ctx = preflight(cfg, entries, args.platforms, args.mods)
    for e in entries:
        for p in args.platforms:
            if key(e, p) not in ledger:
                upload_one(cfg, e, p, ctx, ledger, lambda m: print(m, flush=True))


if __name__ == '__main__':
    main()
