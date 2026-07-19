import os
import shutil

src_dir = 'open-parties-and-claims/Common/src/main/java/xaero'
dest_dir = 'src/main/java/xaero'

# Files that are not strictly in an 'api' folder but are required by the API
required_files = [
    'IOpenPACMinecraftServer.java',
    'IServerDataAPI.java',
    'OpenPACServerAPI.java',
    'OpenPACClientAPI.java',
    'IOpenPACMinecraftClient.java',
    'IClientDataAPI.java'
]

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if '/api/' in root.replace('\\', '/') or '/api' in root.replace('\\', '/') or file in required_files:
            src_file = os.path.join(root, file)
            # Calculate relative path
            rel_path = os.path.relpath(src_file, src_dir)
            dest_file = os.path.join(dest_dir, rel_path)
            
            os.makedirs(os.path.dirname(dest_file), exist_ok=True)
            shutil.copy2(src_file, dest_file)
            print(f"Copied {rel_path}")
