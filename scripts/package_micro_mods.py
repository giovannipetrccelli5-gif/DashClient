#!/usr/bin/env python3
"""Build 100 independent Fabric mini-mod JARs, then bundle by Minecraft version.
Each micro-mod requires DashClient 2.0 and Fabric API, is client-only, and adds
one originally authored HUD readout. Do not install all four styles for one stat.
"""
from pathlib import Path
import io,json,sys,zipfile

mc=sys.argv[1]
root=Path("dist")
root.mkdir(exist_ok=True)
base=Path("build/libs/dashclient-2.0.0.jar")
assert base.is_file(),base
output=root/f"DashClient-MicroMods-mc{mc}.zip"
styles=("Compact","Frost","Midnight","Neon")
stats=("FPS","Coordinates","Ping","Facing Direction","Player Health",
       "Movement Speed","World Time","RAM Usage","Clock","Session Time",
       "Rotation","Dimension","Click Speed","Right Clicks","Keystrokes",
       "Sprint","Food","Armor","Experience","Chunk","Biome","Held Item",
       "Durability","Totems","Waypoint")
with zipfile.ZipFile(base) as original,zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as bundle:
    bundle.writestr("README.txt",f"100 original DashClient HUD mini-mods for Minecraft {mc}.\n"
     "Extract this archive, then install only desired individual .jar addons alongside DashClient 2.0 and Fabric API.\n"
     "All addons require the same Minecraft version as DashClient.\n"
     "Do not install all 100 or multiple styles of the same metric: overlays may overlap or reduce FPS.\n"
     "Sodium and Lithium are third-party mods; download them separately from their original maintainers.\n")
    for n in range(1,101):
        cls=f"DashMicro{n:03d}"
        item=f"com/dashclient/micro/{cls}.class"
        assert item in original.namelist(),f"Missing compiled class: {item}"
        metric=stats[(n-1)//4]
        style=styles[(n-1)%4]
        data={
          "schemaVersion":1,
          "id":f"dashmicro{n:03d}",
          "version":"1.0.0",
          "name":f"Dash {metric} {style}",
          "description":"Optional original DashClient HUD addon with " + metric + " in " + style + " style.",
          "environment":"client",
          "entrypoints":{"client":[f"com.dashclient.micro.{cls}"]},
          "depends":{"fabricloader":">=0.18.0","fabric-api":"*","minecraft":mc,"dashclient":">=2.0.0"}
        }
        jar=io.BytesIO()
        with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as mod:
            mod.writestr("fabric.mod.json",json.dumps(data))
            mod.writestr(item,original.read(item))
        bundle.writestr(f"{cls}-mc{mc}.jar",jar.getvalue())
with zipfile.ZipFile(output) as bundle:
    names=[n for n in bundle.namelist() if n.endswith('.jar')]
    assert len(names)==100,len(names)
    for n in names:
        with zipfile.ZipFile(io.BytesIO(bundle.read(n))) as z:
            assert z.testzip() is None
            assert json.loads(z.read("fabric.mod.json"))["depends"]["minecraft"]==mc
print("Compiled, bundled and checked 100 optional mini mods for",mc)
