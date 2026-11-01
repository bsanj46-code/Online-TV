import fs from 'node:fs';
fs.mkdirSync('www',{recursive:true});
fs.copyFileSync('index.html','www/index.html');
fs.copyFileSync('node_modules/hls.js/dist/hls.min.js','www/hls.min.js');
// Download the public channel index at build time to avoid browser-side CORS.
const sources=['https://raw.githubusercontent.com/iptv-org/iptv/master/streams/ua.m3u','https://iptv.org.ua/iptv/ua.m3u','https://iptv.org.ua/playlist-iptv'];
for(const source of sources){
  try{
    const response=await fetch(source,{signal:AbortSignal.timeout(12000),headers:{'User-Agent':'Online-TV/1.0'}});
    if(!response.ok)throw new Error('HTTP '+response.status);
    const data=await response.text();
    if(!data.includes('#EXTINF:'))throw new Error('No M3U channels');
    fs.writeFileSync('www/ua.m3u',data);
    console.log('Bundled IPTV playlist:',source);
    break;
  }catch(error){console.warn('Playlist unavailable:',source,error.message);}
}
