import urllib.request, json, os, zipfile, io

print("Fetching Modrinth versions...")
url = 'https://api.modrinth.com/v2/project/open-parties-and-claims/version'
req = urllib.request.Request(url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())
    
target_url = None
for v in data:
    if '1.21.1' in v['game_versions'] and 'neoforge' in v['loaders']:
        target_url = v['files'][0]['url']
        break

if not target_url:
    print("Could not find OPAC for 1.21.1")
    exit(1)

print(f"Downloading {target_url}...")
jar_data = urllib.request.urlopen(target_url).read()

print("Extracting API classes...")
os.makedirs('libs', exist_ok=True)
with zipfile.ZipFile(io.BytesIO(jar_data)) as zin:
    with zipfile.ZipFile('libs/opac-api.jar', 'w') as zout:
        for item in zin.infolist():
            if item.filename.startswith('xaero/pac/') and not item.filename.endswith('/Mod.class'):
                # We include everything except the Mod entry points and meta-inf
                if 'META-INF/' not in item.filename and 'OpenPartiesAndClaims' not in item.filename:
                    zout.writestr(item, zin.read(item.filename))

print("Created libs/opac-api.jar")
