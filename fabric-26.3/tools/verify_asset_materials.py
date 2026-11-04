import json,zipfile,pathlib,struct,zlib
import sys
vanilla=zipfile.ZipFile(sys.argv[1])
files={n:vanilla.read(n) for n in vanilla.namelist() if n.startswith('assets/') and n.endswith(('.json','.png'))}
custom=[]
for root in map(pathlib.Path,sys.argv[2:]):
 for p in (root/'assets').rglob('*'):
  if p.is_file() and p.suffix in ['.json','.png']:
   name=str(p.relative_to(root));files[name]=p.read_bytes();custom.append(name)
errors=[];checked=set()
def path(id,folder,ext):
 ns,name=id.split(':',1) if ':' in id else ('minecraft',id)
 return f'assets/{ns}/{folder}/{name}.{ext}'
def model(n,chain=()):
 if n not in files:raise ValueError('missing model '+n)
 if n in chain:raise ValueError('cyclic model '+n)
 d=json.loads(files[n]);parent=d.get('parent');p={}
 if parent and not parent.startswith(('builtin/','minecraft:builtin/')):p=model(path(parent,'models','json'),chain+(n,))
 return {**p,**d,'textures':{**p.get('textures',{}),**d.get('textures',{})}}
def texture(t,links,seen=()):
 if isinstance(t,dict):t=t.get('sprite',t.get('texture'))
 if not isinstance(t,str):raise ValueError('invalid material '+str(t))
 if t.startswith('#'):
  if t in seen:raise ValueError('cyclic texture '+t)
  if t[1:] not in links:raise ValueError('unbound texture '+t)
  return texture(links[t[1:]],links,seen+(t,))
 p=path(t,'textures','png')
 if p not in files:raise ValueError('missing texture '+p)
 if files[p][:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('bad PNG '+p)
 checked.add(p)
roots=set()
def collect(value):
 if isinstance(value,dict):
  if isinstance(value.get('model'),str):roots.add(path(value['model'],'models','json'))
  for v in value.values():collect(v)
 elif isinstance(value,list):
  for v in value:collect(v)
for n in custom:
 if ('/blockstates/' in n or '/items/' in n) and n.endswith('.json'):collect(json.loads(files[n]))
# Balm registers these render-only models independently of blockstate/item definitions.
for n in custom:
 if '/models/block/' in n and n.endswith('.json') and n.rsplit('/',1)[-1] in [x+'_mesh.json' for x in ['string','flint','copper','iron','gold','diamond','emerald','netherite']]+['wooden_crucible_liquid.json']:roots.add(n)
for n in sorted(roots):
 try:
  d=model(n);links=d.get('textures',{})
  for e in d.get('elements',[]):
   for f in e.get('faces',{}).values():texture(f['texture'],links)
  for t in links.values():texture(t,links)
 except Exception as e:errors.append((n,str(e)))
print('Concrete models checked',len(roots),'resolved textures checked',len(checked),'errors',len(errors))
for e in errors:print(*e,sep=': ')
if errors:raise SystemExit(1)
