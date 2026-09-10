"""Read-only RTCM3 envelope/CRC and MSM epoch audit; not a GNSS observation decoder."""
import argparse
import collections
import hashlib
import json
import math
from pathlib import Path

TABLE = []
for value in range(256):
    crc = value << 16
    for _ in range(8):
        crc = (crc << 1) ^ (0x1864CFB if crc & 0x800000 else 0)
    TABLE.append(crc & 0xFFFFFF)


def crc24q(data):
    crc = 0
    for b in data:
        crc = ((crc << 8) & 0xFFFFFF) ^ TABLE[(crc >> 16) ^ b]
    return crc


def bits(data, start, width, signed=False):
    value = (int.from_bytes(data, 'big') >> (len(data) * 8 - start - width)) & ((1 << width) - 1)
    return value - (1 << width) if signed and value & (1 << (width - 1)) else value


def audit(data):
    types = collections.Counter(); epochs = collections.defaultdict(set)
    positions = set(); stations = set(); ends = []; i = bad = skipped = 0
    while i + 6 <= len(data):
        if data[i] != 0xD3 or data[i + 1] & 0xFC:
            i += 1; skipped += 1; continue
        length = ((data[i + 1] & 3) << 8) | data[i + 2]
        end = i + length + 6
        if end > len(data):
            break
        frame = data[i:end]
        if crc24q(frame[:-3]) != int.from_bytes(frame[-3:], 'big'):
            bad += 1; i += 1; skipped += 1; continue
        p = frame[3:-3]; i = end; ends.append(end)
        if len(p) < 2: continue
        t = bits(p, 0, 12); types[t] += 1
        if t in (1005, 1006) and len(p) >= 19:
            positions.add(tuple(round(bits(p, k, 38, True) * 0.0001, 4) for k in (34, 74, 114)))
        if 1071 <= t <= 1127 and len(p) >= 7:
            stations.add(bits(p, 12, 12)); epochs[t].add(bits(p, 24, 30))
    epoch_summary = {}
    for t, values in epochs.items():
        ordered = sorted(values)
        delta = collections.Counter(b - a for a, b in zip(ordered, ordered[1:]))
        epoch_summary[t] = dict(count=len(values), first_ms=ordered[0], last_ms=ordered[-1],
                                intervals_ms=dict(delta))
    return dict(bytes=len(data), sha256=hashlib.sha256(data).hexdigest(), frames=sum(types.values()),
                crc_failures=bad, skipped_bytes=skipped, trailing_bytes=len(data)-i,
                message_types=dict(types), msm_epochs=epoch_summary,
                station_ids=sorted(stations), ecef=sorted(positions)), ends


def dechunk(data):
    """Validate and remove the captured hex-length CRLF transport envelope."""
    i = 0; chunks = []
    while i < len(data):
        j = data.find(b'\r\n', i)
        if j < 0: raise ValueError(f'missing chunk length at {i}')
        length = int(data[i:j], 16); start = j + 2; end = start + length
        if data[end:end + 2] != b'\r\n': raise ValueError(f'bad chunk boundary at {end}')
        chunks.append(data[start:end]); i = end + 2
    return b''.join(chunks)


def main():
    parser = argparse.ArgumentParser(); parser.add_argument('root', type=Path)
    parser.add_argument('output', type=Path); args = parser.parse_args()
    raw = sorted(args.root.glob('raw/**/*.?*binRTCM3'))
    nav = sorted(args.root.glob('nav/**/*.rnx'))
    summary = dict(raw_files=len(raw), raw_bytes=sum(p.stat().st_size for p in raw),
                   nav_files=len(nav), nav_bytes=sum(p.stat().st_size for p in nav),
                   stations=len({p.name.split('.')[0] for p in raw}),
                   raw_days=sorted({p.parts[-3] for p in raw}),
                   nav_days=[p.name for p in nav], sample_files=[])
    for path in sorted((args.root/'raw/2026/249/13').glob('*binRTCM3')):
        raw_data = path.read_bytes()
        result, _ = audit(raw_data); result['path'] = str(path.relative_to(args.root))
        try:
            payload = dechunk(raw_data)
            result['transport'] = 'hex length CRLF chunks'
            result['payload_bytes'] = len(payload)
            result['payload_audit'], _ = audit(payload)
        except ValueError as e:
            result['transport_error'] = str(e)
        summary['sample_files'].append(result)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(summary, ensure_ascii=False, indent=2))
    print(json.dumps({k:v for k,v in summary.items() if k not in ('nav_days','sample_files')},indent=2))
    print('audited_files',len(summary['sample_files']))
    print('bad_crc',sum(x['crc_failures'] for x in summary['sample_files']))


if __name__ == '__main__': main()
