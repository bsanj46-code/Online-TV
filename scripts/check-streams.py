"""Check an actual segment, not just the HLS master response."""
import concurrent.futures,csv,json,subprocess,tempfile,urllib.request,urllib.parse
from pathlib import Path
page=Path('index.html').read_text()
channels=json.loads(page.split('const urls=',1)[1].split(';const playlistUrl=',1)[0])
def check(item):
 name,url=item
 try:
  current=url
  for depth in range(4):
   with urllib.request.urlopen(urllib.request.Request(current,headers={'User-Agent':'Mozilla/5.0','Origin':'https://bsanj46-code.github.io'}),timeout=15) as response:
    data=response.read(12*1024*1024+1)
    if len(data)>12*1024*1024:raise ValueError('segment too large')
   if not data.startswith(b'#EXTM3U'):break
   lines=[x.strip() for x in data.decode().splitlines() if x.strip() and not x.startswith('#')]
   if not lines:raise ValueError('empty playlist')
   current=urllib.parse.urljoin(current,lines[0])
  if data.startswith(b'#EXTM3U'):raise ValueError('no media segment')
  with tempfile.NamedTemporaryFile(suffix='.ts') as f:
   f.write(data);f.flush()
   probe=subprocess.run(['ffprobe','-v','error','-show_entries','stream=codec_name,codec_type','-of','json',f.name],capture_output=True,text=True,timeout=10)
  streams=json.loads(probe.stdout or '{}').get('streams',[])
  video=any(s.get('codec_type')=='video' for s in streams)
  return [name,'MEDIA_DETECTED' if video else 'NO_VIDEO',url,json.dumps(streams)]
 except Exception as exc:return [name,'UNAVAILABLE',url,str(exc)[:180]]
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:rows=list(executor.map(check,channels.items()))
with open('iptv-source-report.csv','w',newline='',encoding='utf-8') as f:
 writer=csv.writer(f);writer.writerow(['channel','result','url','details']);writer.writerows(rows)
for row in rows:print(row[0],row[1],row[3])
passed=sum(r[1]=='MEDIA_DETECTED' for r in rows)
print(f'Media detected: {passed}/{len(rows)}. Server probes do not prove Android/browser playback.')
if not passed:raise SystemExit('No channel media detected')
