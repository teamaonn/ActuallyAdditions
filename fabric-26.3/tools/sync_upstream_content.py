"""Copy upstream resources only for implemented Fabric content; never register placeholders."""
from pathlib import Path
import json,re,shutil
module=Path(__file__).resolve().parents[1];root=module.parent;resources=module/'src/main/resources'
items_source=(root/'src/main/java/de/ellpeck/actuallyadditions/mod/items/ActuallyItems.java').read_text()
items=[]
for line in items_source.splitlines():
 if line.strip().startswith('//'):continue
 if re.search(r'DeferredItem<(?:ItemBase|ItemCrystal)>',line):
  match=re.search(r'ITEMS.register\("([^"]+)"',line)
  if match:items.append(match[1])
blocks_source=(root/'src/main/java/de/ellpeck/actuallyadditions/mod/blocks/ActuallyBlocks.java').read_text()
blocks=[]
for name,kind in re.findall(r'new AABlockReg<>\("([^"]+)", \(\) -> new (ActuallyBlock|SlabBlock|StairBlock|WallBlock)\(',blocks_source):
 blocks.append({'id':name,'kind':{'ActuallyBlock':'block','SlabBlock':'slab','StairBlock':'stairs','WallBlock':'wall'}[kind]})
for crystal in ['restonia','palis','diamatine','void','emeradic','enori']:
 for prefix in ['', 'empowered_']:blocks.append({'id':prefix+crystal+'_crystal_block','kind':'block'})
blocks.append({'id':'black_quartz_ore','kind':'block'})
blocks=list({b['id']:b for b in blocks}.values())
(resources/'aa_content.json').write_text(json.dumps({'items':items,'blocks':blocks},indent=2)+'\n')
machines=['atomic_reconstructor','empowerer','display_stand','powered_furnace','crusher','crusher_double','energizer','enervator','battery_box','breaker','placer','dropper','long_range_breaker','heat_collector','leaf_generator','bio_reactor','canola_press','fermenting_barrel','oil_generator','feeder','xp_solidifier','farmer','ranged_collector','fluid_collector','fluid_placer','lava_factory_controller','vertical_digger','phantom_itemface','phantom_energyface','phantom_liquiface','phantom_redstoneface','phantom_breaker','phantom_placer','coffee_machine']
power=['coal_generator','laser_relay','laser_relay_advanced','laser_relay_extreme','laser_wrench','laser_upgrade_range']
batteries={prefix+'_battery' for prefix in ['single','double','triple','quadruple','quintuple']}
oilnames={"canola_oil","refined_canola_oil","crystallized_oil","empowered_oil"}
crops={"canola","rice","flax","coffee"}
seeds={"canola_seeds","rice_seeds","flax_seeds","coffee_beans","crystallized_canola_seed","empowered_canola_seed"}
decor={"phantom_booster"}|{"lamp_"+n for n in ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]}|{"greenhouse_glass"}
utilities={n+"_aiot" for n in ["wooden","stone","iron","gold","diamond","netherite"]}|{"crafter_on_a_stick","teleport_staff"}
drills={"drill_"+n for n in ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]}|{"drill_upgrade_"+n for n in ["speed","speed_ii","speed_iii","silk_touch","fortune","fortune_ii","three_by_three","five_by_five","block_placing"]}|{"travelers_sack"}
lenses={"lens_of_"+n for n in ["color","the_miner","certain_death","the_killer","detonation","disenchanting"]}
known={"coffee_cup"}|lenses|drills|utilities|{"phantom_connector","filter"}|decor|{"solidified_experience"}|seeds|{n+"_bucket" for n in oilnames}|batteries|set(items)|{b['id'] for b in blocks}|set(machines)|set(power)
assets=resources/'assets/actuallyadditions';generated=root/'src/generated/resources';main=root/'src/main/resources'
def write(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2).replace('c:slimeballs','c:slime_balls')+'\n')
def copy(rel):
 for source in [main/rel,generated/rel]:
  if source.is_file():
   target=resources/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target);return target
 return None
seen=set()
def model(identifier):
 if not identifier.startswith('actuallyadditions:') or identifier in seen:return
 seen.add(identifier);name=identifier.split(':',1)[1];target=copy('assets/actuallyadditions/models/'+name+'.json')
 if target is None:raise RuntimeError('Missing model '+identifier)
 data=json.loads(target.read_text())
 if 'parent' in data:model(data['parent'])
 for texture in data.get('textures',{}).values():
  if texture.startswith('actuallyadditions:'):
   rel='assets/actuallyadditions/textures/'+texture.split(':')[1]+'.png'
   if copy(rel) is None:raise RuntimeError('Missing texture '+rel)
   copy(rel+'.mcmeta')
for name in sorted(known-set(power)):
 source=next((p for p in [main/f'assets/actuallyadditions/models/item/{name}.json',generated/f'assets/actuallyadditions/models/item/{name}.json'] if p.exists()),None)
 if source is None:raise RuntimeError('No item model '+name)
 model('actuallyadditions:item/'+name)
 write(assets/f'items/{name}.json',{'model':{'type':'minecraft:model','model':'actuallyadditions:item/'+name}})
