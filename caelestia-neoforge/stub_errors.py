import os

files_to_stub = [
    "src/main/java/xaero/pac/common/server/player/config/api/v2/PlayerConfigOptions.java",
    "src/main/java/xaero/pac/common/server/player/config/change/PlayerConfigCommonChangeHandlers.java",
    "src/main/java/xaero/pac/common/server/player/data/api/ServerPlayerDataAPI.java",
    "src/main/java/xaero/pac/common/claims/player/mode/api/ClaimingModes.java",
    "src/main/java/xaero/pac/common/server/player/permission/api/UsedPermissionNodes.java",
    "src/main/java/xaero/pac/common/server/player/config/api/PlayerConfigOptions.java",
    "src/main/java/xaero/pac/client/player/config/api/IPlayerConfigClientStorageAPI.java",
    "src/main/java/xaero/pac/common/server/claims/protection/api/IChunkProtectionAPI.java",
    "src/main/java/xaero/pac/common/player/config/group/api/PlayerConfigGroupActionError.java",
    "src/main/java/xaero/pac/client/claims/ClientClaimsManager.java",
    "src/main/java/xaero/pac/client/gui/ConfigMenu.java",
    "src/main/java/xaero/pac/client/gui/group/CreatePlayerGroupScreen.java",
    "src/main/java/xaero/pac/client/gui/group/IncludeElementScreen.java",
    "src/main/java/xaero/pac/client/gui/group/IncludeGroupScreen.java",
    "src/main/java/xaero/pac/client/gui/group/IncludePlayerScreen.java",
    "src/main/java/xaero/pac/client/gui/group/PlayerGroupsScreen.java"
]

for filepath in files_to_stub:
    if os.path.exists(filepath):
        # Extract package and class name
        with open(filepath, 'r') as f:
            lines = f.readlines()
        
        pkg = ""
        for line in lines:
            if line.startswith("package "):
                pkg = line.strip()
                break
        
        classname = os.path.basename(filepath).replace(".java", "")
        
        # Write stub
        with open(filepath, 'w') as f:
            f.write(f"{pkg}\n")
            f.write(f"public interface {classname} {{}}\n")
            
        print(f"Stubbed {filepath}")
