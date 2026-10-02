// Emits reviewed SVG preview assets as JSON; callers apply them as a patch.
// Usage: node scripts/generate-animated-icon-previews.mjs <pinned Flutter SDK>
import fs from 'node:fs';
import path from 'node:path';
const sdk=process.argv[2];
if(!sdk)throw Error('Provide the pinned Flutter SDK path.');
const dir=path.join(sdk,'packages/flutter/lib/src/material/animated_icons/data');
const names=["add_event","arrow_menu","close_menu","ellipsis_search","event_add","home_menu","list_view","menu_arrow","menu_close","menu_home","pause_play","play_pause","search_ellipsis","view_list"];
function balanced(s,open){let depth=0;for(let i=open;i<s.length;i++){if(s[i]==='(')depth++;if(s[i]===')'&&--depth===0)return [s.slice(open+1,i),i+1];}throw Error('Unclosed vector data');}
function calls(s,name){const out=[];let i=0;while((i=s.indexOf(name+'(',i))>=0){const [body,end]=balanced(s,i+name.length);out.push(body);i=end;}return out;}
function number(v){if(!Number.isFinite(v))throw Error('Nonfinite vector');return String(Math.round(v*1000000)/1000000);}
function lerp(a,p){const n=p*(a.length-1),lo=Math.floor(n),hi=Math.ceil(n);return a[lo]+(a[hi]-a[lo])*(n-lo);}
function points(s){return calls(s,'Offset').map(v=>v.split(',').map(Number));}
function vector(arr,p){return [lerp(arr.map(x=>x[0]),p),lerp(arr.map(x=>x[1]),p)].map(number).join(' ');}
const result={};
for(const name of names){
 const source=fs.readFileSync(path.join(dir,name+'.g.dart'),'utf8');
 const size=/Size\(([^,]+),\s*([^\)]+)\)/.exec(source);if(!size||![48,96].includes(+size[1])||+size[1]!==+size[2])throw Error('Unexpected source size');
 const frames=calls(source,'_PathFrames').map(body=>{
  const opacity=/opacities:\s*<double>\[([\s\S]*?)\]/.exec(body)[1].split(',').map(x=>x.trim()).filter(Boolean).map(Number);
  const commands=[];const re=/_Path(MoveTo|LineTo|CubicTo|Close)\(/g;let m;
  while((m=re.exec(body))){const [args,end]=balanced(body,m.index+m[0].length-1);re.lastIndex=end;commands.push({kind:m[1],groups:[...args.matchAll(/<Offset>\[([\s\S]*?)\]/g)].map(x=>points(x[1]))});}
  if(!commands.length)throw Error('Empty path');return {opacity,commands};
 });
 if(!frames.length)throw Error('Missing frames '+name);
 for(const dark of [false,true]){
  const color=dark?'#9ACBEA':'#176CA4';let groups='';
  for(const p of [0,.5,1]){
   let paths='';
   for(const frame of frames){
    const d=frame.commands.map(c=>c.kind==='Close'?'Z':(c.kind==='MoveTo'?'M':c.kind==='LineTo'?'L':'C')+c.groups.map(g=>vector(g,p)).join(' ')).join(' ');
    paths+='<path d="'+d+'" fill="'+color+'" fill-opacity="'+number(lerp(frame.opacity,p))+'"/>';
   }
   groups+='<g transform="translate('+p*96+' 0) scale('+48/Number(size[1])+')">'+paths+'</g>';
  }
  result[name+(dark?'_dark':'')+'.svg']='<svg xmlns="http://www.w3.org/2000/svg" width="96" height="32" viewBox="0 0 144 48">'+groups+'</svg>\n';
 }
}
result['LICENSE-Flutter.txt']=fs.readFileSync(path.join(sdk,'LICENSE'),'utf8');
process.stdout.write(JSON.stringify(result));
