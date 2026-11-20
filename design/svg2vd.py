"""Convert the Glimmerleaf kit SVGs to Android VectorDrawables.
Usage (from the repo root): python3 design/svg2vd.py design/kit/svg app/src/main/res"""
import re, sys, os, math

kit, res = sys.argv[1], sys.argv[2]
out = os.path.join(res, "drawable")

HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated from design/kit/svg/{src}. -->\n'


def _rdp(pts, eps):
    if len(pts) < 3:
        return pts
    (x1, y1), (x2, y2) = pts[0], pts[-1]
    dx, dy = x2 - x1, y2 - y1
    n = math.hypot(dx, dy) or 1e-9
    best, idx = -1.0, 0
    for i in range(1, len(pts) - 1):
        x, y = pts[i]
        d = abs(dy * x - dx * y + x2 * y1 - y2 * x1) / n if (dx or dy) else math.hypot(x - x1, y - y1)
        if d > best:
            best, idx = d, i
    if best <= eps:
        return [pts[0], pts[-1]]
    return _rdp(pts[:idx + 1], eps)[:-1] + _rdp(pts[idx:], eps)


def simplify(d, eps=0.9):
    """Traced outlines are polylines ("M x,y x,y ... Z"). Reduce points so the path fits Android's
    32,767-character resource string limit; each ring stays closed so even-odd holes survive."""
    if re.search(r'[^MZ\d.,\s-]', d):
        return d  # has curves or other commands: leave untouched
    out = []
    for ring in re.findall(r'M([^MZ]*)Z?', d):
        nums = [float(v) for v in re.findall(r'-?\d*\.?\d+', ring)]
        pts = list(zip(nums[0::2], nums[1::2]))
        if len(pts) < 3:
            continue
        # split the closed ring at its farthest point so RDP has two open chains
        far = max(range(len(pts)), key=lambda i: math.hypot(pts[i][0] - pts[0][0], pts[i][1] - pts[0][1]))
        a = _rdp(pts[:far + 1], eps)
        b = _rdp(pts[far:] + [pts[0]], eps)
        ring_pts = a[:-1] + b[:-1]
        area = abs(sum(x1 * y2 - x2 * y1 for (x1, y1), (x2, y2) in zip(ring_pts, ring_pts[1:] + ring_pts[:1]))) / 2
        if len(ring_pts) < 3 or area < 60:
            continue  # tracing slivers along the image edge
        out.append("M" + " ".join(f"{x:g},{y:g}" for x, y in ring_pts) + "Z")
    return "".join(out)


def paths(svg):
    return re.findall(r'<path ([^>]*?)/?>', svg)


def attr(a, name):
    m = re.search(name + r'="([^"]*)"', a)
    return m.group(1) if m else None


def stroke_vd(src, name, vb):
    svg = open(os.path.join(kit, src)).read()
    body = ""
    for a in paths(svg):
        body += (f'    <path android:pathData="{attr(a, "d")}"\n'
                 f'        android:strokeColor="#FF000000" android:strokeWidth="2"\n'
                 f'        android:strokeLineCap="round" android:strokeLineJoin="round" />\n')
    xml = (HEAD.format(src=src) +
           f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           f'    android:width="{vb}dp" android:height="{vb}dp"\n'
           f'    android:viewportWidth="{vb}" android:viewportHeight="{vb}">\n{body}</vector>\n')
    open(os.path.join(out, name + ".xml"), "w").write(xml)


def fill_vd(src, name, size, color_override=None):
    svg = open(os.path.join(kit, src)).read()
    g = re.search(r'<g transform="translate\(([\d.]+) ([\d.]+)\) scale\(([\d.]+)\)">', svg)
    body = ""
    pts = []
    for a in paths(svg):
        d = simplify(attr(a, "d"))
        col = color_override or attr(a, "fill")
        rule = attr(a, "fill-rule")
        body += (f'        <path android:fillColor="{col}"' +
                 (' android:fillType="evenOdd"' if rule == "evenodd" else "") +
                 f'\n            android:pathData="{d}" />\n')
        nums = [float(x) for x in re.findall(r'-?\d*\.?\d+', d)]
        pts += list(zip(nums[0::2], nums[1::2]))
    if g:
        tx, ty, s = map(float, g.groups())
        grp = f'    <group android:translateX="{tx:g}" android:translateY="{ty:g}" android:scaleX="{s:g}" android:scaleY="{s:g}">\n'
        body = grp + body + "    </group>\n"
        # report how far the artwork reaches from the centre of the 108 canvas (safe zone radius 33)
        far = max(math.hypot(tx + x * s - 54, ty + y * s - 54) for x, y in pts)
        xs = [tx + x * s for x, _ in pts]; ys = [ty + y * s for _, y in pts]
        print(f"{name}: bbox x {min(xs):.1f}-{max(xs):.1f} y {min(ys):.1f}-{max(ys):.1f}, max radius {far:.1f}")
    else:
        body = body.replace("        <path", "    <path").replace("\n            android", "\n        android")
    xml = (HEAD.format(src=src) +
           f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           f'    android:width="{size}dp" android:height="{size}dp"\n'
           f'    android:viewportWidth="{size}" android:viewportHeight="{size}">\n{body}</vector>\n')
    open(os.path.join(out, name + ".xml"), "w").write(xml)


for f in sorted(os.listdir(os.path.join(kit, "icons"))):
    stroke_vd("icons/" + f, "ic_" + f[:-4].replace("-", "_"), 24)
for f in sorted(os.listdir(os.path.join(kit, "illustrations"))):
    stroke_vd("illustrations/" + f, "ill_" + f[:-4].replace("-", "_"), 72)

fill_vd("adaptive/foreground.svg", "ic_launcher_foreground", 108)
fill_vd("adaptive/monochrome.svg", "ic_launcher_monochrome", 108)
fill_vd("brand/notification.svg", "ic_notification", 24)
print("done")

# Small brand mark for the top bar: the symbol on its brown rounded square.
svg = open(os.path.join(kit, "brand/symbol-light.svg")).read()
body = ('    <path android:fillColor="#321209" android:pathData="M124,0H723A124,124 0 0 1 847,124V699A124,124 0 0 1 723,823'
        'H124A124,124 0 0 1 0,699V124A124,124 0 0 1 124,0Z" />\n')
for a in paths(svg):
    body += f'    <path android:fillColor="{attr(a, "fill")}" android:fillType="evenOdd"\n        android:pathData="{simplify(attr(a, "d"))}" />\n'
open(os.path.join(out, "brand_mark.xml"), "w").write(
    HEAD.format(src="brand/symbol-light.svg") +
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:width="32dp" android:height="31dp"\n'
    '    android:viewportWidth="847" android:viewportHeight="823">\n' + body + '</vector>\n')
