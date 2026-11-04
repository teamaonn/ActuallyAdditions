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
machines=re.findall(r'(?m)^\s+[A-Z_]+\("([^\"]+)",',(module/'src/main/java/de/ellpeck/actuallyadditions/fabric/content/WorkKind.java').read_text())
power=['coal_generator','laser_relay','laser_relay_advanced','laser_relay_extreme','laser_wrench','laser_upgrade_range']
batteries={prefix+'_battery' for prefix in ['single','double','triple','quadruple','quintuple']}
oilnames={"canola_oil","refined_canola_oil","crystallized_oil","empowered_oil"}
crops={"canola","rice","flax","coffee"}
seeds={"canola_seeds","rice_seeds","flax_seeds","coffee_beans","crystallized_canola_seed","empowered_canola_seed"}
decor={"phantom_booster"}|{"lamp_"+n for n in ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]}|{"greenhouse_glass"}
utilities={n+"_aiot" for n in ["wooden","stone","iron","gold","diamond","netherite"]}|{"crafter_on_a_stick","teleport_staff"}
drills={"drill_"+n for n in ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]}|{"drill_upgrade_"+n for n in ["speed","speed_ii","speed_iii","silk_touch","fortune","fortune_ii","three_by_three","five_by_five","block_placing"]}|{"travelers_sack"}
lenses={"lens_of_"+n for n in ["color","the_miner","certain_death","the_killer","detonation","disenchanting"]}
clusters={n+'_crystal_cluster' for n in ['restonia','palis','diamatine','void','emeradic','enori']}
decor|=clusters|{'tiny_torch'}
known={"coffee_cup"}|lenses|drills|utilities|{"phantom_connector","filter"}|decor|{"solidified_experience"}|seeds|{n+"_bucket" for n in oilnames}|batteries|set(items)|{b['id'] for b in blocks}|set(machines)|set(power)
known|={'handheld_filler','water_bowl','worm','booklet'}
known|=set(re.findall(r'Content.item\("([^\"]+)"',(module/'src/main/java/de/ellpeck/actuallyadditions/fabric/content/Accessories.java').read_text()))|{'drill_pattern','leaf_blo_pattern','phan_con_pattern','book_pattern'}

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
  if v.get('type')=='neoforge:components':
   components={}
   for key,value in v.get('components',{}).items():
    if key in ['minecraft:enchantments','minecraft:stored_enchantments'] and value.get('levels'):components[key]=value['levels']
   return {'fabric:type':'fabric:components','base':ingredient(v['items']),'components':components}
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
write(resources/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['actuallyadditions:'+n for n in sorted({b['id'] for b in blocks}|set(machines)|decor|set(power[:4]))]})
for kind in ['configured_feature','placed_feature']:
 target=copy(f'data/actuallyadditions/worldgen/{kind}/ore_black_quartz.json')
 if kind=='configured_feature':
  data=json.loads(target.read_text());data={'type':'minecraft:ore',**data['config']};data['targets']=[{**x,'state':x['state']['Name']} for x in data['targets']];write(resources/'data/actuallyadditions/worldgen/feature/ore_black_quartz.json',data);target.unlink()
copy('data/actuallyadditions/loot_table/blocks/black_quartz_ore.json')
statuspath=module/'UPSTREAM_CONTENT_STATUS.json';status=json.loads(statuspath.read_text()) if statuspath.exists() else {};status.update({'implemented_items':sorted(known),'copied_recipes':counts,'recipes_waiting_for_other_features':skipped});write(statuspath,status)
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

for name in ['drill','leaf_blo','phan_con','book']:
 copy(f'data/actuallyadditions/banner_pattern/{name}.json')
 tag='leaf_blower' if name=='leaf_blo' else name
 copy(f'data/actuallyadditions/tags/banner_pattern/pattern_item/{tag}.json')
 copy(f'assets/actuallyadditions/textures/entity/banner/{name}.png');copy(f'assets/actuallyadditions/textures/entity/shield/{name}.png')
write(assets/'equipment/goggles.json',{'layers':{'humanoid':[{'texture':'actuallyadditions:goggles'}]}})
p=assets/'textures/entity/equipment/humanoid/goggles.png';p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(main/'assets/actuallyadditions/textures/models/armor/goggles_layer_1.png',p)

for name in clusters:copy(f'data/actuallyadditions/loot_table/blocks/{name}.json')
for block in blocks:copy(f'data/actuallyadditions/loot_table/blocks/{block["id"]}.json')

