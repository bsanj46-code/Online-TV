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
fs.writeFileSync('www/ua.m3u','#EXTM3U\n'+entries.join('\n')+'\n');
console.log('Bundled HTTPS streams:',entries.length/2);