for name in [b['id'] for b in blocks]+machines+sorted(crops|decor):
 state=generated/f'assets/actuallyadditions/blockstates/{name}.json'
 if not state.exists():raise RuntimeError('Missing blockstate '+name)
 data=json.loads(state.read_text());text=json.dumps(data).replace('lit=', 'active=')
 data=json.loads(text)
 def visit(obj):
  if isinstance(obj,dict):
   if isinstance(obj.get('model'),str):model(obj['model'])
   for v in obj.values():visit(v)
  elif isinstance(obj,list):
   for v in obj:visit(v)
 visit(data);write(assets/f'blockstates/{name}.json',data)
 write(resources/f'data/actuallyadditions/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'actuallyadditions:'+name}]}]})
# Restore the real upstream crafting recipes when all referenced items are implemented.
def refs(value):
 if isinstance(value,str):return {value.split(':',1)[1]} if value.startswith('actuallyadditions:') else set()
 if isinstance(value,list):return set().union(*(refs(x) for x in value)) if value else set()
 if isinstance(value,dict):return set().union(*(refs(v) for k,v in value.items() if k not in ['type','tag','neoforge:conditions'])) if value else set()
 return set()
def ingredient(v):
 if isinstance(v,dict):
  if v.get('type')=='actuallyadditions:target_nbt':return ingredient(v['base'])
  if set(v)=={'item'}:return v['item']
  if set(v)=={'tag'}:return '#'+v['tag']
  return {k:ingredient(x) for k,x in v.items()}
 if isinstance(v,list):return [ingredient(x) for x in v]
 return v
counts={};skipped=[]
for p in sorted((generated/'data/actuallyadditions/recipe').rglob('*.json')):
 data=json.loads(p.read_text());typ=data['type'];rel=p.relative_to(generated/'data/actuallyadditions/recipe')
 if not refs(data)<=known|oilnames:skipped.append(str(rel));continue
 if typ in ['actuallyadditions:laser','actuallyadditions:crushing','actuallyadditions:empowering','actuallyadditions:pressing','actuallyadditions:fermenting','actuallyadditions:liquid_fuel','actuallyadditions:mining_lens','actuallyadditions:color_change','actuallyadditions:coffee_ingredient']:
  write(resources/'data/actuallyadditions/aa_processing'/rel,data)
 elif typ in ['minecraft:crafting_shaped','minecraft:crafting_shapeless','minecraft:smelting','minecraft:blasting','minecraft:stonecutting','actuallyadditions:copy_nbt','actuallyadditions:copy_nbt_shapeless']:
  write(resources/'data/actuallyadditions/recipe'/rel,ingredient(data))
 else:continue
 counts[typ]=counts.get(typ,0)+1
# Preserve upstream tag families, pruning unavailable entries rather than invalidating tags.
for p in (generated/'data').rglob('*.json'):
 if '/tags/item/' not in str(p):continue
 data=json.loads(p.read_text());values=[]
 for v in data.get('values',[]):
  name=v if isinstance(v,str) else v.get('id','')
  if name.startswith('actuallyadditions:') and name.split(':',1)[1] not in known:continue
  if name.startswith('#'):values.append({'id':name,'required':False})
  else:values.append(v)
 if values:write(resources/'data'/p.relative_to(generated/'data'),{'replace':False,'values':values})
lang=json.loads((main/'assets/actuallyadditions/lang/en_us.json').read_text())
lang={k:v for k,v in lang.items() if k.startswith(('block.actuallyadditions.','item.actuallyadditions.')) and k.split('.',2)[2] in known}
langpath=assets/'lang/en_us.json';existing=json.loads(langpath.read_text()) if langpath.exists() else {};existing.update(lang);write(langpath,existing)
# Keep all implemented block families mineable with a pickaxe.
write(resources/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['actuallyadditions:'+n for n in sorted({b['id'] for b in blocks}|set(machines)|set(power[:4]))]})
for kind in ['configured_feature','placed_feature']:
 target=copy(f'data/actuallyadditions/worldgen/{kind}/ore_black_quartz.json')
 if kind=='configured_feature':
  data=json.loads(target.read_text());data={'type':'minecraft:ore',**data['config']};data['targets']=[{**x,'state':x['state']['Name']} for x in data['targets']];write(resources/'data/actuallyadditions/worldgen/feature/ore_black_quartz.json',data);target.unlink()
copy('data/actuallyadditions/loot_table/blocks/black_quartz_ore.json')
write(module/'UPSTREAM_CONTENT_STATUS.json',{'implemented_items':sorted(known),'copied_recipes':counts,'recipes_waiting_for_other_features':skipped})
print(len(known),'registered item IDs;',counts)

for name in sorted(oilnames):
 for suffix in ['still','flowing']:
  copy(f'assets/actuallyadditions/textures/block/fluid/{name}_{suffix}.png');copy(f'assets/actuallyadditions/textures/block/fluid/{name}_{suffix}.png.mcmeta')
 write(assets/f'blockstates/{name}.json',{'variants':{'':{'model':'minecraft:block/water'}}})
 write(assets/f'lang/en_us.json',{**json.loads((assets/'lang/en_us.json').read_text()),'block.actuallyadditions.'+name:name.replace('_',' ').title()})

for name in sorted(oilnames):
 texture='item_bucket_canola_oil' if name=='canola_oil' else 'item_bucket_oil'
 copy(f'assets/actuallyadditions/textures/item/{texture}.png')
 write(assets/f'models/item/{name}_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':'actuallyadditions:item/'+texture}})

for name in crops:
 copy(f'data/actuallyadditions/loot_table/blocks/{name}.json')
write(resources/'data/minecraft/tags/block/crops.json',{'replace':False,'values':['actuallyadditions:'+n for n in sorted(crops)]})

write(resources/'data/actuallyadditions/tags/block/mineable/aio.json',{'replace':False,'values':['#minecraft:mineable/pickaxe','#minecraft:mineable/axe','#minecraft:mineable/shovel','#minecraft:mineable/hoe']})

write(resources/'data/actuallyadditions/tags/block/mineable/drill.json',{'replace':False,'values':['#minecraft:mineable/pickaxe','#minecraft:mineable/shovel']})