original_lang=json.loads((main/'assets/actuallyadditions/lang/en_us.json').read_text())
chapters=[{'title':'Fabric controls','text':'Link two relays with the Laser Linking Wrench. Sneak-use the wrench to remove links. Open a relay to configure mode and priority. Advanced item relays accept two reusable filters: input and output. Item Interfaces expose adjacent item relay networks; Hopping Interfaces move four items every ten ticks. Fluid relays in input mode pull from attached tanks.\n\nInstall range upgrades on both ends for connections beyond 15 blocks. Invisibility upgrades hide beams; goggles or a held wrench reveal them.\n\nBind Phantomfaces with the Phantom Connector. Boosters above supported machines double their range, up to three boosters. Player Interfaces bind to their placer or to a player selected with the Player Probe.\n\nOpen a drill to install upgrades. Sneaking disables area mining. Use the placing upgrade to choose a hotbar slot. Sneak-use batteries to toggle inventory charging.\n\nSneak-select a filler material, then select two corners and hold the filler. The Coffee Machine takes beans, empty cups, water and ingredients; press Start. Item Filters use ghost slots without consuming examples. The Item Tag selects tags from an item in your other hand.'}]
for key,value in original_lang.items():
 if not key.startswith('booklet.actuallyadditions.chapter.') or '.text.' in key:continue
 texts=[(k,v) for k,v in original_lang.items() if k.startswith(key+'.text.')]
 if not texts:continue
 text='\n\n'.join(v for k,v in sorted(texts,key=lambda x:int(x[0].rsplit('.',1)[1])))
 chapters.append({'title':value,'text':re.sub(r'<[^>]*>','',text.replace('<n>','\n'))})
write(assets/'guide.json',chapters)

