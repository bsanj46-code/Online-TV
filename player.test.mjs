import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
class Element {constructor(){this.children=[];this.value='';this.paused=true;this.classList={add(){},remove(){},contains(){return false}}}append(...a){this.children.push(...a)}replaceChildren(){this.children=[]}setAttribute(){}removeAttribute(){}addEventListener(){}pause(){this.paused=true}load(){}play(){this.paused=false;return Promise.resolve()}canPlayType(){return true}}
class Hls {static Events={MANIFEST_PARSED:'parsed',ERROR:'error'};static ErrorTypes={MEDIA_ERROR:'media'};static instances=[];static isSupported(){return true}constructor(){this.handlers={};Hls.instances.push(this)}on(e,cb){this.handlers[e]=cb}loadSource(u){this.url=u}attachMedia(){}destroy(){this.destroyed=true}recoverMediaError(){}}
const elements=new Map(), get=id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id)};
const context={document:{getElementById:get,createElement:()=>new Element(),addEventListener(){},body:new Element()},window:{Hls,addEventListener(){}},Hls,localStorage:{getItem:()=>null,setItem(){}},setTimeout:()=>1,clearTimeout(){},fetch:async()=>{throw Error('offline')},AbortSignal:{timeout(){}},console,screen:{}};
const html=readFileSync('index.html','utf8'),script=html.match(/<script>([\s\S]*?)<\/script>/)[1];vm.createContext(context);vm.runInContext(script,context);
assert.equal(get('list').children.length,48,'all existing channels stay visible');
const click=n=>{const b=get('list').children.map(r=>r.children[0]).find(x=>x.textContent===n);assert.ok(b,n+' visible');b.onclick()};
click('2+2');const old=Hls.instances.at(-1);assert.match(old.url,/lovable.app\/api\/public\/tv-stream\?channel=2plus2/);
click('Мега');const latest=Hls.instances.at(-1);assert.ok(old.destroyed);assert.match(latest.url,/mega-abr/);
const count=Hls.instances.length;old.handlers.error(null,{fatal:true});assert.equal(Hls.instances.length,count,'old channel errors cannot switch the new channel');
latest.handlers.error(null,{fatal:true});assert.equal(get('list').children.length,48,'failed channels remain available for retry');
get('q').value='Discovery';get('q').oninput();assert.equal(get('list').children.length,1,'search filters the catalog');
console.log('Player checks passed: complete catalog, shared HTTPS, switching cleanup, stale errors, retry, search.');
