import fs from 'node:fs';
fs.mkdirSync('www',{recursive:true});
fs.copyFileSync('index.html','www/index.html');
fs.copyFileSync('node_modules/hls.js/dist/hls.min.js','www/hls.min.js');
// Publicly listed Ukrainian streams. Bundle at build time to avoid CORS on playlist fetching.
const sources=[
 'https://iptv-org.github.io/iptv/countries/ua.m3u',
 'https://iptv-org.github.io/iptv/languages/ukr.m3u',
 'https://raw.githubusercontent.com/iptv-org/iptv/master/streams/ua.m3u',
 'https://iptv.org.ua/iptv/ua.m3u'
];
const entries=[],seen=new Set();
for(const source of sources){
 try{
  const response=await fetch(source,{signal:AbortSignal.timeout(15000),headers:{'User-Agent':'Online-TV/1.0'}});
  if(!response.ok)throw Error('HTTP '+response.status);
  const data=await response.text();
  if(!data.includes('#EXTINF:'))throw Error('No M3U channels');
  let info='';
  let added=0;
  for(const raw of data.split(/\r?\n/)){
   const line=raw.trim();
   if(line.startsWith('#EXTINF:')){info=line;continue;}
   if(info&&line.startsWith('https://')){
    const key=info+'|'+line;
    if(!seen.has(key)){entries.push(info,line);seen.add(key);added++;}
    info='';
   }else if(line&&!line.startsWith('#'))info='';
  }
  console.log('IPTV source:',source,'HTTPS entries:',added);
 }catch(error){console.warn('Playlist unavailable:',source,error.message);}
}
// Include HTTPS streams from the Free-TV/IPTV Ukraine list used by the Android app.
try{
 const response=await fetch('https://raw.githubusercontent.com/Free-TV/IPTV/master/lists/ukraine.md',{signal:AbortSignal.timeout(15000)});
 if(!response.ok)throw Error('HTTP '+response.status);
 const markdown=await response.text();let added=0;
 for(const line of markdown.split(/\r?\n/)){
  if(!/^\|\s*\d+\s*\|/.test(line))continue;
  const cells=line.split('|');
  const name=(cells[2]||'').trim().replace(/\s*Ⓢ/g,'');
  const match=(cells[3]||'').match(/\[[^\]]*\]\((https:\/\/[^)]+\.m3u8(?:\?[^)]*)?)\)/);
  if(!name||!match)continue;
  const key=name+'|'+match[1];
  if(!seen.has(key)){entries.push('#EXTINF:-1,'+name,match[1]);seen.add(key);added++;}
 }
 console.log('Free-TV/IPTV HTTPS entries:',added);
}catch(error){console.warn('Free-TV/IPTV unavailable:',error.message);}
fs.writeFileSync('www/ua.m3u','#EXTM3U\n'+entries.join('\n')+'\n');
console.log('Bundled HTTPS streams:',entries.length/2);