# Random patches became a single-block feature plus placement modifiers in 26.3.
for name in sorted(crops):
 patch=name+'_patch'
 write(resources/f'data/actuallyadditions/worldgen/feature/{patch}.json',{'type':'minecraft:simple_block','to_place':{'id':'actuallyadditions:'+name,'properties':{'age':'7','persistent':'true'}}})
 placement=json.loads((generated/f'data/actuallyadditions/worldgen/placed_feature/{patch}.json').read_text())
 placement['placement'] += [{'type':'minecraft:count','count':10},{'type':'minecraft:offset',**{axis:{'type':'minecraft:trapezoid','min':-spread,'max':spread,'plateau':0} for axis,spread in [('x',7),('y',3),('z',7)]}},{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_block_tag','tag':'minecraft:air'}}]
 write(resources/f'data/actuallyadditions/worldgen/placed_feature/{patch}.json',placement)

# Native 26.3 loot uses composed predicates/modifiers; the old fields are ignored.
def loot_native(v):
 if isinstance(v,list):return [loot_native(x) for x in v]
 if not isinstance(v,dict):return int(v) if isinstance(v,float) and v.is_integer() else v
 out={}
 for k,x in v.items():
  if k=='functions':out['modifier']=loot_native(x)
  elif k=='conditions':out['condition']={'type':'minecraft:all_of','terms':loot_native(x)} if len(x)>1 else loot_native(x[0]) if x else []
  elif k in ['function','condition']:out['type']=x
  elif k not in ['bonus_rolls']:out[k]=loot_native(x)
 if out.get('type')=='minecraft:tag':out['items']='#'+out.pop('name')
 if out.get('type')=='minecraft:block_state_property':
  out['type']='minecraft:match_block';out['blocks']=out.pop('block');out['state']=out.pop('properties',{})
 return out
copy('data/actuallyadditions/loot_table/engineer_house.json')
for p in (resources/'data/actuallyadditions/loot_table').rglob('*.json'):
 data=loot_native(json.loads(p.read_text()))
 # Native slab doubles, which were missing from the upstream generated tables.
 name=p.stem
 if name.endswith('_slab'):
  data['pools'][0]['entries'][0]['modifier']={'type':'minecraft:set_count','count':2,'condition':{'type':'minecraft:match_block','blocks':'actuallyadditions:'+name,'state':{'type':'double'}}}
 write(p,data)
# Structure palette states are upgraded by Minecraft's structure data fixer.
structure=copy('data/actuallyadditions/structure/andrew_period_house.nbt')
import nbtlib
nbt=nbtlib.load(structure)
for state in nbt['palette']:
 props=state.get('Properties',{})
 if 'lit' in props and str(state.get('Name','')).startswith('actuallyadditions:'):props['active']=props.pop('lit')
for block in nbt['blocks']:
 data=block.get('nbt');name=str(data.get('id','')) if data is not None else ''
 if not name.startswith('actuallyadditions:'):continue
 kind=name.split(':',1)[1]
 target='coal_generator' if kind=='coal_generator' else 'energy_laser_relay' if kind.startswith('laser_relay') else 'work_machine'
 block['nbt']=nbtlib.Compound({'id':nbtlib.String('actuallyadditions:'+target),'energy':nbtlib.Int(int(data.get('Energy',0)))})
nbt.save(structure)
data=json.loads((generated/'data/actuallyadditions/worldgen/processor_list/engineer_house.json').read_text())
for processor in data['processors']:
 for rule in processor.get('rules',[]):
  old=rule['output_state'];rule['output_state']={'id':old['Name'],'properties':{'active':old.get('Properties',{}).get('lit','false')}}
write(resources/'data/actuallyadditions/worldgen/processor_list/engineer_house.json',data)
copy('assets/actuallyadditions/textures/entity/villager/profession/engineer.png')
copy('assets/actuallyadditions/sounds.json')
for p in (main/'assets/actuallyadditions/sounds').glob('*.ogg'):copy('assets/actuallyadditions/sounds/'+p.name)
langpath=assets/'lang/en_us.json';write(langpath,{**json.loads(langpath.read_text()),'entity.minecraft.villager.actuallyadditions.engineer':'Engineer'})

def count(a,b):return a if a==b else {'type':'minecraft:uniform','min':a,'max':b}
trade_data=[
 (1,False,'black_quartz_ore',1,2,2,3),(1,False,'black_quartz',1,2,6,8),(1,False,'laser_wrench',1,3,1,1),
 (2,True,'coffee_beans',20,30,1,2),(2,False,'item_interface',3,5,1,1),(2,False,'laser_relay',10,20,1,2),
 (3,True,'tiny_torch',30,40,1,2),(3,False,'wood_casing',1,2,1,2),
 (4,False,'iron_casing',3,5,1,2),(4,True,'empowerer',1,1,15,20),(4,False,'laser_relay_extreme',30,40,1,1)]
trade_ids={i:[] for i in range(1,6)}
for level,buy,item,a,b,c,d in trade_data:
 name=f'engineer/{level}/{item}';trade_ids[level].append('actuallyadditions:'+name)
 data={'wants':{'id':'actuallyadditions:'+item if buy else 'minecraft:emerald','count':count(a,b)},'gives':{'id':'minecraft:emerald' if buy else 'actuallyadditions:'+item},'max_uses':16,'xp':2 if level==1 else 10,'reputation_discount':.05,'given_item_modifier':{'type':'minecraft:set_count','count':count(c,d)}}
 write(resources/f'data/actuallyadditions/villager_trade/{name}.json',data)
for level,ids in trade_ids.items():write(resources/f'data/actuallyadditions/trade_set/engineer/level_{level}.json',{'amount':min(2,len(ids)),'trades':ids})

# Shared Fabric machine state supports all six facings; fill vertical rotations
# for upstream models whose original block allowed only horizontal placement.
for name in list(machines)+power[:4]:
 p=assets/f'blockstates/{name}.json'
 if not p.exists():continue
 data=json.loads(p.read_text());variants=data.get('variants',{})
 for key,value in list(variants.items()):
  if 'facing=north' not in key:continue
  for facing,x in [('up',270),('down',90)]:
   target=key.replace('facing=north','facing='+facing)
   if target in variants:continue
   def rotated(model):return {**model,'x':(model.get('x',0)+x)%360}
   variants[target]=[rotated(m) for m in value] if isinstance(value,list) else rotated(value)
 write(p,data)

# Blockbench's unassigned #missing faces need a valid native casing material.
for p in (assets/'models').rglob('*.json'):
 data=json.loads(p.read_text());faces=[face for element in data.get('elements',[]) for face in element.get('faces',{}).values()]
 if any(face.get('texture')=='#missing' for face in faces):
  textures=data.setdefault('textures',{});fallback=next((name for name in ['base','side','particle'] if name in textures),None)
  if fallback is None:raise RuntimeError('No casing material for '+str(p))
  raw=p.read_text();raw=re.sub(r'(\"textures\"\s*:\s*\{)(\s*)',lambda m:m[1]+m[2]+'\"missing\": '+json.dumps('#'+fallback)+','+m[2],raw,count=1);p.write_text(raw)
