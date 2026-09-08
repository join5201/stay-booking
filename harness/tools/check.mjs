// 상태: 실행하지 말 것. tmp/document-review/check.mjs의 사본이며 H3의 입력이다.
//
// 깨진 곳 둘.
// 1. 경로가 2026-09-07 개편 이전 것이다. root 바로 아래에서 o2o-service-types.md와
//    o2o-space-stay-types.md를 찾는데 지금은 document/ 아래에 있다. harness-prompts/도
//    지금은 harness/prompts/다. 그대로 돌리면 ENOENT로 죽는다.
// 2. 더 나쁜 것은 죽지 않고 도는 경우다. 이 스크립트는 검사만 하지 않고 위 두 파일을
//    fs.writeFileSync로 덮어쓴다. 그 둘은 건드리지 말 것 범위의 원본이다.
//
// H3에서 fill, g1, g2 세 명령으로 다시 쓰면서 이 두 가지를 고친다.
// 검사는 읽기만 한다. 고치는 것은 사람이나 생성자가 한다.
//
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root='C:/Dev/potenup/99_projects/o2o';
const refNames=['o2o-service-types.md','o2o-space-stay-types.md'];
for(const name of refNames){
 const p=path.join(root,name);
 let s=fs.readFileSync(p,'utf8');
 s=s.replace(/공간\u00b7숙박/g,'공간과 숙박').replace(/\u00b7/g,' / ').replace(/\u2014/g,':');
 fs.writeFileSync(p,s,'utf8');
}
const files=[...fs.readdirSync(root).filter(n=>n.endsWith('.md')), ...fs.readdirSync(path.join(root,'harness-prompts')).filter(n=>n.endsWith('.md')).map(n=>'harness-prompts/'+n)];
const errors=[];
let localLinks=0;
for(const file of files){
 const s=fs.readFileSync(path.join(root,file),'utf8');
 if(/[\u2014\u00b7]/u.test(s))errors.push(file+': forbidden punctuation');
 if(/\]\(claude\//.test(s))errors.push(file+': stale link');
 for(const m of s.matchAll(/\]\((C:\/[^)]+)\)/g)){
  const target=m[1].replace(/#.*$/,'').replace(/:\d+$/,'');
  if(!fs.existsSync(target))errors.push(file+': missing '+target);
  localLinks++;
 }
 let fenced=false;
 for(const line of s.split(/\r?\n/))if(/^```/.test(line))fenced=!fenced;
 if(fenced)errors.push(file+': unclosed code fence');
}
const api=fs.readFileSync(path.join(root,'11-o2o-api-spec.md'),'utf8');
const candidate=fs.readFileSync(path.join(root,'tmp/api-spec-v2/11-o2o-api-spec.candidate.md'),'utf8');
if(api!==candidate)errors.push('API v2 changed unexpectedly');
const apiIds=new Set([...api.matchAll(/API ID: `([^`]+)`/g)].map(m=>m[1]));
const testIds=new Set([...api.matchAll(/<a id="(t\d+)"/g)].map(m=>m[1].toUpperCase()));
for(const name of ['02-o2o-feature-list.md','10-6-o2o-harness-implementation-plan.md']){
 const s=fs.readFileSync(path.join(root,name),'utf8');
 for(const m of s.matchAll(/\b(?:CAT|INV|RATE|PROMO|SEARCH|BOOK|PAY|INTERNAL)-\d+\b/g))if(!apiIds.has(m[0]))errors.push(name+': unknown API '+m[0]);
 for(const m of s.matchAll(/\bT\d{2}\b/g))if(!testIds.has(m[0]))errors.push(name+': unknown test '+m[0]);
}
const result={documents:files.length,localLinks,apiEndpoints:apiIds.size,contractScenarios:testIds.size,apiUnchanged:api===candidate,errors};
fs.writeFileSync(path.join(root,'tmp/document-review/check-result.json'),JSON.stringify(result,null,2));
console.log(JSON.stringify(result,null,2));
if(errors.length)process.exitCode=1;
