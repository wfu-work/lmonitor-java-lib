"""Reproduce LMonitor report checks with user-supplied data and license paths."""
from pathlib import Path
import argparse,subprocess,json,hashlib,re,sys,math
parser=argparse.ArgumentParser()
parser.add_argument('--data',type=Path,required=True)
parser.add_argument('--license',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--classpath',type=Path,default=Path('target/report-work/classpath.txt'))
args=parser.parse_args()
R=Path(__file__).resolve().parents[2];T=R/'target/report-work';S=R
E=args.output.resolve();DATA=args.data.resolve()
T.mkdir(parents=True,exist_ok=True);E.mkdir(parents=True,exist_ok=True)
if any(E.iterdir()):raise SystemExit('Output directory must be empty to preserve prior evidence')
cp=str(S/'target/classes')+':'+args.classpath.read_text().strip()
for name in ['JavaContractProbe','GnssReportProbe']:
 subprocess.run(['javac','-proc:none','-cp',cp,'-d',str(T),str(R/f'scripts/report-tests/{name}.java')],check=True)
with (E/'updated-java-contract.log').open('w') as f:subprocess.run(['java','-cp',str(T)+':'+cp,'JavaContractProbe',str(E/'updated-java-contract.json')],stdout=f,stderr=f,check=True)
from rtcm_audit import audit,dechunk
fixtures=[];paths={}
for hour in [12,13]:
 for station in ['PSYCMM0010','PSYCMM0009','PSYCMM129']:
  src=DATA/f'raw/2026/249/{hour:02}/{station}.2026249binRTCM3';raw=src.read_bytes();data=dechunk(raw);stats,_=audit(data)
  dest=T/f'{station}-{hour}.rtcm3';dest.write_bytes(data);paths[(station,hour)]=str(dest)
  fixtures.append(dict(source=str(src),source_sha256=hashlib.sha256(raw).hexdigest(),source_bytes=len(raw),hour=hour,station=station,**stats))
(E/'updated-fixtures.json').write_text(json.dumps(fixtures,indent=2))
def cfg(name,h=13,base='PSYCMM129',end=None):return dict(id=name,rover=paths[('PSYCMM0010',h)],base=paths[(base,h)],nav=str(DATA/'nav/2026/BRDM2490.rnx'),start=f'2026/09/06 {h:02}:00:00',end=end or f'2026/09/06 {h+1:02}:00:00',mode=3,outMode=1)
runs=[]
def run(name,cs,n=1,rounds=1):
 f=T/(name+'-configs.json');f.write_text(json.dumps(cs));log=E/(name+'.log');out=E/(name+'.json')
 with log.open('w') as stream:
  try:p=subprocess.run(['java','-Xmx2g','-cp',str(T)+':'+cp,'GnssReportProbe',str(f),str(args.license),str(out),str(n),str(rounds),'false'],stdout=stream,stderr=stream,timeout=100);code=p.returncode
  except subprocess.TimeoutExpired:code='timeout'
 text=log.read_text();log.write_text(re.sub(r'[0-9A-Fa-f]{8}(?:-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}','[UUID REDACTED]',text))
 runs.append(dict(name=name,exit_code=code,concurrency=n,rounds=rounds));(E/'updated-runs.json').write_text(json.dumps(runs,indent=2));print(runs[-1],flush=True)
 if out.exists():
  j=json.loads(out.read_text());print([(s['id'],s['round'],s['callbackCount'],None if not s['result'] else (s['result']['solStatus'],s['result']['fixedRate'],s['result']['errMsg']),s['exception']) for s in j['samples']],flush=True)
run('updated-baselines',[cfg('short-13',base='PSYCMM0009'),cfg('long-12',12),cfg('long-13')])
run('updated-repeatability',[cfg('long-13')],rounds=4)
run('updated-windows',[cfg('window-1',end='2026/09/06 13:01:00'),cfg('window-15',end='2026/09/06 13:15:00'),cfg('window-30',end='2026/09/06 13:30:00')])
run('updated-concurrent-2',[cfg('short-13',base='PSYCMM0009'),cfg('long-12',12),cfg('long-13')],n=2,rounds=2)

meta=[];paths=[]
for station in ['PSYCMM0010','PSYCMM129']:
 payload=bytearray();sources=[]
 for f in sorted((DATA/'raw/2026/249').glob('*/'+station+'.*')):
  raw=f.read_bytes();payload.extend(dechunk(raw));sources.append(dict(path=str(f),bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest()))
  if len(payload)>=2500000:break
 stats,ends=audit(payload);end=max(n for n in ends if n<=2500000);b=payload[:end];stats,_=audit(b);dest=T/(station+'-2500000.rtcm3');dest.write_bytes(b);paths.append(str(dest));meta.append(dict(station=station,sources=sources,**stats))
(E/'updated-payload-fixtures.json').write_text(json.dumps(meta,indent=2));c=[dict(id='payload-5MB-long',rover=paths[0],base=paths[1],nav=str(DATA/'nav/2026/BRDM2490.rnx'),start='2026/09/06 00:00:00',end='2026/09/06 12:00:00',mode=3,outMode=1)]
f=T/'updated-payload-config.json';f.write_text(json.dumps(c));cp=str(T)+':'+str(R/'target/classes')+':'+args.classpath.read_text().strip()
with (E/'updated-payload.log').open('w') as stream:
 try:p=subprocess.run(['java','-Xmx2g','-cp',cp,'GnssReportProbe',str(f),str(args.license),str(E/'updated-payload.json'),'1','3','false'],stdout=stream,stderr=stream,timeout=150);code=p.returncode
 except subprocess.TimeoutExpired:code='timeout'
f=E/'updated-payload.log';f.write_text(re.sub(r'[0-9A-Fa-f]{8}(?:-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}','[UUID REDACTED]',f.read_text()));(E/'updated-payload-run.json').write_text(json.dumps(dict(exit_code=code,rounds=3,concurrency=1)));print('payload',code,flush=True)
