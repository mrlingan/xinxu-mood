from pathlib import Path
import json
p=Path('design'); stroke='#52665C';sage='#C9DFCD';peach='#F1C5B6';butter='#F4DB9F';white='#FFFCF5'
def svg(body,w=64,h=64):return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><g stroke="{stroke}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">{body}</g></svg>'
def path(d,fill='none'):return f'<path d="{d}" fill="{fill}"/>'
def circle(cx,cy,r,fill):return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}"/>'
def eyes(x=25,y=30):return path(f'M{x} {y}v2m14 -2v2')
cloud=path('M17 45C4 45 5 29 16 28C14 12 39 10 44 25C59 22 63 44 48 45Z',white)
leaf=path('M13 46C9 22 29 14 51 14C54 37 39 52 19 49Z',sage)+path('M18 47L43 23')
sun=circle(32,32,17,butter)+path('M32 7V3m0 58v-4M7 32H3m58 0h-4M14 14l-3 -3m42 42l-3 -3M14 50l-3 3m42 -42l-3 3')+eyes(25,29)+path('M27 38q5 5 10 0')
heart=path('M32 52C23 44 6 32 12 20C17 10 28 14 32 21C37 9 54 12 55 25C56 37 40 47 32 52Z',peach)
icons={
'home':path('M9 29L32 10L55 29V51Q55 56 50 56H14Q9 56 9 51Z',sage)+path('M25 56V39Q32 33 39 39V56')+path('M27 24q5 5 10 0'),
'diary':path('M17 9H45Q52 9 52 16V52Q52 56 47 56H17Q11 56 11 50V15Q11 9 17 9Z',white)+path('M21 10V55M28 22H43M28 29H39')+path('M33 44C20 36 33 31 33 38C35 30 49 35 33 44Z',peach),
'insights':path('M11 53H55M16 45V35M30 45V26M44 45V17')+path('M13 23Q26 10 42 12L53 7')+path('M45 7H53V15'),
'profile':path('M17 20C5 21 8 6 18 10L23 15M41 15C49 3 61 17 50 23',butter)+path('M12 35C12 12 52 12 52 35V46Q52 57 32 57Q12 57 12 46Z',white)+eyes(24,30)+path('M29 40q3 3 6 0M32 40v5')+circle(20,39,3,peach)+circle(44,39,3,peach),
'mood_1':cloud+eyes(24,30)+path('M26 41q6 -5 12 0')+path('M42 42q-7 8 0 9q7 -1 0 -9',sage),
'mood_2':cloud+eyes(24,29)+path('M27 40q5 -4 10 0'),
'mood_3':leaf+path('M25 31v2m12 -8v2M28 39q4 3 8 -1'),
'mood_4':sun,
'mood_5':heart+eyes(25,28)+path('M27 37q5 7 10 0')+circle(20,35,3,white)+circle(44,35,3,white),
'happy':sun,'calm':leaf+eyes(24,29)+path('M27 39q5 4 10 -1'),
'sad':cloud+eyes(24,29)+path('M27 40q5 -4 10 0'),
'anxious':cloud+path('M23 30q4 -6 8 0m4 0q4 -6 8 0M25 40q6 -4 13 0M17 10l3 -5m28 8l4 -4'),
'angry':path('M31 7C34 22 49 20 50 37C51 60 12 61 12 39C12 29 22 25 23 18L28 27Q34 22 31 7Z',peach)+path('M23 34l5 2m13 -2l-5 2M27 45q5 -4 10 0'),
'tired':path('M44 9C13 11 7 47 30 54C43 58 54 49 55 38C35 49 25 21 44 9Z',butter)+path('M25 35l5 2m8 0l5 -2M30 44h6'),
'hope':path('M31 55V27M31 38C10 39 10 18 12 15C29 15 35 24 31 38Z',sage)+path('M32 31C51 34 54 16 52 11C37 11 29 20 32 31Z',sage)+path('M23 55H40'),
'lonely':circle(32,31,17,butter)+path('M7 44C-1 29 54 7 59 20C65 35 9 56 7 44')+eyes(24,26)+path('M27 36h9'),
'breath':cloud+path('M5 51H41q10 0 9 -6M17 58H52q10 0 8 -9'),
'sort':path('M8 20L32 8L56 20V48Q56 53 50 53H14Q8 53 8 48Z',white)+path('M9 23L32 40L55 23')+path('M32 34C15 23 28 16 32 24C36 14 50 23 32 34Z',peach),
'pause':path('M13 18H45V41Q45 53 29 53Q13 53 13 41Z',butter)+path('M45 24C61 19 62 43 45 41M7 57H52M25 12q-5 -4 0 -9m10 9q-5 -4 0 -9'),
'photo':path('M12 10H52Q57 10 57 15V49Q57 54 52 54H12Q7 54 7 49V15Q7 10 12 10Z',white)+circle(42,23,6,butter)+path('M8 45L23 29L38 46L47 36L56 45',sage),
'live':circle(32,32,25,sage)+circle(32,32,18,white)+path('M27 22L43 32L27 42Z',peach)+path('M52 8l4 -4M9 9L5 5'),
'video':path('M9 17H36Q42 17 42 23V44Q42 49 36 49H9Q4 49 4 43V23Q4 17 9 17Z',sage)+path('M42 26L59 17V49L42 40Z',butter),
'close':path('M18 18L46 46M46 18L18 46'),
'plus':path('M32 13V51M13 32H51'),
'arrow':path('M13 32H51M36 17L51 32L36 47'),
'play':circle(32,32,25,white)+path('M26 20L44 32L26 44Z',sage),
'lock':path('M20 26V18C20 1 44 1 44 18V26')+path('M16 26H48Q53 26 53 31V50Q53 56 47 56H17Q11 56 11 50V31Q11 26 16 26Z',sage)+circle(32,39,3,white)+path('M32 42V47'),
'spark':path('M32 7Q33 28 54 32Q33 35 32 57Q29 35 8 32Q29 28 32 7Z',butter),
'check':path('M15 33L27 45L51 18'),
'gallery':path('M15 9H53V45M7 18H46V55H7Z',white)+circle(33,29,5,butter)+path('M8 48L19 35L30 47L37 40L45 48',sage)
}
hero=svg(path('M35 211Q85 198 145 212T285 212',sage)+path('M128 69C98 1 77 27 101 88M153 68C159 2 192 14 178 88',white)+path('M76 129C67 58 200 45 211 123C237 133 231 193 193 197H103C54 198 42 151 76 129Z',white)+path('M120 118v5m48 -5v5M134 137q11 9 21 -1')+circle(105,135,10,peach)+circle(185,133,10,peach)+path('M42 95q16 -8 28 0M234 80q17 -8 28 0M239 88q9 -4 17 0')+path('M210 181q28 -35 54 -6q-12 36 -42 25Z',sage)+path('M219 198L253 179'),320,240)
assets=[{'name':'xx_'+k,'svg':svg(v),'label':k} for k,v in icons.items()]+[{'name':'xx_friend','svg':hero,'label':'微风兔 / Breeze Friend'}]
(p/'icon-specs.json').write_text(json.dumps(assets,ensure_ascii=False,indent=2))
root=Path.cwd();script='''const sketch = require('sketch');
const doc = sketch.getSelectedDocument();
const page = doc.selectedPage; page.name = '心绪 / Breeze Garden';
const board = new sketch.Artboard({name:'01 · 可爱治愈图标系统', parent:page,frame:{x:0,y:0,width:1120,height:1040},background:{enabled:true,color:'#FAF7F0'}});
new sketch.Text({parent:board,text:'心绪 · 微风花园',frame:{x:48,y:28,width:900,height:60},style:{fontSize:36,fontWeight:7,textColor:'#52665C'}});
new sketch.Text({parent:board,text:'柔软线条 / 奶油白 / 鼠尾草绿 / 杏桃粉  ·  原创图标',frame:{x:48,y:92,width:1000,height:36},style:{fontSize:18,textColor:'#849184'}});
'''
script+='const assets = '+json.dumps(assets,ensure_ascii=False)+';\n'
script+='''assets.forEach((asset,i)=>{
 const group=sketch.createLayerFromData(asset.svg,'svg');
 group.name=asset.name;group.parent=board;
 group.frame.x=52+(i%8)*134;group.frame.y=160+Math.floor(i/8)*155;
 sketch.export(group,{formats:'png',scales:'2',output:OUTPUT,filename:asset.name+'.png',overwriting:true,trimmed:false});
 sketch.export(group,{formats:'svg',output:OUTPUT,filename:asset.name+'.svg',overwriting:true});
 new sketch.Text({parent:board,text:asset.label,frame:{x:48+(i%8)*134,y:235+Math.floor(i/8)*155,width:128,height:28},style:{fontSize:12,textColor:'#52665C'}});
});
sketch.export(board,{formats:'png',output:OUTPUT,filename:'design-board.png',overwriting:true});
doc.save(DOCUMENT,err=>console.log(err?String(err):'Saved Breeze Garden design + all exports'));
'''.replace('OUTPUT',json.dumps(str(root/'design/exports'))).replace('DOCUMENT',json.dumps(str(root/'design/心绪-微风花园.sketch')))
(p/'sketch-create.js').write_text(script)
print(len(assets),'original icon assets ready for native Sketch creation')
